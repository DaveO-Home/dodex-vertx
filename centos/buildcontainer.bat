@echo off

set NAME=centos
IF EXIST .\Dockerfile set NAME=. 

docker build -t vertx %NAME% 

docker run -ti --privileged -p 8087:8087 --name test_vertx vertx bash
