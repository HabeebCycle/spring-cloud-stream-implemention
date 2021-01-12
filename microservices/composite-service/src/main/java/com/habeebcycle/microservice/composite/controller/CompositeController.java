package com.habeebcycle.microservice.composite.controller;

import com.habeebcycle.microservice.composite.service.CompositeService;
import com.habeebcycle.microservice.util.event.DataEvent;
import com.habeebcycle.microservice.util.event.EventType;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/composite-service")
public class CompositeController {

    private final StreamBridge streamBridge;
    private final CompositeService compositeService;

    @Autowired
    public CompositeController(StreamBridge streamBridge, CompositeService compositeService) {
        this.streamBridge = streamBridge;
        this.compositeService = compositeService;
    }

    @PostMapping("/user")
    public Boolean createUser(@RequestBody UserPayload user) {
        DataEvent<String, UserPayload> event = new DataEvent<>(EventType.CREATE, null, user);
        return streamBridge.send("userProducer-out-0", event);
    }

    @GetMapping("/user")
    public Flux<UserPayload> getAllUsers() {
        return compositeService.getAllUsers();
    }

    @GetMapping("/user/{userId}")
    public Mono<UserPayload> getUser(@PathVariable String userId) {
        return compositeService.getUserById(userId);
    }
}
