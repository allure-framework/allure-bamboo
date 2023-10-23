## Allure Bamboo Plugin

This repository contains source code of Allure plugin
for [Atlassian Bamboo CI](https://www.atlassian.com/software/bamboo). It allows you to generate Allure report
from [existing Allure XML files](https://github.com/allure-framework/allure-core/wiki#gathering-information-about-tests).

### Building and Installing

#### Short way

Download precompiled JAR from [releases page](https://github.com/allure-framework/allure-bamboo-plugin/releases) and
install it manually as
described [here](https://confluence.atlassian.com/display/UPM/Installing+add-ons#Installingadd-ons-Installingbyfileupload).
We use JDK 1.7+ to compile the plugin so be sure to use Java 1.7+ for running Bamboo.

#### Long way

1. Set up Atlassian plugin SDK as
described [here](https://developer.atlassian.com/display/DOCS/Set+up+the+Atlassian+Plugin+SDK+and+Build+a+Project).
2. Clone this repository
3. Run `$ atlas-run`
4. Access http://localhost:6990/bamboo/ to view development instance of Bamboo
5. Verify that plugin is working as expected
6. Install **target/allure-bamboo-plugin-VERSION.jar** manually as
described [here](https://confluence.atlassian.com/display/UPM/Installing+add-ons#Installingadd-ons-Installingbyfileupload).

### Configuration and Usage

Please follow the guide on the official Allure docs: https://docs.qameta.io/allure/#_bamboo
