package io.qameta.allure.bamboo.compat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.Driver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Starts Bamboo in Docker, loads the plugin from Bamboo home, publishes a smoke plan, and verifies report rendering.
 */
public final class CompatibilitySmokeRunner {

    private static final int BAMBOO_HTTP_PORT = 8085;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration BAMBOO_READY_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration REPORT_READY_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration HTTP_RETRY_DELAY = Duration.ofSeconds(2);
    private static final int HTTP_SEND_ATTEMPTS = 5;
    private static final String BAMBOO_ADMIN_USERNAME = "admin";
    private static final String BAMBOO_ADMIN_PASSWORD = "admin";
    private static final String ALLURE_DOWNLOAD_BASE_URL
            = "https://github.com/allure-framework/allure2/releases/download/";
    private static final String BAMBOO_HOME_PATH = "/var/atlassian/application-data/bamboo";
    private static final String BAMBOO_AGENT_HOME_PATH = "/var/atlassian/application-data/bamboo-agent";
    private static final String BAMBOO_INSTALL_LIB_PATH = "/opt/atlassian/bamboo/lib";
    private static final String BAMBOO_NETWORK_ALIAS = "bamboo";
    private static final String STAGED_PLUGIN_JAR_PATH = "/tmp/allure-bamboo-compat-plugin.jar";
    private static final String GENERAL_SETUP_FORM_ID = "setupGeneralConfiguration";
    private static final String LICENSE_SETUP_FORM_ID = "validateLicense";
    private static final String ALLURE_CONFIG_FORM_ID = "saveAllureReportConfigForm";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private HttpClient client = buildClient();
    private final Config config = Config.fromSystemProperties();
    private final Path diagnosticsRoot = config.artifactRoot();
    private final Path responsesDir = diagnosticsRoot.resolve("responses");
    private final Path logsDir = diagnosticsRoot.resolve("logs");
    private final Path downloadsDir = diagnosticsRoot.resolve("downloads");
    private final Path bambooHomeDir = diagnosticsRoot.resolve("bamboo-home");
    private final Path agentHomeDir = diagnosticsRoot.resolve("agent-home");
    private String currentBaseUrl;
    private Integer currentBuildNumber;

    private CompatibilitySmokeRunner() {
        // main entry point only
    }

    public static void main(final String[] args) throws Exception {
        new CompatibilitySmokeRunner().run();
    }

    private void run() throws Exception {
        prepareDirectories();
        final Path pluginJar = resolvePluginJar();
        final Path h2Jar = resolveH2Jar();

        try (Network network = Network.newNetwork();
             GenericContainer<?> bamboo = createContainer(network, pluginJar, h2Jar)) {
            GenericContainer<?> agent = null;
            try {
                bamboo.start();
                currentBaseUrl = "http://" + bamboo.getHost() + ":" + bamboo.getMappedPort(BAMBOO_HTTP_PORT);
                waitForBamboo(currentBaseUrl);
                installPlugin(bamboo, pluginJar);
                verifyPluginPage(currentBaseUrl);
                configurePlugin(currentBaseUrl);
                agent = createAgentContainer(network, resolveContainerHostname(bamboo), resolveContainerIpAddress(bamboo));
                agent.start();
                waitForRemoteAgent(currentBaseUrl, agent, resolveAgentIdentity(agent));
                SmokePlanSpecs.publish(currentBaseUrl, BAMBOO_ADMIN_USERNAME, BAMBOO_ADMIN_PASSWORD);
                final int firstBuildNumber = queueAndWaitForBuild(currentBaseUrl);
                verifyReportEndpoints(currentBaseUrl, firstBuildNumber);
                final int secondBuildNumber = queueAndWaitForBuild(currentBaseUrl);
                verifyReportEndpoints(currentBaseUrl, secondBuildNumber);
                verifyHistoryEndpoints(currentBaseUrl, secondBuildNumber);
                currentBuildNumber = queueAndWaitForBuild(currentBaseUrl);
                verifyReportEndpoints(currentBaseUrl, currentBuildNumber);
                verifyHistoryEndpoints(currentBaseUrl, currentBuildNumber);
                verifyCleanupBehavior(currentBaseUrl, firstBuildNumber, secondBuildNumber, currentBuildNumber);
                writeSummary(currentBaseUrl, firstBuildNumber, secondBuildNumber, currentBuildNumber);
            } finally {
                captureDiagnostics(bamboo, agent, currentBaseUrl, currentBuildNumber);
                if (agent != null) {
                    agent.stop();
                }
            }
        }
    }

    private void prepareDirectories() throws IOException {
        deleteRecursively(responsesDir);
        deleteRecursively(logsDir);
        deleteRecursively(downloadsDir);
        deleteRecursively(bambooHomeDir);
        deleteRecursively(agentHomeDir);
        Files.deleteIfExists(diagnosticsRoot.resolve("summary.md"));
        Files.createDirectories(responsesDir);
        Files.createDirectories(logsDir);
        Files.createDirectories(downloadsDir);
        Files.createDirectories(bambooHomeDir);
        Files.createDirectories(agentHomeDir);
    }

    private Path resolvePluginJar() throws IOException {
        if (config.pluginJar() != null) {
            return config.pluginJar();
        }

        final Path targetDir = config.rootDir().resolve("target");
        try (Stream<Path> stream = Files.list(targetDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().startsWith("allure-bamboo-"))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find plugin jar in " + targetDir));
        }
    }

