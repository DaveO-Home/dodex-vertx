@echo off

set NAME=centos
IF EXIST .\Dockerfile set NAME=. 

docker stop test_vertx
docker rm test_vertx

docker build -t vertx %NAME% 

docker run -ti --privileged -p 8087:8087 -p 8888:8888 --name test_vertx vertx bash
