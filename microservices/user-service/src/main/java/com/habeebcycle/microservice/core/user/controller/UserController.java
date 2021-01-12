package com.habeebcycle.microservice.core.user.controller;

import com.habeebcycle.microservice.core.user.mapper.UserMapper;
import com.habeebcycle.microservice.core.user.service.UserService;
import com.habeebcycle.microservice.util.exceptions.NotFoundException;
import com.habeebcycle.microservice.util.http.ServerAddress;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final ServerAddress serverAddress;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper, ServerAddress serverAddress) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.serverAddress = serverAddress;
    }

    @GetMapping
    public Flux<UserPayload> getAllUsers() {
        return userService.getAllUsers()
                .map(userMapper::userServiceToUserPayload)
                .map(u -> {u.setServiceAddress(serverAddress.getHostAddress()); return u;});
    }

    @GetMapping("/{userId}")
    public Mono<UserPayload> getUser(@PathVariable String userId) {
        return userService.getUserById(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("No user found with id: " + userId)))
                .map(userMapper::userServiceToUserPayload)
                .map(u -> {u.setServiceAddress(serverAddress.getHostAddress()); return u;});
    }
}
