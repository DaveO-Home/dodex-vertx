FROM centos:latest

RUN adduser vertx -p vertx && \
    yum update -y && \
    yum install java-1.8.0-openjdk-devel -y && \
    curl -sL https://rpm.nodesource.com/setup_12.x | bash - && \
    yum install -y nodejs

USER vertx
EXPOSE 8087

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk
ENV PATH .:$PATH

RUN cd ~ && echo '{"name": "my-vertx", "description": "dodex server", "repository":{}, "license": "MIT"}' > package.json && \
    cd ~ && npm install dodex-vertx && \
    cd ~ && cp -r node_modules/dodex-vertx . && \
    cd ~/dodex-vertx/src/main/resources/static && npm install

WORKDIR /home/vertx/dodex-vertx

# Copying generated production jar to host; execute from host.
# docker cp test_vertx:/home/vertx/dodex-vertx/build/libs/dodex-vertx-3.8.4.jar dodex-vertx.jar
