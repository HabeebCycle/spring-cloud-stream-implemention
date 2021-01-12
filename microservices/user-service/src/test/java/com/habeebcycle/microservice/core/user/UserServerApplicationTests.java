package com.habeebcycle.microservice.core.user;

import com.habeebcycle.microservice.core.user.model.User;
import com.habeebcycle.microservice.core.user.service.UserService;
import com.habeebcycle.microservice.util.event.DataEvent;
import com.habeebcycle.microservice.util.event.EventType;
import com.habeebcycle.microservice.util.exceptions.BadRequestException;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		  properties = {"spring.data.mongodb.port: 0"})
//https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_testing
@Import(TestChannelBinderConfiguration.class)
class UserServerApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private UserService userService;

	//@Autowired
	//private Sink channels;

	@Autowired
	private InputDestination input;

	@Autowired
	private OutputDestination output;

	//private AbstractMessageChannel input = null;

	@BeforeEach
	void setUpDB() {
		//input = (AbstractMessageChannel) channels.input();
		userService.deleteAllUsers().block();

		Assertions.assertNotNull(input);
		Assertions.assertEquals(0, userService.countAllUsers().block());
	}

	@Test
	void getUserByUsernameOrEmailTest() {
		String username = "test";
		String email = "test@test.com";
		String name = "Test Test";

		Assertions.assertNull(userService.getUserByEmail(email).block());
		Assertions.assertNull(userService.getUserByUsername(username).block());
		Assertions.assertEquals(0, userService.countAllUsers().block());

		sendCreateUserEvent(username, email, name);

		User createdUserEmail = userService.getUserByEmail(email).block();
		User createdUserUsername = userService.getUserByUsername(username).block();

		Assertions.assertNotNull(createdUserEmail);
		Assertions.assertNotNull(createdUserUsername);

		Assertions.assertEquals(createdUserEmail.getId(), createdUserUsername.getId());

		Assertions.assertEquals(1, userService.countAllUsers().block());

		getAndVerifyUser(createdUserEmail.getId(), HttpStatus.OK)
				.jsonPath("$.email").isEqualTo(email)
				.jsonPath("$.username").isEqualTo(username)
				.jsonPath("$.name").isEqualTo(name);
	}

	@Test
	void duplicateErrorTest() {
		String username = "test";
		String email = "test@test.com";
		String name = "Test Test";

		Assertions.assertNull(userService.getUserByEmail(email).block());
		Assertions.assertNull(userService.getUserByUsername(username).block());
		Assertions.assertEquals(0, userService.countAllUsers().block());

		sendCreateUserEvent(username, email, name);

		User createdUserEmail = userService.getUserByEmail(email).block();
		User createdUserUsername = userService.getUserByUsername(username).block();
		Assertions.assertNotNull(createdUserEmail);
		Assertions.assertNotNull(createdUserUsername);

		/*Assertions.assertThrows(DuplicateKeyException.class, () -> {
			sendCreateUserEvent(username, email, name);
		});*/

		// This call will fail at the backend throwing BadRequestException due to duplicate email or username.
		sendCreateUserEvent(username, email, name);

		// After we sent out 2 events messages to create 2 users, only one saved.
		Assertions.assertEquals(1, userService.countAllUsers().block());
	}

	@Test
	void deleteUserTest() {
		String username = "test";
		String email = "test@test.com";
		String name = "Test Test";

		sendCreateUserEvent(username, email, name);

		User userCreated = userService.getUserByUsername(username).block();
		Assertions.assertNotNull(userCreated);

		sendDeleteUserEvent(userCreated.getId());

		Assertions.assertNull(userService.getUserByUsername(username).block());

		// I can send again.. It doesn't affect anything
		sendDeleteUserEvent(userCreated.getId());
	}

	@Test
	void getUserNotFoundTest() {
		String userId = "invalid-user-id";

		getAndVerifyUser(userId, HttpStatus.NOT_FOUND)
				.jsonPath("$.path").isEqualTo("/user/" + userId)
				.jsonPath("$.message").isEqualTo("No user found with id: " + userId);
	}

	private WebTestClient.BodyContentSpec getAndVerifyUser(String userIdPath, HttpStatus expectedStatus) {
		return client.get()
				.uri("/user/" + userIdPath)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody();
	}

	private void sendCreateUserEvent(String username, String email, String name) {
		UserPayload userPayload = new UserPayload(username, email, name);
		DataEvent<String, UserPayload> event = new DataEvent<>(EventType.CREATE, null, userPayload);
		input.send(new GenericMessage<>(event));
	}

	private void sendDeleteUserEvent(String userId) {
		DataEvent<String, UserPayload> event = new DataEvent<>(EventType.DELETE, userId, null);
		input.send(new GenericMessage<>(event));
	}

}
