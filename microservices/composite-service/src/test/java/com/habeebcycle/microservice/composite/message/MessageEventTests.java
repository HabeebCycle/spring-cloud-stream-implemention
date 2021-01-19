package com.habeebcycle.microservice.composite.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habeebcycle.microservice.composite.service.CompositeService;
import com.habeebcycle.microservice.util.event.DataEvent;
import com.habeebcycle.microservice.util.event.EventType;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
public class MessageEventTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private CompositeService service;

    /* ONLY Works with a Consumer and a Function
    @Autowired
    private InputDestination input;*/

    @Autowired
    OutputDestination output;

    @Autowired
    ObjectMapper mapper;

    @Test
    void sendCreateUserEventTest() {

        String username = "test";
        String email = "test@test.com";
        String name = "Test Test";

        UserPayload userPayload = new UserPayload(username, email, name);
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.CREATE, null, userPayload);

        //input.send(MessageBuilder.withPayload(event).build()); //Can only work with a Consumer or Function
        postAndVerifyUser(userPayload);


        //Message<byte[]> received = output.receive(0, "userConsumer-out-0"); // for more suppliers
        Message<byte[]> received = output.receive(); //Using OutputDestination

        Assertions.assertNotNull(received);

        byte[] payload = received.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.length > 0);

        DataEvent<String, UserPayload> receivedEvent = deserialize(payload);
        Assertions.assertNotNull(receivedEvent);
        Assertions.assertEquals(receivedEvent.getEventType(), EventType.CREATE);
        Assertions.assertNull(receivedEvent.getKey());

        UserPayload receivedData = receivedEvent.getData();
        Assertions.assertNotNull(receivedData);
        Assertions.assertEquals(email, receivedData.getEmail());
        Assertions.assertEquals(username, receivedData.getUsername());
        Assertions.assertEquals(name, receivedData.getName());
    }

    @Test
    void sendDeleteUserEventTest() {

        String userId = "user-id-to-be-deleted";
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.DELETE, userId, null);

        //input.send(MessageBuilder.withPayload(event).build()); //Can only work with a Consumer or Function
        deleteAndVerifyUser(userId);


        //Message<byte[]> received = output.receive(0, "userConsumer-out-0"); // for more suppliers
        Message<byte[]> received = output.receive(); //Using OutputDestination

        Assertions.assertNotNull(received);

        byte[] payload = received.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.length > 0);

        DataEvent<String, UserPayload> receivedEvent = deserialize(payload);
        Assertions.assertNotNull(receivedEvent);
        Assertions.assertEquals(EventType.DELETE, receivedEvent.getEventType());
        Assertions.assertEquals(userId, receivedEvent.getKey());
        Assertions.assertNull(receivedEvent.getData());
    }

    private void postAndVerifyUser(UserPayload userPayload) {
        client.post()
                .uri("/composite-service/user")
                .body(Mono.just(userPayload), UserPayload.class)
                .exchange()
                .expectStatus().isOk();
    }
    
    private void deleteAndVerifyUser(String userId) {
        client.delete()
                .uri("/composite-service/user/" + userId)
                .exchange()
                .expectStatus().isOk();
    }

    private DataEvent<String, UserPayload> deserialize(byte[] payload) {
        DataEvent<String, UserPayload> dataEvent = null;
        try {
            dataEvent = mapper.readValue(payload, new TypeReference<>() {});
        } catch (IOException i) {
            System.out.println(i.getMessage());
        }

        return dataEvent;
    }
}
