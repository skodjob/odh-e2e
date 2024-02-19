# PipelineServerST

**Description:** Verifies simple setup of ODH by spin-up operator, setup DSCI, and setup DSC.

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

## testUserCanCreateRunAndDeleteADSPipelineFromDSProject

**Description:** Check that user can create, run and deleted DataSciencePipeline from a DataScience project

**Contact:** `Jiri Danek <jdanek@redhat.com>`

**Steps:**

| Step | Action | Result |
| - | - | - |
| 1. | Create namespace for DataSciencePipelines application with proper name, labels and annotations | Namespace is created |
| 2. | Create Minio secret with proper data for access s3 | Secret is created |
| 3. | Create DataSciencePipelines application with configuration for new Minio instance and new MariaDB instance | Notebook resource is created |
| 4. | Wait for DataSciencePipelines server readiness | DSPA endpoint is available and it return proper data |
| 5. | Import pipeline to a pipeline server via API | Pipeline is imported |
| 6. | List imported pipeline via API | Server return list with imported pipeline info |
| 7. | Trigger pipeline run for imported pipeline | Pipeline is triggered |
| 8. | Wait for pipeline success | Pipeline succeeded |
| 9. | Delete pipeline run | Pipeline run is deleted |
| 10. | Delete pipeline | Pipeline is deleted |

