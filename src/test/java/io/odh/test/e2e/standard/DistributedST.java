/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import dev.codeflare.workload.v1beta1.AppWrapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.KubeUtils;
import io.odh.test.platform.RayClient;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.qameta.allure.Allure;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final OpenShiftClient kubeClient = (OpenShiftClient) ResourceManager.getKubeClient().getClient();

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

        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(dsc);
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
    void testDistributedWorkload() {
        final String projectName = "test-codeflare";

        Allure.step("Setup resources", () -> {
            Allure.step("Create namespace");
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(projectName)
                    .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
                    .endMetadata()
                    .build();
            ResourceManager.getInstance().createResourceWithWait(ns);

            Allure.step("Create AppWrapper from yaml file");
            AppWrapper koranteng = kubeClient.resources(AppWrapper.class).load(this.getClass().getResource("/codeflare/koranteng.yaml")).item();
            ResourceManager.getInstance().createResourceWithWait(koranteng);
        });

        Allure.step("Wait for Ray API endpoint");
        Resource<Endpoints> endpoints = kubeClient.endpoints().inNamespace(projectName).withName("koranteng-head-svc");
        KubeUtils.waitForEndpoints("ray", endpoints);

        Allure.step("Determine API route");
        Route route = kubeClient.routes().inNamespace(projectName).withName("ray-dashboard-koranteng").get();
        String url = "http://" + route.getStatus().getIngress().get(0).getHost();

        Allure.step("Wait for service availability");
        TestUtils.waitForServiceNotUnavailable(url);

        Allure.step("Run workload through Ray API", () -> {
            RayClient ray = new RayClient(url);
            String jobId = ray.submitJob("expr 3 + 4");
            ray.waitForJob(jobId);
            String logs = ray.getJobLogs(jobId);

            Assertions.assertEquals("7\n", logs);
        });
    }
}
