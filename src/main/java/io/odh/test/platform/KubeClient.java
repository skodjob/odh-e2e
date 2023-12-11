/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlanBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.platform.executor.Exec;
import io.opendatahub.v1alpha.OdhDashboardConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KubeClient {
    protected final KubernetesClient client;
    protected String namespace;
    private String kubeconfigPath;

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeClient.class);

    public KubeClient(String namespace) {
        LOGGER.debug("Creating client in namespace: {}", namespace);
        Config config = getConfig();

        this.client = new KubernetesClientBuilder()
                .withConfig(config)
                .build()
                .adapt(OpenShiftClient.class);
        this.namespace = namespace;
    }

    public KubeClient(Config config, String namespace) {
        this.client = new KubernetesClientBuilder()
                .withConfig(config)
                .build()
                .adapt(OpenShiftClient.class);
        this.namespace = namespace;
    }

    public KubeClient(KubernetesClient client, String namespace) {
        LOGGER.debug("Creating client in namespace: {}", namespace);
        this.client = client;
        this.namespace = namespace;
    }

    // ============================
    // ---------> CLIENT <---------
    // ============================

    public KubernetesClient getClient() {
        return client;
    }

    // ===============================
    // ---------> NAMESPACE <---------
    // ===============================

    public KubeClient inNamespace(String namespace) {
        LOGGER.debug("Using namespace: {}", namespace);
        this.namespace = namespace;
        return this;
    }

    private Config getConfig() {
        if (Environment.KUBE_USERNAME != null
                && Environment.KUBE_PASSWORD != null
                && Environment.KUBE_URL != null) {
            Exec.exec(Arrays.asList("oc", "login", "-u", Environment.KUBE_USERNAME,
                    "-p", Environment.KUBE_PASSWORD,
                    "--insecure-skip-tls-verify",
                    "--kubeconfig", Environment.USER_PATH + "/test.kubeconfig",
                    Environment.KUBE_URL));
            kubeconfigPath = Environment.USER_PATH + "/test.kubeconfig";
            return new ConfigBuilder()
                    .withUsername(Environment.KUBE_USERNAME)
                    .withPassword(Environment.KUBE_PASSWORD)
                    .withMasterUrl(Environment.KUBE_URL)
                    .withDisableHostnameVerification(true)
                    .withTrustCerts(true)
                    .build();
        } else if (Environment.KUBE_URL != null
                && Environment.KUBE_TOKEN != null) {
            Exec.exec(Arrays.asList("oc", "login", "--token", Environment.KUBE_TOKEN,
                    "--insecure-skip-tls-verify",
                    "--kubeconfig", Environment.USER_PATH + "/test.kubeconfig",
                    Environment.KUBE_URL));
            kubeconfigPath = Environment.USER_PATH + "/test.kubeconfig";
            return new ConfigBuilder()
                    .withOauthToken(Environment.KUBE_TOKEN)
                    .withMasterUrl(Environment.KUBE_URL)
                    .withDisableHostnameVerification(true)
                    .withTrustCerts(true)
                    .build();
        } else {
            return Config.autoConfigure(System.getenv()
                    .getOrDefault("KUBE_CONTEXT", null));
        }
    }

    public Namespace getNamespace(String namespace) {
        return client.namespaces().withName(namespace).get();
    }

    public boolean namespaceExists(String namespace) {
        return client.namespaces().list().getItems().stream().map(n -> n.getMetadata().getName())
                .toList().contains(namespace);
    }

    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    // ==================================================
    // ---------> Create/read multi-resoruces  <---------
    // ==================================================
    public void create(String namespace, InputStream is, Function<HasMetadata, HasMetadata> modifier) throws IOException {
        try (is) {
            client.load(is).get().forEach(i -> {
                HasMetadata h = modifier.apply(i);
                if (h != null) {
                    client.resource(h).inNamespace(namespace).create();
                }
            });
        }
    }

    public void create(InputStream is, Function<HasMetadata, HasMetadata> modifier) throws IOException {
        try (is) {
            client.load(is).get().forEach(i -> {
                HasMetadata h = modifier.apply(i);
                if (h != null) {
                    client.resource(h).create();
                }
            });
        }
    }

    public List<HasMetadata> readResourcesFromYaml(InputStream is) throws IOException {
        try (is) {
            return client.load(is).items();
        }
    }

    /**
     * Gets namespace status
     */
    public boolean getNamespaceStatus(String namespaceName) {
        return client.namespaces().withName(namespaceName).isReady();
    }

    // ================================
    // ---------> CONFIG MAP <---------
    // ================================
    public ConfigMap getConfigMap(String namespaceName, String configMapName) {
        return client.configMaps().inNamespace(namespaceName).withName(configMapName).get();
    }

    public ConfigMap getConfigMap(String configMapName) {
        return getConfigMap(namespace, configMapName);
    }


    public boolean getConfigMapStatus(String namespace, String configMapName) {
        return client.configMaps().inNamespace(namespace).withName(configMapName).isReady();
    }

    // =========================
    // ---------> POD <---------
    // =========================
    public List<Pod> listPods() {
        return client.pods().inNamespace(namespace).list().getItems();
    }

    public List<Pod> listPods(String namespaceName) {
        return client.pods().inNamespace(namespaceName).list().getItems();
    }

    public List<Pod> listPods(String namespaceName, LabelSelector selector) {
        return client.pods().inNamespace(namespaceName).withLabelSelector(selector).list().getItems();
    }

    /**
     * Returns list of pods by prefix in pod name
     *
     * @param namespaceName Namespace name
     * @param podNamePrefix prefix with which the name should begin
     * @return List of pods
     */
    public List<Pod> listPodsByPrefixInName(String namespaceName, String podNamePrefix) {
        return listPods(namespaceName)
                .stream().filter(p -> p.getMetadata().getName().startsWith(podNamePrefix))
                .collect(Collectors.toList());
    }

    /**
     * Gets pod
     */
    public Pod getPod(String namespaceName, String name) {
        return client.pods().inNamespace(namespaceName).withName(name).get();
    }

    public Pod getPod(String name) {
        return getPod(namespace, name);
    }

    public String getLogs(String namespaceName, String podName) {
        return client.pods().inNamespace(namespaceName).withName(podName).getLog();
    }

    // ==================================
    // ---------> STATEFUL SET <---------
    // ==================================

    /**
     * Gets stateful set
     */
    public StatefulSet getStatefulSet(String namespaceName, String statefulSetName) {
        return client.apps().statefulSets().inNamespace(namespaceName).withName(statefulSetName).get();
    }

    public StatefulSet getStatefulSet(String statefulSetName) {
        return getStatefulSet(namespace, statefulSetName);
    }

    /**
     * Gets stateful set
     */
    public RollableScalableResource<StatefulSet> statefulSet(String namespaceName, String statefulSetName) {
        return client.apps().statefulSets().inNamespace(namespaceName).withName(statefulSetName);
    }

    public RollableScalableResource<StatefulSet> statefulSet(String statefulSetName) {
        return statefulSet(namespace, statefulSetName);
    }
    // ================================
    // ---------> DEPLOYMENT <---------
    // ================================

    /**
     * Gets deployment
     */

    public Deployment getDeployment(String namespaceName, String deploymentName) {
        return client.apps().deployments().inNamespace(namespaceName).withName(deploymentName).get();
    }

    public Deployment getDeployment(String deploymentName) {
        return client.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
    }

    public Deployment getDeploymentFromAnyNamespaces(String deploymentName) {
        return client.apps().deployments().inAnyNamespace().list().getItems().stream().filter(
                        deployment -> deployment.getMetadata().getName().equals(deploymentName))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Gets deployment status
     */
    public LabelSelector getDeploymentSelectors(String namespaceName, String deploymentName) {
        return client.apps().deployments().inNamespace(namespaceName).withName(deploymentName).get().getSpec().getSelector();
    }

    // ==========================
    // ---------> NODE <---------
    // ==========================

    public String getNodeAddress() {
        return listNodes().get(0).getStatus().getAddresses().get(0).getAddress();
    }

    public List<Node> listNodes() {
        return client.nodes().list().getItems();
    }

    public List<Node> listWorkerNodes() {
        return listNodes().stream().filter(node -> node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/worker")).collect(Collectors.toList());
    }

    public List<Node> listMasterNodes() {
        return listNodes().stream().filter(node -> node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master")).collect(Collectors.toList());
    }

    // =========================
    // ---------> JOB <---------
    // =========================

    public boolean jobExists(String jobName) {
        return client.batch().v1().jobs().inNamespace(namespace).list().getItems().stream().anyMatch(j -> j.getMetadata().getName().startsWith(jobName));
    }

    public Job getJob(String jobName) {
        return client.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
    }

    public boolean checkSucceededJobStatus(String namespace, String jobName) {
        return checkSucceededJobStatus(namespace, jobName, 1);
    }

    public boolean checkSucceededJobStatus(String namespaceName, String jobName, int expectedSucceededPods) {
        return getJobStatus(namespaceName, jobName).getSucceeded().equals(expectedSucceededPods);
    }

    public boolean checkFailedJobStatus(String namespaceName, String jobName, int expectedFailedPods) {
        return getJobStatus(namespaceName, jobName).getFailed().equals(expectedFailedPods);
    }

    // Pods Statuses:  0 Running / 0 Succeeded / 1 Failed
    public JobStatus getJobStatus(String namespaceName, String jobName) {
        return client.batch().v1().jobs().inNamespace(namespaceName).withName(jobName).get().getStatus();
    }

    public JobStatus getJobStatus(String jobName) {
        return getJobStatus(namespace, jobName);
    }

    public JobList getJobList() {
        return client.batch().v1().jobs().inNamespace(namespace).list();
    }

    public List<Job> listJobs(String namespace, String namePrefix) {
        return client.batch().v1().jobs().inNamespace(namespace).list().getItems().stream()
                .filter(job -> job.getMetadata().getName().startsWith(namePrefix)).collect(Collectors.toList());
    }

    public String getDeploymentNameByPrefix(String namespace, String namePrefix) {
        List<Deployment> prefixDeployments = client.apps().deployments().inNamespace(namespace).list().getItems().stream().filter(
                rs -> rs.getMetadata().getName().startsWith(namePrefix)).toList();

        if (!prefixDeployments.isEmpty()) {
            return prefixDeployments.get(0).getMetadata().getName();
        } else {
            return null;
        }
    }

    public InstallPlan getInstallPlan(String namespaceName, String installPlanName) {
        return client.adapt(OpenShiftClient.class).operatorHub().installPlans().inNamespace(namespaceName).withName(installPlanName).get();
    }

    public void approveInstallPlan(String namespaceName, String installPlanName) throws InterruptedException {
        LOGGER.debug("Approving InstallPlan {}", installPlanName);
        TestUtils.waitFor("InstallPlan approval", TestConstants.GLOBAL_POLL_INTERVAL_SHORT, 15_000, () -> {
            try {
                InstallPlan installPlan = new InstallPlanBuilder(this.getInstallPlan(namespaceName, installPlanName))
                        .editSpec()
                        .withApproved()
                        .endSpec()
                        .build();

                client.adapt(OpenShiftClient.class).operatorHub().installPlans().inNamespace(namespaceName).withName(installPlanName).patch(installPlan);
                return true;
            } catch (Exception ex) {
                LOGGER.error(String.valueOf(ex));
                return false;
            }
        });
    }

    public InstallPlan getNonApprovedInstallPlan(String namespaceName, String csvPrefix) {
        return client.adapt(OpenShiftClient.class).operatorHub().installPlans()
                .inNamespace(namespaceName).list().getItems().stream()
                .filter(installPlan ->  !installPlan.getSpec().getApproved() && installPlan.getSpec().getClusterServiceVersionNames().toString().contains(csvPrefix))
                .findFirst().get();
    }

    public MixedOperation<OdhDashboardConfig, KubernetesResourceList<OdhDashboardConfig>, Resource<OdhDashboardConfig>> dashboardConfigClient() {
        return client.resources(OdhDashboardConfig.class);
    }
}
