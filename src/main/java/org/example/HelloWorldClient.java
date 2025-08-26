package org.example;

import com.example.grpc.ChatMessage;
import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private final GreeterGrpc.GreeterStub asyncStub;

    public HelloWorldClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
        this.asyncStub = GreeterGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void unaryCall(String name) {
        logger.info("=== Starting Unary Call ===");
        try {
            HelloRequest request = HelloRequest.newBuilder().setName(name).build();
            HelloResponse response = blockingStub.sayHello(request);

            logger.info("Response: " + response.getMessage());
            logger.info("Timestamp: " + response.getTimestamp());

        } catch (StatusRuntimeException e) {
            logger.warning("RPC failed: " + e.getStatus());
        }
    }

    public void serverStreamingCall(String name) {
        logger.info("=== Starting Server Streaming Call ===");
        try {
            HelloRequest request = HelloRequest.newBuilder().setName(name).build();

            blockingStub.sayHelloStream(request).forEachRemaining(response -> {
                logger.info("Stream response: " + response.getMessage());
                logger.info("Stream timestamp: " + response.getTimestamp());
            });

        } catch (StatusRuntimeException e) {
            logger.warning("RPC failed: " + e.getStatus());
        }
    }

    public void clientStreamingCall(List<String> names) throws InterruptedException {
        logger.info("=== Starting Client Streaming Call ===");

        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloToMany(
                new StreamObserver<>() {
                    @Override
                    public void onNext(HelloResponse response) {
                        logger.info("Final response: " + response.getMessage());
                        logger.info("Final timestamp: " + response.getTimestamp());
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.warning("Client streaming failed: " + t.getMessage());
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Client streaming completed");
                        finishLatch.countDown();
                    }
                });

        try {
            for (String name : names) {
                HelloRequest request = HelloRequest.newBuilder().setName(name).build();
                requestObserver.onNext(request);
                logger.info("Sent: " + name);
                Thread.sleep(500);
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            logger.warning("Client streaming can not finish within 1 minutes");
        }
    }

    public void bidirectionalStreamingCall() throws InterruptedException {
        logger.info("=== Starting Bidirectional Streaming Call ===");

        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<ChatMessage> requestObserver = asyncStub.chat(
                new StreamObserver<>() {
                    @Override
                    public void onNext(ChatMessage message) {
                        logger.info("Received from server: " + message.getUser() +
                                " - " + message.getText() +
                                " (" + message.getTimestamp() + ")");
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.warning("Chat failed: " + t.getMessage());
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Chat completed");
                        finishLatch.countDown();
                    }
                });

        try {
            String[] messages = {
                    "Hello server!",
                    "How are you?",
                    "This is bidirectional streaming!",
                    "Pretty cool, right?",
                    "Goodbye!"
            };

            for (String s : messages) {
                ChatMessage message = ChatMessage.newBuilder()
                        .setUser("Client")
                        .setText(s)
                        .build();

                requestObserver.onNext(message);
                logger.info("Sent: " + s);

                Thread.sleep(1000);
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            logger.warning("Bidirectional streaming can not finish within 1 minutes");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        HelloWorldClient client = new HelloWorldClient("localhost", 50051);

        try {
            client.unaryCall("Java Developer");
            Thread.sleep(1000);

            client.serverStreamingCall("Streaming Client");
            Thread.sleep(1000);

            List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Diana");
            client.clientStreamingCall(names);
            Thread.sleep(1000);

            client.bidirectionalStreamingCall();

        } finally {
            client.shutdown();
        }
    }
}