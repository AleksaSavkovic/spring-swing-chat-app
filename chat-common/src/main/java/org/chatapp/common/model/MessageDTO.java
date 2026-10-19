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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private UUID id;
    private UserDTO sender;

    private UUID chatId;
    private String content;
    private MessageType messageType;
    private UUID replyToId;

    @Builder.Default
    private List<MessageStatusDTO> statuses = new ArrayList<>();

    @JsonSerialize(using = CustomDateSerializer.class)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    private Instant sentTime;

    @Builder.Default
    private boolean active = true;
}
