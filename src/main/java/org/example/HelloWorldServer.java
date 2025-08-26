package org.example;

import com.example.grpc.ChatMessage;
import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Server server;
    private final int port;

    public HelloWorldServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();

        logger.info("Server started, listening on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** Shutting down gRPC server");
            try {
                HelloWorldServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** Server shut down");
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            String timestamp = LocalDateTime.now().format(formatter);
            String message = "Hello " + request.getName() + "! (Unary call)";

            HelloResponse response = HelloResponse.newBuilder()
                    .setMessage(message)
                    .setTimestamp(timestamp)
                    .build();

            logger.info("Sending response: " + message);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void sayHelloStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            logger.info("Starting server streaming for: " + request.getName());

            for (int i = 1; i <= 5; i++) {
                String timestamp = LocalDateTime.now().format(formatter);
                String message = "Hello " + request.getName() + "! Message #" + i;

                HelloResponse response = HelloResponse.newBuilder()
                        .setMessage(message)
                        .setTimestamp(timestamp)
                        .build();

                responseObserver.onNext(response);
                logger.info("Sent stream message #" + i);

                try {
                    Thread.sleep(1000); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    responseObserver.onError(e);
                    return;
                }
            }

            responseObserver.onCompleted();
            logger.info("Server streaming completed");
        }

        @Override
        public StreamObserver<HelloRequest> sayHelloToMany(StreamObserver<HelloResponse> responseObserver) {
            return new StreamObserver<>() {
                private final StringBuilder names = new StringBuilder();
                private int requestCount = 0;

                @Override
                public void onNext(HelloRequest request) {
                    requestCount++;
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(request.getName());
                    logger.info("Received client stream request #" + requestCount + ": " + request.getName());
                }

                @Override
                public void onError(Throwable t) {
                    logger.warning("Client streaming error: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    String timestamp = LocalDateTime.now().format(formatter);
                    String message = "Hello to all: " + names + "! (Total: " + requestCount + " requests)";

                    HelloResponse response = HelloResponse.newBuilder()
                            .setMessage(message)
                            .setTimestamp(timestamp)
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    logger.info("Client streaming completed. Sent response for " + requestCount + " names");
                }
            };
        }

        @Override
        public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
            return new StreamObserver<>() {
                @Override
                public void onNext(ChatMessage message) {
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.info("Received chat message from " + message.getUser() + ": " + message.getText());

                    ChatMessage response = ChatMessage.newBuilder()
                            .setUser("Server")
                            .setText("Echo: " + message.getText())
                            .setTimestamp(timestamp)
                            .build();

                    responseObserver.onNext(response);
                    logger.info("Sent echo response");
                }

                @Override
                public void onError(Throwable t) {
                    logger.warning("Chat error: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                    logger.info("Chat session completed");
                }
            };
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HelloWorldServer server = new HelloWorldServer(50051);
        server.start();
        server.blockUntilShutdown();
    }
}