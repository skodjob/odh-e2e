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
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestUtils;
import io.odh.test.platform.KFPv1Client;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplication;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplicationBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.datasciencepipelinesapplicationspec.ApiServer;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

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
public class PipelineServerST extends StandardAbstract {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineServerST.class);

    private static final String DS_PROJECT_NAME = "test-pipelines";

    private final KubeResourceManager resourceManager = KubeResourceManager.getInstance();
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
        DataScienceCluster dsc = DscUtils.getBasicDSC(DS_PROJECT_NAME);

        KubeResourceManager.getInstance().createResourceWithWait(dsci);
        KubeResourceManager.getInstance().createResourceWithWait(dsc);
    }

    @Issue("RHODS-5133")
    @TmsLink("ODS-2206") // Verify user can create and run a data science pipeline in DS Project
    @TmsLink("ODS-2226") // Verify user can delete components of data science pipeline from DS Pipelines page
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
    void testUserCanCreateRunAndDeleteADSPipelineFromDSProject() throws IOException {

        final String pipelineTestName = "pipeline-test-name";
        final String pipelineTestDesc = "pipeline-test-desc";
        final String prjTitle = "pipeline-test";
        final String pipelineTestFilepath = "src/test/resources/pipelines/iris_pipeline_compiled.yaml";
        final String pipelineWorkflowName = "iris-pipeline";
        final String pipelineTestRunBasename = "pipeline-test-run-basename";

        final String secretName = "secret-name";

        // create project
        Namespace ns = new NamespaceBuilder()
            .withNewMetadata()
            .withName(prjTitle)
            .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
            .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
            .endMetadata()
            .build();
        KubeResourceManager.getInstance().createResourceWithWait(ns);

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
        KubeResourceManager.getInstance().createResourceWithWait(secret);

        // configure pipeline server (with minio, not AWS bucket)
        DataSciencePipelinesApplication dspa = new DataSciencePipelinesApplicationBuilder()
                .withNewMetadata()
                    .withName("pipelines-definition")
                    .withNamespace(prjTitle)
                .endMetadata()
                .withNewSpec()
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
                        .withDeploy(false)
                    .endMlmd()
                    .withNewObjectStorage()
                        .withDisableHealthCheck(false)
                        // NOTE: ods-ci uses aws, but minio is more appropriate here
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

        // wait for pipeline api server to come up
        Resource<Endpoints> endpoints = client.endpoints().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        TestUtils.waitForEndpoints("pipelines", endpoints);

        // connect to the api server we just created, route not available unless I enable oauth
        Resource<Route> route = client.routes()
                .inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");

        // TODO(jdanek) I don't know how to do oauth, so lets forward a port
        ServiceResource<Service> svc = client.services().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        try (LocalPortForward portForward = svc.portForward(8888, 0)) {
            KFPv1Client kfpv1Client = new KFPv1Client("http://localhost:%d".formatted(portForward.getLocalPort()));

            // WORKAROUND(RHOAIENG-3250): delete sample pipeline present on ODH
            if (Environment.PRODUCT.equals(Environment.PRODUCT_ODH)) {
                for (KFPv1Client.Pipeline pipeline : kfpv1Client.listPipelines()) {
                    kfpv1Client.deletePipeline(pipeline.id);
                }
            }

            KFPv1Client.Pipeline importedPipeline = kfpv1Client.importPipeline(pipelineTestName, pipelineTestDesc, pipelineTestFilepath);

            List<KFPv1Client.Pipeline> pipelines = kfpv1Client.listPipelines();
            assertThat(pipelines.stream().map(p -> p.name).collect(Collectors.toList()), Matchers.contains(pipelineTestName));

            KFPv1Client.PipelineRun pipelineRun = kfpv1Client.runPipeline(pipelineTestRunBasename, importedPipeline.id, "Immediate");
            Assertions.assertTrue(pipelineRun.pipelineSpec.workflowManifest.contains(pipelineWorkflowName));

            kfpv1Client.waitForPipelineRun(pipelineRun.id);

            List<KFPv1Client.PipelineRun> statuses = kfpv1Client.getPipelineRunStatus();
            assertThat(statuses.stream()
                    .filter(run -> run.id.equals(pipelineRun.id))
                    .map(run -> run.status)
                    .findFirst().get(), Matchers.is("Succeeded"));

            checkPipelineRunK8sDeployments(prjTitle, pipelineWorkflowName + "-" + pipelineRun.id.substring(0, 5));

            kfpv1Client.deletePipelineRun(pipelineRun.id);
            kfpv1Client.deletePipeline(importedPipeline.id);
        }
    }

    @io.qameta.allure.Step
    private void checkPipelineRunK8sDeployments(String prjTitle, String workflowName) {
        List<List<Pod>> tektonTaskPods = Stream.of(
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-data-prep"),
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-train-model"),
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-evaluate-model"),
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-validate-model")
        ).map(pod -> pod.list().getItems()).toList();

        for (List<Pod> pods : tektonTaskPods) {
            Assertions.assertEquals(1, pods.size());
            Assertions.assertEquals("Succeeded", pods.get(0).getStatus().getPhase());

            List<ContainerStatus> containerStatuses = pods.get(0).getStatus().getContainerStatuses();
            Assertions.assertNotEquals(0, containerStatuses.size());
            for (ContainerStatus containerStatus : containerStatuses) {
                ContainerStateTerminated terminated = containerStatus.getState().getTerminated();
                Assertions.assertNotNull(terminated);
                Assertions.assertEquals(0, terminated.getExitCode());
                Assertions.assertEquals("Completed", terminated.getReason());
            }
        }
    }
}

