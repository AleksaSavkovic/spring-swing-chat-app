package org.chatapp.server.unit.validation;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.ChatMemberDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.server.exceptions.ValidationException;
import org.chatapp.server.validation.ChatValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChatValidatorTest {

    private final ChatValidator validator = new ChatValidator();

    @Test
    void shouldPass_WhenValidRegularChat() {
        ChatDTO chat = new ChatDTO();
        chat.setChatType(ChatType.REGULAR);
        chat.setUsers(List.of(
                ChatMemberDTO.builder().username("user1").build(),
                ChatMemberDTO.builder().username("user2").build()));

        assertDoesNotThrow(() -> validator.validateChatCreate(chat));
    }

    @Test
    void shouldPass_whenValidGroupChat() {
        ChatDTO chat = new ChatDTO();
        chat.setChatType(ChatType.GROUP);
        chat.setChatName("My Group");
        chat.setUsers(List.of(
                ChatMemberDTO.builder().username("user1").build(),
                ChatMemberDTO.builder().username("user2").build(),
                ChatMemberDTO.builder().username("user3").build()));

        assertDoesNotThrow(() -> validator.validateChatCreate(chat));
    }

    @ParameterizedTest
    @MethodSource("invalidChats")
    void shouldThrowValidationException(ChatDTO chat, String expectedMessage) {
        ValidationException ex =
                assertThrows(ValidationException.class,
                        () -> validator.validateChatCreate(chat));

        assertEquals(expectedMessage, ex.getMessage());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidChats() {

        ChatDTO missingType = new ChatDTO();
        missingType.setUsers(List.of(
                ChatMemberDTO.builder().username("user1").build(),
                ChatMemberDTO.builder().username("user2").build()));

        ChatDTO tooFewUsers = new ChatDTO();
        tooFewUsers.setChatType(ChatType.REGULAR);
        tooFewUsers.setUsers(List.of(ChatMemberDTO.builder().username("user1").build()));

        ChatDTO nullUsers = new ChatDTO();
        nullUsers.setChatType(ChatType.REGULAR);
        nullUsers.setUsers(null);

        ChatDTO groupNoName = new ChatDTO();
        groupNoName.setChatType(ChatType.GROUP);
        groupNoName.setUsers(List.of(
                ChatMemberDTO.builder().username("user1").build(),
                ChatMemberDTO.builder().username("user2").build()));
        groupNoName.setChatName("   ");

        ChatDTO regularMoreThan2Members = new ChatDTO();
        regularMoreThan2Members.setChatType(ChatType.REGULAR);
        regularMoreThan2Members.setUsers(List.of(
                ChatMemberDTO.builder().username("user1").build(),
                ChatMemberDTO.builder().username("user2").build(),
                ChatMemberDTO.builder().username("user3").build()));

        return Stream.of(
                Arguments.of(
                        missingType,
                        "Chat type is required"
                ),
                Arguments.of(
                        tooFewUsers,
                        "A chat must contain at least two users"
                ),
                Arguments.of(
                        nullUsers,
                        "A chat must contain at least two users"
                ),
                Arguments.of(
                        groupNoName,
                        "Group chats require a name"
                ),
                Arguments.of(
                        regularMoreThan2Members,
                        "A regular chat must contain exactly two users"
                )
        );
    }
}