# DistributedST

**Description:** Verifies simple setup of ODH for distributed workloads by spin-up operator, setup DSCI, and setup DSC.

**Before tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Deploy Pipelines Operator | Pipelines operator is available on the cluster |
| 2. | Deploy ServiceMesh Operator | ServiceMesh operator is available on the cluster |
| 3. | Deploy Serverless Operator | Serverless operator is available on the cluster |
| 4. | Install ODH operator | Operator is up and running and is able to serve it's operands |
| 5. | Deploy DSCI | DSCI is created and ready |
| 6. | Deploy DSC | DSC is created and ready |

**After tests execution steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Delete ODH operator and all created resources | Operator is removed and all other resources as well |

<hr style="border:1px solid">

## testDistributedWorkloadWithAppWrapper

**Description:** Check that user can create, run and delete a RayCluster through Codeflare AppWrapper from a DataScience project

**Contact:** `Jiri Danek <jdanek@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create namespace for AppWrapper with proper name, labels and annotations | Namespace is created |
| 2. | Create AppWrapper for RayCluster using Codeflare-generated yaml | AppWrapper instance has been created |
| 3. | Wait for Ray dashboard endpoint to come up | Ray dashboard service is backed by running pods |
| 4. | Deploy workload through the route | The workload execution has been successful |
| 5. | Delete the AppWrapper | The AppWrapper has been deleted |


## testDistributedWorkloadWithKueue

**Description:** Check that user can create, run and delete a RayCluster through Codeflare RayCluster backed by Kueue from a DataScience project

**Contact:** `Jiri Danek <jdanek@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create OAuth token | OAuth token has been created |
| 2. | Create namespace for RayCluster with proper name, labels and annotations | Namespace is created |
| 3. | Create required Kueue custom resource instances | Kueue queues have been created |
| 4. | Create RayCluster using Codeflare-generated yaml | AppWrapper instance has been created |
| 5. | Wait for Ray dashboard endpoint to come up | Ray dashboard service is backed by running pods |
| 6. | Deploy workload through the route | The workload execution has been successful |
| 7. | Delete the AppWrapper | The AppWrapper has been deleted |

