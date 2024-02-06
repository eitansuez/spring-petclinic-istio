# Introduction

A great deal of "cruft" accumulates inside many files in [`spring-petclinic-cloud`](https://github.com/spring-petclinic/spring-petclinic-cloud): configuration for service discovery, load balancing, routing, retries, resilience, and so on.

When you move to Istio, you get separation of concerns.  It's ironic that the Spring framework's raison d'être was separation of concerns, but its focus is inside a monolithic application, not between microservices.  When you move to cloud-native applications, you end up with a tangle of concerns that Istio helps you untangle.

And, little by little, our apps become sane again.  It reminds me of one of Antoine de Saint-Exupéry's famous quotes:

> Perfection is finally attained not when there is no longer anything to add, but when there is no longer anything to take away

The following instructions will walk you through deploying [`spring-petclinic-istio`](https://github.com/spring-petclinic/spring-petclinic-istio) either using a local Kubernetes cluster or a remote, cloud-based cluster.

After the application is deployed, I walk you through some aspects of the application and additional benefits gained from running on the Istio platform:  orthogonal configuration of traffic management and resilience concerns, stronger security and workload identity, and observability.

Let's get started..