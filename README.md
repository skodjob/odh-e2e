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
* standard
* continuous
* all (standard + upgrade)

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