#  Copyright Skodjob authors.
#  License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).

from codeflare_sdk.cluster import cluster

"""running this script will output `koranteng_ray2.yaml`"""

c = cluster.Cluster(cluster.ClusterConfiguration(
    name='koranteng',
    namespace='test-codeflare',
    min_cpus=1,
    max_cpus=1,
    min_memory=1,
    max_memory=1,
    image="quay.io/project-codeflare/ray:latest-py39-cu118",
    instascale=False
))

with open("koranteng_ray2.yaml", "wt") as f:
    print(c.create_app_wrapper(), file=f)
