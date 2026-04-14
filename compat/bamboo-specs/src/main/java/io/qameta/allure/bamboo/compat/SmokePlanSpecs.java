package io.qameta.allure.bamboo.compat;

import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.SimpleUserPasswordCredentials;

/**
 * Publishes a minimal Bamboo plan that produces one shared {@code allure-results} artifact.
 */
public final class SmokePlanSpecs {

    private static final String PROJECT_KEY = "ALCOMP";
    private static final String PLAN_KEY = "SMOKE";
    private static final String JOB_KEY = "JOB1";

    private SmokePlanSpecs() {
        // utility class
    }

    @SuppressWarnings("deprecation")
    public static void main(final String[] args) {
        publish(
                getRequiredEnv("BAMBOO_URL"),
                getRequiredEnv("BAMBOO_USERNAME"),
                getRequiredEnv("BAMBOO_PASSWORD")
        );
    }

    public static String getPlanKey() {
        return PROJECT_KEY + "-" + PLAN_KEY;
    }

    @SuppressWarnings("deprecation")
    public static void publish(final String bambooUrl,
                               final String bambooUsername,
                               final String bambooPassword) {
        final BambooServer bambooServer = new BambooServer(
                bambooUrl,
                new SimpleUserPasswordCredentials(bambooUsername, bambooPassword)
        );
        bambooServer.publish(createPlan());
    }

    private static Plan createPlan() {
        final Project project = new Project()
                .key(PROJECT_KEY)
                .name("Allure Compatibility");

        final Job smokeJob = new Job("Smoke Job", JOB_KEY)
                .tasks(new ScriptTask()
                        .description("Generate a tiny allure-results fixture")
                        .interpreterBinSh()
                        .inlineBody("#!/bin/sh\n"
                                + "set -eu\n"
                                + "mkdir -p allure-results\n"
                                + "cat > allure-results/compat-result.json <<'EOF'\n"
                                + "{\n"
                                + "  \"uuid\": \"compat-smoke-result\",\n"
                                + "  \"historyId\": \"compat-smoke-history\",\n"
                                + "  \"testCaseId\": \"compat-smoke-case\",\n"
                                + "  \"name\": \"Compatibility smoke test\",\n"
                                + "  \"fullName\": \"io.qameta.allure.bamboo.compat.Smoke.compatibility\",\n"
                                + "  \"status\": \"passed\",\n"
                                + "  \"stage\": \"finished\",\n"
                                + "  \"steps\": [],\n"
                                + "  \"attachments\": [],\n"
                                + "  \"parameters\": [],\n"
                                + "  \"links\": [],\n"
                                + "  \"labels\": [\n"
                                + "    {\"name\": \"suite\", \"value\": \"compatibility\"},\n"
                                + "    {\"name\": \"package\", \"value\": \"io.qameta.allure.bamboo.compat\"},\n"
                                + "    {\"name\": \"testClass\", \"value\": \"Smoke\"},\n"
                                + "    {\"name\": \"testMethod\", \"value\": \"compatibility\"},\n"
                                + "    {\"name\": \"framework\", \"value\": \"compat\"},\n"
                                + "    {\"name\": \"language\", \"value\": \"shell\"}\n"
                                + "  ],\n"
                                + "  \"start\": 1700000000000,\n"
                                + "  \"stop\": 1700000000100\n"
                                + "}\n"
                                + "EOF\n"
                                + "ls -R allure-results\n"))
                .artifacts(new Artifact("allure-results")
                        .location("allure-results")
                        .copyPatterns("**/*")
                        .shared(true));

        return new Plan(project, "Allure Smoke", PLAN_KEY)
                .description("Nightly compatibility smoke plan for the Allure Bamboo plugin")
                .stages(new Stage("Smoke Stage").jobs(smokeJob));
    }

    private static String getRequiredEnv(final String key) {
        final String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + key);
        }
        return value;
    }
}
