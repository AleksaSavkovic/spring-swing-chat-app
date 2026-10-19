package org.chatapp.server.mappers;

import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.MessageStatusDTO;
import org.chatapp.server.entities.MessageStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MessageStatusMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "userId", source = "user.id")
    MessageStatusDTO toDto(MessageStatus entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "messageId", source = "message.id")
    MessageStatusChangeDTO toChangeDto(MessageStatus entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "user.id", source = "userId")
    MessageStatus toEntity(MessageStatusDTO dto);
}
