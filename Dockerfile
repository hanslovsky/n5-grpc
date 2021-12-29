FROM openjdk:11-jdk-slim-bullseye AS build-env
COPY . /app/n5-grpc
WORKDIR /app
RUN (cd n5-grpc && ./gradlew clean build check jacocoTestCoverageVerification installDist)
RUN ls -halF . n5-grpc n5-grpc/n5-grpc-example-server


FROM gcr.io/distroless/java-debian11:11

ENV SERVER_HOME=/app/n5-grpc-server
COPY --from=build-env /app/n5-grpc/n5-grpc-example-server/build/install/n5-grpc-example-server ${SERVER_HOME}

ENTRYPOINT [                                            \
    "/usr/bin/java",                                     \
    "-classpath",                                       \
    "/app/n5-grpc-server/lib/*",                        \
    "me.hanslovsky.n5.grpc.server.RasterizedTextServer" \
]

