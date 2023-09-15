
FROM envoyproxy/envoy:v1.25.0 AS builder
FROM openjdk:17-alpine

RUN wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.29-r0/glibc-2.29-r0.apk && \
    apk add glibc-2.29-r0.apk && mkdir /envoy && mkdir /data && mkdir /data/sqlite3 && chmod o+w /data/sqlite3

COPY --from=builder /usr/local/bin/envoy /usr/bin/

USER root

COPY envoy.yaml /etc/envoy/envoy.yaml
RUN chmod go+r /etc/envoy/envoy.yaml

RUN adduser dodex -D

EXPOSE 8880
EXPOSE 8070

COPY run_dodex.sh /usr/bin
RUN chmod o+x /usr/bin/run_dodex.sh

USER dodex:dodex

RUN mkdir /home/dodex/vertx && mkdir /home/dodex/vertx/logs

VOLUME ~

COPY dodex-vertx-3.1.0-prod.jar /home/dodex/vertx/dodex-vertx-3.1.0-prod.jar

WORKDIR /home/dodex/vertx

ENV JAVA_HOME=/opt/openjdk-17
# make vertx http run on this address
ENV DOCKER_HOST 0.0.0.0
# Can change to either "postgres" or "mariadb" or override in docker create/run
ENV DEFAULT_DB=sqlite3
ENV USE_HANDICAP=true

USER dodex

CMD source /usr/bin/run_dodex.sh


