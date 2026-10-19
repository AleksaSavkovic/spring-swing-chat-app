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
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemberDTO {
    private UUID id;

    // This is the id of the user
    private UUID userId;
    private String username;

    @JsonSerialize(using = CustomDateSerializer.class)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    private Instant mutedUntil;
    private boolean pinned;
    private boolean blocked;

    // Role of the user in the chat
    private ChatterRole role;
}
