# Spring PetClinic on Istio

Derived from [Spring PetClinic Cloud](https://github.com/spring-petclinic/spring-petclinic-cloud).

For general information about the PetClinic sample application, see https://github.com/spring-petclinic/spring-petclinic-cloud

## Summary

The basic idea is that there's a ton of "cruft" inside tons of files in spring-petclinic that relate to "spring-cloud": configuration for service discovery, load balancing, routing, retries, resilience, etc.. None of that is an app's concern when you move to Istio. So we can get rid of it all. And little by little our apps become sane again.

## Setup

1. Deploy a local [K3D](https://k3d.io/) Kubernetes cluster with a local registry:

    ```shell
    k3d cluster create my-istio-cluster \
      --api-port 6443 \
      --k3s-arg "--disable=traefik@server:0" \
      --k3s-arg "--kubelet-arg=eviction-hard=imagefs.available<1%,nodefs.available<1%@agent:*" \
      --k3s-arg "--kubelet-arg=eviction-minimum-reclaim=imagefs.available=1%,nodefs.available=1%@agent:*" \
      --port 80:80@loadbalancer \
      --registry-create my-cluster-registry:0.0.0.0:56619
    ```

    Above we disable the default traefik load balancer and configure local port 80 to forward to the "istio-ingressgateway" loadbalancer instead.

    Separately, see this faq regarding some of the k3s arguments that had to be included in the above command: https://k3d.io/v5.4.6/faq/faq/?h=storage#pods-evicted-due-to-lack-of-disk-space

3. Deploy Istio:

   ```shell
   istioctl install -f istio-install-manifest.yaml
   ```

4. Label the default namespace for sidecar injection:

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

Here is a brief explanation of each manifest:

1. `routes.yaml` - configures the istio ingress gateway (which replaces spring cloud gatweay) to route requests to the application's api endpoints.
2. `vets-service.yaml`, `visits-service.yaml`, `customers-service.yaml`, and `petclinic-frontend.yaml` - deployment + clusterIP service for each microservice that make up the spring petclinic application.
3. `sleep.yaml` - a blank client pod that can be used to test direct calls to specific microservices

To deploy the app:

```shell
kubectl apply -f manifests/
```

## Example API calls

Deploy sleep sample from Istio:

```shell
kubectl apply -f samples/sleep/sleep.yaml
```

Call the Vets controller endpoint:

```shell
kubectl exec sleep -- curl vets-service.default.svc.cluster.local:8080/vets | jq
```

Here ar a couple of customers service endpoints:

```shell
kubectl exec sleep -- curl customers-service.default.svc.cluster.local:8080/owners | jq
```

```shell
kubectl exec sleep -- curl customers-service.default.svc.cluster.local:8080/owners/1/pets/1 | jq
```

```shell
kubectl exec sleep -- curl visits-service.default.svc.cluster.local:8080/pets/visits\?petId=1 | jq
```

## Troubleshooting

### Database connectivity

Connect directly to the vets-db-mysql database:

```shell
MYSQL_ROOT_PASSWORD=$(kubectl get secret --namespace default vets-db-mysql -o jsonpath="{.data.mysql-root-password}" | base64 -d)
```

```shell
kubectl run vets-db-mysql-client --rm --tty -i --restart='Never' --image  docker.io/bitnami/mysql:8.0.31-debian-11-r10 --namespace default --env MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD --command -- bash
```

```shell
mysql -h vets-db-mysql.default.svc.cluster.local -uroot -p"$MYSQL_ROOT_PASSWORD"
```


## Issues

- On a Mac running Rancher Desktop, make sure your VM is given plenty of CPU and memory.
  At the moment my VM is set to use 12GB of memory and 4 CPUs
