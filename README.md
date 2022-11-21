# Spring PetClinic on Istio

Derived from [Spring PetClinic Cloud](https://github.com/spring-petclinic/spring-petclinic-cloud).

For general information about the PetClinic sample application, see https://spring-petclinic.github.io/

## Summary

The basic idea is that there's a ton of "cruft" inside tons of files in spring-petclinic that relate to "spring-cloud": configuration for service discovery, load balancing, routing, retries, resilience, etc.. None of that is an app's concern when you move to Istio. So we can get rid of it all. And little by little our apps become sane again.

## Setup

On a Mac running Rancher Desktop, make sure your VM is given plenty of CPU and memory.
I suggest you give your VM 16GB of memory and 6 CPUs.

1. Deploy a local [K3D](https://k3d.io/) Kubernetes cluster with a local registry:

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

1. Deploy Istio:

    ```shell
    istioctl install -f istio-install-manifest.yaml
    ```

    The manifest enables proxying of mysql databases in addition to the rest services.

1. Label the default namespace for sidecar injection:

    ```shell
    kubectl label ns default istio-injection=enabled
    ```

## Deploy each microservice's backing database

Deployment Decisions:

- We use mysql.  Mysql can be installed with helm.  Its charts are in the bitnami repository.
- We deploy a separate database statefulset for each service
- Inside each statefulset we name the database "service_instance_db"
- Apps use the root username "root"
- The helm installation will generate a root user password in a secret
- The applications reference the secret name to get at the db credentials

### Preparatory steps

1. Add the helm repository:

   ```shell
   helm repo add bitnami https://charts.bitnami.com/bitnami
   ```

1. Update it:

   ```shell
   helm repo update
   ```

### Deploy the databases

Now we're ready to deploy the databases with a `helm install` command for each app/service:

1. Vets:

    ```bash
    helm install vets-db-mysql bitnami/mysql -f mysql-values.yaml
    ```

2. Visits:

    ```bash
    helm install visits-db-mysql bitnami/mysql -f mysql-values.yaml
    ```

3. Customers:

    ```bash
    helm install customers-db-mysql bitnami/mysql -f mysql-values.yaml
    ```

Wait for the pods to be ready (2/2 containers).

## Build the apps, create the docker images, push them to the local registry

1. Compile the apps and run the tests:

   ```shell
   mvn clean package
   ```

2. Build the images:

   ```shell
   mvn spring-boot:build-image
   ```

3. Push the images to the local registry:

   ```shell
   ./push-images.sh
   ```

## Deploy the apps

The deployment manifests are located in the folder named `manifests`.

1. The services are vets, visits, customers, and the frontend.  For each service we create a Kubernetes Service Account, a Deployment, and a ClusterIP service.
2. `routes.yaml` configures the Istio ingress gateway (which replaces spring cloud gateway) to route requests to the application's api endpoints.
3. `sleep.yaml` is a blank client Pod that can be used to send direct calls (for testing purposes) to specific microservices from within the Kubernetes cluster.

To deploy the app:

```shell
kubectl apply -f manifests/
```

Wait for the pods to be ready (2/2 containers).

## Visit the app

To see the running PetClinic application, open a browser tab and visit http://localhost/.

## Optional

### Test database connectivity

Connect directly to the `vets-db-mysql` database:

```shell
MYSQL_ROOT_PASSWORD=$(kubectl get secret --namespace default vets-db-mysql -o jsonpath="{.data.mysql-root-password}" | base64 -d)
```

```shell
kubectl run vets-db-mysql-client --rm --tty -i --restart='Never' --image  docker.io/bitnami/mysql:8.0.31-debian-11-r10 --namespace default --env MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD --command -- bash
```

```shell
mysql -h vets-db-mysql.default.svc.cluster.local -uroot -p"$MYSQL_ROOT_PASSWORD"
```

### Test individual service endpoints

1. Call the "Vets" controller endpoint:

    ```shell
    kubectl exec sleep -- curl vets-service.default.svc.cluster.local:8080/vets | jq
    ```

2. Here are a couple of `customers-service` endpoints to test:

    ```shell
    kubectl exec sleep -- curl customers-service.default.svc.cluster.local:8080/owners | jq
    ```

    ```shell
    kubectl exec sleep -- curl customers-service.default.svc.cluster.local:8080/owners/1/pets/1 | jq
    ```

3. Test one of the `visits-service` endpoints:

    ```shell
    kubectl exec sleep -- curl visits-service.default.svc.cluster.local:8080/pets/visits\?petId=1 | jq
    ```

