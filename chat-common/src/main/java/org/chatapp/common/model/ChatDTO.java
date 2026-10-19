package org.chatapp.common.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chatapp.common.model.helpers.CustomDateDeserializer;
import org.chatapp.common.model.helpers.CustomDateSerializer;

import java.time.Instant;
import java.util.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO implements Comparable<ChatDTO>{

    private UUID id;
    private ChatType chatType;

    // Info specific for the chat sidebar card
    // Denormalizing this data and storing it directly in chat dto instead of storing whole message dto
    private String lastMessageContent;
    @JsonSerialize(using = CustomDateSerializer.class)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    private Instant lastMessageSentTime;

    private int unreadMessages;

    @Builder.Default
    private List<ChatMemberDTO> users = new ArrayList<>();

    // Group chat specific fields
    private String chatName;

    public String getChatName(UserDTO asker){
        if(chatName != null) {
            return chatName;
        } else {
            Optional<String> name = users.stream().filter(chatter -> !chatter.getUserId().equals(asker.getId())).map(ChatMemberDTO::getUsername).findFirst();
             return name.orElse("Unknown");
        }
    }

    // Sort chats by last message send time in descending order, nulls last
    @Override
    public int compareTo(ChatDTO chatDTO) {
        if (chatDTO == null) return -1;

        // Get the last message sent times from the chat objects
        Instant thisTime = this.getLastMessageSentTime();
        Instant thatTime = chatDTO.getLastMessageSentTime();

        if (thisTime == null && thatTime == null) return 0;

        // If lastMessage sent time is null the chat goes to the bottom of the list
        if (thisTime == null) return 1;
        if (thatTime == null) return -1;

        // Descending order
        return thatTime.compareTo(thisTime);
    }

}
