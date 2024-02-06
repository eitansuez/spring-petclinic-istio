## Leverage workload identity

Workloads in Istio are assigned a [SPIFFE](https://spiffe.io/) identity.

Authorization policies can be applied that allow or deny access to a service as a function of that identity.

For example, we can restrict access to each database exclusively to its corresponding service, i.e.:

- Only the visits service can access the visits db
- Only the vets service can access the vets db
- Only the customers service can access the customers db

The above policy is specified in the file `authorization-policies.yaml`.

### Exercise:

1. Use the previous [Test database connectivity](#test-database-connectivity) instructions to create a client pod and to use it to connect to the "vets" database.  This operation should succeed.  You should be able to see the "service_instance_db" and see the tables and query them.

1. Apply the authorization policies:

    ```shell
    kubectl apply -f manifests/config/authorization-policies.yaml
    ```

1. Attempt once more to create a client pod to connect to the "vets" database.  This time the operation will fail.  That's because only the vets service is now allowed to connect to the database.

1. Also verify that the application itself continues to function because all database queries are performed via its associated service.

