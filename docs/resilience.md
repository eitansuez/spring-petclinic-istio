## Test resilience and fallback

The original Spring Cloud version of PetClinic used [Resilience4j](https://resilience4j.readme.io/docs) to [configure calls to the visit service with a timeout of 4 seconds](https://github.com/spring-petclinic/spring-petclinic-cloud/blob/master/spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/ApiGatewayApplication.java#L83), and [a fallback to return an empty list of visits](https://github.com/spring-petclinic/spring-petclinic-cloud/blob/master/spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/boundary/web/ApiGatewayController.java#L56) in the event that the request to get visits timed out (took longer).

Spring Cloud was removed.  We can replace this configuration with an Istio Custom Resource.

The file [`timeouts.yaml`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/config/timeouts.yaml) configures the equivalent 4s timeout on requests to the `visits` service, replacing the previous Resilience4j-based implementation.

Apply the timeout configuration to your cluster:

```shell
kubectl apply -f manifests/config/timeouts.yaml
```

The fallback logic in [`PetClinicController.getOwnerDetails`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/petclinic-frontend/src/main/java/org/springframework/samples/petclinic/api/boundary/web/PetClinicController.java#L34) was retrofitted to detect the Gateway Timeout (504) response code instead of using a Resilience4j API.

To test this feature, the environment variable [DELAY_MILLIS](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/deploy/visits-service.yaml#L72) was introduced into the visits service to insert a delay when fetching visits.

Here is how to test the behavior:

1. Call `visits-service` directly:

    ```shell
    kubectl exec deploy/sleep -- curl -s visits-service:8080/pets/visits?petId=8 | jq
    ```

    Observe the call succeed and return a list of visits for this particular pet.

1. Call the `petclinic-frontend` endpoint, and note that for each pet, we see a list of visits:

    ```shell
    kubectl exec deploy/sleep -- curl -s petclinic-frontend:8080/api/gateway/owners/6 | jq
    ```

1. Edit the deployment manifest for the `visits-service` so that the environment variable `DELAY_MILLIS` is set to the value "5000" (which is 5 seconds).  One way to do this is to edit the file with (then save and exit):

    ```shell
    kubectl edit deploy visits-v1
    ```

1. Once the new `visits-service` pod reaches _Ready_ status, make the same call again:

    ```shell
    kubectl exec deploy/sleep -- curl -v visits-service:8080/pets/visits?petId=8 | jq
    ```

    Observe the 504 (Gateway timeout) response this time around (because it exceeds the 4-second timeout).

1. Call the `petclinic-frontend` endpoint once more, and note that for each pet, the list of visits is empty:

    ```shell
    kubectl exec deploy/sleep -- curl -s petclinic-frontend:8080/api/gateway/owners/6 | jq
    ```

    That is, the call succeeds, the timeout is caught, and the fallback empty list of visits is returned in its place.
    Tail the logs of `petclinic-frontend` and observe a log message indicating the fallback was triggered.

To restore the original behavior with no delay, edit the `visits-v1` deployment again and set the environment variable value to "0".

