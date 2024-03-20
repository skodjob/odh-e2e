/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.listeners.ResourceManagerDeleteHandler;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.KFPv2Client;
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
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.datasciencepipelines.DevFlagsBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.datasciencepipelines.devflags.ManifestsBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplication;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplicationBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.datasciencepipelinesapplicationspec.ApiServer;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
@ExtendWith(ResourceManagerDeleteHandler.class)
public class PipelineV2ServerST extends StandardAbstract {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineV2ServerST.class);

    private static final String DS_PROJECT_NAME = "test-pipelines";

    private final ResourceManager resourceManager = ResourceManager.getInstance();
    private final KubernetesClient client = ResourceManager.getKubeClient().getClient();

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
                                        new WorkbenchesBuilder().withManagementState(Workbenches.ManagementState.MANAGED).build()
                                )
                                .withDashboard(
                                        new DashboardBuilder().withManagementState(Dashboard.ManagementState.MANAGED).build()
                                )
                                .withKserve(
                                        new KserveBuilder().withManagementState(Kserve.ManagementState.MANAGED).build()
                                )
                                .withKueue(
                                        new KueueBuilder().withManagementState(Kueue.ManagementState.MANAGED).build()
                                )
                                .withCodeflare(
                                        new CodeflareBuilder().withManagementState(Codeflare.ManagementState.MANAGED).build()
                                )
                                .withDatasciencepipelines(
                                        new DatasciencepipelinesBuilder()
                                                .withManagementState(Datasciencepipelines.ManagementState.MANAGED)
                                                // https://github.com/opendatahub-io/data-science-pipelines-operator/blob/main/datasciencecluster/datasciencecluster.yaml
                                                .withDevFlags(
                                                        new DevFlagsBuilder()
                                                                .withManifests(
                                                                        List.of(
                                                                                new ManifestsBuilder()
                                                                                        .withUri("https://github.com/opendatahub-io/data-science-pipelines-operator/tarball/main")
                                                                                        .withContextDir("config")
                                                                                        .withSourcePath("overlays/odh")
                                                                                        .build()
                                                                        )
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .withModelmeshserving(
                                        new ModelmeshservingBuilder().withManagementState(Modelmeshserving.ManagementState.MANAGED).build()
                                )
                                .withRay(
                                        new RayBuilder().withManagementState(Ray.ManagementState.MANAGED).build()
                                )
                                .build())
                .endSpec()
                .build();

        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(dsc);
    }

    /// ODS-2206 - Verify user can create and run a data science pipeline in DS Project
    /// ODS-2226 - Verify user can delete components of data science pipeline from DS Pipelines page
    /// https://issues.redhat.com/browse/RHODS-5133
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
    void testUserCanOperateDSv2PipelineFromDSProject() throws IOException {
        OpenShiftClient ocClient = (OpenShiftClient) client;

        final String pipelineTestName = "pipeline-test-name";
        final String pipelineTestDesc = "pipeline-test-desc";
        final String prjTitle = "pipeline-test";
        final String pipelineTestFilepath = "src/test/resources/pipelines/iris_pipeline_compiled_kfpv2.yaml";
        final String pipelineTestRunBasename = "pipeline-test-run-basename";

        final String secretName = "mlpipeline-minio-artifact"; // todo: can't use custom name in v2, bug?

        // create project
        Namespace ns = new NamespaceBuilder()
            .withNewMetadata()
            .withName(prjTitle)
            .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
            .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
            .endMetadata()
            .build();
        ResourceManager.getInstance().createResourceWithWait(ns);

        // create minio secret
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
        ResourceManager.getInstance().createResourceWithWait(secret);

        // configure pipeline server (with minio, not AWS bucket)
        DataSciencePipelinesApplication dspa = new DataSciencePipelinesApplicationBuilder()
                .withNewMetadata()
                    .withName("pipelines-definition")
                    .withNamespace(prjTitle)
                .endMetadata()
                .withNewSpec()
                    // https://github.com/opendatahub-io/data-science-pipelines-operator/blob/main/config/samples/v2/dspa-simple/dspa_simple.yaml
                    .withDspVersion("v2")
                    .withNewMlpipelineUI()
                        .withImage("quay.io/opendatahub/ds-pipelines-frontend:latest")
                    .endMlpipelineUI()
                    // todo: v1 values below, this will need review and updating
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
                        .withTerminateStatus(ApiServer.TerminateStatus.CANCELLED)
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
                    // todo: 2024-03-04T08:12:51Z    INFO    Encountered error when parsing CR:
                    // [MLMD explicitly disabled in DSPA, but is a required component for V2 Pipelines]
                    // {"namespace": "pipeline-test", "dspa_name": "pipelines-definition"}               │
                    .withNewMlmd()
                        .withDeploy(true)
                    .endMlmd()
                    .withNewObjectStorage()
                        .withDisableHealthCheck(false)
                        // NOTE: ods-ci uses aws, but minio is more appropriate here
                        // todo: │   Warning  Failed          7s (x4 over 44s)   kubelet            Error: secret "mlpipeline-minio-artifact" not found
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
        ResourceManager.getInstance().createResourceWithWait(dspa);

        // wait for pipeline api server to come up
        Resource<Endpoints> endpoints = client.endpoints().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        waitForEndpoints(endpoints);

        // connect to the api server we just created, route not available unless I enable oauth
        Resource<Route> route = ocClient.routes()
                .inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");

        // TODO(jdanek) I still don't know how to do oauth, so let's forward a port
        ServiceResource<Service> svc = client.services().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        try (LocalPortForward portForward = svc.portForward(8888, 0)) {
            KFPv2Client kfpClient = new KFPv2Client("http://localhost:%d".formatted(portForward.getLocalPort()));

            // WORKAROUND(RHOAIENG-3250): delete sample pipeline present on ODH
            if (Environment.PRODUCT.equals(Environment.PRODUCT_ODH)) {
                for (KFPv2Client.Pipeline pipeline : kfpClient.listPipelines()) {
                    kfpClient.deletePipeline(pipeline.pipelineId);
                }
            }

            for (KFPv2Client.Pipeline pipeline : kfpClient.listPipelines()) {
                for (KFPv2Client.PipelineVersion pipelineVersion : kfpClient.listPipelineVersions(pipeline.pipelineId)) {
                    kfpClient.deletePipelineVersion(pipeline.pipelineId, pipelineVersion.pipelineVersionId);
                }
                kfpClient.deletePipeline(pipeline.pipelineId);
            }

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
    }

    private void checkPipelineRunK8sDeployments(String prjTitle, String runId) {
        List<Pod> argoTaskPods = client.pods().inNamespace(prjTitle).withLabel("pipeline/runid=" + runId).list().getItems();
        Assertions.assertEquals(4, argoTaskPods.size());

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
                workflowName + ".root.data-prep-driver",
                workflowName + ".root.train-model-driver",
                workflowName + ".root.data-prep.executor",
                workflowName + ".root-driver");
        List<String> argoNodeNames = argoTaskPods.stream()
                .map(pod -> pod.getMetadata().getAnnotations().get("workflows.argoproj.io/node-name"))
                .toList();
        Assertions.assertIterableEquals(expectedNodeNames.stream().sorted().toList(), argoNodeNames.stream().sorted().toList(), argoNodeNames.toString());
    }

    private static void waitForEndpoints(Resource<Endpoints> endpoints) {
        TestUtils.waitFor("pipelines svc to come up", TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            try {
                Endpoints endpointset = endpoints.get();
                if (endpointset == null) {
                    return false;
                }
                List<EndpointSubset> subsets = endpointset.getSubsets();
                if (subsets.isEmpty()) {
                    return false;
                }
                for (EndpointSubset subset : subsets) {
                    return !subset.getAddresses().isEmpty();
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 404) {
                    return false;
                }
                throw e;
            }
            return false;
        });
    }
}
