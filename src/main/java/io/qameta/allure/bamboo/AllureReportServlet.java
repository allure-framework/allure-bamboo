package io.qameta.allure.bamboo;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.sonatype.aether.util.StringUtils.isEmpty;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AllureReportServlet extends HttpServlet {
    private static final Pattern URL_PATTERN = Pattern.compile(".*/plugins/servlet/allure/report/([^/]{2,})/([^/]+)/?(.*)/");
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureReportServlet.class);
    private final AllureArtifactsManager artifactsManager;
    private final ResultsSummaryManager resultsSummaryManager;

    @Inject
    public AllureReportServlet(AllureArtifactsManager artifactsManager,
                               ResultsSummaryManager resultsSummaryManager) {
        this.artifactsManager = artifactsManager;
        this.resultsSummaryManager = resultsSummaryManager;
    }

    public static Pattern getUrlPattern() {
        return URL_PATTERN;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        getArtifactUrl(request, response).ifPresent(file -> {
            try (InputStream inputStream = new URL(file).openStream()) {
                setResponseHeaders(response, file);
                IOUtils.copy(inputStream, response.getOutputStream());
            } catch (IOException e) {
                LOGGER.error("Failed to send file {} of Allure Report ", file);
            }
        });
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        getArtifactUrl(request, response).ifPresent(file -> {
            try (InputStream inputStream = new URL(file).openStream()) {
                setResponseHeaders(response, file);
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                LOGGER.error("Failed to send file {} of Allure Report ", file);
            }
        });
    }

    private void setResponseHeaders(HttpServletResponse response, String fileUrl) throws IOException {

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            final URI file = new URL(fileUrl).toURI();
            final String mimeType = ofNullable(getServletContext().getMimeType(fileUrl)).orElse(
                    Files.probeContentType(Paths.get(file.getPath()))
            );
            final String charsetPostfix = of("application", "text").anyMatch(mimeType::contains) ? ";charset=utf-8" : "";
            response.setHeader("Content-Type", mimeType + charsetPostfix);
            response.setHeader("Content-Disposition", "inline; filename=\"" + Paths.get(file.getPath()).getFileName().toString() + "\"");

        } catch (URISyntaxException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    private Optional<String> getArtifactUrl(HttpServletRequest request, HttpServletResponse response) {
        final Matcher matcher = URL_PATTERN.matcher(request.getRequestURI());
        if (matcher.matches()) {
            response.setHeader("X-Frame-Options", "ALLOWALL");
            final String planKey = matcher.group(1);
            final String buildNumber = matcher.group(2);
            final String filePath = matcher.group(3);
            if (wasUploadSuccess(response, planKey, parseInt(buildNumber))) {
                return artifactsManager.getArtifactUrl(planKey, buildNumber, filePath);
            }
        } else {
            LOGGER.info("Path {} does not match pattern", request.getRequestURI());
        }
        return Optional.empty();
    }

    private boolean wasUploadSuccess(HttpServletResponse response, String planKey, int buildNumber) {
        final PlanResultKey planResultKey = getPlanResultKey(planKey, buildNumber);
        ResultsSummary results = resultsSummaryManager.getResultsSummary(planResultKey);
        if (results != null) {
            AllureBuildResult uploadResult = fromCustomData(results.getCustomBuildData());
            if (!uploadResult.isSuccess()) {
                uploadResultWasNotSuccess(response, uploadResult);
                return false;
            }
            return true;
        }
        return false;
    }


    private void uploadResultWasNotSuccess(HttpServletResponse response, AllureBuildResult uploadResult) {
        final String errorMessage = isEmpty(uploadResult.getFailureDetails()) ?
                "Unknown error has occurred during Allure Build. Please refer the server logs for details." :
                "Something went wrong with Allure Report generation. Here are some details: \n" + uploadResult.getFailureDetails();
        try {
            response.setHeader("Content-Type", "text/plain");
            response.setHeader("Content-Length", String.valueOf(errorMessage.length()));
            response.setHeader("Content-Disposition", "inline");
            response.getWriter().write(errorMessage);
        } catch (IOException e) {
            LOGGER.error("Failed to render error of Allure Report build ", e);
        }
    }
}
