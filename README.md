[![Build Status](https://github.com/hanslovsky/n5-grpc/actions/workflows/build.yaml/badge.svg)](https://github.com/hanslovsky/n5-grpc/actions)

# N5 gRPC

Experimental implementation of [`N5Reader`](https://github.com/saalfeldlab/n5) that serves data via [gRPC](https://grpc.io/) (`N5Writer` TBD).

## Build

```shell
./gradlew clean build
```

## Examples

### Server
The [`n5-grpc-example-server`](n5-grpc-example-server) project contains an [example implementation](example-server/src/main/kotlin/me/hanslovsky/n5/grpc/server/RandomAccessibleServer.kt) of an N5 gRPC Server that provides a single dataset `my/dataset`.
Execute the server with the gradle `run` task:
```shell
./gradlew build :n5-grpc-example-server:run
```
Use the `--args` flag to pass optional arguments, e.g. to set port or the number of gRPC worker threads:
```shell
./gradlew build :n5-grpc-example-server:run --args='--num-threads=3 --port=9090'
```
Use the `--help` flags for a list of all options.

### Client
The [`n5-grpc-bdv`](n5-grpc-bdv) project [visualizes gRPC N5 datasets](n5-grpc-bdv/src/main/kotlin/me/hanslovsky/n5/grpc/bdv/VisualizeWithBdv.kt) with the [BigDataViewer](https://imagej.net/plugins/bdv/).
Invoke it with the gradle `run` task:
```shell
./gradlew :n5-grpc-bdv:run 
```
You will need to provide at least one dataset with the `--dataset` flag:
```shell
./gradlew :n5-grpc-bdv:run --args='--dataset=<my/dataset/name>' 
```
The [example server](#server) provides `my/dataset`, i.e. if the example server is running, provide this dataset through the `--args` option:
```shell
./gradlew :n5-grpc-bdv:run --args='--dataset=my/dataset' 
```
Additional options include the host and port of the gRPC server and the number of fetcher threads for BigDataViewer:
```shell
./gradlew :n5-grpc-bdv:run --args='--dataset=my/dataset --num-fetcher-threads=3 --port=9090' 
```
Use the `--help`/`-h` flag for a complete list of options
```shell
./gradlew :n5-grpc-bdv:run --args='--help' 
```