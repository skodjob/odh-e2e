# ODH-E2E
E2e test suite for opendatahub using fabric8 kubernetes client

[![UnitTest](https://github.com/ExcelentProject/odh-e2e/actions/workflows/test.yaml/badge.svg?branch=main)](https://github.com/ExcelentProject/odh-e2e/actions/workflows/test.yaml)

## Run full suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn verify --settings settings.xml
```

## RUN Unit test of the suite
```commandline
GITHUB_TOKEN="your_github_read_token" mvn test --settings settings.xml
```