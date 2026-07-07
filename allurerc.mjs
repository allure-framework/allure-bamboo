const { ALLURE_SERVICE_TOKEN } = process.env;

const allureService = ALLURE_SERVICE_TOKEN
  ? {
      accessToken: ALLURE_SERVICE_TOKEN,
    }
  : undefined;

export default {
  name: "Allure Bamboo",
  output: "./target/allure-report",
  plugins: {
    awesome: {
      options: {
        groupBy: ["module"],
        appendTitlePath: true,
        publish: true,
      },
    },
    testops: {
      options: {
        launchName: `Allure Bamboo GitHub actions run (${new Date().toISOString()})`,
      },
    },
  },
  ...(allureService ? { allureService } : {}),
};
