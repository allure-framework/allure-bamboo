package io.qameta.allure.bamboo;

class AllureGenerateResult {
    private final String output;
    private final boolean containsTestcases;

    AllureGenerateResult(String output, boolean containsTestCases) {
        this.output = output;
        this.containsTestcases = containsTestCases;
    }

    String getOutput() {
        return output;
    }

    boolean isContainsTestCases() {
        return containsTestcases;
    }
}
