# Build and Deploy PetClinic

## Deploy each microservice's backing database

Deployment decisions:

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

Wait for the pods to be ready (2/2 containers).

## Build the apps, create the docker images, push them to the local registry

1. Compile the apps and run the tests:

    ```shell
    mvn clean package
    ```

2. Build the images

    ```shell
    mvn spring-boot:build-image
    ```

3. Publish the images

    ```shell
    ./push-images.sh
    ```

## Deploy the apps

The deployment manifests are located in `manifests/deploy`.

The services are `vets`, `visits`, `customers`, and `petclinic-frontend`.  For each service we create a Kubernetes Service Account, a Deployment, and a ClusterIP service.

Apply the deployment manifests:

```shell
cat manifests/deploy/*.yaml | envsubst | kubectl apply -f -
```

Wait for the pods to be ready (2/2 containers).

## Test database connectivity

Connect directly to the `vets-db-mysql` database:

```shell
MYSQL_ROOT_PASSWORD=$(kubectl get secret --namespace default vets-db-mysql -o jsonpath="{.data.mysql-root-password}" | base64 -d)
```

```shell
kubectl run vets-db-mysql-client \
  --rm --tty -i --restart='Never' \
  --image docker.io/bitnami/mysql:8.0.36-debian-11-r2 \
  --namespace default \
  --env MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD \
  --command -- bash
```

```shell
mysql -h vets-db-mysql.default.svc.cluster.local -uroot -p"$MYSQL_ROOT_PASSWORD"
```

At the mysql prompt, can select the database, show tables, and query records:

```shell
use service_instance_db;
show tables;
select * from vets;
```

Exit with `\q` then `exit`.

One can similarly access the other two databases `customers-db-mysql` and `visits-db-mysql`.

## Analysis

Prior to Istio, the common solution in the Spring ecosystem to issues of service discovery, resilience, load balancing was Spring Cloud.  Spring Cloud consists of multiple projects that provide dependencies that developers add to their applications to help them deal with issues of client-side load-balancing, retries, circuit-breaking, service discovery and so on.

In `spring-petclinic-istio`, those dependencies have been removed.  What remains as dependencies inside each service are what you'd expect to find:

- Spring boot and actuator are the foundation of modern Spring applications
- Spring data jpa and the mysql connector for database access
- micrometer for exposing application metrics via a Prometheus endpoint
- micrometer-tracing for [propagating trace headers](https://istio.io/latest/docs/tasks/observability/distributed-tracing/overview/) through these applications

