@echo off

docker run -ti --privileged -p 8087:8087 -p 8888:8888 --name test_vertx vertx bash
