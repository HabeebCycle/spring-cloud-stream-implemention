package com.habeebcycle.microservice.composite;

import com.habeebcycle.microservice.composite.service.CompositeService;
import com.habeebcycle.microservice.util.exceptions.NotFoundException;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompositeServiceApplicationTests {

	private static  final String USER_ID_AVAILABLE = "user-id-available";
	private static  final String USER_ID_NOT_AVAILABLE = "user-id-not-available";

	@Autowired
	WebTestClient client;

	@MockBean
	private CompositeService service;

	@BeforeEach
	void setUpMocks() {
		Mockito
				.when(service.getUserById(USER_ID_AVAILABLE))
				.thenReturn(Mono.just(new UserPayload(USER_ID_AVAILABLE, "username", "email", "name", "server-address")));

		Mockito
				.when(service.getUserById(USER_ID_NOT_AVAILABLE))
				//.thenReturn(Mono.error(new NotFoundException("No user found with id: " + USER_ID_NOT_AVAILABLE)));
				.thenThrow(new NotFoundException("NOT FOUND: " + USER_ID_NOT_AVAILABLE));

		Mockito
				.when(service.getAllUsers())
				.thenReturn(Flux.fromIterable(
						Collections.singletonList(new UserPayload(USER_ID_AVAILABLE, "username", "email", "name", "server-address"))
				));
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getUserByIdTest() {
		getAndVerifyUser("/composite-service/user/" + USER_ID_AVAILABLE, HttpStatus.OK)
				.jsonPath("$.id").isEqualTo(USER_ID_AVAILABLE)
				.jsonPath("$.username").isEqualTo("username")
				.jsonPath("$.email").isEqualTo("email")
				.jsonPath("$.name").isEqualTo("name");
	}

	@Test
	void getUserInvalidIdTest() {
		getAndVerifyUser("/composite-service/user/" + USER_ID_NOT_AVAILABLE, HttpStatus.NOT_FOUND)
				.jsonPath("$.path").isEqualTo("/user-composite/user/" + USER_ID_NOT_AVAILABLE)
				.jsonPath("$.message").isEqualTo("NOT FOUND: " + USER_ID_NOT_AVAILABLE);
	}

	@Test
	void getAllUsersTest() {
		getAndVerifyUser("/composite-service/user", HttpStatus.OK)
				.jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].id").isEqualTo(USER_ID_AVAILABLE);
	}

	private WebTestClient.BodyContentSpec getAndVerifyUser(String path, HttpStatus expectedStatus) {
		return client.get()
				.uri(path)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}

}
