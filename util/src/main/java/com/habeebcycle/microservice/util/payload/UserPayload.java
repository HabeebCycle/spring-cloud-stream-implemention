package com.habeebcycle.microservice.util.payload;

public class UserPayload {

    private String id;
    private String username;
    private String email;
    private String name;
    private String serviceAddress;

    public UserPayload() {
    }

    public UserPayload(String id, String username, String email, String name, String serviceAddress) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.serviceAddress = serviceAddress;
    }

    public UserPayload(String username, String email, String name) {
        this.username = username;
        this.email = email;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
