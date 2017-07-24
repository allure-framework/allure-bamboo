package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static org.sonatype.aether.util.StringUtils.isEmpty;

public class AllureReportServlet extends HttpServlet {
    private static final Pattern URL_PATTERN = Pattern.compile(".*/plugins/servlet/allure/report/([^/]{2,})/([^/]+)/?(.*)");
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final Matcher matcher = URL_PATTERN.matcher(request.getRequestURI());
        if (matcher.matches()) {
            response.setHeader("X-Frame-Options", "ALLOWALL");
            final String planKeyString = matcher.group(1);
            final String buildNumber = matcher.group(2);
            final String filePath = matcher.group(3);
            final PlanResultKey planResultKey = getPlanResultKey(planKeyString, parseInt(buildNumber));
            ofNullable(resultsSummaryManager.getResultsSummary(planResultKey)).ifPresent(results -> {
                final AllureBuildResult uploadResult = fromCustomData(results.getCustomBuildData());
                if (uploadResult.isSuccess()) {
                    artifactsManager.getArtifactUrl(planKeyString, buildNumber, filePath).ifPresent(file -> {
                        try (final InputStream input = new URL(file).openStream()) {
                            final Path path = Paths.get(file);
                            response.setHeader("Content-Type", getServletContext().getMimeType(filePath));
                            response.setHeader("Content-Disposition", "inline; filename=\"" + path.getFileName().toString() + "\"");
                            IOUtils.copy(input, response.getOutputStream());
                        } catch (IOException e) {
                            LOGGER.error("Failed to send file {} of Allure Report ", file);
                        }
                    });
                } else {
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
            });
        } else {
            LOGGER.info("Path {} does not match pattern", request.getRequestURI());
        }
    }
}
