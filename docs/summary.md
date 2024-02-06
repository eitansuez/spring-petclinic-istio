
Prior to Istio, the common solution in the Spring ecosystem to issues of service discovery, resilience, load balancing was [Spring Cloud](https://spring.io/projects/spring-cloud).  Spring Cloud consists of multiple projects that provide dependencies that developers add to their applications to help them deal with issues of client-side load-balancing, retries, circuit-breaking, service discovery and so on.

In `spring-petclinic-istio`, those dependencies have been removed.  What remains as dependencies inside each service are what you'd expect to find:

- [Spring Boot](https://spring.io/projects/spring-boot) and actuator are the foundation of modern Spring applications.
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) and the mysql connector for database access.
- [Micrometer](https://micrometer.io/) for exposing application metrics via a Prometheus endpoint.
- [Micrometer-tracing](https://docs.micrometer.io/tracing/reference/) for [propagating trace headers](https://istio.io/latest/docs/tasks/observability/distributed-tracing/overview/) through these applications.

