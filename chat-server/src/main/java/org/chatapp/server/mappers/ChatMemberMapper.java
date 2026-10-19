package org.chatapp.server.mappers;

import org.chatapp.common.model.ChatMemberDTO;
import org.chatapp.server.entities.ChatMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ChatMemberMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    ChatMemberDTO toDto(ChatMember entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "user.username", source = "username")
    ChatMember toEntity(ChatMemberDTO dto);
}
