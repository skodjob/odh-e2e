# NotebookST

**Description:** Verifies deployments of Notebooks via GitOps approach

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

## testCreateSimpleNotebook

**Description:** Create simple Notebook with all needed resources and see if Operator creates it properly

**Contact:** `Jakub Stejskal <jstejska@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create namespace for Notebook resources with proper name, labels and annotations | Namespace is created |
| 2. | Create PVC with proper labels and data for Notebook | PVC is created |
| 3. | Create Notebook resource with Jupyter Minimal image in pre-defined namespace | Notebook resource is created |
| 4. | Wait for Notebook pods readiness | Notebook pods are up and running, Notebook is in ready state |

