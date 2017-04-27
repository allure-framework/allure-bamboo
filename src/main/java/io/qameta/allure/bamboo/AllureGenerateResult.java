package io.qameta.allure.bamboo;

class AllureGenerateResult {
    private final String output;
    private final boolean containsTestcases;

    AllureGenerateResult(String output, boolean containsTestcases) {
        this.output = output;
        this.containsTestcases = containsTestcases;
    }

    String getOutput() {
        return output;
    }

    boolean isContainsTestcases() {
        return containsTestcases;
    }
}
