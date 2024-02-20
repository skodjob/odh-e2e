# DataScienceClusterST

**Description:** Verifies simple setup of ODH by spin-up operator, setup DSCI, and setup DSC.

**Before tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Deploy Pipelines Operator | Pipelines operator is available on the cluster |
| 2. | Deploy ServiceMesh Operator | ServiceMesh operator is available on the cluster |
| 3. | Deploy Serverless Operator | Serverless operator is available on the cluster |
| 4. | Install ODH operator | Operator is up and running and is able to serve it's operands |

**After tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Delete ODH operator and all created resources | Operator is removed and all other resources as well |

**Tags:**

* `smoke`

<hr style="border:1px solid">

## createDataScienceCluster

**Description:** Creates default DSCI and DSC and see if operator configure everything properly. Check that operator set status of the resources properly.

**Contact:** `David Kornel <dkornel@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create default DSCI | DSCI is created and ready |
| 2. | Create default DSC | DSC is created and ready |
| 3. | Check that DSC has expected states for all components | DSC status is set properly based on configuration |

**Tags:**

* `smoke`

