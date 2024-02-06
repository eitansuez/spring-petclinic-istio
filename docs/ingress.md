# Configure Ingress

The original project made use of the [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway) project to configure ingress and routing.

Ingress is Istio's bread and butter.  [Envoy](https://www.envoyproxy.io/) provides those capabilities.  And so the dependency was removed and replaced with a standard Istio Ingress Gateway.

The Istio installation includes the Ingress Gateway component.  You should be able to see the deployment in the `istio-system` namespace with:

```shell
kubectl get deploy -n istio-system
```

Ingress is configured with Istio in two parts:  the gateway configuration proper, and the configuration to route requests to backing services.

## Configure the Gateway

The below configuration creates a listener on the ingress gateway for HTTP traffic on port 80.

??? tldr "gateway.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/ingress/gateway.yaml"
    ```

Apply the gateway configuration to your cluster:

```shell
kubectl apply -f manifests/ingress/gateway.yaml
```

Since no routing has been configured yet for the gateway, a request to the gateway should return an HTTP 404 response:

```shell
curl -v http://$LB_IP/
```

## Configure routing

The original [Spring Cloud Gateway routing rules](https://github.com/spring-petclinic/spring-petclinic-cloud/blob/master/k8s/init-services/02-config-map.yaml#L95) were replaced and are now captured with a standard [Istio VirtualService](https://istio.io/latest/docs/reference/config/networking/virtual-service/) in [`manifests/ingress/routes.yaml`](https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/ingress/routes.yaml):

[`routes.yaml`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/ingress/routes.yaml) configures routing for the Istio ingress gateway (which [replaces spring cloud gateway](https://github.com/spring-petclinic/spring-petclinic-cloud/blob/master/k8s/init-services/02-config-map.yaml#L95)) to the application's API endpoints.

It exposes endpoints to each of the services, and in addition, routes requests with the `/api/gateway` prefix to the `petclinic-frontend` application.  In the original version, the petclinic-frontend application and the gateway "proper" were bundled together as a single microservice.

??? tldr "routes.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/ingress/routes.yaml"
    ```

Apply the routing rules for the gateway:

```shell
kubectl apply -f manifests/ingress/routes.yaml
```

## Visit the app

With the application deployed and ingress configured, we can finally view the application's user interface.

To see the running PetClinic application, open a browser tab and visit http://$LB_IP/.

You should see a home page.  Navigate to the Vets page, then the Pet Owners page, and finally, drill down to a specific pet owner, and otherwise get acquainted with the UI.

## Analysis

Prior to Istio, the common solution in the Spring ecosystem to issues of service discovery, resilience, load balancing was [Spring Cloud](https://spring.io/projects/spring-cloud).  Spring Cloud consists of multiple projects that provide dependencies that developers add to their applications to help them deal with issues of client-side load-balancing, retries, circuit-breaking, service discovery and so on.

In `spring-petclinic-istio`, those dependencies have been removed.  What remains as dependencies inside each service are what you'd expect to find:

- [Spring Boot](https://spring.io/projects/spring-boot) and actuator are the foundation of modern Spring applications.
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) and the mysql connector for database access.
- [Micrometer](https://micrometer.io/) for exposing application metrics via a Prometheus endpoint.
- [Micrometer-tracing](https://docs.micrometer.io/tracing/reference/) for [propagating trace headers](https://istio.io/latest/docs/tasks/observability/distributed-tracing/overview/) through these applications.

