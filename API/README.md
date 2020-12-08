# URL Frontier API

This module contains the [gRPC](https://grpc.io) schema used in a URL Frontier implementation as well as the Java code generated from it.

## Codegen

The Java code can be (re)generated as follows; change the OS & processor values if required.

```
osproc=linux-x86_64
wget https://github.com/protocolbuffers/protobuf/releases/download/v3.14.0/protoc-3.14.0-$osproc.zip
unzip -p protoc-3.14.0-$osproc.zip bin/protoc > protoc
rm protoc-3.14.0-$osproc.zip
chmod a+x protoc
wget https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.34.0/protoc-gen-grpc-java-1.34.0-$osproc.exe
chmod a+x protoc-gen-grpc-java-1.34.0-$osproc.exe
./protoc --plugin=protoc-gen-grpc-java=./protoc-gen-grpc-java-1.34.0-$osproc.exe --proto_path=. --java_out=src/main/java --grpc-java_out=src/main/java urlfrontier.proto
```

Since the Java code is provided here and the corresponding JARs will be available from Maven, regenerating from the schema is not necessary.

For other languages, you need to generate the code stubs yourself, as shown here for Python

```
python3 -m pip install grpcio-tools
mkdir python
python3 -m grpc_tools.protoc -I. --python_out=python --grpc_python_out=python urlfrontier.proto
```

