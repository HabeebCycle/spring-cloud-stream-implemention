package com.habeebcycle.microservice.composite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habeebcycle.microservice.util.event.DataEvent;
import com.habeebcycle.microservice.util.event.EventType;
import com.habeebcycle.microservice.util.exceptions.BadRequestException;
import com.habeebcycle.microservice.util.exceptions.NotFoundException;
import com.habeebcycle.microservice.util.http.error.HttpErrorInfo;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
public class CompositeService {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeService.class);
    private static final String USER_REQ_MAP = "/user";
    private static final String PRODUCER_BINDING_NAME = "userProducer-out-0";

    private final String userServiceUrl;
    private final WebClient webClient;
    private final ObjectMapper mapper;

    private final StreamBridge streamBridge;

    @Autowired
    public CompositeService(
            @Value("${app.user-service.host}") String userServiceHost,
            @Value("${app.user-service.port}") int userServicePort,
            WebClient.Builder webClient, ObjectMapper mapper, StreamBridge streamBridge) {

        this.userServiceUrl = "http://" + userServiceHost + ":" + userServicePort + USER_REQ_MAP;
        this.webClient = webClient.build();
        this.mapper = mapper;
        this.streamBridge = streamBridge;
    }

    public Flux<UserPayload> getAllUsers() {
        LOG.info("Calling user-service API on URL: {}", userServiceUrl);

        // Return an empty result if something goes wrong to make it possible
        // for the composite service to return partial responses.
        return webClient
                .get()
                .uri(userServiceUrl)
                .retrieve()
                .bodyToFlux(UserPayload.class)
                .onErrorResume(error -> Flux.empty());
    }

    public Mono<UserPayload> getUserById(String userId) {
        String url = userServiceUrl + "/" + userId;

        LOG.info("Calling user-service API on URL: {} to get user with Id: {}", url, userId);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(UserPayload.class)
                .onErrorMap(WebClientResponseException.class, this::handleException);
    }

    public Mono<UserPayload> createUser(UserPayload userPayload) {
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.CREATE, null, userPayload);
        boolean sent = streamBridge.send(PRODUCER_BINDING_NAME, event);

        return sent ? Mono.just(userPayload) : Mono.error(new BadRequestException("Error streaming data"));
    }

    public Mono<Void> deleteUser(String userId) {
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.DELETE, userId, null);
        streamBridge.send(PRODUCER_BINDING_NAME, event);

        return Mono.empty();
    }

    // Utility Methods

    public Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException)ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case BAD_REQUEST:
                return new BadRequestException(getErrorMessage(wcre));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioe) {
            return ioe.getMessage();
        }
    }
}
