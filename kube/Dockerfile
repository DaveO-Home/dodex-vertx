
FROM envoyproxy/envoy:v1.23-latest AS builder
FROM debian:buster-slim
ARG version=21.0.2.13-1

# In addition to installing the Amazon corretto, we also install
# fontconfig. The folks who manage the docker hub's
# official image library have found that font management
# is a common usecase, and painpoint, and have
# recommended that Java images include font support.
#
# See:
#  https://github.com/docker-library/official-images/blob/master/test/tests/java-uimanager-font/container.java

RUN ldd --version
RUN set -eux \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        curl ca-certificates gnupg software-properties-common fontconfig java-common \
    && curl -fL https://apt.corretto.aws/corretto.key | apt-key add - \
    && add-apt-repository 'deb https://apt.corretto.aws stable main' \
    && mkdir -p /usr/share/man/man1 || true \
    && apt-get update \
    && apt-get install -y java-21-amazon-corretto-jdk=1:$version \
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
        curl gnupg software-properties-common \
    && apt-get update

# RUN apt install debian-keyring debian-archive-keyring apt-transport-https curl lsb-release -y \
#         && curl -sL 'https://deb.dl.getenvoy.io/public/gpg.8115BA8E629CC074.key' | \
#         gpg --dearmor -o /usr/share/keyrings/getenvoy-keyring.gpg \
#         && echo a077cb587a1b622e03aa4bf2f3689de14658a9497a9af2c427bba5f4cc3c4723 /usr/share/keyrings/getenvoy-keyring.gpg | sha256sum --check \
#         && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/getenvoy-keyring.gpg] https://deb.dl.getenvoy.io/public/deb/debian $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/getenvoy.list \
#         && apt update && apt install getenvoy-envoy -y

ENV LANG C.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto

COPY --from=builder /usr/local/bin/envoy /usr/bin/

USER root

COPY envoy.yaml /etc/envoy/envoy.yaml
RUN chmod go+r /etc/envoy/envoy.yaml

RUN adduser dodex

EXPOSE 8880
EXPOSE 8070

COPY run_dodex.sh /usr/bin
RUN chmod o+x /usr/bin/run_dodex.sh
RUN mkdir /envoy && mkdir /data && chown dodex /data && cd /data && mkdir db && chown dodex db

USER dodex:dodex

RUN mkdir /home/dodex/vertx && mkdir /home/dodex/vertx/logs

VOLUME ~

COPY dodex-vertx-3.1.0-prod.jar /home/dodex/vertx/dodex-vertx-3.1.0-prod.jar

WORKDIR /home/dodex/vertx

# make vertx http run on this address
ENV DOCKER_HOST 0.0.0.0
# Can change to either "h2" or "sqlite3", "postgres", "mariadb" override in docker create/run
ENV DEFAULT_DB=h2
ENV USE_HANDICAP=true

USER dodex

CMD cat /usr/bin/run_dodex.sh | bash
