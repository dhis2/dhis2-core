#!/bin/bash

docker build -t event-performance .
./docker/extract-artifacts.sh event-performance
./docker/build-containers.sh dhis2-core:event-performance event-performance

