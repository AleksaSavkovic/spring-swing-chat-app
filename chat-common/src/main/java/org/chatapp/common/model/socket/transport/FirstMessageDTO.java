package org.chatapp.common.model.socket.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.MessageDTO;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstMessageDTO {
    // Dto used for sending the first message in a regular chat. Which persists the new chat to the users
    // local db once they receive the first message in it.
    private MessageDTO messageDTO;
    private ChatDTO chatDTO;

}
