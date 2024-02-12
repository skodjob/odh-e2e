# ODH-E2E
E2E test suite for opendatahub using fabric8 kubernetes client

[![UnitTest](https://github.com/skodjob/odh-e2e/actions/workflows/test.yaml/badge.svg?branch=main)](https://github.com/ExcelentProject/odh-e2e/actions/workflows/test.yaml)

### Requirements
* maven >= 3.6
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
* **KUBE_USERNAME** - user of the cluster
* **KUBE_PASSWORD** - password of kube user
* **KUBE_TOKEN** - token of kube access (use instead of username/password)
* **KUBE_URL** - url of the cluster (api url)
* **PRODUCT** - odh or rhoai
* **SKIP_INSTALL_OPERATOR** - skip odh/rhoai operator install
* **SKIP_DEPLOY_DSCI_DSC** - skip odh/rhoai deploy of DSCI and DSC
* **INSTALL_FILE** - yaml definition of operator (default is downloaded latest)
* **INSTALL_FILE_PREVIOUS** - yaml definition of operator for upgrade testing (default is downloaded latest released)
* **OPERATOR_IMAGE_OVERRIDE** - image override in yaml definition
* **OLM_SOURCE_NAME** - olm source
* **OLM_SOURCE_NAMESPACE** - olm source namesapce
* **OLM_OPERATOR_VERSION** - install operator version
* **OLM_OPERATOR_CHANNEL** - channel
* **OPERATOR_INSTALL_TYPE** - bundle or olm (bundle uses yaml file def, olm uses olm properties)

## Examples how to run selected test suites

### Run continuous suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn verify -Pcontinuous
```

### Run standard (CRUD) suite
* Running all tests from standard test suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn verify -Pstandard
```
* Select single test from standard test suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn verify -Pstandard -Dit.test=DataScienceClusterST#createDataScienceCluster
```

### Run upgrade suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn verify -Pupgrade
```

### RUN Unit test of the suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn test
```

## Reproducing test run
When every test run is executed, test suite automatically creates a `config.yaml` file
which contains all configured environment variables. Location of config file 
is `$LOG_DIR/test-run-YYYY-MM-DD_HH-mm/config.yaml` where `$LOG_DIR` is by
default `${repo_root}/target/logs`.

```commandline
GITHUB_TOKEN="your_github_read_token" ENV_FILE=path_to_file/config.yaml mvn verify -Psmoke
```

## Debug
During failures all logs relevant logs are collected and stored in `target/logs` so users can go through the logs and see if the problem was in project or in tests.

As part of `target/logs` you can find file `config.yaml` that contains all env variables used by the test run. 
You can easily re-use it by pass path to the file into `ENV_FILE` environment variable. 
Note that we suggest to copy the file outside the target dir.
