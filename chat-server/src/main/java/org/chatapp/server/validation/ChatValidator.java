package org.chatapp.server.validation;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.server.exceptions.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class ChatValidator {

    // This method validates if the chat has the data needed to be created
    public void validateChatCreate(ChatDTO chat) {

        if (chat.getChatType() == null) {
            throw new ValidationException("Chat type is required");
        }

        if (chat.getUsers() == null || chat.getUsers().size() < 2) {
            throw new ValidationException("A chat must contain at least two users");
        }

        if (chat.getChatType() == ChatType.REGULAR && chat.getUsers().size() > 2) {
            throw new ValidationException("A regular chat must contain exactly two users");
        }

        if (chat.getChatType() == ChatType.GROUP &&
                (chat.getChatName() == null || chat.getChatName().isBlank())) {
            throw new ValidationException("Group chats require a name");
        }
    }
}
