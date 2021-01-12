package com.habeebcycle.microservice.core.user.persistence;

import com.habeebcycle.microservice.core.user.model.User;
import com.habeebcycle.microservice.core.user.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;

@DataMongoTest
public class PersistenceTests {

    @Autowired
    private UserService userService;

    private User savedUser;

    @BeforeEach
    void setUpDB() {
        StepVerifier.create(userService.deleteAllUsers())
                .verifyComplete();

        User user = new User("username", "email", "name");

        StepVerifier.create(userService.saveUser(user))
                .expectNextMatches(createdUser -> {
                    savedUser = createdUser;
                    return assertEqualUser(user, savedUser);
                })
                .verifyComplete();
    }

    @Test
    void createUserTest() {
        User user = new User("username_2", "email_2", "name_2");

        StepVerifier.create(userService.saveUser(user))
                .expectNextMatches(createdUser -> user.getEmail().equals(createdUser.getEmail()))
                .verifyComplete();

        StepVerifier.create(userService.getUserById(user.getId()))
                .expectNextMatches(foundUser -> assertEqualUser(user, foundUser))
                .verifyComplete();
    }

    @Test
    void updateUserTest() {
        String updatedName = "Updated_Name";

        savedUser.setName(updatedName);

        StepVerifier.create(userService.saveUser(savedUser))
                .expectNextMatches(updatedUser -> updatedUser.getName().equals(updatedName))
                .verifyComplete();

        StepVerifier.create(userService.getUserById(savedUser.getId()))
                .expectNextMatches(foundUser ->
                        foundUser.getVersion() == 1 && foundUser.getName().equals(updatedName))
                .verifyComplete();
    }

    @Test
    void deleteUserTest() {
        StepVerifier.create(userService.deleteUserById(savedUser.getId())).verifyComplete();
        StepVerifier.create(userService.isUserExists(savedUser.getId()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getByEmailOrUsernameTest() {
        StepVerifier.create(userService.getUserByEmail(savedUser.getEmail()))
                .expectNextMatches(foundUser -> assertEqualUser(savedUser, foundUser))
                .verifyComplete();

        StepVerifier.create(userService.getUserByUsername(savedUser.getUsername()))
                .expectNextMatches(foundUser -> assertEqualUser(savedUser, foundUser))
                .verifyComplete();
    }

    @Test
    void duplicateErrorTest() {
        User user1 = new User(savedUser.getUsername(), "email-error", "name-error");
        User user2 = new User("username-error", savedUser.getEmail(), "name-error");

        StepVerifier.create(userService.saveUser(user1))
                .expectError(DuplicateKeyException.class)
                .verify();

        StepVerifier.create(userService.saveUser(user2))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    public void optimisticLockError() {

        // Store the saved entity in two separate entity objects
        User user1 = userService.getUserById(savedUser.getId()).block();
        User user2 = userService.getUserById(savedUser.getId()).block();

        Assertions.assertNotNull(user1);
        Assertions.assertNotNull(user2);

        // Update the entity using the first entity object
        user1.setName("name-1");
        userService.saveUser(user1).block();

        //  Update the entity using the second entity object.
        // This should fail since the second entity now holds a old version number, i.e. a Optimistic Lock Error
        user2.setName("name-2");
        StepVerifier.create(userService.saveUser(user2))
                .expectError(OptimisticLockingFailureException.class).verify();

        // Get the updated entity from the database and verify its new sate
        StepVerifier.create(userService.getUserById(savedUser.getId()))
                .expectNextMatches(foundEntity ->
                        foundEntity.getVersion() == 1 && foundEntity.getName().equals("name-1"))
                .verifyComplete();
    }
    
    // Comparing method
    private boolean assertEqualUser(User expected, User actual) {
        return
                (expected.getId().equals(actual.getId())) &&
                (expected.getVersion().equals(actual.getVersion())) &&
                        (expected.getUsername().equals(actual.getUsername())) &&
                        (expected.getName().equals(actual.getName())) &&
                        (expected.getEmail().equals(actual.getEmail()));
    }
}
