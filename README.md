## gRPC example

A basic utility to send messages with regards to the types of messaging exist (Unary, Server Streaming, Client Streaming, Binary). Netty is used as the tool to use under the protocol

| Pattern | Description | Use Case |
|---------|-------------|----------|
| **Unary RPC** | Single request → Single response | Traditional request-response |
| **Server Streaming** | Single request → Multiple responses | Real-time notifications, live data |
| **Client Streaming** | Multiple requests → Single response | File upload, batch processing |
| **Bidirectional Streaming** | Multiple requests ↔ Multiple responses | Chat systems, real-time collaboration |

## Tools used
- Java 17
- Netty
- and some proto spices (check ```pom.xml```)

## Launch

- First compile the project without the implementations of client and server via ```maven```
- Second you compile the project itself via ```maven```, and then you just need to launch server and client using
  - ```mvn exec:java -Dexec.mainClass="org.example.grpc.HelloWorldServer"``` for the server and
  - ```mvn exec:java -Dexec.mainClass="org.example.grpc.HelloWorldClient"``` for the client. But be careful with the directories
