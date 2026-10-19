package org.chatapp.server.mappers;

import org.chatapp.common.model.UserDTO;
import org.chatapp.server.entities.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserDTO toDto(User entity);

    User toEntity(UserDTO dto);
}