    private Path resolveH2Jar() {
        try {
            return Path.of(Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve the H2 JDBC driver jar from the classpath", e);
        }
    }

    private GenericContainer<?> createContainer(final Network network,
                                                final Path pluginJar,
                                                final Path h2Jar) {
        return new GenericContainer<>(DockerImageName.parse("atlassian/bamboo:" + config.bambooVersion()))
                .withNetwork(network)
                .withNetworkAliases(BAMBOO_NETWORK_ALIAS)
                .withCreateContainerCmdModifier(command -> command.withHostName(BAMBOO_NETWORK_ALIAS))
                .withExposedPorts(BAMBOO_HTTP_PORT)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(pluginJar),
                        STAGED_PLUGIN_JAR_PATH
                )
                .withCopyFileToContainer(
                        MountableFile.forHostPath(h2Jar),
                        BAMBOO_INSTALL_LIB_PATH + "/" + h2Jar.getFileName()
                )
                .withEnv("ATL_BAMBOO_ENABLE_UNATTENDED_SETUP", "true")
                .withEnv("ATL_BAMBOO_SKIP_CONFIG", "true")
                .withEnv("ATL_LICENSE", config.productLicense())
                .withEnv("ATL_BASE_URL", "http://localhost:" + BAMBOO_HTTP_PORT)
                .withEnv("ATL_ADMIN_USERNAME", BAMBOO_ADMIN_USERNAME)
                .withEnv("ATL_ADMIN_PASSWORD", BAMBOO_ADMIN_PASSWORD)
                .withEnv("ATL_ADMIN_FULLNAME", "Compatibility Admin")
                .withEnv("ATL_ADMIN_EMAIL", "compatibility@example.invalid")
                .withEnv("ATL_IMPORT_OPTION", "clean")
                .withEnv("ATL_DB_TYPE", "h2")
                .withEnv("JVM_MINIMUM_MEMORY", "1024m")
                .withEnv("JVM_MAXIMUM_MEMORY", "2048m")
                .withStartupTimeout(BAMBOO_READY_TIMEOUT)
                .withEnv("JVM_SUPPORT_RECOMMENDED_ARGS",
                        "-Dupm.plugin.upload.enabled=true -Dbamboo.websudo.disabled=true");
    }

    private GenericContainer<?> createAgentContainer(final Network network,
                                                     final String bambooHostname,
                                                     final String bambooIpAddress) {
        final GenericContainer<?> container = new GenericContainer<>(
                DockerImageName.parse("atlassian/bamboo-agent-base:" + config.bambooVersion())
        )
                .withNetwork(network)
                .withEnv("BAMBOO_SERVER", "http://" + BAMBOO_NETWORK_ALIAS + ":" + BAMBOO_HTTP_PORT + "/agentServer")
                .withEnv("WRAPPER_JAVA_INITMEMORY", "512")
                .withEnv("WRAPPER_JAVA_MAXMEMORY", "1024");
        if (bambooHostname != null && !bambooHostname.isBlank()
                && bambooIpAddress != null && !bambooIpAddress.isBlank()) {
            container.withExtraHost(bambooHostname, bambooIpAddress);
        }
        return container;
    }

    private void waitForBamboo(final String baseUrl) throws Exception {
        pollUntil(baseUrl + "/start.action", BAMBOO_READY_TIMEOUT, "Bamboo web UI",
                response -> response.statusCode() == 200 && !response.body().isBlank());

        HttpResponse<String> currentResponse = sendText(baseUrl + "/start.action");
        Files.writeString(responsesDir.resolve("root-before-ready.html"), currentResponse.body(), StandardCharsets.UTF_8);

        if (isLicenseSetupPage(currentResponse.body())) {
            try {
                currentResponse = pollUntil(baseUrl + "/start.action", Duration.ofSeconds(45),
                        "Bamboo unattended license setup",
                        response -> response.statusCode() == 200 && !isLicenseSetupPage(response.body()));
            } catch (IllegalStateException unattendedTimeout) {
                completeLicenseSetup(baseUrl, currentResponse.body());
                resetClient();
                currentResponse = pollUntil(baseUrl + "/start.action", Duration.ofMinutes(2),
                        "Bamboo license setup",
                        response -> response.statusCode() == 200 && !isLicenseSetupPage(response.body()));
            }
        }

        if (isGeneralSetupPage(currentResponse.body())) {
            completeGeneralSetup(baseUrl, currentResponse.body());
            resetClient();
        }

        final HttpResponse<String> readyRoot = pollUntil(baseUrl + "/start.action", BAMBOO_READY_TIMEOUT,
                "Bamboo setup finalisation",
                this::isBambooReadyResponse);
        Files.writeString(responsesDir.resolve("root-after-ready.html"), readyRoot.body(), StandardCharsets.UTF_8);
        resetClient();
    }

