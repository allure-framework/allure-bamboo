/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static java.lang.Integer.parseInt;

@UnrestrictedAccess
public class AllureReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Pattern URL_PATTERN = Pattern
            .compile(".*/plugins/servlet/allure/report/([^/]{2,})/([^/]+)/?(.*)");
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureReportServlet.class);
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FAILED_TO_SEND_FILE_OF_ALLURE_REPORT = "Failed to send file {} of Allure Report ";
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";

    private final transient AllureArtifactsManager artifactsManager;
    private final ResultsSummaryManager resultsSummaryManager;

    public AllureReportServlet(final AllureArtifactsManager artifactsManager,
                               final ResultsSummaryManager resultsSummaryManager) {
        this.artifactsManager = artifactsManager;
        this.resultsSummaryManager = resultsSummaryManager;
    }

    static Pattern getUrlPattern() {
        return URL_PATTERN;
    }

    private boolean isValidUrl(final String urlString) {
        try {

            final URL url = URI.create(urlString).toURL();
            final String protocol = url.getProtocol().toLowerCase();
            final String host = url.getHost().toLowerCase();

            final String baseHost = artifactsManager.getBaseHost();

            return List.of("file", "http", "https").contains(protocol)
                    && List.of("", "localhost", baseHost).contains(host);
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
                setResponseHeaders(response, file);
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
                setResponseHeaders(response, file);
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                LOGGER.error(FAILED_TO_SEND_FILE_OF_ALLURE_REPORT, file);
            }
        });
    }

    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    private void setResponseHeaders(final HttpServletResponse response,
                                    final String fileUrl) throws IOException {

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

        } catch (MalformedURLException e) {
            // should never happen
            throw new AllurePluginException("Unexpected error", e);
        }
    }

    private Optional<String> getArtifactUrl(final HttpServletRequest request,
                                            final HttpServletResponse response) {
        final Matcher matcher = URL_PATTERN.matcher(request.getRequestURI());
        try {
            if (matcher.matches()) {
                if (StringUtils.isBlank(response.getHeader(X_FRAME_OPTIONS))) {
                    response.setHeader(X_FRAME_OPTIONS, "ALLOWALL");
                }
                final String planKey = matcher.group(1);
                final String buildNumber = matcher.group(2);
                final String filePath = matcher.group(3);
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
