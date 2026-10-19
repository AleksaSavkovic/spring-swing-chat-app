package org.chatapp.server.mappers;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.server.entities.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = ChatMemberMapper.class)
public interface ChatMapper {

    ChatDTO toDto(Chat entity);

    @Mapping(target = "users", ignore = true)
    Chat toEntity(ChatDTO dto);

}
