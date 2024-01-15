# ODH-E2E
E2E test suite for opendatahub using fabric8 kubernetes client

[![UnitTest](https://github.com/skodjob/odh-e2e/actions/workflows/test.yaml/badge.svg?branch=main)](https://github.com/ExcelentProject/odh-e2e/actions/workflows/test.yaml)

### Requirements
* maven >= 3.6
* java jdk >= 17
* oc

## List of test suites
* smoke
* upgrade
* olm-upgrade
* bundle-upgrade
* standard
* continuous
* all (standard + upgrade)

## Environment variables
* **ENV_FILE** - path to yaml file with env variables values
* **KUBE_USERNAME** - user of the cluster
* **KUBE_PASSWORD** - password of kube user
* **KUBE_TOKEN** - token of kube access (use instead of username/password)
* **KUBE_URL** - url of the cluster (api url)
* **PRODUCT** - odh or rhoai
* **SKIP_INSTALL_OPERATOR** - skip odh/rhoai operator install
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
