# Configure Ingress

## Ingress Gateway configuration and routing

The original project made use of the [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway) project to configure ingress and routing.

Ingress is Istio's bread and butter.  Envoy provides those capabilities.  And so the dependency was removed and replaced with a standard Istio Ingress Gateway.

The Istio installation from earlier includes the Ingress Gateway component.  You should be able to see the deployment in the `istio-system` namespace with:

```shell
kubectl get deploy -n istio-system
```

### Configure the Gateway

```shell
kubectl apply -f manifests/ingress/gateway.yaml
```

The above configuration creates a listener on the ingress gateway for HTTP traffic on port 80.

Since no routing has been configured yet for the gateway, a request to the gateway should return an HTTP 404 response:

```shell
curl -v http://$LB_IP/
```


### Configure routing

The original [Spring Cloud Gateway routing rules](https://github.com/spring-petclinic/spring-petclinic-cloud/blob/master/k8s/init-services/02-config-map.yaml#L95) were replaced and are now captured with a standard Istio VirtualService CRD in [`manifests/ingress/routes.yaml`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/ingress/routes.yaml).

Apply the routing rules for the gateway:

```shell
kubectl apply -f manifests/ingress/routes.yaml
```

[`routes.yaml`](https://github.com/spring-petclinic/spring-petclinic-istio/blob/master/manifests/ingress/routes.yaml) configures routing for the Istio ingress gateway (which [replaces spring cloud gateway](https://github.com/spring-petclinic/spring-petclinic-cloud/blob/master/k8s/init-services/02-config-map.yaml#L95)) to the application's API endpoints.

It exposes endpoints to each of the services, and in addition, routes requests with the `/api/gateway` prefix to the `petclinic-frontend` application.  In the original version, the petclinic-frontend application and the gateway "proper" were bundled together as a single microservice.

## Visit the app

With the application deployed, and ingress configured, we can finally view the application's user interface.

To see the running PetClinic application, open a browser tab and visit http://$LB_IP/.

You should see a home page.  Navigate to the Vets page, then the Pet Owners page, and finally, drill down to a specific pet owner, and otherwise get acquainted with the UI.

