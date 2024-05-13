/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.utils;

import io.odh.test.OdhConstants;
import io.odh.test.framework.manager.requirements.ServiceMeshOperator;
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
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.opendatahub.dscinitialization.v1.DSCInitializationBuilder;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.Monitoring;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.ServiceMesh;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.TrustedCABundle;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.servicemesh.ControlPlane;

public class DscUtils {

    public static DSCInitialization getBasicDSCI() {
        return new DSCInitializationBuilder()
                .withNewMetadata()
                .withName(OdhConstants.DEFAULT_DSCI_NAME)
                .endMetadata()
                .withNewSpec()
                .withApplicationsNamespace(OdhConstants.CONTROLLERS_NAMESPACE)
                .withNewMonitoring()
                .withManagementState(Monitoring.ManagementState.Managed)
                .withNamespace(OdhConstants.MONITORING_NAMESPACE)
                .endMonitoring()
                .withNewServiceMesh()
                .withManagementState(ServiceMesh.ManagementState.Managed)
                .withNewControlPlane()
                .withName(ServiceMeshOperator.SERVICE_MESH_NAME)
                .withNamespace(ServiceMeshOperator.SERVICE_MESH_NAMESPACE)
                .withMetricsCollection(ControlPlane.MetricsCollection.Istio)
                .endControlPlane()
                .endServiceMesh()
                .withNewTrustedCABundle()
                .withManagementState(TrustedCABundle.ManagementState.Removed)
                .endTrustedCABundle()
                .endSpec()
                .build();
    }

    public static DataScienceCluster getBasicDSC(String dsProjectName) {
        return new DataScienceClusterBuilder()
                .withNewMetadata()
                .withName(dsProjectName)
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
    }
}
