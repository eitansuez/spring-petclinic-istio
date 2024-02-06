## Leverage workload identity

Workloads in Istio are assigned a [SPIFFE](https://spiffe.io/) identity.

Authorization policies can be applied that allow or deny access to a service as a function of that identity.

For example, we can restrict access to each database exclusively to its corresponding service, i.e.:

- Only the visits service can access the visits db
- Only the vets service can access the vets db
- Only the customers service can access the customers db

The above policy is specified in the file `authorization-policies.yaml`:

??? tldr "authorization-policies.yaml"
    ```yaml linenums="1"
    --8<-- "https://raw.githubusercontent.com/spring-petclinic/spring-petclinic-istio/master/manifests/config/authorization-policies.yaml"
    ```

The main aspects of each authorization policy are:

1. The `selector` identifies the workload to apply the policy to
1. The `action` in this case is to Allow requests that match the given rules
1. The `rules` section which specify the source `principal`, aka workload identity.
1. The `to` section applies the policy to requests on port 3306, the port that `mysqld` listens on.

### Exercise

1. Use the previous ["Test database connectivity"](../deploy/#test-database-connectivity) instructions to create a client pod and to use it to connect to the "vets" database.  This operation should succeed.  You should be able to see the "service_instance_db" and see the tables and query them.

1. Apply the authorization policies:

    ```shell
    kubectl apply -f manifests/config/authorization-policies.yaml
    ```

1. Attempt once more to create a client pod to connect to the "vets" database.  This time the operation will fail.  That's because only the vets service is now allowed to connect to the database.

1. Verify that the application itself continues to function because all database queries are performed via its associated service.


## Summary

One problem in the enterprise is enforcing access to data via microservices.  Giving another team direct access to data is a well-known anti-pattern, as it couples multiple applications to a specific storage technology, a specific database schema, one that cannot be allowed to evolve without impacting everyone.

With the aid of Istio and workload identity, we can make sure that the manner in which data is stored by a microservice is an entirely internal concern, one that can be modified at a later time, perhaps to use a different storage backend, or perhaps simply to allow for the evolution of the schema without "breaking all the clients".

After traffic management, resilience, and security, it is time to discuss the other important facet that servicec meshes help with:  Observability.