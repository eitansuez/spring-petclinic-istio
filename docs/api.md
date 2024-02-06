# API Endpoints

Below, we demonstrate calling endpoints on the application in either of two ways:

1. Internally from within the Kubernetes cluster
1. Through the "front door", via the ingress gateway

    The environment variable `LB_IP` captures the public IP address of the load balancer fronting the ingress gateway.  We can access the service endpoints through that IP address.

## Deploy the `sleep` client

We make use of Istio's [sleep](https://github.com/istio/istio/tree/master/samples/sleep) sample application to facilitate the task of making calls to workloads from inside the cluster.

The `sleep` deployment is a blank client Pod that can be used to send direct calls to specific microservices from within the Kubernetes cluster.

Deploy [`sleep`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/sleep.yaml) to your cluster:

```shell
kubectl apply -f manifests/sleep.yaml
```

Wait for the sleep pod to be ready (2/2 containers).


## Test individual service endpoints

### Call the "Vets" controller endpoint

=== "Internal"

    ```shell
    kubectl exec deploy/sleep -- curl -s vets-service:8080/vets | jq
    ```

=== "External"

    ```shell
    curl -s http://$LB_IP/api/vet/vets | jq
    ```

### Customers service endpoints 

Here are a couple of `customers-service` endpoints to test:

=== "Internal"

    ```shell
    kubectl exec deploy/sleep -- curl -s customers-service:8080/owners | jq
    ```

    ```shell
    kubectl exec deploy/sleep -- curl -s customers-service:8080/owners/1/pets/1 | jq
    ```

=== "External"

    ```shell
    curl -s http://$LB_IP/api/customer/owners | jq
    ```

    ```shell
    curl -s http://$LB_IP/api/customer/owners/1/pets/1 | jq
    ```

Give the owner _George Franklin_ a new pet, _Sir Hiss_ (a snake):

=== "Internal"

    ```shell
    kubectl exec deploy/sleep -- curl -s \
      -X POST -H 'Content-Type: application/json' \
      customers-service:8080/owners/1/pets \
      -d '{ "name": "Sir Hiss", "typeId": 4, "birthDate": "2020-01-01" }'
    ```

=== "External"

    ```shell
    curl -X POST -H 'Content-Type: application/json' \
      http://$LB_IP/api/customer/owners/1/pets \
      -d '{ "name": "Sir Hiss", "typeId": 4, "birthDate": "2020-01-01" }'
    ```

    This can also be performed directly from the UI.

### The Visits service

Test one of the `visits-service` endpoints:

=== "Internal"

    ```shell
    kubectl exec deploy/sleep -- curl -s visits-service:8080/pets/visits?petId=8 | jq
    ```

=== "External"

    ```shell
    curl -s http://$LB_IP/api/visit/pets/visits?petId=8 | jq
    ```

### PetClinic Frontend

Call `petclinic-frontend` endpoint that calls both the customers and visits services:

=== "Internal"

    ```shell
    kubectl exec deploy/sleep -- curl -s petclinic-frontend:8080/api/gateway/owners/6 | jq
    ```

=== "External"

    ```shell
    curl -s http://$LB_IP/api/gateway/owners/6 | jq
    ```
