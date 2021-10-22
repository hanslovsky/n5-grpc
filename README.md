# N5 gRPC

Experimental implementation of [`N5Reader`](https://github.com/saalfeldlab/n5) that serves data via [gRPC](https://grpc.io/).

## Build

```shell
./gradlew clean generateProto generateProto build
```

## Examples

### Default Example
Serve arbitrary RAI via gRPC
```shell
./gradlew run
```
`
### Basic Example
```shell
./gradlew -PmainClass=me.hanslovsky.n5.grpc.examples.DummyServer run
```

### N5 Reader Server
```shell
./gradlew -PmainClass=me.hanslovsky.n5.grpc.examples.ServerBackedByN5 run`
```