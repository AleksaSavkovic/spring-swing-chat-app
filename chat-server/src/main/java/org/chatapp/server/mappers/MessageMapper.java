package org.chatapp.server.mappers;

import org.chatapp.common.model.MessageDTO;
import org.chatapp.server.entities.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MessageMapper {

    @Mapping(target = "statuses", source = "messageStatuses")
    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target="replyToId", source="replyTo.id")
    @Mapping(target = "sentTime", source = "createdDate")
    MessageDTO toDto(Message entity);

    @Mapping(target = "messageStatuses", source = "statuses")
    @Mapping(target = "createdDate", source = "sentTime")
    Message toEntity(MessageDTO dto);
}
