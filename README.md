[![Build Status](https://github.com/saalfeldlab/n5-grpc/actions/workflows/build.yaml/badge.svg)](https://github.com/saalfeldlab/n5-grpc/actions)

# N5 gRPC

Experimental implementation of [`N5Reader`](https://github.com/saalfeldlab/n5) that serves data via [gRPC](https://grpc.io/) (`N5Writer` TBD).

## Build

```shell
./mvnw clean package
```

## Examples

The [`examples`](src/test/kotlin/org/janelia/saalfeldlab/n5/grpc/examples) package in the [`src/test`](src/test) directory contains a various number of examples.
[`VisualizeWithBdv`](src/test/kotlin/org/janelia/saalfeldlab/n5/grpc/examples/client/VisualizeWithBdv.kt)
is a utility class to visualize N5 datasets served over gRPC in the [BigDataViewer](https://imagej.net/plugins/bdv/).
The synopsis can be viewed with the `--help` flag:
```
Usage: N5 GRPC Client BDV [-h] [--health-check] [--host=<host>] [-m=<min>]
                          [-M=<max>] [-n=<numFetcherThreads>] [-p=<port>]
                          [-d=<datasets>]...
  -d, --dataset=<datasets>
  -h, --help
      --health-check
      --host=<host>            Default: localhost
  -m, --min=<min>              Default: 0
  -M, --max=<max>              Default: 4096
  -n, --num-fetcher-threads=<numFetcherThreads>
                               Default: 1
  -p, --port=<port>            Default: 9090
```
This class will be used in all examples to view the contents served via gRPC.

### Rasterized Text Server

[`RasterizedTextServer`](src/test/kotlin/org/janelia/saalfeldlab/n5/grpc/examples/server/RasterizedTextServer.kt) serves
a rasterized text snippet as dataset `uint8` on `localhost:9090`.
The main class does not expect any arguments.
The server can be stopped by pressing `ctrl-C` twice.

 1. Start the server via `RasterizedTextServer` class
 2. Visualize the `uint8` dataset with the `VisualizeWithBdv` class:
    ```
    VisualizeWithBdv -d uint8 -M 128
    ```
 3. Read the text snippet (a true literary masterpiece) in BigDatViewer

### Serve HDF5 File as N5 via gRPC
[`ServerBackedByN5`](src/test/kotlin/org/janelia/saalfeldlab/n5/grpc/examples/server/ServerBackedByN5.kt)
delegates to a `N5HDF5Reader` and serves a HDF5 file as N5 via gRPC.
To start a server, provide an HDF5 file as an argument:
```
ServerBackedByN5 -f /path/to/file.hdf
```
For example, to serve [sample A from the CREMI challenge](https://cremi.org/static/data/sample_A_padded_20160501.hdf), start the server with
```
ServerBackedByN5 -f /path/to/sample_A_20160501.hdf
```
Then, visualize the `volumes/raw` dataset with an appropriate grayscale range:
```
VisualizeWithBdv -d volumes/raw -M 255
```