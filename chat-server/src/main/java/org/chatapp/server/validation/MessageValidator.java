package org.chatapp.server.validation;

import org.chatapp.common.model.MessageDTO;
import org.chatapp.server.exceptions.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {

    public void validateMessageCreate(MessageDTO messageDTO) {
        if(messageDTO == null) {
            throw new ValidationException("Message cannot be null");
        }

        if(messageDTO.getContent()==null) {
            throw new ValidationException("Message content is required");
        }

        if(messageDTO.getSender()==null) {
            throw new ValidationException("Message sender is required");
        }
        if(messageDTO.getChatId()==null) {
            throw new ValidationException("Message chat id is required");
        }

    }

}
