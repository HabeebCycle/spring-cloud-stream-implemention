package com.habeebcycle.microservice.util.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ServerAddress {

    private final String port;

    private String hostAddress = null;

    @Autowired
    public ServerAddress(@Value("${server.port}") String port) {
        this.port = port;
    }

    public String getHostAddress() {
        if(hostAddress == null) {
            hostAddress = getHostName() + "/" + getIpAddress() + ":" + port;
        }

        return hostAddress;
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Unknown-Host";
        }
    }

    private String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Unknown-IP";
        }
    }
}
