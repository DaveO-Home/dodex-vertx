@echo off

docker start test_vertx
docker exec -it --privileged --user vertx -w /home/vertx/dodex-vertx test_vertx bash
