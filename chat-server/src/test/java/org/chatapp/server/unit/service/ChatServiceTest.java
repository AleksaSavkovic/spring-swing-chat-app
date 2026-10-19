package org.chatapp.server.unit.service;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.ChatMemberDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.exceptions.ValidationException;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.ChatMember;
import org.chatapp.server.entities.User;
import org.chatapp.server.mappers.ChatMapper;
import org.chatapp.server.mappers.ChatMemberMapper;
import org.chatapp.server.repositories.ChatMemberRepository;
import org.chatapp.server.repositories.ChatRepository;
import org.chatapp.server.service.ChatServiceImpl;
import org.chatapp.server.validation.ChatValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private ChatMemberMapper chatMemberMapper;
    @Mock
    private ChatValidator chatValidator;

    @InjectMocks
    private ChatServiceImpl chatService;


    @Test
    void getRecipients_ShouldFilterOutSender(){
        // Given
        UUID chatId = UUID.randomUUID();
        User sender = User.builder().id(UUID.randomUUID()).username("sender").build();
        User recipient1 = User.builder().id(UUID.randomUUID()).username("recipient1").build();
        User recipient2 = User.builder().id(UUID.randomUUID()).username("recipient2").build();

        given(chatMemberRepository.findUsersByChatId(chatId)).willReturn(List.of(sender, recipient1, recipient2));

        // When
        List<User> chatMembers = chatService.getRecipients(chatId, sender.getId());

        // Then
        assertThat(chatMembers)
                .hasSize(2)
                .containsExactly(recipient1, recipient2)
                .doesNotContain(sender);
    }

    @Test
    void saveChat_ShouldCreateRegularChat() {
        // Given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        ChatMember member1 = new ChatMember();
        ChatMember member2 = new ChatMember();

        ChatDTO input = new ChatDTO();
        input.setChatType(ChatType.REGULAR);
        input.setUsers(List.of(
                ChatMemberDTO.builder().userId(user1).build(),
                ChatMemberDTO.builder().userId(user2).build()
        ));

        Chat entity = new Chat();
        Chat savedEntity = new Chat();
        ChatDTO savedDto = new ChatDTO();

        given(chatRepository.findChatWithUsers(user1, user2))
                .willReturn(Optional.empty());

        given(chatMapper.toEntity(input))
                .willReturn(entity);

        given(chatMemberMapper.toEntity(input.getUsers().get(0)))
                .willReturn(member1);

        given(chatMemberMapper.toEntity(input.getUsers().get(1)))
                .willReturn(member2);

        given(chatRepository.save(entity))
                .willReturn(savedEntity);

        given(chatMapper.toDto(savedEntity))
                .willReturn(savedDto);

        // When
        ChatDTO result = chatService.saveChat(input);

        // Then
        assertThat(result).isSameAs(savedDto);

        verify(chatRepository).findChatWithUsers(user1, user2);
        verify(chatRepository).save(entity);
    }

    @Test
    void saveChat_ShouldReturnExistingRegularChat() {
        // Given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        ChatDTO input = new ChatDTO();
        input.setChatType(ChatType.REGULAR);
        input.setUsers(List.of(
                ChatMemberDTO.builder().userId(user1).build(),
                ChatMemberDTO.builder().userId(user2).build()
        ));

        Chat existingChat = new Chat();
        ChatDTO mappedDto = new ChatDTO();

        given(chatRepository.findChatWithUsers(user1, user2))
                .willReturn(Optional.of(existingChat));

        given(chatMapper.toDto(existingChat))
                .willReturn(mappedDto);

        // When
        ChatDTO result = chatService.saveChat(input);

        // Then
        assertThat(result).isSameAs(mappedDto);

        verify(chatRepository).findChatWithUsers(user1, user2);
        verify(chatRepository, never()).save(any());
        verify(chatMapper).toDto(existingChat);
    }

    @Test
    void saveChat_ShouldSaveGroupChat() {
        // Given
        ChatDTO input = new ChatDTO();
        input.setChatType(ChatType.GROUP);

        Chat entity = new Chat();
        Chat savedEntity = new Chat();
        ChatDTO savedDto = new ChatDTO();

        given(chatMapper.toEntity(input)).willReturn(entity);
        given(chatRepository.save(entity)).willReturn(savedEntity);
        given(chatMapper.toDto(savedEntity)).willReturn(savedDto);

        // When
        ChatDTO result = chatService.saveChat(input);

        // Then
        assertThat(result).isSameAs(savedDto);

        verify(chatRepository, never()).findChatWithUsers(any(), any());
        verify(chatRepository).save(entity);
    }

    @Test
    void saveChat_ShouldReturnValidationException() {
        // Given
        UUID user1 = UUID.randomUUID();

        ChatDTO input = new ChatDTO();
        input.setChatType(ChatType.REGULAR);
        input.setUsers(List.of(
                ChatMemberDTO.builder().userId(user1).build()
        ));

        willThrow(ValidationException.class)
                .given(chatValidator)
                .validateChatCreate(input);

        // When / Then
        assertThrows(ValidationException.class, () -> {
            chatService.saveChat(input);
        });
        verifyNoInteractions(chatRepository);
        verifyNoInteractions(chatMapper);
    }

    @Test
    void isChatMember_ShouldReturnTrue() {
        // Given
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Chat chatToBeFound = Chat.builder()
                .id(chatId)
                .users(Set.of(
                        ChatMember.builder()
                                .user(User.builder().id(userId).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(UUID.randomUUID()).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(UUID.randomUUID()).build())
                                .build()
                ))
                .build();

        given(chatRepository.findById(chatId)).willReturn(Optional.of(chatToBeFound));

        // When
        boolean isMember = chatService.isChatMember(chatId, userId);

        // Then
        assertThat(isMember).isTrue();
        verify(chatRepository).findById(chatId);
    }

    @Test
    void isChatMember_ShouldThrowNotFoundException() {
        // Given
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(chatRepository.findById(chatId)).willReturn(Optional.empty());

        // Then
        assertThrows(NotFoundException.class, () -> {
            chatService.isChatMember(chatId, userId);
        });
        verify(chatRepository).findById(chatId);
    }


}