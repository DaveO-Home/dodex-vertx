@echo off

docker run -ti --privileged -p 8087:8087 --name test_vertx vertx bash
