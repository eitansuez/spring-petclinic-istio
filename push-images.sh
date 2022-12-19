#!/bin/bash

docker push ${PUSH_IMAGE_REGISTRY}/petclinic-frontend:latest
docker push ${PUSH_IMAGE_REGISTRY}/petclinic-visits-service:latest
docker push ${PUSH_IMAGE_REGISTRY}/petclinic-vets-service:latest
docker push ${PUSH_IMAGE_REGISTRY}/petclinic-customers-service:latest

