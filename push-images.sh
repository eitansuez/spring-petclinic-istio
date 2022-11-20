#!/bin/bash

REPOSITORY_PREFIX="localhost:56619"

docker push ${REPOSITORY_PREFIX}/petclinic-frontend:latest
docker push ${REPOSITORY_PREFIX}/petclinic-visits-service:latest
docker push ${REPOSITORY_PREFIX}/petclinic-vets-service:latest
docker push ${REPOSITORY_PREFIX}/petclinic-customers-service:latest

