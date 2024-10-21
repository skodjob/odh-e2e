/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestUtils;
import io.odh.test.install.InstallTypes;
import io.odh.test.platform.KFPv2Client;
import io.odh.test.platform.TlsUtils;
import io.odh.test.platform.httpClient.OAuthToken;
import io.odh.test.utils.CsvUtils;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.DataScienceClusterBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.ComponentsBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.CodeflareBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.DashboardBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.DatasciencepipelinesBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.KserveBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kueue;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.KueueBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.ModelmeshservingBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.RayBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.WorkbenchesBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplication;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplicationBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.datasciencepipelinesapplicationspec.ApiServer;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.qameta.allure.Allure;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
@SuiteDoc(
    description = @Desc("Verifies simple setup of ODH by spin-up operator, setup DSCI, and setup DSC."),
    beforeTestSteps = {
        @Step(value = "Deploy Pipelines Operator", expected = "Pipelines operator is available on the cluster"),
        @Step(value = "Deploy ServiceMesh Operator", expected = "ServiceMesh operator is available on the cluster"),
        @Step(value = "Deploy Serverless Operator", expected = "Serverless operator is available on the cluster"),
        @Step(value = "Install ODH operator", expected = "Operator is up and running and is able to serve it's operands"),
        @Step(value = "Deploy DSCI", expected = "DSCI is created and ready"),
        @Step(value = "Deploy DSC", expected = "DSC is created and ready")
    },
    afterTestSteps = {
        @Step(value = "Delete ODH operator and all created resources", expected = "Operator is removed and all other resources as well")
    }
)
@DisabledIf(value = "isDSPv1Only", disabledReason = "Old versions of ODH don't support DSPv2.")
public class PipelineV2ServerST extends StandardAbstract {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineV2ServerST.class);

    private static final String DS_PROJECT_NAME = "test-pipelines";

    private final OpenShiftClient client = KubeResourceManager.getKubeClient().getOpenShiftClient();

    @BeforeAll
    void deployDataScienceCluster() {
        if (Environment.SKIP_DEPLOY_DSCI_DSC) {
            LOGGER.info("DSCI and DSC deploy is skipped");
            return;
        }

        // Create DSCI
        DSCInitialization dsci = DscUtils.getBasicDSCI();
        // Create DSC
        DataScienceCluster dsc = new DataScienceClusterBuilder()
                .withNewMetadata()
                .withName(DS_PROJECT_NAME)
                .endMetadata()
                .withNewSpec()
                .withComponents(
                        new ComponentsBuilder()
                                .withWorkbenches(
                                        new WorkbenchesBuilder().withManagementState(Workbenches.ManagementState.Managed).build()
                                )
                                .withDashboard(
                                        new DashboardBuilder().withManagementState(Dashboard.ManagementState.Managed).build()
                                )
                                .withKserve(
                                        new KserveBuilder().withManagementState(Kserve.ManagementState.Managed).build()
                                )
                                .withKueue(
                                        new KueueBuilder().withManagementState(Kueue.ManagementState.Managed).build()
                                )
                                .withCodeflare(
                                        new CodeflareBuilder().withManagementState(Codeflare.ManagementState.Managed).build()
                                )
                                .withDatasciencepipelines(
                                        new DatasciencepipelinesBuilder().withManagementState(Datasciencepipelines.ManagementState.Managed).build()
                                )
                                .withModelmeshserving(
                                        new ModelmeshservingBuilder().withManagementState(Modelmeshserving.ManagementState.Managed).build()
                                )
                                .withRay(
                                        new RayBuilder().withManagementState(Ray.ManagementState.Managed).build()
                                )
                                .build())
                .endSpec()
                .build();

        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(dsci);
        KubeResourceManager.getInstance().createResourceWithWait(dsc);
    }

    /// ODS-2206 - Verify user can create and run a data science pipeline in DS Project
    /// ODS-2226 - Verify user can delete components of data science pipeline from DS Pipelines page
    /// https://issues.redhat.com/browse/RHODS-5133
    @SuppressWarnings({"checkstyle:MethodLength"})
    @TestDoc(
        description = @Desc("Check that user can create, run and deleted DataSciencePipeline from a DataScience project"),
        contact = @Contact(name = "Jiri Danek", email = "jdanek@redhat.com"),
        steps = {
            @Step(value = "Create namespace for DataSciencePipelines application with proper name, labels and annotations", expected = "Namespace is created"),
            @Step(value = "Create Minio secret with proper data for access s3", expected = "Secret is created"),
            @Step(value = "Create DataSciencePipelinesApplication with configuration for new Minio instance and new MariaDB instance", expected = "DataSciencePipelinesApplication resource is created"),
            @Step(value = "Wait for DataSciencePipelines server readiness", expected = "DSP API endpoint is available and it return proper data"),
            @Step(value = "Import pipeline to a pipeline server via API", expected = "Pipeline is imported"),
            @Step(value = "List imported pipeline via API", expected = "Server return list with imported pipeline info"),
            @Step(value = "Trigger pipeline run for imported pipeline", expected = "Pipeline is triggered"),
            @Step(value = "Wait for pipeline success", expected = "Pipeline succeeded"),
            @Step(value = "Delete pipeline run", expected = "Pipeline run is deleted"),
            @Step(value = "Delete pipeline", expected = "Pipeline is deleted"),
        }
    )
    @Test
    void testUserCanOperateDSv2PipelineFromDSProject() throws Exception {
        final String pipelineTestName = "pipeline-test-name";
        final String pipelineTestDesc = "pipeline-test-desc";
        final String prjTitle = "pipeline-test";
        final String pipelineTestFilepath = "src/test/resources/pipelines/iris_pipeline_compiled_kfpv2.yaml";
        final String pipelineTestRunBasename = "pipeline-test-run-basename";

        final String secretName = "mlpipeline-minio-artifact"; // TODO(jdanek): can't use custom name in v2, bug?

        Allure.step("Setup CRs", () -> {
            Allure.step("Create Data Science Project");
            Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                .withName(prjTitle)
                .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
                .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
                .endMetadata()
                .build();
            KubeResourceManager.getInstance().createResourceWithWait(ns);

            Allure.step("Create Minio secret");
            Secret secret = new SecretBuilder()
                    .withNewMetadata()
                        .withName(secretName)
                        .addToLabels("opendatahub.io/dashboard", "true")
                        .withNamespace(prjTitle)
                    .endMetadata()
                    .addToStringData("AWS_ACCESS_KEY_ID", "KEY007")
                    .addToStringData("AWS_S3_BUCKET", "HolyGrail")
                    .addToStringData("AWS_SECRET_ACCESS_KEY", "gimmeAccessPlz")
                    .withType("Opaque")
                    .build();
            KubeResourceManager.getInstance().createResourceWithWait(secret);

            Allure.step("Create DataSciencePipelinesApplication instance with build-in Minio enabled");
            DataSciencePipelinesApplication dspa = new DataSciencePipelinesApplicationBuilder()
                    .withNewMetadata()
                        .withName("pipelines-definition")
                        .withNamespace(prjTitle)
                    .endMetadata()
                    .withNewSpec()
                        // https://github.com/opendatahub-io/data-science-pipelines-operator/blob/main/config/samples/v2/dspa-simple/dspa_simple.yaml
                        .withDspVersion("v2")
                        // TODO(jdanek): v1 values below, this will need review and updating closer to release
                        .withNewApiServer()
                            .withApplyTektonCustomResource(true)
                            .withArchiveLogs(false)
                            .withAutoUpdatePipelineDefaultVersion(true)
                            .withCollectMetrics(true)
                            .withDbConfigConMaxLifetimeSec(120L)
                            .withDeploy(true)
                            .withEnableOauth(true)
                            .withEnableSamplePipeline(false)
                            .withInjectDefaultScript(true)
                            .withStripEOF(true)
                            .withTerminateStatus(ApiServer.TerminateStatus.Cancelled)
                            .withTrackArtifacts(true)
                        .endApiServer()
                        .withNewDatabase()
                            .withDisableHealthCheck(false)
                            .withNewMariaDB()
                                .withDeploy(true)
                                .withPipelineDBName("mlpipeline")
                                .withNewPvcSize("10Gi")
                                .withUsername("mlpipeline")
                            .endMariaDB()
                        .endDatabase()
                        .withNewMlmd()
                            .withDeploy(true)
                        .endMlmd()
                        .withNewObjectStorage()
                            .withDisableHealthCheck(false)
                            .withNewMinio()
                                .withDeploy(true)
                                .withImage("quay.io/minio/minio")
                                .withNewPvcSize("1Gi")
                                .withBucket("HolyGrail")
                                .withNewS3CredentialsSecret()
                                    .withAccessKey("AWS_ACCESS_KEY_ID")
                                    .withSecretKey("AWS_SECRET_ACCESS_KEY")
                                    .withSecretName(secretName)
                                .endMinioS3CredentialsSecret()
                            .endMinio()
                        .endObjectStorage()
                        .withNewPersistenceAgent()
                            .withDeploy(true)
                            .withNumWorkers(2L)
                        .endPersistenceAgent()
                        .withNewScheduledWorkflow()
                            .withCronScheduleTimezone("UTC")
                            .withDeploy(true)
                        .endScheduledWorkflow()
                    .endSpec()
                    .build();
            KubeResourceManager.getInstance().createResourceWithWait(dspa);
        });

        Allure.step("Wait for Pipeline API server to come up");
        Resource<Endpoints> endpoints = client.endpoints().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        TestUtils.waitForEndpoints("pipelines", endpoints);

        Allure.step("Fetch OpenShift's ingress CA for a HTTPS client");
        Secret ingressCaCerts = client.secrets().inNamespace("openshift-ingress").withName("router-certs-default").get();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(TlsUtils.getSSLContextFromSecret(ingressCaCerts))
                .build();

        Allure.step("Connect to the Pipeline API server");
        Route route = client.routes()
                .inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition").get();

        String url = "https://" + route.getStatus().getIngress().get(0).getHost();
        String redirectUrl = url + "/oauth/callback";
        String oauthToken = Allure.step("Create OAuth Token",
                () -> new OAuthToken().getToken(redirectUrl));

        KFPv2Client kfpClient = new KFPv2Client(httpClient, url, oauthToken);

        // WORKAROUND(RHOAIENG-3250): delete sample pipeline present on ODH
        deletePreexistingPipelinesAndVersions(kfpClient);

        KFPv2Client.Pipeline importedPipeline = kfpClient.importPipeline(pipelineTestName, pipelineTestDesc, pipelineTestFilepath);

        List<KFPv2Client.Pipeline> pipelines = kfpClient.listPipelines();
        assertThat(pipelines.stream().map(p -> p.displayName).collect(Collectors.toList()), Matchers.contains(pipelineTestName));

        Map<String, Object> parameters = Map.of(
                "min_max_scaler", false,
                "neighbors", 1,
                "standard_scaler", true
        );
        KFPv2Client.PipelineRun pipelineRun = kfpClient.runPipeline(pipelineTestRunBasename, importedPipeline.pipelineId, parameters, "Immediate");

        kfpClient.waitForPipelineRun(pipelineRun.runId);

        List<KFPv2Client.PipelineRun> statuses = kfpClient.getPipelineRunStatus();
        assertThat(statuses.stream()
                .filter(run -> run.runId.equals(pipelineRun.runId))
                .map(run -> run.state)
                .findFirst().orElseThrow(), Matchers.is("SUCCEEDED"));

        checkPipelineRunK8sDeployments(prjTitle, pipelineRun.runId);

        kfpClient.deletePipelineRun(pipelineRun.runId);
        for (KFPv2Client.PipelineVersion pipelineVersion : kfpClient.listPipelineVersions(importedPipeline.pipelineId)) {
            kfpClient.deletePipelineVersion(importedPipeline.pipelineId, pipelineVersion.pipelineVersionId);
        }
        kfpClient.deletePipeline(importedPipeline.pipelineId);
    }

    @io.qameta.allure.Step
    private static void deletePreexistingPipelinesAndVersions(KFPv2Client kfpClient) {
        for (KFPv2Client.Pipeline pipeline : kfpClient.listPipelines()) {
            for (KFPv2Client.PipelineVersion pipelineVersion : kfpClient.listPipelineVersions(pipeline.pipelineId)) {
                kfpClient.deletePipelineVersion(pipeline.pipelineId, pipelineVersion.pipelineVersionId);
            }
            kfpClient.deletePipeline(pipeline.pipelineId);
        }
    }

    @io.qameta.allure.Step
    private void checkPipelineRunK8sDeployments(String prjTitle, String runId) {
        List<Pod> argoTaskPods = client.pods().inNamespace(prjTitle).withLabel("pipeline/runid=" + runId).list().getItems();
        Assertions.assertEquals(7, argoTaskPods.size());

        for (Pod pod : argoTaskPods) {
            Assertions.assertEquals("Succeeded", pod.getStatus().getPhase());

            List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            Assertions.assertNotEquals(0, containerStatuses.size());
            for (ContainerStatus containerStatus : containerStatuses) {
                ContainerStateTerminated terminated = containerStatus.getState().getTerminated();
                Assertions.assertNotNull(terminated);
                Assertions.assertEquals(0, terminated.getExitCode());
                Assertions.assertEquals("Completed", terminated.getReason());
            }
        }

        final String workflowName = argoTaskPods.get(0).getMetadata().getLabels().get("workflows.argoproj.io/workflow");
        List<String> expectedNodeNames = List.of(
                workflowName + ".root-driver",
                workflowName + ".root.create-dataset-driver",
                workflowName + ".root.create-dataset.executor",
                workflowName + ".root.normalize-dataset-driver",
                workflowName + ".root.normalize-dataset.executor",
                workflowName + ".root.train-model-driver",
                workflowName + ".root.train-model.executor");
        List<String> argoNodeNames = argoTaskPods.stream()
                .map(pod -> pod.getMetadata().getAnnotations().get("workflows.argoproj.io/node-name"))
                .toList();
        Assertions.assertIterableEquals(expectedNodeNames.stream().sorted().toList(), argoNodeNames.stream().sorted().toList(), argoNodeNames.toString());
    }

    static boolean isDSPv1Only() {
        final CsvUtils.Version maxOdhVersion = CsvUtils.Version.fromString("2.10.0");
        final CsvUtils.Version maxRhoaiVersion = CsvUtils.Version.fromString("2.9.0");

        // can't tell, but since it's likely a recent ODH, then `return false;` is probably correct
        if (!Environment.OPERATOR_INSTALL_TYPE.equalsIgnoreCase(InstallTypes.OLM.toString())) {
            return false;
        }

        CsvUtils.Version operatorVersion = CsvUtils.Version.fromString(Objects.requireNonNull(CsvUtils.getOperatorVersionFromCsv()));

        if (Environment.PRODUCT.equalsIgnoreCase(Environment.PRODUCT_ODH)) {
            return operatorVersion.compareTo(maxOdhVersion) < 0;
        } else {
            return operatorVersion.compareTo(maxRhoaiVersion) < 0;
        }
    }
}
