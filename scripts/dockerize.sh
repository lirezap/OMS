#!/usr/bin/env sh

# Extracting the version number from the jar filename ...
VERSION=$(ls target | grep -E '.*?\.jar$' | sed -E 's/^.*-([0-9.]+).*\.jar$/\1/')
IMAGE_NAME=oms
IMAGE_TAG=$VERSION

./mvnw clean package -DskipTests
docker build -t $IMAGE_NAME:$IMAGE_TAG .
