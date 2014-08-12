## Allure Bamboo Plugin

This repository contains source code of Allure plugin for [Atlassian Bamboo CI](https://www.atlassian.com/software/bamboo).

### Building and Installing
#### Short way
Download precompiled JAR from [releases page](https://github.com/allure-framework/allure-bamboo-plugin/releases) and install it manually as described [here](https://confluence.atlassian.com/display/UPM/Installing+add-ons#Installingadd-ons-Installingbyfileupload). We use JDK 1.7+ to compile the plugin so be sure to use Java 1.7+ for running Bamboo.
#### Long way
1. Set up Atlassian plugin SDK as described [here](https://developer.atlassian.com/display/DOCS/Set+up+the+Atlassian+Plugin+SDK+and+Build+a+Project).
2. Clone this repository
3. Run `$ atlas-run`
4. Access http://localhost:6990/bamboo/ to view development instance of Bamboo
5. Verify that plugin is working as expected
6. Install **target/allure-bamboo-plugin-VERSION.jar** manually as described [here](https://confluence.atlassian.com/display/UPM/Installing+add-ons#Installingadd-ons-Installingbyfileupload).

### Usage
When installed this plugin provides a new task called **Allure**. To use it configure your build as follows:
1. Add Allure task to your job:
![Add Task](https://raw.githubusercontent.com/allure-framework/allure-bamboo-plugin/master/img/add_task.png)
2. Configure task - specify glob pattern to the folder where Allure should search for XML files and desired report version to be used:
![Fill Task Fields](https://raw.githubusercontent.com/allure-framework/allure-bamboo-plugin/master/img/task_fields.png)
3. Define an artifact. It's important to use exact values from the picture below for **Location** and **Copy pattern** fields. It's up to you to choose artifact name.
![Define Artifact](https://raw.githubusercontent.com/allure-framework/allure-bamboo-plugin/master/img/artifact_definition.png)
4. Run the build as usually and click on Allure report artifact on the **Artifacts** tab:
![View Artifact](https://raw.githubusercontent.com/allure-framework/allure-bamboo-plugin/master/img/view_artifact.png)