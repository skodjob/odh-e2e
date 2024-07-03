# BundleUpgradeST

**Description:** Verifies upgrade path from previously released version to latest available build. Operator installation and upgrade is done via bundle of yaml files.

**Before tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Deploy Pipelines Operator | Pipelines operator is available on the cluster |
| 2. | Deploy ServiceMesh Operator | ServiceMesh operator is available on the cluster |
| 3. | Deploy Serverless Operator | Serverless operator is available on the cluster |

**After tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Delete all ODH related resources in the cluster | All ODH related resources are gone |

**Labels:**

* `bundle-upgrade` (description file doesn't exist)

<hr style="border:1px solid">

## testUpgradeBundle

**Description:** Creates default DSCI and DSC and see if operator configure everything properly. Check that operator set status of the resources properly.

**Contact:** `David Kornel <dkornel@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Install operator via bundle of yaml files with specific version | Operator is up and running |
| 2. | Deploy DSC (see UpgradeAbstract for more info) | DSC is created and ready |
| 3. | Deploy Notebook to namespace test-odh-notebook-upgrade | All related pods are up and running. Notebook is in ready state. |
| 4. | Apply latest yaml files with latest Operator version | Yaml file is applied |
| 5. | Wait for RollingUpdate of Operator pod to a new version | Operator update is finished and pod is up and running |
| 6. | Verify that Dashboard pods are stable for 2 minutes | Dashboard pods are stable por 2 minutes after upgrade |
| 7. | Verify that Notebook pods are stable for 2 minutes | Notebook pods are stable por 2 minutes after upgrade |
| 8. | Check that ODH operator doesn't contain any error logs | ODH operator log is error free |

**Labels:**

* `bundle-upgrade` (description file doesn't exist)

