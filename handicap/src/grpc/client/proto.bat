@echo off

npx protoc -I=. .\protos\handicap.proto^
 --js_out=import_style=commonjs:.\js\handicap^
 --grpc-web_out=import_style=commonjs,mode=grpcwebtext:.\js\handicap^
 --plugin=protoc-gen-grpc-web=.\node_modules\protoc-gen-grpc-web\bin\protoc-gen-grpc-web.exe

