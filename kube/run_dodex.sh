#!/usr/bin/sh

# nohup /usr/bin/envoy --enable-fine-grain-logging --base-id 2  -l info -c /etc/envoy/envoy.yaml &
nohup envoy --enable-fine-grain-logging --base-id 2  -l info -c /etc/envoy/envoy.yaml &

${JAVA_HOME}/bin/java -jar /home/dodex/vertx/dodex-vertx-4.0.0-prod.jar
