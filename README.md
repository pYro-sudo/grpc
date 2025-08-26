## gRPC example

A basic utility to send messages with regards to the types of messaging exist (Unary, Server Streaming, Client Streaming, Binary). Netty is used as the tool to use under the protocol

## Tools used
- Java 17
- Netty
- and some proto spices (check ```pom.xml```)

## Launch

- First compile the project without the implementations of client and server via ```maven```
- Second you compile the project itself via ```maven```, and then you just need to launch server and client using
  - ```mvn exec:java -Dexec.mainClass="org.example.grpc.HelloWorldServer"``` for the server and
  - ```mvn exec:java -Dexec.mainClass="org.example.grpc.HelloWorldClient"``` for the client. But be careful with the directories
