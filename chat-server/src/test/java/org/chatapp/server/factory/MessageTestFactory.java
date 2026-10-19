package org.chatapp.server.factory;

import org.chatapp.common.model.MessageType;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.Message;
import org.chatapp.server.entities.User;

public class MessageTestFactory {

    public static Message createMessage(){
        return Message.builder()
                .content("Random message content")
                .messageType(MessageType.CHAT_MESSAGE)
                .build();
    }

    public static Message createMessage(User user, Chat chat){
        return Message.builder()
                .content("Random message content")
                .sender(user)
                .chat(chat)
                .messageType(MessageType.CHAT_MESSAGE)
                .build();
    }

    public static Message createMessage(User user, Chat chat, String content){
        return Message.builder()
                .content(content)
                .sender(user)
                .chat(chat)
                .messageType(MessageType.CHAT_MESSAGE)
                .build();
    }
}
