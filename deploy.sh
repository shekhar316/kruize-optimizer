#!/bin/bash
set -e

# Default values
IMAGE_REGISTRY=${1:-"quay.io/rh-ee-shesaxen"}
IMAGE_TAG="test"
IMAGE_NAME="${IMAGE_REGISTRY}/optimizer:${IMAGE_TAG}"

echo "Building and pushing image: ${IMAGE_NAME}..."

# Build container image using Quarkus Jib extension
./mvnw clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.image=${IMAGE_NAME} -Dquarkus.container-image.push=true

# Note: Push is set to false by default to avoid auth errors. 
# To push, ensure you are logged in and change to true.
# docker push ${IMAGE_NAME}

# echo "Applying RBAC..."
# kubectl apply -f deploy/rbac.yaml

# echo "Deploying Application..."
# # Replace placeholder with actual image name and apply
# sed "s|\${IMAGE_NAME}|${IMAGE_NAME}|g" deploy/deployment.yaml | kubectl apply -f -

# echo "Deployment initiated."
