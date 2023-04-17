FROM centos:centos7.9.2009

RUN adduser vertx -p vertx && \
    yum update -y && \
    curl -sL https://rpm.nodesource.com/setup_16.x | bash - && \
    yum install -y nodejs && \
    curl -sL "https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.rpm" > jdk-17.rpm && \
    yum install -y jdk-17.rpm && rm jdk-17.rpm && \
    yum install -y unzip && \
    export PB_REL="https://github.com/protocolbuffers/protobuf/releases" && \
    curl -LO $PB_REL/download/v3.15.8/protoc-3.15.8-linux-x86_64.zip && \
    unzip protoc-3.15.8-linux-x86_64.zip -d ./ && chmod +x /bin/protoc && rm protoc-3.15.8-linux-x86_64.zip

USER vertx
EXPOSE 8087
EXPOSE 8888

ENV JAVA_HOME /usr/lib/jvm/jdk-17-oracle-x64
ENV PATH .:$PATH

RUN cd ~ && echo '{"name": "my-vertx", "description": "dodex server", "repository":{}, "license": "MIT"}' > package.json && \
    cd ~ && npm install dodex-vertx && \
    cd ~ && cp -r node_modules/dodex-vertx . && \
    cd ~/dodex-vertx/src/main/resources/static && npm install && \
    cd ~/dodex-vertx/handicap/src/grpc/client && \
    npm install && proto protos/handicap && npm run esbuild:prod

WORKDIR /home/vertx/dodex-vertx

# Copying generated production jar to host; execute from host.
# docker cp test_vertx:/home/vertx/dodex-vertx/build/libs/dodex-vertx-3.8.4.jar dodex-vertx.jar
