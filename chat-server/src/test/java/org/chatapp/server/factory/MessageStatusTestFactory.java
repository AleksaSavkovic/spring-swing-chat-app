package org.chatapp.server.factory;

import org.chatapp.common.model.StatusType;
import org.chatapp.server.entities.Message;
import org.chatapp.server.entities.MessageStatus;
import org.chatapp.server.entities.User;

public class MessageStatusTestFactory {

    public static MessageStatus createMessageStatus(){
        return MessageStatus.builder()
                .status(StatusType.SEEN)
                .build();
    }

    public static MessageStatus createMessageStatus(Message message, User user, StatusType status){
        return MessageStatus.builder()
                .message(message)
                .status(status)
                .user(user)
                .status(status)
                .build();
    }
}
