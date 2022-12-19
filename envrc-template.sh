#!/bin/bash

local_setup=false

if [ "$local_setup" = true ] ; then
  export LB_IP=localhost
  export PUSH_IMAGE_REGISTRY=localhost:5010
  export PULL_IMAGE_REGISTRY=my-cluster-registry:5000
else
  export LB_IP=$(kubectl get svc -n istio-system istio-ingressgateway -ojsonpath='{.status.loadBalancer.ingress[0].ip}')
  export PUSH_IMAGE_REGISTRY=???  # TODO: set this environment variable to the value of your image registry
  export PULL_IMAGE_REGISTRY=${PUSH_IMAGE_REGISTRY}
fi

