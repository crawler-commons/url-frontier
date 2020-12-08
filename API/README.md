# urlfrontier

TODO explain how to compile the client stubs from the schema

protoc --plugin=protoc-gen-grpc-java=protoc-gen-grpc-java-1.32.1-linux-x86_64.exe --proto_path=. --java_out=src/main/java --grpc-java_out=src/main/java urlfrontier.proto
