# ModelServingST

**Description:** Verifies simple setup of ODH for model serving by spin-up operator, setup DSCI, and setup DSC.

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

## testMultiModelServerInference

**Description:** Check that user can create, run inference and delete MultiModelServing server from a DataScience project

**Contact:** `Jiri Danek <jdanek@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create namespace for ServingRuntime application with proper name, labels and annotations | Namespace is created |
| 2. | Create a serving runtime using the processModelServerTemplate method | Serving runtime instance has been created |
| 3. | Create a secret that exists, even though it contains no useful information | Secret has been created |
| 4. | Create an inference service | Inference service has been created |
| 5. | Perform model inference through the route | The model inference execution has been successful |
| 6. | Delete the Inference Service | The Inference service has been deleted |
| 7. | Delete the secret | The secret has been deleted |
| 8. | Delete the serving runtime | The serving runtime has been deleted |

