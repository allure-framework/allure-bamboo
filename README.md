[site]: https://allurereport.org "Official Website"
[docs]: https://allurereport.org/docs/integrations-bamboo/ "Documentation"
[marketplace]: https://marketplace.atlassian.com/apps/1217177/allure-report-for-bamboo "Atlassian Marketplace"
[releases]: https://github.com/allure-framework/allure-bamboo/releases "Releases"
[release]: https://github.com/allure-framework/allure-bamboo/releases/latest "Latest release"
[release-badge]: https://img.shields.io/github/release/allure-framework/allure-bamboo.svg?style=flat
[build]: https://github.com/allure-framework/allure-bamboo/actions/workflows/build.yml
[build-badge]: https://github.com/allure-framework/allure-bamboo/actions/workflows/build.yml/badge.svg
[license]: https://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"

# Allure Report for Bamboo

[![build-badge][]][build] [![release-badge][]][release]

**Allure Report for Bamboo** is a plugin for [Atlassian Bamboo](https://www.atlassian.com/software/bamboo) that generates [Allure Report][site] from the test results of your builds and shows it right on the build page.

[<img src="https://allurereport.org/public/img/allure-report.svg" height="85px" alt="Allure Report logo" align="right" />](https://allurereport.org "Allure Report")

- Learn more about Allure Report at [https://allurereport.org](https://allurereport.org)
- 📚 [Documentation](https://allurereport.org/docs/) – discover official documentation for Allure Report
- ❓ [Questions and Support](https://github.com/orgs/allure-framework/discussions/categories/questions-support) – get help from the team and community
- 📢 [Official announcements](https://github.com/orgs/allure-framework/discussions/categories/announcements) – stay updated with our latest news and updates
- 💬 [General Discussion](https://github.com/orgs/allure-framework/discussions/categories/general-discussion) – engage in casual conversations, share insights and ideas with the community

---

## Installation

Install the plugin from the [Atlassian Marketplace][marketplace].

Alternatively, download the JAR from the [releases page][releases] and [install it manually](https://confluence.atlassian.com/upm/installing-marketplace-apps-273875715.html#InstallingMarketplaceapps-Installanappfromafile).

## Configuration and Usage

Follow the [Bamboo integration guide][docs] in the official Allure Report documentation.

## Development

Building the plugin requires JDK 17.

Build the plugin JAR (`target/allure-bamboo-<version>.jar`):

```bash
./mvnw clean package
```

Run the tests and static checks (the same gate as CI):

```bash
./mvnw clean verify
```

To try the plugin in a local Bamboo instance:

1. Set up the [Atlassian Plugin SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/set-up-the-atlassian-plugin-sdk-and-build-a-project/).
2. Run `atlas-run` in the repository root.
3. Open http://localhost:6990/bamboo/ to access the development instance with the plugin installed.

### Debug logging

In a development instance, open http://localhost:6990/bamboo/admin/configLog4j.action, add the `io.qameta.allure.bamboo` package, and select the `DEBUG` level. Logs are written to `target/bamboo/home/logs/atlassian-bamboo.log`.

## License

The Allure Report for Bamboo plugin is released under the [Apache License 2.0][license].
