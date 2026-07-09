/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.bamboo;

import com.atlassian.annotations.security.UnrestrictedAccess;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.security.BambooPermissionManager;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanKey;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static java.lang.Integer.parseInt;

@UnrestrictedAccess
public class AllureReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Pattern URL_PATTERN = Pattern
            .compile(".*/plugins/servlet/allure/report/([^/]{2,})/([^/]+)/?(.*)");
    // Absolute path (leading slash/backslash or Windows drive) or a parent-directory ".." segment.
    private static final Pattern UNSAFE_PATH = Pattern
            .compile("^[/\\\\]|^[a-zA-Z]:|(^|[/\\\\])\\.\\.([/\\\\]|$)");
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureReportServlet.class);
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FAILED_TO_SEND_FILE_OF_ALLURE_REPORT = "Failed to send file {} of Allure Report ";
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String PROTOCOL_FILE = "file";
    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String NOSNIFF = "nosniff";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String INDEX_HTML = "index.html";
    private static final String SAMEORIGIN = "SAMEORIGIN";
    // Only the Bamboo UI (same origin) may frame report responses; blocks external clickjacking.
    private static final String FRAME_ANCESTORS_CSP = "frame-ancestors 'self'";
    private static final String SANDBOX_CSP = "sandbox allow-scripts; base-uri 'none'; form-action 'none'; object-src 'none'; frame-ancestors 'self'";

    private final transient AllureArtifactsManager artifactsManager;
    private final transient ResultsSummaryManager resultsSummaryManager;
    private final transient BambooPermissionManager permissionManager;
    private final transient LoginUriProvider loginUriProvider;
    private final transient UserManager userManager;

    public AllureReportServlet(final AllureArtifactsManager artifactsManager,
                               final ResultsSummaryManager resultsSummaryManager,
                               final BambooPermissionManager permissionManager,
                               final LoginUriProvider loginUriProvider,
                               final UserManager userManager) {
        this.artifactsManager = artifactsManager;
        this.resultsSummaryManager = resultsSummaryManager;
        this.permissionManager = permissionManager;
        this.loginUriProvider = loginUriProvider;
        this.userManager = userManager;
    }

    static Pattern getUrlPattern() {
        return URL_PATTERN;
    }

    private boolean isValidUrl(final String urlString) {
        try {

            final URL url = URI.create(urlString).toURL();
            final String protocol = url.getProtocol().toLowerCase();
            final String host = url.getHost().toLowerCase();

            // Local report files have no host; their path is contained to the report dir when the URL is built.
            if (PROTOCOL_FILE.equals(protocol)) {
                return host.isEmpty();
            }
            // Remote artifacts are only ever served from the Bamboo host itself.
            final String baseHost = artifactsManager.getBaseHost();
            return (PROTOCOL_HTTP.equals(protocol) || PROTOCOL_HTTPS.equals(protocol)) && host.equals(baseHost);
        } catch (Exception e) {
            LOGGER.error("Invalid URL: {}", urlString, e);
            return false;
        }
    }

    @Override
    public void doGet(final HttpServletRequest request,
                      final HttpServletResponse response) {
        getArtifactUrl(request, response).ifPresent(file -> {
            if (!isValidUrl(file)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            try (InputStream inputStream = URI.create(file).toURL().openStream()) {
                setResponseHeaders(response, file, reportRelativePath(request));
                IOUtils.copy(inputStream, response.getOutputStream());
            } catch (IOException e) {
                LOGGER.error(FAILED_TO_SEND_FILE_OF_ALLURE_REPORT, file);
            }
        });
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    @Override
    public void doHead(final HttpServletRequest request,
                       final HttpServletResponse response) {
        getArtifactUrl(request, response).ifPresent(file -> {
            if (!isValidUrl(file)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            try (InputStream inputStream = URI.create(file).toURL().openStream()) {
                setResponseHeaders(response, file, reportRelativePath(request));
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                LOGGER.error(FAILED_TO_SEND_FILE_OF_ALLURE_REPORT, file);
            }
        });
    }

    private void setResponseHeaders(final HttpServletResponse response,
                                    final String fileUrl,
                                    final String reportRelativePath)
            throws IOException {

        try {

            response.setStatus(HttpServletResponse.SC_OK);
            final URI file = URI.create(fileUrl);
            final Path filePath = Path.of(file.getPath());
            final String fileName = filePath.getFileName().toString();
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new AllurePluginException("Invalid file name: " + fileName);
            }

            final String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            final String mimeType = Optional.ofNullable(getServletContext().getMimeType(fileUrl))
                    .orElse(Files.probeContentType(filePath));
            final String charsetPostfix = Stream.of("application", "text")
                    .anyMatch(mimeType::contains) ? ";charset=utf-8" : "";
            response.setHeader(CONTENT_TYPE, mimeType + charsetPostfix);
            response.setHeader(CONTENT_DISPOSITION, "inline; filename=\"" + sanitizedFileName + "\"");
            response.setHeader(X_CONTENT_TYPE_OPTIONS, NOSNIFF);
            response.setHeader(
                    CONTENT_SECURITY_POLICY,
                    isSandboxedDocument(fileName, reportRelativePath) ? SANDBOX_CSP : FRAME_ANCESTORS_CSP
            );

        } catch (MalformedURLException e) {
            // should never happen
            throw new AllurePluginException("Unexpected error", e);
        }
    }

    /**
     * Non-entrypoint active documents (HTML/SVG attachments) are served with a sandbox CSP so their
     * scripts cannot run with the Bamboo origin. Only the report root {@code index.html} stays relaxed
     * so the report itself keeps working; every other active document — including a nested
     * {@code index.html} such as an attachment served from {@code data/attachments/index.html} — is
     * sandboxed. The served file name is always the basename, so the report-relative request path is
     * needed to tell the root entrypoint from a nested one.
     */
    private boolean isSandboxedDocument(final String servedFileName,
                                        final String reportRelativePath) {
        final String name = servedFileName.toLowerCase(Locale.ROOT);
        final boolean activeDocument = name.endsWith(".html") || name.endsWith(".htm")
                || name.endsWith(".xhtml") || name.endsWith(".svg");
        return activeDocument && !(INDEX_HTML.equals(name) && isRootEntrypoint(reportRelativePath));
    }

    /**
     * Whether the report-relative request path resolves to the report root {@code index.html} — an
     * empty path (root directory request) or {@code index.html} itself. Any subdirectory is not.
     */
    private static boolean isRootEntrypoint(final String reportRelativePath) {
        final String rel = (reportRelativePath == null ? "" : reportRelativePath)
                .replaceFirst("^/+", "").replaceFirst("/+$", "").toLowerCase(Locale.ROOT);
        return rel.isEmpty() || INDEX_HTML.equals(rel);
    }

    private static String reportRelativePath(final HttpServletRequest request) {
        final Matcher matcher = URL_PATTERN.matcher(request.getRequestURI());
        return matcher.matches() ? matcher.group(3) : "";
    }

    private Optional<String> getArtifactUrl(final HttpServletRequest request,
                                            final HttpServletResponse response) {
        final Matcher matcher = URL_PATTERN.matcher(request.getRequestURI());
        try {
            if (matcher.matches()) {
                if (StringUtils.isBlank(response.getHeader(X_FRAME_OPTIONS))) {
                    response.setHeader(X_FRAME_OPTIONS, SAMEORIGIN);
                }
                final String planKey = matcher.group(1);
                final String buildNumber = matcher.group(2);
                final String filePath = matcher.group(3);
                if (isRejectedPath(filePath)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return Optional.empty();
                }
                if (!hasReadPermission(planKey)) {
                    denyAccess(request, response);
                    return Optional.empty();
                }
                if (wasUploadSuccess(response, planKey, parseInt(buildNumber))) {
                    return artifactsManager.getArtifactUrl(planKey, buildNumber, filePath);
                }
            } else {
                LOGGER.info("Path {} does not match pattern", request.getRequestURI());
            }
        } catch (Exception e) {
            LOGGER.error("There is a problem to parse request uri: {}", request.getRequestURI(), e);
        }
        return Optional.empty();
    }

    private boolean hasReadPermission(final String planKey) {
        return permissionManager.hasPlanPermission(BambooPermission.READ, getPlanKey(planKey));
    }

    /**
     * Rejects report paths that are absolute or traverse out of the report, checking both the raw
     * and the percent-decoded form so this holds even if a future Bamboo starts decoding the URI.
     * Malformed percent-encoding is also rejected.
     */
    private boolean isRejectedPath(final String rawPath) {
        final String decoded;
        try {
            decoded = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rejecting malformed URL-encoded report path: {}", rawPath);
            return true;
        }
        return UNSAFE_PATH.matcher(rawPath).find() || UNSAFE_PATH.matcher(decoded).find();
    }

    private void denyAccess(final HttpServletRequest request,
                            final HttpServletResponse response) {
        // A logged-in user who lacks permission gets 403; an anonymous user is sent to log in first.
        if (StringUtils.isNotBlank(userManager.getRemoteUsername())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            response.sendRedirect(loginUriProvider.getLoginUri(currentUri(request)).toASCIIString());
        } catch (IOException e) {
            LOGGER.error("Failed to redirect anonymous user to login for {}", request.getRequestURI(), e);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private URI currentUri(final HttpServletRequest request) {
        final StringBuffer url = request.getRequestURL();
        final String query = request.getQueryString();
        if (StringUtils.isNotBlank(query)) {
            url.append('?').append(query);
        }
        return URI.create(url.toString());
    }

    private boolean wasUploadSuccess(final HttpServletResponse response,
                                     final String planKey,
                                     final int buildNumber) {
        final PlanResultKey planResultKey = getPlanResultKey(planKey, buildNumber);
        final ResultsSummary results = resultsSummaryManager.getResultsSummary(planResultKey);
        if (results != null) {
            final AllureBuildResult uploadResult = fromCustomData(results.getCustomBuildData());
            if (!uploadResult.isSuccess()) {
                uploadResultWasNotSuccess(response, uploadResult);
                return false;
            }
            return true;
        }
        return false;
    }

    private void uploadResultWasNotSuccess(final HttpServletResponse response,
                                           final AllureBuildResult uploadResult) {
        final String errorMessage = StringUtils.isEmpty(uploadResult.getFailureDetails())
                ? "Unknown error has occurred during Allure Build. Please refer the server logs for details."
                : "Something went wrong with Allure Report generation. Here are some details: \n"
                        + uploadResult.getFailureDetails();
        try {
            response.setHeader(CONTENT_TYPE, "text/plain");
            response.setHeader("Content-Length", String.valueOf(errorMessage.length()));
            response.setHeader(CONTENT_DISPOSITION, "inline");
            response.getWriter().write(errorMessage);
        } catch (IOException e) {
            LOGGER.error("Failed to render error of Allure Report build ", e);
        }
    }
}
