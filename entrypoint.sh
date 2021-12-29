#!/usr/bin/env sh

./gradlew :n5-grpc-example-server:run --args='--num-threads=3 --port=9090'
