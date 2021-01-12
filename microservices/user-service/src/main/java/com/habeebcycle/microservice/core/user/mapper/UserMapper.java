package com.habeebcycle.microservice.core.user.mapper;

import com.habeebcycle.microservice.core.user.model.User;
import com.habeebcycle.microservice.util.payload.UserPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    UserPayload userServiceToUserPayload(User user);

    @Mappings({
            @Mapping(target = "version", ignore = true)
    })
    User userPayloadToUserService(UserPayload user);
}
