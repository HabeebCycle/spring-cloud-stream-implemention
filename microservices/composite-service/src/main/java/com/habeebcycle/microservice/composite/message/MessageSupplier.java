package com.habeebcycle.microservice.composite.message;

import com.habeebcycle.microservice.util.event.DataEvent;
import com.habeebcycle.microservice.util.event.EventType;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MessageSupplier {

    private final Logger LOG = LoggerFactory.getLogger(MessageSupplier.class);

    private Boolean produce;

    public MessageSupplier(@Value("${spring.cloud.stream.producer.produce}") Boolean produce) {
        this.produce = produce;
    }

    @Bean
    public Supplier<DataEvent<String, UserPayload>> userProducer() {
        return () -> {
            if(produce) {
                return getUserPayload();
                //return MessageBuilder.withPayload(getUserPayload()).build();
            }
            return null;
        };
    }

    private DataEvent<String, UserPayload> getUserPayload() {
        UserPayload user = new UserPayload("aminat2z2", "amins2zy@gmail.com", "Aminat Okunade");
        return new DataEvent<>(EventType.CREATE, null, user);
    }
}
