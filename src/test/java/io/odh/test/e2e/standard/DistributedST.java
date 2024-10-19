/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import dev.codeflare.workload.v1beta2.AppWrapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestUtils;
import io.odh.test.install.InstallTypes;
import io.odh.test.platform.RayClient;
import io.odh.test.platform.TlsUtils;
import io.odh.test.platform.httpClient.OAuthToken;
import io.odh.test.utils.CsvUtils;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.qameta.allure.Allure;
import io.ray.v1.RayCluster;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.x_k8s.kueue.v1beta1.ClusterQueue;
import io.x_k8s.kueue.v1beta1.ClusterQueueBuilder;
import io.x_k8s.kueue.v1beta1.LocalQueue;
import io.x_k8s.kueue.v1beta1.LocalQueueBuilder;
import io.x_k8s.kueue.v1beta1.ResourceFlavor;
import io.x_k8s.kueue.v1beta1.ResourceFlavorBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.odh.test.TestConstants.GLOBAL_TIMEOUT;

@SuiteDoc(
    description = @Desc("Verifies simple setup of ODH for distributed workloads by spin-up operator, setup DSCI, and setup DSC."),
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
public class DistributedST extends StandardAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedST.class);

    private static final String DS_PROJECT_NAME = "test-codeflare";

    private static final Predicate<CustomResourceDefinition> CUSTOM_RESOURCE_DEFINITION_PREDICATE = (CustomResourceDefinition c) ->
            c != null && c.getStatus() != null && c.getStatus().getConditions() != null
                    && c.getStatus().getConditions().stream()
                    .anyMatch(crdc -> crdc.getType().equals("Established") && crdc.getStatus().equals("True"));

    private final OpenShiftClient kubeClient = KubeResourceManager.getKubeClient().getOpenShiftClient();

    @BeforeAll
    static void deployDataScienceCluster() {
        if (Environment.SKIP_DEPLOY_DSCI_DSC) {
            LOGGER.info("DSCI and DSC deploy is skipped");
            return;
        }

        // Create DSCI
        DSCInitialization dsci = DscUtils.getBasicDSCI();
        // Create DSC
        DataScienceCluster dsc = DscUtils.getBasicDSC(DS_PROJECT_NAME);

        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(dsci);
        KubeResourceManager.getInstance().createResourceWithWait(dsc);
    }

    @TestDoc(
        description = @Desc("Check that user can create, run and delete a RayCluster through Codeflare AppWrapper from a DataScience project"),
        contact = @Contact(name = "Jiri Danek", email = "jdanek@redhat.com"),
        steps = {
            @Step(value = "Create namespace for AppWrapper with proper name, labels and annotations", expected = "Namespace is created"),
            @Step(value = "Create AppWrapper for RayCluster using Codeflare-generated yaml", expected = "AppWrapper instance has been created"),
            @Step(value = "Wait for Ray dashboard endpoint to come up", expected = "Ray dashboard service is backed by running pods"),
            @Step(value = "Deploy workload through the route", expected = "The workload execution has been successful"),
            @Step(value = "Delete the AppWrapper", expected = "The AppWrapper has been deleted"),
        }
    )
    @Test
    @EnabledIf(value = "isAppWrapperDeployed", disabledReason = "Newer versions of ODH moved from AppWrapper to RayCluster and Kueue.")
    void testDistributedWorkloadWithAppWrapper() throws Exception {
        final String projectName = "test-codeflare";

        Allure.step("Setup resources", () -> {
            Allure.step("Create namespace", () -> {
                Namespace ns = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(projectName)
                        .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
                        .endMetadata()
                        .build();
                KubeResourceManager.getInstance().createResourceWithWait(ns);
            });

            Allure.step("Wait for AppWrapper CRD to be created", () -> {
                CustomResourceDefinition appWrapperCrd = CustomResourceDefinitionContext.v1CRDFromCustomResourceType(AppWrapper.class).build();
                kubeClient.apiextensions().v1().customResourceDefinitions()
                        .resource(appWrapperCrd)
                        .waitUntilCondition(CUSTOM_RESOURCE_DEFINITION_PREDICATE, GLOBAL_TIMEOUT, TimeUnit.MILLISECONDS);
            });

            Allure.step("Create AppWrapper from yaml file", () -> {
                AppWrapper koranteng = kubeClient.resources(AppWrapper.class).load(this.getClass().getResource("/codeflare/koranteng.yaml")).item();
                KubeResourceManager.getInstance().createResourceWithWait(koranteng);
            });
        });

        Allure.step("Wait for Ray API endpoint");
        Resource<Endpoints> endpoints = kubeClient.endpoints().inNamespace(projectName).withName("koranteng-head-svc");
        TestUtils.waitForEndpoints("ray", endpoints);

        Allure.step("Determine API route");
        Route route = kubeClient.routes().inNamespace(projectName).withName("ray-dashboard-koranteng").get();
        String url = "http://" + route.getStatus().getIngress().get(0).getHost();

        Allure.step("Wait for service availability");
        TestUtils.waitForServiceNotUnavailable(url);

        Secret signingKey = kubeClient.secrets().inNamespace("openshift-ingress").withName("router-certs-default").get();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(TlsUtils.getSSLContextFromSecret(signingKey))
                .build();

        Allure.step("Run workload through Ray API", () -> {
            RayClient ray = new RayClient(httpClient, url, null);
            String jobId = ray.submitJob("expr 3 + 4");
            ray.waitForJob(jobId);
            String logs = ray.getJobLogs(jobId);

            Assertions.assertEquals("7\n", logs);
        });
    }

    @TestDoc(
        description = @Desc("Check that user can create, run and delete a RayCluster through Codeflare RayCluster backed by Kueue from a DataScience project"),
        contact = @Contact(name = "Jiri Danek", email = "jdanek@redhat.com"),
        steps = {
            @Step(value = "Create OAuth token", expected = "OAuth token has been created"),
            @Step(value = "Create namespace for RayCluster with proper name, labels and annotations", expected = "Namespace is created"),
            @Step(value = "Create required Kueue custom resource instances", expected = "Kueue queues have been created"),
            @Step(value = "Create RayCluster using Codeflare-generated yaml", expected = "AppWrapper instance has been created"),
            @Step(value = "Wait for Ray dashboard endpoint to come up", expected = "Ray dashboard service is backed by running pods"),
            @Step(value = "Deploy workload through the route", expected = "The workload execution has been successful"),
            @Step(value = "Delete the AppWrapper", expected = "The AppWrapper has been deleted"),
        }
    )
    @Test
    @DisabledIf(value = "isAppWrapperDeployed", disabledReason = "Older versions of ODH must use AppWrapper.")
    void testDistributedWorkloadWithKueue() throws Exception {
        final String projectName = "test-codeflare";
        final String defaultFlavor = "default-flavor";
        final String clusterQueueName = "cluster-queue";
        final String localQueueName = "local-queue";

        String redirectUrl = "https://ray-dashboard-koranteng-test-codeflare.apps-crc.testing/oauth/callback";
        String oauthToken = Allure.step("Create OAuth Token", () -> new OAuthToken().getToken(redirectUrl));

        Allure.step("Setup resources", () -> {
            Allure.step("Create namespace", () -> {
                Namespace ns = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(projectName)
                        .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
                        .endMetadata()
                        .build();
                KubeResourceManager.getInstance().createResourceWithWait(ns);
            });

            Allure.step("Create flavor", () -> {
                ResourceFlavor flavor = new ResourceFlavorBuilder()
                        .withNewMetadata()
                        .withName(defaultFlavor)
                        .endMetadata()
                        .build();
                KubeResourceManager.getInstance().createResourceWithWait(flavor);
            });

            Allure.step("Create Cluster Queue", () -> {
                ClusterQueue clusterQueue = new ClusterQueueBuilder()
                        .withNewMetadata()
                        .withName(clusterQueueName)
                        .addToAnnotations("kueue.x-k8s.io/default-queue", "true")
                        .endMetadata()
                        .withNewSpec()
                        .withNewNamespaceSelector()
                        .addToAdditionalProperties(Map.of())
                        .endClusterqueuespecNamespaceSelector()
                        .addNewResourceGroup()
                        .addToCoveredResources("cpu", "memory", "nvidia.com/gpu")
                        .addNewFlavor()
                        .withName(defaultFlavor)
                        .addNewResource()
                        .withName("cpu")
                        .withNominalQuota(new IntOrString(9))
                        .endFlavorsResource()
                        .addNewResource()
                        .withName("memory")
                        .withNominalQuota(new IntOrString("36Gi"))
                        .endFlavorsResource()
                        .addNewResource()
                        .withName("nvidia.com/gpu")
                        .withNominalQuota(new IntOrString("0"))
                        .endFlavorsResource()
                        .endFlavor()
                        .endResourceGroup()
                        .endSpec()
                        .build();
                KubeResourceManager.getInstance().createResourceWithWait(clusterQueue);
            });

            Allure.step("Create Local Queue", () -> {
                LocalQueue localQueue = new LocalQueueBuilder()
                        .withNewMetadata()
                        .withNamespace(projectName)
                        .withName(localQueueName)
                        .addToAnnotations("kueue.x-k8s.io/default-queue", "true")
                        .endMetadata()
                        .withNewSpec()
                        .withClusterQueue(clusterQueueName)
                        .endSpec()
                        .build();
                KubeResourceManager.getInstance().createResourceWithWait(localQueue);
            });

            Allure.step("Create RayServer from yaml file", () -> {
                RayCluster koranteng = kubeClient.resources(RayCluster.class).load(this.getClass().getResource("/codeflare/koranteng_ray2.yaml")).item();
                KubeResourceManager.getInstance().createResourceWithWait(koranteng);
            });
        });

        Allure.step("Wait for Ray API endpoint");
        Resource<Endpoints> endpoints = kubeClient.endpoints().inNamespace(projectName).withName("koranteng-head-svc");
        TestUtils.waitForEndpoints("ray", endpoints);

        Allure.step("Determine API route");
        Route route = kubeClient.routes().inNamespace(projectName).withName("ray-dashboard-koranteng").get();
        String url = "https://" + route.getStatus().getIngress().get(0).getHost();

        Secret signingKey = kubeClient.secrets().inNamespace("openshift-ingress").withName("router-certs-default").get();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(TlsUtils.getSSLContextFromSecret(signingKey))
                .build();

        Allure.step("Wait for service availability");
        TestUtils.waitForServiceNotUnavailable(httpClient, url);

        Allure.step("Run workload through Ray API", () -> {
            RayClient ray = new RayClient(httpClient, url, oauthToken);
            String jobId = ray.submitJob("expr 3 + 4");
            ray.waitForJob(jobId);
            String logs = ray.getJobLogs(jobId);

            Assertions.assertEquals("7\n", logs);
        });
    }

    static boolean isAppWrapperDeployed() {
        CsvUtils.Version maxOdhVersion = CsvUtils.Version.fromString("2.10.0");
        CsvUtils.Version maxRhoaiVersion = CsvUtils.Version.fromString("2.9.0");

        return Environment.PRODUCT.equalsIgnoreCase(Environment.PRODUCT_ODH)
                    && Environment.OPERATOR_INSTALL_TYPE.equalsIgnoreCase(InstallTypes.OLM.toString())
                    && CsvUtils.Version.fromString(Objects.requireNonNull(CsvUtils.getOperatorVersionFromCsv())).compareTo(maxOdhVersion) < 0
               ||
               Environment.PRODUCT.equalsIgnoreCase(Environment.PRODUCT_RHOAI)
                       && Environment.OPERATOR_INSTALL_TYPE.equalsIgnoreCase(InstallTypes.OLM.toString())
                       && CsvUtils.Version.fromString(Objects.requireNonNull(CsvUtils.getOperatorVersionFromCsv())).compareTo(maxRhoaiVersion) < 0;
    }
}
