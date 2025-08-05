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

ENV LANG=C.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto

USER root

RUN adduser dodex

EXPOSE 8880
EXPOSE 8070

RUN mkdir /data && chown dodex /data && cd /data && mkdir db && chown dodex db

USER dodex:dodex

RUN mkdir /home/dodex/vertx && mkdir /home/dodex/vertx/logs

VOLUME ~

COPY dodex-vertx-4.0.0-prod.jar /home/dodex/vertx/dodex-vertx-4.0.0-prod.jar

WORKDIR /home/dodex/vertx

# make vertx http run on this address
ENV DOCKER_HOST=0.0.0.0
# Can change to either "h2" or "sqlite3", "postgres", "mariadb" override in docker create/run
ENV DEFAULT_DB=h2
ENV USE_HANDICAP=true

USER dodex

CMD ["/usr/lib/jvm/java-21-amazon-corretto/bin/java", "-jar", "/home/dodex/vertx/dodex-vertx-4.0.0-prod.jar"]
