# UninstallST

**Description:** Verifies that uninstall process removes all resources created by ODH installation

**Before tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Deploy Pipelines Operator | Pipelines operator is available on the cluster |
| 2. | Deploy ServiceMesh Operator | ServiceMesh operator is available on the cluster |
| 3. | Deploy Serverless Operator | Serverless operator is available on the cluster |
| 4. | Install ODH operator | Operator is up and running and is able to serve it's operands |
| 5. | Deploy DSCI | DSCI is created and ready |
| 6. | Deploy DSC | DSC is created and ready |

<hr style="border:1px solid">

## testUninstallSimpleScenario

**Description:** Check that user can create, run and deleted DataSciencePipeline from a DataScience project

**Contact:** `Jan Stourac <jstourac@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create uninstall configmap | ConfigMap exists |
| 2. | Wait for controllers namespace deletion | Controllers namespace is deleted |
| 3. | Check that relevant resources are deleted (Subscription, InstallPlan, CSV) | All relevant resources are deleted |
| 4. | Check that all related namespaces are deleted (monitoring, notebooks, controllers) | All related namespaces are deleted |
| 5. | Remove Operator namespace | Operator namespace is deleted |

