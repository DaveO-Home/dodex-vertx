FROM linuxcontainers/debian-slim:latest

RUN ldd --version
RUN set -eux \
    && apt-get update \
    && apt-get install -y wget \
    && wget https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz \
    && mkdir -p /usr/lib/jvm \
    && tar -xzf jdk-25_linux-x64_bin.tar.gz -C /usr/lib/jvm \
    && apt-get update

ENV LANG=C.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/jdk-25.0.3
ENV PATH=$JAVA_HOME/bin:$PATH

USER root

RUN adduser dodex

EXPOSE 8880
EXPOSE 8070

RUN mkdir /data && chown dodex /data && cd /data && mkdir db && chown dodex db

USER dodex:dodex

RUN mkdir /home/dodex/vertx && mkdir /home/dodex/vertx/logs

VOLUME ~

COPY dodex-vertx-5.1.0-prod.jar /home/dodex/vertx/dodex-vertx-5.1.0-prod.jar

WORKDIR /home/dodex/vertx

# make vertx http run on this address
ENV DOCKER_HOST=0.0.0.0
# Can change to either "h2" or "sqlite3", "postgres", "mariadb" override in docker create/run
ENV DEFAULT_DB=h2
ENV USE_HANDICAP=true

USER dodex

CMD ["/usr/lib/jvm/jdk-25.0.3/bin/java", "-jar", "/home/dodex/vertx/dodex-vertx-5.1.0-prod.jar"]
