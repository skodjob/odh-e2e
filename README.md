# ODH-E2E
E2E test suite for [opendatahub.io](https://github.com/opendatahub-io/opendatahub-operator) operator using fabric8 kubernetes client

[![UnitTest](https://github.com/skodjob/odh-e2e/actions/workflows/test.yaml/badge.svg?branch=main)](https://github.com/ExcelentProject/odh-e2e/actions/workflows/test.yaml)

### Requirements
* java jdk >= 17
* oc

## List of test tags
* `standard` - e2e tests that install the operator and needed operands and verify the use cases. Could be used as regression profile.
  * `smoke` - quick verification for install ODH and some notebooks
* `upgrade` - perform operator upgrade verification
  * `olm-upgrade` - tests that perform upgrade with operator installed via OLM
  * `bundle-upgrade` - tests that perform upgrade with operator installed via yaml files
* `continuous` - specific tests designed to verify [this](https://github.com/skodjob/deployment-hub/tree/main/open-data-hub) scenario. Do not use it unless you install the scenario from the link!
* `all` (standard + upgrade)

## Environment variables
* **ENV_FILE** - path to yaml file with env variables values
* **KUBE_TOKEN** - token of kube access (use instead of username/password)
* **KUBE_URL** - url of the cluster (api url)
* **PRODUCT** - odh or rhoai
* **SKIP_INSTALL_OPERATOR_DEPS** - skip installation of the odh/rhoai operator dependencies
* **SKIP_INSTALL_OPERATOR** - skip odh/rhoai operator install
* **SKIP_DEPLOY_DSCI_DSC** - skip odh/rhoai deploy of DSCI and DSC
* **INSTALL_FILE** - yaml definition of operator (default is downloaded latest)
* **INSTALL_FILE_PREVIOUS** - yaml definition of operator for upgrade testing (default is downloaded latest released)
* **OPERATOR_IMAGE_OVERRIDE** - image override in yaml definition
* **OLM_SOURCE_NAME** - olm source
* **OLM_SOURCE_NAMESPACE** - olm source namespace
* **OLM_OPERATOR_VERSION** - install operator version
* **OLM_OPERATOR_CHANNEL** - channel
* **OPERATOR_INSTALL_TYPE** - bundle or olm (bundle uses yaml file def, olm uses olm properties)

## Examples how to run selected test suites

### Run continuous suite
```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw verify -Pcontinuous
```

### Run standard (CRUD) suite
* Running all tests from standard test suite
```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw verify -Pstandard
```
* Select single test from standard test suite
```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw verify -Pstandard -Dit.test=DataScienceClusterST#createDataScienceCluster
```

### Run upgrade suite
```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw verify -Pupgrade
```

### RUN Unit test of the suite
```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw test
```

## Reproducing test run
When every test run is executed, test suite automatically creates a `config.yaml` file
which contains all configured environment variables. Location of config file 
is `$LOG_DIR/test-run-YYYY-MM-DD_HH-mm/config.yaml` where `$LOG_DIR` is by
default `${repo_root}/target/logs`.

```commandline
GITHUB_TOKEN="your_github_read_token" ENV_FILE=path_to_file/config.yaml ./mvnw verify -Psmoke
```

## Debug
During failures all logs relevant logs are collected and stored in `target/logs` so users can go through the logs and see if the problem was in project or in tests.

As part of `target/logs` you can find file `config.yaml` that contains all env variables used by the test run. 
You can easily re-use it by pass path to the file into `ENV_FILE` environment variable. 
Note that we suggest to copy the file outside the target dir.

## Testing docs
We are using [test-metadata-generator](https://github.com/skodjob/test-metadata-generator) maven plugin for annotating tests and generate test documentation from it.
The docs are generated with every build and the changes should be committed when there are some.

The plugin is still under development so the format could change.
For more info see the plugin repository on GitHub.

## Allure reports
Enable the `-Pallure` Maven profile to collect testrun data for Allure reporting.

```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw verify -Pstandard -Pallure
```

Then use the Allure Maven plugin to open a HTML report with the results in a web browser.

```commandline
GITHUB_TOKEN="your_github_read_token" ./mvnw allure:serve
```

## Authors
* [David Kornel](https://github.com/kornys) <kornys@outlookcom>
* [Jakub Stejskal](https://github.com/Frawless) <xstejs24@gmail.com>

This test suite uses [test-frame](https://github.com/skodjob/test-frame) module.
