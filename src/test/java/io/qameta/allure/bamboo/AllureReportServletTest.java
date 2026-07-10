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

import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.security.BambooPermissionManager;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.AllureReportServlet.getUrlPattern;
import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureReportServletTest {

    private static final String PLAN_KEY = "STPCI-STPITCONFLUENCE60";
    private static final int BUILD_NUMBER = 15;
    private static final String SANDBOX_CSP = "sandbox allow-scripts; base-uri 'none'; form-action 'none'; object-src 'none'; frame-ancestors 'self'";

    @Rule
    public MockitoRule mockitoRule = rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private AllureArtifactsManager artifactsManager;
    @Mock
    private ResultsSummaryManager resultsSummaryManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ServletContext servletContext;
    @Mock
    private BambooPermissionManager permissionManager;
    @Mock
    private LoginUriProvider loginUriProvider;
    @Mock
    private UserManager userManager;

    private ByteArrayOutputStream outputStream;
    private StringWriter responseWriter;
    private AllureReportServlet servlet;

    @Before
    public void setUp() throws Exception {
        servlet = new AllureReportServlet(
                artifactsManager, resultsSummaryManager, permissionManager,
                loginUriProvider, userManager
        ) {
            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }
        };
        outputStream = new ByteArrayOutputStream();
        responseWriter = new StringWriter();
        when(response.getOutputStream()).thenReturn(TestSupport.servletOutputStream(outputStream));
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(response.getHeader("X-Frame-Options")).thenReturn(null);
        when(artifactsManager.getBaseHost()).thenReturn("bamboo.example");
        when(servletContext.getMimeType(anyString())).thenReturn("text/html");
        when(permissionManager.hasPlanPermission(any(BambooPermission.class), any(PlanKey.class))).thenReturn(true);
    }

    @Test
    public void itShouldMatchThePattern() {
        final Matcher matcher = getUrlPattern()
                .matcher("/plugins/servlet/allure/report/" + PLAN_KEY + "/" + BUILD_NUMBER + "/");
        assertThat(matcher.matches()).isTrue();
    }

    @Test
    public void itShouldStreamReportFilesForValidLocalUrls() throws Exception {
        final java.nio.file.Path reportFile = temporaryFolder.newFile("index.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(reportFile, "<html>ok</html>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("index.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "index.html"))
                .thenReturn(Optional.of(reportFile.toUri().toURL().toString()));

        servlet.doGet(request, response);

        assertThat(outputStream.toString(StandardCharsets.UTF_8)).contains("<html>ok</html>");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("Content-Type", "text/html;charset=utf-8");
        verify(response).setHeader("Content-Disposition", "inline; filename=\"index.html\"");
    }

    @Test
    public void itShouldSandboxNonEntrypointHtmlAttachments() throws Exception {
        final java.nio.file.Path attachment = temporaryFolder.newFile("xss-attachment.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(attachment, "<script>alert(1)</script>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("xss-attachment.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "xss-attachment.html"))
                .thenReturn(Optional.of(attachment.toUri().toURL().toString()));

        servlet.doGet(request, response);

        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("Content-Security-Policy", SANDBOX_CSP);
    }

    @Test
    public void itShouldNotSandboxReportEntrypoints() throws Exception {
        final java.nio.file.Path entrypoint = temporaryFolder.newFile("index.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(entrypoint, "<html>report</html>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("index.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "index.html"))
                .thenReturn(Optional.of(entrypoint.toUri().toURL().toString()));

        servlet.doGet(request, response);

        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("Content-Security-Policy", "frame-ancestors 'self'");
    }

    @Test
    public void itShouldSandboxSubdirectoryIndexHtml() throws Exception {
        final java.nio.file.Path attachment = temporaryFolder.newFile("index.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(attachment, "<script>alert(1)</script>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("data/attachments/index.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "data/attachments/index.html"))
                .thenReturn(Optional.of(attachment.toUri().toURL().toString()));

        servlet.doGet(request, response);

        verify(response).setHeader("Content-Security-Policy", SANDBOX_CSP);
    }

    @Test
    public void itShouldSandboxIndexHtmlServedForNestedDirectoryRequest() throws Exception {
        final java.nio.file.Path attachment = temporaryFolder.newFile("index.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(attachment, "<script>alert(1)</script>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("data/attachments"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "data/attachments"))
                .thenReturn(Optional.of(attachment.toUri().toURL().toString()));

        servlet.doGet(request, response);

        verify(response).setHeader("Content-Security-Policy", SANDBOX_CSP);
    }

    @Test
    public void itShouldSandboxSingleDirectoryIndexHtmlSinceOnlyRootIsAnEntrypoint() throws Exception {
        final java.nio.file.Path entrypoint = temporaryFolder.newFile("index.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(entrypoint, "<html>report</html>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("awesome/index.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "awesome/index.html"))
                .thenReturn(Optional.of(entrypoint.toUri().toURL().toString()));

        servlet.doGet(request, response);

        verify(response).setHeader("Content-Security-Policy", SANDBOX_CSP);
    }

    @Test
    public void itShouldSetHeadersForHeadRequests() throws Exception {
        final java.nio.file.Path reportFile = temporaryFolder.newFile("report.html").toPath();
        final ResultsSummary resultsSummary = successfulResultSummary();
        Files.writeString(reportFile, "<html>ok</html>", StandardCharsets.UTF_8);
        when(request.getRequestURI()).thenReturn(reportUri("report.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "report.html"))
                .thenReturn(Optional.of(reportFile.toUri().toURL().toString()));

        servlet.doHead(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setHeader("Content-Disposition", "inline; filename=\"report.html\"");
    }

    @Test
    public void itShouldRejectUrlsOutsideTheAllowedHostList() {
        final ResultsSummary resultsSummary = successfulResultSummary();
        when(request.getRequestURI()).thenReturn(reportUri("index.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);
        when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "index.html"))
                .thenReturn(Optional.of("https://evil.example/index.html"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void itShouldRejectEncodedTraversalPathsWithBadRequest() {
        when(request.getRequestURI()).thenReturn(reportUri("..%2f..%2fbamboo.cfg.xml"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(artifactsManager, never()).getArtifactUrl(anyString(), anyString(), anyString());
    }

    @Test
    public void itShouldRejectMalformedUrlEncodingWithBadRequest() {
        when(request.getRequestURI()).thenReturn(reportUri("bad%2gpath.html"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(artifactsManager, never()).getArtifactUrl(anyString(), anyString(), anyString());
    }

    @Test
    public void itShouldReturnForbiddenForAuthenticatedUsersWithoutPermission() {
        when(request.getRequestURI()).thenReturn(reportUri("index.html"));
        when(permissionManager.hasPlanPermission(any(BambooPermission.class), any(PlanKey.class)))
                .thenReturn(false);
        when(userManager.getRemoteUsername()).thenReturn("bob");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(artifactsManager, never()).getArtifactUrl(anyString(), anyString(), anyString());
        verifyNoInteractions(resultsSummaryManager);
    }

    @Test
    public void itShouldRedirectAnonymousUsersToLogin() throws Exception {
        final String loginUri = "https://bamboo.example/userlogin!default.action";
        when(request.getRequestURI()).thenReturn(reportUri("index.html"));
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://bamboo.example" + reportUri("index.html")));
        when(permissionManager.hasPlanPermission(any(BambooPermission.class), any(PlanKey.class)))
                .thenReturn(false);
        when(userManager.getRemoteUsername()).thenReturn(null);
        when(loginUriProvider.getLoginUri(any(URI.class))).thenReturn(URI.create(loginUri));

        servlet.doGet(request, response);

        verify(response).sendRedirect(loginUri);
        verify(artifactsManager, never()).getArtifactUrl(anyString(), anyString(), anyString());
    }

    @Test
    public void itShouldRenderFailureDetailsWhenUploadDidNotSucceed() throws Exception {
        final ResultsSummary resultsSummary = failedResultSummary("Detailed failure");
        when(request.getRequestURI()).thenReturn(reportUri("index.html"));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                .thenReturn(resultsSummary);

        servlet.doGet(request, response);

        assertThat(responseWriter.toString()).contains("Detailed failure");
        verify(response).setHeader("Content-Type", "text/plain");
        verify(artifactsManager, never()).getArtifactUrl(anyString(), anyString(), anyString());
    }

    @Test
    public void itShouldReturnNotFoundForMissingArtifactsOnHeadRequests() throws Exception {
        final java.nio.file.Path missingFile = temporaryFolder.getRoot().toPath().resolve("missing.html");
        final ResultsSummary resultsSummary = successfulResultSummary();
        step("prepare a successful build summary whose requested artifact is missing on disk", () -> {
            when(request.getRequestURI()).thenReturn(reportUri("missing.html"));
            when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, BUILD_NUMBER)))
                    .thenReturn(resultsSummary);
            when(artifactsManager.getArtifactUrl(PLAN_KEY, Integer.toString(BUILD_NUMBER), "missing.html"))
                    .thenReturn(Optional.of(missingFile.toUri().toURL().toString()));
            attachText("Missing artifact URL", missingFile.toUri().toString());
        });

        step("issue a HEAD request for the missing report artifact", () -> servlet.doHead(request, response));

        step("verify the servlet returns not found for the missing artifact", () -> verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND));
    }

    private String reportUri(final String fileName) {
        return "/plugins/servlet/allure/report/" + PLAN_KEY + "/" + BUILD_NUMBER + "/" + fileName;
    }

    private ResultsSummary successfulResultSummary() {
        final ResultsSummary resultsSummary = org.mockito.Mockito.mock(ResultsSummary.class);
        final Map<String, String> customData = new HashMap<>();
        allureBuildResult(true, null).withHandlerClass("handler").dumpToCustomData(customData);
        doReturn(customData).when(resultsSummary).getCustomBuildData();
        return resultsSummary;
    }

    private ResultsSummary failedResultSummary(final String details) {
        final ResultsSummary resultsSummary = org.mockito.Mockito.mock(ResultsSummary.class);
        final Map<String, String> customData = new HashMap<>();
        allureBuildResult(false, details).dumpToCustomData(customData);
        doReturn(customData).when(resultsSummary).getCustomBuildData();
        return resultsSummary;
    }
}
