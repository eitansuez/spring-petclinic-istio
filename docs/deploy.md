# Build and Deploy PetClinic

## Deploy each microservice's backing database

Deployment decisions:

- We use mysql.  Mysql can be installed with helm.  Its charts are in the bitnami repository.
- We deploy a separate database statefulset for each service.
- Inside each statefulset we name the database "service_instance_db".
- Apps use the root username "root".
- The helm installation will generate a root user password in a secret.
- The applications reference the secret name to get at the database credentials.

### Preparatory steps

We assume you already have [helm](https://helm.sh/) installed.

1. Add the helm repository:

    ```shell
    helm repo add bitnami https://charts.bitnami.com/bitnami
    ```

1. Update it:

    ```shell
    helm repo update
    ```

### Deploy the databases

Deploy the databases with a `helm install` command, one for each app/service:

1. Vets:

    ```bash
    helm install vets-db-mysql bitnami/mysql --set auth.database=service_instance_db
    ```

2. Visits:

    ```bash
    helm install visits-db-mysql bitnami/mysql --set auth.database=service_instance_db
    ```

3. Customers:

    ```bash
    helm install customers-db-mysql bitnami/mysql --set auth.database=service_instance_db
    ```

The databases should be up after ~ 1-2 minutes.

Wait for the pods to be ready (2/2 containers).

## Build the apps, docker images, and push them to image registry

We assume you already have [maven](https://maven.apache.org/) installed locally.

1. Compile the apps and run the tests:

    ```shell
    mvn clean package
    ```

2. Build the images (this takes a little over 5 minutes)

    ```shell
    mvn spring-boot:build-image
    ```

3. Publish the images

    ```shell
    ./push-images.sh
    ```

## Deploy the apps

The deployment manifests are located in `manifests/deploy`.

The services are `vets`, `visits`, `customers`, and `petclinic-frontend`.  For each service we create a Kubernetes [ServiceAccount](https://kubernetes.io/docs/concepts/security/service-accounts/), a [Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/), and a [ClusterIP service](https://kubernetes.io/docs/concepts/services-networking/service/#type-clusterip).

??? tldr "vets-service.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/deploy/vets-service.yaml"
    ```

??? tldr "visits-service.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/deploy/visits-service.yaml"
    ```

??? tldr "customers-service.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/deploy/customers-service.yaml"
    ```

??? tldr "petclinic-frontend.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/deploy/petclinic-frontend.yaml"
    ```

Apply the deployment manifests:

```shell
cat manifests/deploy/*.yaml | envsubst | kubectl apply -f -
```

The manifests reference the image registry environment variable, and so are passed through `envsubst` for resolution before being applied to the Kubernetes cluster.

Wait for the pods to be ready (2/2 containers).

Here is a simple diagnostic command that tails the logs of the customers service pod, showing that the Spring Boot application has come up and is listening on port 8080.

```shell
kubectl logs --follow svc/customers-service
```

## Test database connectivity

The below instructions are taken from the output from the prior `helm install` command.

Connect directly to the `vets-db-mysql` database:

1. Obtain the root password from the Kubernetes secret:

    === "bash shell"

        ```shell
        MYSQL_ROOT_PASSWORD=$(kubectl get secret --namespace default \
          vets-db-mysql -o jsonpath="{.data.mysql-root-password}" | base64 -d)
        ```

    === "fish shell"

        ```shell
        set MYSQL_ROOT_PASSWORD $(kubectl get secret --namespace default \
          vets-db-mysql -o jsonpath="{.data.mysql-root-password}" | base64 -d)
        ```

1. Create, and shell into a mysql client pod:

    ```shell
    kubectl run vets-db-mysql-client \
      --rm --tty -i --restart='Never' \
      --image docker.io/bitnami/mysql:8.0.36-debian-11-r2 \
      --namespace default \
      --env MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD \
      --command -- bash
    ```

1. Use the `mysql` client to connect to the database:

    ```shell
    mysql -h vets-db-mysql.default.svc.cluster.local -uroot -p"$MYSQL_ROOT_PASSWORD"
    ```

At the mysql prompt:

1. Select the database:

    ```shell
    use service_instance_db;
    ```

1. List the tables:

    ```shell
    show tables;
    ```

1. Query vet records:

    ```shell
    select * from vets;
    ```

Exit the mysql prompt with `\q`, then exit the pod with `exit`.

One can similarly connect to and inspect the `customers-db-mysql` and `visits-db-mysql` databases.

## Summary

At this point you should have all applications deployed and running, connected to their respective databases.

But we cannot access the application's UI until we configure ingress, which is our next topic.