    private void installPlugin(final GenericContainer<?> bamboo,
                               final Path pluginJar) throws Exception {
        final String targetPath = BAMBOO_HOME_PATH + "/shared/plugins/" + pluginJar.getFileName();
        final String command = String.join(" && ",
                "install -d -o bamboo -g bamboo " + BAMBOO_HOME_PATH + "/shared/plugins",
                "cp " + STAGED_PLUGIN_JAR_PATH + " " + targetPath,
                "chown bamboo:bamboo " + targetPath
        );
        final var result = bamboo.execInContainer("sh", "-lc", command);
        Files.writeString(logsDir.resolve("plugin-shared-home-install.log"),
                "exitCode=" + result.getExitCode() + "\nstdout:\n" + result.getStdout()
                        + "\nstderr:\n" + result.getStderr(),
                StandardCharsets.UTF_8);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Failed to stage plugin in Bamboo shared home: " + result.getStderr());
        }
    }

    private void verifyPluginPage(final String baseUrl) throws Exception {
        pollUntil(baseUrl + "/admin/editAllureReportConfig.action", Duration.ofMinutes(5),
                "Allure admin page",
                response -> response.statusCode() == 200
                        && !isGeneralSetupPage(response.body())
                        && response.body().contains(ALLURE_CONFIG_FORM_ID));
    }

    private void configurePlugin(final String baseUrl) throws Exception {
        final Map<String, String> form = new LinkedHashMap<>();
        form.put("downloadEnabled", "true");
        form.put("customLogoEnabled", "false");
        form.put("enabledByDefault", "true");
        form.put("enabledReportsCleanup", "true");
        form.put("localStoragePath", config.bambooStoragePath());
        form.put("downloadBaseUrl", ALLURE_DOWNLOAD_BASE_URL);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/saveAllureReportConfig.action"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("X-Atlassian-Token", "no-check")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form)))
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Files.writeString(responsesDir.resolve("save-config.html"), response.body(), StandardCharsets.UTF_8);
        if (response.statusCode() < 200
                || response.statusCode() >= 400
                || isGeneralSetupPage(response.body())
                || !response.body().contains(ALLURE_CONFIG_FORM_ID)) {
            throw new IllegalStateException("Failed to configure plugin, HTTP " + response.statusCode());
        }
    }

    private void waitForRemoteAgent(final String baseUrl,
                                    final GenericContainer<?> agent,
                                    final AgentIdentity initialAgentIdentity) throws Exception {
        final Instant deadline = Instant.now().plus(config.agentTimeout());
        Exception lastException = null;
        AgentIdentity agentIdentity = initialAgentIdentity;

        while (Instant.now().isBefore(deadline)) {
            if (!agent.isRunning()) {
                throw new IllegalStateException("Remote agent container exited before registering. See agent logs.");
            }

            try {
                enableRemoteAgentSupportIfPresent(baseUrl);
                final HttpResponse<String> agentsResponse = sendText(baseUrl + "/admin/agent/viewAgents.action");
                Files.writeString(responsesDir.resolve("agents.html"), agentsResponse.body(), StandardCharsets.UTF_8);
                agentIdentity = enrichAgentIdentity(agentsResponse.body(), agentIdentity);
                approvePendingRemoteAgents(baseUrl, agentsResponse.body(), agentIdentity);

                final HttpResponse<String> onlineAgents = sendText(baseUrl + "/rest/api/latest/agent/remote?online=true");
                Files.writeString(responsesDir.resolve("remote-agents-online.json"),
                        onlineAgents.body(), StandardCharsets.UTF_8);
                if (containsRemoteAgent(onlineAgents.body(), agentIdentity)) {
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }

            Thread.sleep(5_000L);
        }

        throw new IllegalStateException("Timed out waiting for a remote Bamboo agent to come online", lastException);
    }

    private AgentIdentity resolveAgentIdentity(final GenericContainer<?> agent) throws Exception {
        final String agentName = agent.execInContainer("hostname").getStdout().trim();
        final String agentIp = resolveContainerIpAddress(agent);
        return new AgentIdentity(agentName, agentIp, null);
    }

    private String resolveContainerHostname(final GenericContainer<?> container) throws Exception {
        return container.execInContainer("hostname").getStdout().trim();
    }

    private String resolveContainerIpAddress(final GenericContainer<?> container) {
        return container.getCurrentContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .stream()
                .map(network -> network.getIpAddress())
                .filter(ipAddress -> ipAddress != null && !ipAddress.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not determine Bamboo container IP address"));
    }

    private void enableRemoteAgentSupportIfPresent(final String baseUrl) throws Exception {
        final HttpResponse<String> response = sendText(baseUrl + "/admin/agent/viewAgents.action");
        Files.writeString(responsesDir.resolve("agents.html"), response.body(), StandardCharsets.UTF_8);
        if (response.statusCode() != 200 || !response.body().contains("Enable remote agent support")) {
            return;
        }

        final FormSubmission form = extractFormWithLabel(response.body(), "Enable remote agent support")
                .orElseThrow(() -> new IllegalStateException(
                        "Remote agent support is disabled, but the enable form could not be found"));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(resolveRelativeUri(baseUrl, form.action()))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("X-Atlassian-Token", "no-check")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form.fields())))
                .build();

        final HttpResponse<String> enableResponse = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
        Files.writeString(responsesDir.resolve("enable-remote-agent-support.html"),
                enableResponse.body(), StandardCharsets.UTF_8);
        if (enableResponse.statusCode() < 200 || enableResponse.statusCode() >= 400) {
            throw new IllegalStateException("Failed to enable remote agent support, HTTP "
                    + enableResponse.statusCode());
        }
    }

    private void approvePendingRemoteAgents(final String baseUrl,
                                            final String agentsHtml,
                                            final AgentIdentity agentIdentity) throws Exception {
        approvePendingRemoteAgentAuthentication(baseUrl, agentsHtml, agentIdentity);

        final HttpResponse<String> pendingResponse = sendText(baseUrl + "/rest/api/latest/agent/authentication?pending=true");
        Files.writeString(responsesDir.resolve("remote-agents-pending.json"),
                pendingResponse.body(), StandardCharsets.UTF_8);
        if (pendingResponse.statusCode() != 200 || !isJsonDocument(pendingResponse.body())) {
            return;
        }

        for (String agentUuid : extractAgentUuids(readJson(pendingResponse.body())).toList()) {
            if (!agentIdentity.matchesUuid(agentUuid)) {
                continue;
            }
            final HttpResponse<String> approvalResponse = sendPutWithoutBody(
                    baseUrl + "/rest/api/latest/agent/authentication/" + agentUuid
            );
            if (approvalResponse.statusCode() != 204
                    && approvalResponse.statusCode() != 200
                    && approvalResponse.statusCode() != 409) {
                throw new IllegalStateException("Failed to approve remote agent " + agentUuid
                        + ", HTTP " + approvalResponse.statusCode() + " body: " + approvalResponse.body());
            }
        }
    }

    private void approvePendingRemoteAgentAuthentication(final String baseUrl,
                                                         final String html,
                                                         final AgentIdentity agentIdentity) throws Exception {
        final Optional<String> pendingAgentUuid = extractPendingAuthenticationUuid(html, agentIdentity);
        if (pendingAgentUuid.isEmpty()) {
            return;
        }

        final String action = extractFormAction(html, "remoteAgentAuthenticationConfiguration")
                .orElse("/admin/agent/configureAgentAuthentications.action");
        final Map<String, String> form = new LinkedHashMap<>();
        extractInputValue(html, "atl_token").ifPresent(token -> form.put("atl_token", token));
        form.put("remoteAgentAuthentications", pendingAgentUuid.get());
        form.put("approve", "Approve access");

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(resolveRelativeUri(baseUrl, action))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("X-Atlassian-Token", "no-check")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form)))
                .build();

        final HttpResponse<String> approvalResponse = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
        Files.writeString(responsesDir.resolve("approve-remote-agent-authentication.html"),
                approvalResponse.body(),
                StandardCharsets.UTF_8);
        if (approvalResponse.statusCode() < 200 || approvalResponse.statusCode() >= 400) {
            throw new IllegalStateException("Failed to approve remote agent authentication, HTTP "
                    + approvalResponse.statusCode());
        }
    }

    private AgentIdentity enrichAgentIdentity(final String html,
                                              final AgentIdentity currentIdentity) {
        return extractAuthenticationUuid(html, currentIdentity.agentIp())
                .map(currentIdentity::withUuid)
                .orElse(currentIdentity);
    }

    private int queueAndWaitForBuild(final String baseUrl) throws Exception {
        final String queueUrl = baseUrl + "/rest/api/latest/queue/" + SmokePlanSpecs.getPlanKey();
        final HttpResponse<String> queueResponse = pollUntil(queueUrl, Duration.ofMinutes(5),
                "build queue availability",
                response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return true;
                    }
                    if (response.statusCode() == 400 && response.body().contains("system is STARTING")) {
                        return false;
                    }
                    throw new IllegalStateException("Failed to queue build, HTTP " + response.statusCode()
                            + " body: " + response.body());
                },
                this::sendPostWithoutBody);
        Files.writeString(responsesDir.resolve("queue.json"), queueResponse.body(), StandardCharsets.UTF_8);

        final JsonNode queued = MAPPER.readTree(queueResponse.body());
        final int buildNumber = Optional.ofNullable(queued.get("buildNumber"))
                .map(JsonNode::asInt)
                .orElseThrow(() -> new IllegalStateException("Queue response did not contain buildNumber"));

        final String resultUrl = baseUrl + "/rest/api/latest/result/" + SmokePlanSpecs.getPlanKey() + "-" + buildNumber;
        pollUntil(resultUrl, config.buildTimeout(), "build completion", response -> {
            if (response.statusCode() != 200) {
                return false;
            }
            final JsonNode result = readJson(response.body());
            final String lifeCycleState = result.path("lifeCycleState").asText();
            if (!"Finished".equalsIgnoreCase(lifeCycleState)) {
                return false;
            }
            final String buildState = result.path("buildState").asText();
            if (!"Successful".equalsIgnoreCase(buildState)) {
                throw new IllegalStateException("Build finished with state " + buildState);
            }
            return true;
        });
        final HttpResponse<String> resultResponse = sendText(resultUrl);
        Files.writeString(responsesDir.resolve("build-result.json"), resultResponse.body(), StandardCharsets.UTF_8);

        return buildNumber;
    }

    private void verifyReportEndpoints(final String baseUrl, final int buildNumber) throws Exception {
        final String reportUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + buildNumber + "/index.html";
        final String zipUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + buildNumber + "/report.zip";
        final Path buildDownloadsDir = downloadsDir.resolve("build-" + buildNumber);
        Files.createDirectories(buildDownloadsDir);

        final HttpResponse<String> reportResponse = pollUntil(reportUrl, REPORT_READY_TIMEOUT,
                "Allure report", response -> response.statusCode() == 200 && response.body().contains("<html"));
        Files.writeString(buildDownloadsDir.resolve("index.html"), reportResponse.body(), StandardCharsets.UTF_8);

        final HttpResponse<byte[]> zipResponse = sendBytes(zipUrl);
        if (zipResponse.statusCode() != 200 || zipResponse.body().length == 0) {
            throw new IllegalStateException("Expected non-empty report.zip from " + zipUrl);
        }
        Files.write(buildDownloadsDir.resolve("report.zip"), zipResponse.body());
    }

    private void verifyHistoryEndpoints(final String baseUrl, final int buildNumber) throws Exception {
        final String historyJsonUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + buildNumber + "/history/history.json";
        final String trendUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + buildNumber + "/history/history-trend.json";
        final Path buildDownloadsDir = downloadsDir.resolve("build-" + buildNumber);
        Files.createDirectories(buildDownloadsDir);

        final HttpResponse<String> historyResponse = pollUntil(historyJsonUrl, REPORT_READY_TIMEOUT,
                "Allure history",
                response -> response.statusCode() == 200 && !response.body().isBlank());
        final HttpResponse<String> trendResponse = pollUntil(trendUrl, REPORT_READY_TIMEOUT,
                "Allure history trend",
                response -> response.statusCode() == 200 && !response.body().isBlank());
        Files.writeString(buildDownloadsDir.resolve("history.json"), historyResponse.body(), StandardCharsets.UTF_8);
        Files.writeString(buildDownloadsDir.resolve("history-trend.json"),
                trendResponse.body(),
                StandardCharsets.UTF_8);
    }

    private void verifyCleanupBehavior(final String baseUrl,
                                       final int removedBuildNumber,
                                       final int retainedBuildNumber,
                                       final int latestBuildNumber) throws Exception {
        final String removedReportUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + removedBuildNumber + "/index.html";
        final String retainedReportUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + retainedBuildNumber + "/index.html";
        final String latestReportUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + latestBuildNumber + "/index.html";

        final HttpResponse<String> removedResponse = sendHead(removedReportUrl);
        if (removedResponse.statusCode() != 404) {
            throw new IllegalStateException("Expected cleaned up report to return 404, got "
                    + removedResponse.statusCode() + " from " + removedReportUrl);
        }

        final HttpResponse<String> retainedResponse = sendHead(retainedReportUrl);
        if (retainedResponse.statusCode() != 200) {
            throw new IllegalStateException("Expected retained report to return 200, got "
                    + retainedResponse.statusCode() + " from " + retainedReportUrl);
        }

        final HttpResponse<String> latestResponse = sendHead(latestReportUrl);
        if (latestResponse.statusCode() != 200) {
            throw new IllegalStateException("Expected latest report to return 200, got "
                    + latestResponse.statusCode() + " from " + latestReportUrl);
        }
    }

    private void writeSummary(final String baseUrl,
                              final int firstBuildNumber,
                              final int secondBuildNumber,
                              final int thirdBuildNumber) throws IOException {
        final String latestReportUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + thirdBuildNumber + "/index.html";
        final String latestZipUrl = baseUrl + "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                + "/" + thirdBuildNumber + "/report.zip";
        Files.writeString(diagnosticsRoot.resolve("summary.md"),
                "# Bamboo compatibility smoke summary\n\n"
                        + "- Bamboo version: `" + config.bambooVersion() + "`\n"
                        + "- Plan key: `" + SmokePlanSpecs.getPlanKey() + "`\n"
                        + "- Build numbers: `" + firstBuildNumber + "`, `" + secondBuildNumber
                        + "`, `" + thirdBuildNumber + "`\n"
                        + "- Latest report URL: " + latestReportUrl + "\n"
                        + "- Latest zip URL: " + latestZipUrl + "\n"
                        + "- History verified on builds: `" + secondBuildNumber + "`, `" + thirdBuildNumber + "`\n"
                        + "- Cleanup verified: build `" + firstBuildNumber + "` removed after build `"
                        + thirdBuildNumber + "`\n",
                StandardCharsets.UTF_8);
    }

    private void captureDiagnostics(final GenericContainer<?> bamboo,
                                    final GenericContainer<?> agent,
                                    final String baseUrl,
                                    final Integer buildNumber) throws IOException {
        Files.writeString(logsDir.resolve("docker.log"), bamboo.getLogs(), StandardCharsets.UTF_8);
        copyBambooDiagnostics(bamboo);
        if (agent != null) {
            Files.writeString(logsDir.resolve("agent-docker.log"), agent.getLogs(), StandardCharsets.UTF_8);
            copyAgentDiagnostics(agent);
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            writeResponseDump(baseUrl, "/", "root.html");
            writeResponseDump(baseUrl, "/admin/editAllureReportConfig.action", "allure-admin.html");
            writeResponseDump(baseUrl, "/admin/agent/viewAgents.action", "agents.html");
            if (buildNumber != null) {
                writeResponseDump(baseUrl,
                        "/rest/api/latest/result/" + SmokePlanSpecs.getPlanKey() + "-" + buildNumber,
                        "build-result.json");
                writeResponseDump(baseUrl,
                        "/plugins/servlet/allure/report/" + SmokePlanSpecs.getPlanKey()
                                + "/" + buildNumber + "/index.html",
                        "allure-index.html");
            }
        }
    }

    private void copyBambooDiagnostics(final GenericContainer<?> bamboo) throws IOException {
        copyFromContainerIfPresent(bamboo,
                BAMBOO_HOME_PATH + "/logs/atlassian-bamboo.log",
                bambooHomeDir.resolve("logs/atlassian-bamboo.log"));
        copyFromContainerIfPresent(bamboo,
                BAMBOO_HOME_PATH + "/logs/atlassian-bamboo-access.log",
                bambooHomeDir.resolve("logs/atlassian-bamboo-access.log"));
        copyFromContainerIfPresent(bamboo,
                BAMBOO_HOME_PATH + "/bamboo.cfg.xml",
                bambooHomeDir.resolve("bamboo.cfg.xml"));
        copyFromContainerIfPresent(bamboo,
                BAMBOO_HOME_PATH + "/cluster-node.properties",
                bambooHomeDir.resolve("cluster-node.properties"));
        copyFromContainerIfPresent(bamboo,
                BAMBOO_HOME_PATH + "/unattended-setup.properties",
                bambooHomeDir.resolve("unattended-setup.properties"));
    }

    private void copyAgentDiagnostics(final GenericContainer<?> agent) throws IOException {
        copyFromContainerIfPresent(agent,
                BAMBOO_AGENT_HOME_PATH + "/atlassian-bamboo-agent.log",
                agentHomeDir.resolve("atlassian-bamboo-agent.log"));
        copyFromContainerIfPresent(agent,
                BAMBOO_AGENT_HOME_PATH + "/logs/atlassian-bamboo.log",
                agentHomeDir.resolve("logs/atlassian-bamboo.log"));
        copyFromContainerIfPresent(agent,
                BAMBOO_AGENT_HOME_PATH + "/conf/wrapper.conf",
                agentHomeDir.resolve("conf/wrapper.conf"));
        copyFromContainerIfPresent(agent,
                BAMBOO_AGENT_HOME_PATH + "/configuration/jmsclient.ts",
                agentHomeDir.resolve("configuration/jmsclient.ts"));
        copyFromContainerIfPresent(agent,
                BAMBOO_AGENT_HOME_PATH + "/installer.properties",
                agentHomeDir.resolve("installer.properties"));
        copyFromContainerIfPresent(agent,
                BAMBOO_AGENT_HOME_PATH + "/bamboo-agent.cfg.xml",
                agentHomeDir.resolve("bamboo-agent.cfg.xml"));
    }

    private void copyFromContainerIfPresent(final GenericContainer<?> container,
                                            final String containerPath,
                                            final Path hostPath) throws IOException {
        try {
            Files.createDirectories(hostPath.getParent());
            container.copyFileFromContainer(containerPath, hostPath.toString());
        } catch (Exception e) {
            Files.writeString(logsDir.resolve(hostPath.getFileName() + ".copy-error.txt"),
                    "Failed to copy " + containerPath + ": " + e,
                    StandardCharsets.UTF_8);
        }
    }

    private void writeResponseDump(final String baseUrl,
                                   final String path,
                                   final String fileName) throws IOException {
        try {
            final HttpResponse<String> response = sendText(baseUrl + path);
            Files.writeString(responsesDir.resolve(fileName), response.body(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Files.writeString(logsDir.resolve(fileName + ".error.txt"), e.toString(), StandardCharsets.UTF_8);
        }
    }

    private HttpResponse<String> pollUntil(final String url,
                                           final Duration timeout,
                                           final String description,
                                           final ResponsePredicate predicate) throws Exception {
        return pollUntil(url, timeout, description, predicate, this::sendText);
    }

    private HttpResponse<String> pollUntil(final String url,
                                           final Duration timeout,
                                           final String description,
                                           final ResponsePredicate predicate,
                                           final RequestExecutor executor) throws Exception {
        final Instant deadline = Instant.now().plus(timeout);
        Exception lastException = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                final HttpResponse<String> response = executor.execute(url);
                if (predicate.test(response)) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
            }
            Thread.sleep(10_000L);
        }
        throw new IllegalStateException("Timed out waiting for " + description + " at " + url, lastException);
    }

    private HttpResponse<String> sendText(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
                .GET()
                .build();
        return sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> sendBytes(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .GET()
                .build();
        return sendWithRetry(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<String> sendHead(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("Accept", "text/html, application/json;q=0.9, */*;q=0.8")
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        return sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPostWithoutBody(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPutWithoutBody(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth())
                .header("Accept", "application/json")
                .header("X-Atlassian-Token", "no-check")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
    }

    private <T> HttpResponse<T> sendWithRetry(final HttpRequest request,
                                              final BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= HTTP_SEND_ATTEMPTS; attempt++) {
            try {
                return client.send(request, bodyHandler);
            } catch (IOException e) {
                lastException = e;
                resetClient();
                if (attempt == HTTP_SEND_ATTEMPTS) {
                    break;
                }
                Thread.sleep(HTTP_RETRY_DELAY.toMillis());
            }
        }
        throw lastException == null
                ? new IOException("HTTP request failed without an exception cause")
                : lastException;
    }

    private void ensureSuccess(final HttpResponse<?> response, final String action) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Failed to " + action + ", HTTP " + response.statusCode());
        }
    }

    private void completeGeneralSetup(final String baseUrl, final String html) throws Exception {
        final Map<String, String> form = new LinkedHashMap<>();
        form.put("instanceName", extractInputValue(html, "instanceName").orElse("Atlassian Bamboo"));
        form.put("baseUrl", baseUrl);
        form.put("configDir", extractRequiredInputValue(html, "configDir"));
        form.put("buildDir", extractRequiredInputValue(html, "buildDir"));
        form.put("artifactsDir", extractRequiredInputValue(html, "artifactsDir"));
        form.put("repositoryLogsDir", extractRequiredInputValue(html, "repositoryLogsDir"));
        form.put("buildWorkingDir", extractRequiredInputValue(html, "buildWorkingDir"));
        form.put("brokerClientURI", normaliseBrokerClientUri(extractRequiredInputValue(html, "brokerClientURI")));
        form.put("save", "Continue");
        extractInputValue(html, "atl_token").ifPresent(token -> form.put("atl_token", token));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/setup/validateGeneralConfiguration.action"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form)))
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Files.writeString(responsesDir.resolve("setup-general-submit.html"), response.body(), StandardCharsets.UTF_8);
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new IllegalStateException("Failed to complete Bamboo general setup, HTTP "
                    + response.statusCode());
        }
    }

    private String normaliseBrokerClientUri(final String brokerClientUri) {
        final Pattern pattern = Pattern.compile("ssl://([^:?/)]+)(:\\d+)");
        final Matcher matcher = pattern.matcher(brokerClientUri);
        if (!matcher.find()) {
            return brokerClientUri;
        }
        return matcher.replaceFirst("ssl://" + BAMBOO_NETWORK_ALIAS + "$2");
    }

    private void completeLicenseSetup(final String baseUrl, final String html) throws Exception {
        IllegalStateException lastFailure = null;
        for (String candidate : licenseCandidates().toList()) {
            final HttpResponse<String> response = submitLicense(baseUrl, html, candidate);
            if (response.statusCode() >= 200
                    && response.statusCode() < 400
                    && !isLicenseSetupPage(response.body())) {
                Files.writeString(responsesDir.resolve("setup-license-submit-success.html"),
                        response.body(),
                        StandardCharsets.UTF_8);
                return;
            }

            lastFailure = new IllegalStateException("Failed to complete Bamboo license setup, HTTP "
                    + response.statusCode());
            Files.writeString(responsesDir.resolve("setup-license-submit-failed.html"),
                    response.body(),
                    StandardCharsets.UTF_8);
        }

        throw lastFailure == null
                ? new IllegalStateException("Failed to complete Bamboo license setup")
                : lastFailure;
    }

    private HttpResponse<String> submitLicense(final String baseUrl,
                                               final String html,
                                               final String licenseValue) throws Exception {
        final Map<String, String> form = new LinkedHashMap<>();
        form.put("sid", extractRequiredInputValue(html, "sid"));
        form.put("licenseString", licenseValue);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/setup/validateLicense.action?" + toFormData(form)))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Stream<String> licenseCandidates() {
        final String provided = config.productLicense();
        final Stream.Builder<String> candidates = Stream.builder();
        candidates.add(provided);

        final String whitespaceNormalised = provided
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('+', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        if (!whitespaceNormalised.equals(provided)) {
            candidates.add(whitespaceNormalised);
        }

        return candidates.build().distinct();
    }

    private boolean isLicenseSetupPage(final String html) {
        return html.contains("<title>Bamboo setup wizard - Atlassian Bamboo</title>")
                && html.contains("id=\"" + LICENSE_SETUP_FORM_ID + "\"");
    }

    private boolean isGeneralSetupPage(final String html) {
        return html.contains("<title>Configure instance - Atlassian Bamboo</title>")
                || html.contains("id=\"" + GENERAL_SETUP_FORM_ID + "\"");
    }

    private boolean isSetupPage(final String body) {
        return body.contains("<meta name=\"step\" content=\"")
                || body.contains("/scripts/setup/bambooSetup.js")
                || body.contains("Please wait while Bamboo finalises your setup")
                || body.contains("Please wait while Bamboo sets up your database")
                || isGeneralSetupPage(body);
    }

    private boolean isBambooReadyPage(final String body) {
        return !body.isBlank()
                && !isSetupPage(body)
                && !isUnknownActionPage(body)
                && !body.contains("Unable to create a tracker when osgi is not running")
                && !body.contains("There is no Action mapped for namespace [/admin] and action name [editAllureReportConfig]");
    }

    private boolean isBambooReadyResponse(final HttpResponse<String> response) {
        if (response.statusCode() != 200 || !isBambooReadyPage(response.body())) {
            return false;
        }

        final String path = response.uri().getPath();
        return !path.startsWith("/setup/")
                && !path.startsWith("/bootstrap/")
                && !"/setup/finishsetup.action".equals(path);
    }

    private boolean isHtmlDocument(final String body) {
        final String trimmed = body.stripLeading();
        return trimmed.startsWith("<!DOCTYPE html") || trimmed.startsWith("<html");
    }

    private boolean isUnknownActionPage(final String body) {
        return body.contains("<title>Page not found - Atlassian Bamboo</title>")
                || body.contains("There is no Action mapped for namespace");
    }

    private boolean isJsonDocument(final String body) {
        final String trimmed = body.stripLeading();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private boolean containsRemoteAgent(final String body, final AgentIdentity agentIdentity) {
        if (!isJsonDocument(body)) {
            return false;
        }
        return containsAgentObject(readJson(body), agentIdentity);
    }

    private boolean containsAgentObject(final JsonNode node, final AgentIdentity agentIdentity) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isObject()) {
            if (agentIdentity.matches(node.path("name").asText(null), node.path("agentUuid").asText(null))
                    || agentIdentity.matches(node.path("name").asText(null), node.path("uuid").asText(null))) {
                return true;
            }
            for (JsonNode child : node) {
                if (containsAgentObject(child, agentIdentity)) {
                    return true;
                }
            }
            return false;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsAgentObject(child, agentIdentity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Stream<String> extractAgentUuids(final JsonNode node) {
        final Stream.Builder<String> builder = Stream.builder();
        collectAgentUuids(node, builder);
        return builder.build().distinct();
    }

    private void collectAgentUuids(final JsonNode node, final Stream.Builder<String> builder) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            if (node.hasNonNull("agentUuid")) {
                builder.add(node.get("agentUuid").asText());
            }
            if (node.hasNonNull("uuid")) {
                builder.add(node.get("uuid").asText());
            }
            for (JsonNode child : node) {
                collectAgentUuids(child, builder);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectAgentUuids(child, builder);
            }
        }
    }

    private Optional<String> extractInputValue(final String html, final String name) {
        final Pattern pattern = Pattern.compile(
                "<input[^>]*name=\"" + Pattern.quote(name) + "\"[^>]*value=\"([^\"]*)\"",
                Pattern.CASE_INSENSITIVE
        );
        final Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private String extractRequiredInputValue(final String html, final String name) {
        return extractInputValue(html, name)
                .orElseThrow(() -> new IllegalStateException("Could not find setup field: " + name));
    }

    private Optional<String> extractFormAction(final String html, final String formId) {
        final Pattern pattern = Pattern.compile(
                "(?is)<form[^>]*id=\"" + Pattern.quote(formId) + "\"[^>]*action=\"([^\"]+)\""
        );
        final Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<FormSubmission> extractFormWithLabel(final String html, final String label) {
        final Pattern pattern = Pattern.compile("(?is)<form[^>]*action=\"([^\"]+)\"[^>]*>(.*?)</form>");
        final Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            final String action = matcher.group(1);
            final String innerHtml = matcher.group(2);
            if (!innerHtml.contains(label)) {
                continue;
            }

            final Map<String, String> formFields = new LinkedHashMap<>();
            final Pattern inputPattern = Pattern.compile(
                    "(?is)<input[^>]*name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"[^>]*>"
            );
            final Matcher inputMatcher = inputPattern.matcher(innerHtml);
            while (inputMatcher.find()) {
                formFields.put(inputMatcher.group(1), inputMatcher.group(2));
            }

            final Pattern buttonPattern = Pattern.compile(
                    "(?is)<button[^>]*name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"[^>]*>\\s*"
                            + Pattern.quote(label) + "\\s*</button>"
            );
            final Matcher buttonMatcher = buttonPattern.matcher(innerHtml);
            if (buttonMatcher.find()) {
                formFields.put(buttonMatcher.group(1), buttonMatcher.group(2));
            }

            final Pattern submitPattern = Pattern.compile(
                    "(?is)<input[^>]*type=\"submit\"[^>]*name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"[^>]*>"
            );
            final Matcher submitMatcher = submitPattern.matcher(innerHtml);
            while (submitMatcher.find()) {
                if (submitMatcher.group(2).contains(label)) {
                    formFields.put(submitMatcher.group(1), submitMatcher.group(2));
                }
            }

            return Optional.of(new FormSubmission(action, formFields));
        }
        return Optional.empty();
    }

    private Optional<String> extractPendingAuthenticationUuid(final String html,
                                                              final AgentIdentity agentIdentity) {
        final Pattern pattern = Pattern.compile(
                "(?is)<tr[^>]*data-row=\"([^\"]+)\"[^>]*>.*?"
                        + "<td[^>]*data-cell-type=\"ipAddress\">\\s*([^<]+?)\\s*</td>.*?"
                        + "<td[^>]*data-cell-type=\"uuid\">\\s*([^<]+?)\\s*</td>.*?"
                        + "<td[^>]*data-cell-type=\"status\"[^>]*>\\s*([^<]+?)\\s*</td>.*?</tr>"
        );
        final Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            final String rowUuid = matcher.group(1).trim();
            final String ipAddress = matcher.group(2).trim();
            final String agentUuid = matcher.group(3).trim();
            final String status = matcher.group(4).trim();
            if (!agentIdentity.matchesIp(ipAddress)) {
                continue;
            }
            if (status.toUpperCase().contains("WAITING")) {
                return Optional.of(agentUuid.isBlank() ? rowUuid : agentUuid);
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractAuthenticationUuid(final String html, final String expectedIpAddress) {
        final Pattern pattern = Pattern.compile(
                "(?is)<tr[^>]*data-row=\"([^\"]+)\"[^>]*>.*?"
                        + "<td[^>]*data-cell-type=\"ipAddress\">\\s*([^<]+?)\\s*</td>.*?"
                        + "<td[^>]*data-cell-type=\"uuid\">\\s*([^<]+?)\\s*</td>.*?</tr>"
        );
        final Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            final String rowUuid = matcher.group(1).trim();
            final String ipAddress = matcher.group(2).trim();
            final String agentUuid = matcher.group(3).trim();
            if (expectedIpAddress.equalsIgnoreCase(ipAddress)) {
                return Optional.of(agentUuid.isBlank() ? rowUuid : agentUuid);
            }
        }
        return Optional.empty();
    }

    private URI resolveRelativeUri(final String baseUrl, final String relativeOrAbsolutePath) {
        return URI.create(baseUrl).resolve(relativeOrAbsolutePath);
    }

    private HttpClient buildClient() {
        return HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .cookieHandler(cookieManager)
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    private void resetClient() {
        client = buildClient();
    }

    private void deleteRecursively(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            final Path[] paths = stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .toArray(Path[]::new);
            for (Path entry : paths) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private JsonNode readJson(final String value) {
        try {
            return MAPPER.readTree(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON response", e);
        }
    }

    private String toFormData(final Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String basicAuth() {
        final String token = BAMBOO_ADMIN_USERNAME + ":" + BAMBOO_ADMIN_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface ResponsePredicate {
        boolean test(HttpResponse<String> response) throws Exception;
    }

    @FunctionalInterface
    private interface RequestExecutor {
        HttpResponse<String> execute(String url) throws Exception;
    }

    private record FormSubmission(String action, Map<String, String> fields) {
    }

    private record AgentIdentity(String agentName, String agentIp, String agentUuid) {

        private AgentIdentity withUuid(final String resolvedAgentUuid) {
            return new AgentIdentity(agentName, agentIp, resolvedAgentUuid);
        }

        private boolean matches(final String reportedAgentName, final String reportedAgentUuid) {
            return matchesName(reportedAgentName) || matchesUuid(reportedAgentUuid);
        }

        private boolean matchesName(final String reportedAgentName) {
            return reportedAgentName != null && agentName.equalsIgnoreCase(reportedAgentName);
        }

        private boolean matchesUuid(final String reportedAgentUuid) {
            return reportedAgentUuid != null
                    && agentUuid != null
                    && agentUuid.equalsIgnoreCase(reportedAgentUuid);
        }

        private boolean matchesIp(final String reportedAgentIp) {
            return reportedAgentIp != null && agentIp.equalsIgnoreCase(reportedAgentIp);
        }
    }

    private record Config(
            Path rootDir,
            String bambooVersion,
            String productLicense,
            Path artifactRoot,
            Path pluginJar,
            String bambooStoragePath,
            Duration agentTimeout,
            Duration buildTimeout
    ) {

        private static Config fromSystemProperties() {
            final Path rootDir = Path.of(getRequiredProperty("compat.rootDir")).toAbsolutePath();
            final String bambooVersion = getRequiredProperty("compat.version");
            final String productLicense = getRequiredProperty("compat.productLicense");
            final Path artifactRoot = Path.of(System.getProperty(
                    "compat.artifactRoot",
                    rootDir.resolve("compat-artifacts").resolve(bambooVersion).toString()
            )).toAbsolutePath();
            final Path pluginJar = Optional.ofNullable(System.getProperty("compat.pluginJar"))
                    .filter(value -> !value.isBlank())
                    .map(Path::of)
                    .map(Path::toAbsolutePath)
                    .orElse(null);
            final String bambooStoragePath = System.getProperty(
                    "compat.bambooStoragePath",
                    BAMBOO_HOME_PATH + "/allure-compat-reports"
            );
            final Duration agentTimeout = Duration.ofSeconds(
                    Long.getLong("compat.agentTimeoutSeconds", 900L)
            );
            final Duration buildTimeout = Duration.ofSeconds(
                    Long.getLong("compat.buildTimeoutSeconds", 900L)
            );

            return new Config(
                    rootDir,
                    bambooVersion,
                    productLicense,
                    artifactRoot,
                    pluginJar,
                    bambooStoragePath,
                    agentTimeout,
                    buildTimeout
            );
        }

        private static String getRequiredProperty(final String key) {
            final String value = System.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required system property: " + key);
            }
            return value;
        }
    }
}
