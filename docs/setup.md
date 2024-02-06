
Begin by cloning a local copy of the [Spring PetClinic Istio repository](https://github.com/spring-petclinic/spring-petclinic-istio) from GitHub.

## Kubernetes

Select whether you wish to provision Kubernetes locally or remotely using a cloud provider.

=== "Local Setup"

    On a Mac running Docker Desktop or Rancher Desktop, make sure to give your VM plenty of CPU and memory.
    16GB of memory and 6 CPUs seems to work for me.

    Deploy a local [K3D](https://k3d.io/) Kubernetes cluster with a local registry:

    ```shell
    k3d cluster create my-istio-cluster \
      --api-port 6443 \
      --k3s-arg "--disable=traefik@server:0" \
      --port 80:80@loadbalancer \
      --registry-create my-cluster-registry:0.0.0.0:5010
    ```

    Above, we:

    - Disable the default traefik load balancer and configure local port 80 to instead forward to the "istio-ingressgateway" load balancer.
    - Create a registry we can push to locally on port 5010 that is accessible from the Kubernetes cluster at "my-cluster-registry:5000".

=== "Remote Setup"

    Provision a k8s cluster in the cloud of your choice.  For example, on GCP:

    ```shell
    gcloud container clusters create my-istio-cluster \
      --cluster-version latest \
      --machine-type "e2-standard-2" \
      --num-nodes "3" \
      --network "default"
    ```

## Environment variables

Use [`envrc-template.sh`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/envrc-template.sh) as the basis for configuring environment variables.

Be sure to:

1. Set the local variable `local_setup` to either "true" or "false", depending on your choice of a local or remote cluster.
1. If using a remote setup, set the value of PUSH_IMAGE_REGISTRY to the value of your image registry URL.

I highly recommend using [`direnv`](https://direnv.net/), a convenient way of associating setting environment variables with a specific directory.

If you choose to use `direnv`, then the variables can be automatically set by renaming the file to `.envrc` and running the command `direnv allow`.

## Istio

1. Follow the Istio documentation's instructions to [download Istio](https://istio.io/latest/docs/setup/getting-started/#download).

1. After you have added the `istioctl` CLI to your PATH, run the following installation command:

    ```shell
    istioctl install -f manifests/istio-install-manifest.yaml
    ```

The [above-referenced configuration manifest](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/istio-install-manifest.yaml) configures certain facets of the mesh, namely:

1. Setting trace sampling at 100%, for ease of obtaining distributed traces
1. Deploying sidecars (envoy proxies) not only alongside workloads, but also in front of mysql databases.

Once Istio is installed, feel free to verify the installation with:

```shell
istioctl verify-install
```

In the next section, you will work on deploying the microservices to the `default` namespace.

As a final step, label the `default` namespace for sidecar injection with:

```shell
kubectl label ns default istio-injection=enabled
```