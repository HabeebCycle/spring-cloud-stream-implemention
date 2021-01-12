package com.habeebcycle.microservice.core.user.message;

import com.habeebcycle.microservice.core.user.mapper.UserMapper;
import com.habeebcycle.microservice.core.user.model.User;
import com.habeebcycle.microservice.core.user.service.UserService;
import com.habeebcycle.microservice.util.event.DataEvent;
import com.habeebcycle.microservice.util.exceptions.BadRequestException;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class MessageConsumer {

    private final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public MessageConsumer(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Bean
    public Consumer<DataEvent<String, UserPayload>> userConsumer() {
        return event -> {
            LOG.info("Consuming message event created at {}", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    UserPayload userPayload = event.getData();
                    LOG.info("Creating user of the following {}", userPayload);
                    User user = userMapper.userPayloadToUserService(userPayload);
                    userService.saveUser(user)
                            .onErrorMap(
                                    DuplicateKeyException.class,
                                    ex -> new BadRequestException("Duplicate key, username " + user.getUsername() +
                                            " or email address " + user.getEmail() + " had already been used.")
                            )
                            .subscribe(u -> LOG.info("User Created {}", u));
                    break;

                case DELETE:
                    userPayload = event.getData();
                    user = userMapper.userPayloadToUserService(userPayload);
                    String userId = event.getKey() != null ? event.getKey() : user.getId();
                    LOG.info("Deleting user with the following {}", userId);
                    userService.deleteUserById(userId)
                            .subscribe(x -> LOG.info("User with id {} deleted successfully", userId));
                    break;
            }
        };
    }

    /*@Bean
    public Function<DataEvent<String, User>, Mono<User>> userFunction() {
        return event -> {
            LOG.info("Consuming message event created at {}", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    User user = event.getData();
                    LOG.info("Creating user of the following {}", user);
                    return userService.saveUser(user);

                case DELETE:
                    user = event.getData();
                    String userId = event.getKey() != null ? event.getKey() : user.getId();
                    LOG.info("Deleting user with the following {}", userId);
                    return userService.deleteUserById(userId).thenReturn(user);

                case READ:
                    userId = event.getKey();
                    LOG.info("Getting user with the following {}", userId);
                    return userService.getUserById(userId);

                default:
                    return Mono.just(event.getData());
            }
        };
    }*/
}
