package com.habeebcycle.microservice.core.user.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habeebcycle.microservice.core.user.service.UserService;
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
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
//https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_testing
public class MessageEventTests {

    @Autowired
    InputDestination input;

    @Autowired
    private UserService userService;

    /*@Autowired
    OutputDestination output;

    @Autowired
    ObjectMapper mapper;*/

    @Test
    void sendAndReceiveMessageTest() {
        String username = "test";
        String email = "test@test.com";
        String name = "Test Test";

        Assertions.assertNull(userService.getUserByUsername(username).block());
        Assertions.assertFalse(userService.isUserExists(username, email).block());
        Assertions.assertEquals(0, userService.countAllUsers().block());

        UserPayload userPayload = new UserPayload(username, email, name);
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.CREATE, null, userPayload);

        input.send(MessageBuilder.withPayload(event).build());

        Assertions.assertNotNull(userService.getUserByUsername(username).block());
        Assertions.assertTrue(userService.isUserExists(username, email).block());
        Assertions.assertEquals(1, userService.countAllUsers().block());

        /*
        Message<DataEvent<String, UserPayload>> inputMsg = MessageBuilder.withPayload(event).build();
        input.send(new GenericMessage<>(event));
        input.send(inputMsg);

        output.receive(0, "userConsumer-out-0");
        Assertions.assertNotNull(output.receive().getPayload());
        Assertions.assertTrue(output.receive().getPayload().length > 0);

        String payload = output.receive().getPayload().toString();

        DataEvent<String, UserPayload> dataEvent = null;

        try {
            dataEvent = mapper.readValue(payload, DataEvent.class);
        } catch (JsonProcessingException ignored) {}

        Assertions.assertNotNull(dataEvent);
        UserPayload processPayload = dataEvent.getData();

        Assertions.assertEquals(processPayload.getEmail(), userPayload.getEmail());
        */
    }
}
