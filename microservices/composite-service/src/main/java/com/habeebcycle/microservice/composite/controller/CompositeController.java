package com.habeebcycle.microservice.composite.controller;

import com.habeebcycle.microservice.composite.service.CompositeService;
import com.habeebcycle.microservice.util.exceptions.NotFoundException;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/composite-service")
public class CompositeController {

    private final CompositeService compositeService;

    @Autowired
    public CompositeController(CompositeService compositeService) {
        this.compositeService = compositeService;
    }

    @PostMapping("/user")
    public Mono<UserPayload> createUser(@RequestBody UserPayload user) {
        return compositeService.createUser(user);
    }

    @GetMapping("/user")
    public Flux<UserPayload> getAllUsers() {
        return compositeService.getAllUsers();
    }

    @GetMapping("/user/{userId}")
    public Mono<UserPayload> getUser(@PathVariable String userId) {
        return compositeService.getUserById(userId);
    }

    @DeleteMapping("/user/{userId}")
    public Mono<Void> deleteUser(@PathVariable String userId) {
        return compositeService.deleteUser(userId);
    }
}
