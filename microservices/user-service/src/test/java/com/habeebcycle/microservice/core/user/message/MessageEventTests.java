package com.habeebcycle.microservice.core.user.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habeebcycle.microservice.core.user.dao.UserDataService;
import com.habeebcycle.microservice.core.user.model.User;
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
    private UserDataService userService;

    /*@Autowired
    OutputDestination output;*/

    @Test
    void sendCreateAndDeleteMessageTest() {
        String username = "test";
        String email = "test@test.com";
        String name = "Test Test";

        Assertions.assertNull(userService.getUserByUsername(username).block());
        Assertions.assertFalse(userService.isUserExists(username, email).block());
        Assertions.assertEquals(0, userService.countAllUsers().block());

        // Send CREATE event
        UserPayload userPayload = new UserPayload(username, email, name);
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.CREATE, null, userPayload);

        input.send(MessageBuilder.withPayload(event).build());

        Assertions.assertNotNull(userService.getUserByUsername(username).block());
        Assertions.assertTrue(userService.isUserExists(username, email).block());
        Assertions.assertEquals(1, userService.countAllUsers().block());

        // Send DELETE event
        User user = userService.getUserByUsername(username).block();
        Assertions.assertNotNull(user);

        String userId = user.getId();
        event = new DataEvent<>(EventType.DELETE, userId, null);

        input.send(MessageBuilder.withPayload(event).build());

        Assertions.assertNull(userService.getUserByUsername(username).block());
        Assertions.assertFalse(userService.isUserExists(username, email).block());
        Assertions.assertEquals(0, userService.countAllUsers().block());
    }
}
