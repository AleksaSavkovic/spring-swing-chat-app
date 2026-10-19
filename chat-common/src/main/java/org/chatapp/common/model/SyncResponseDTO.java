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
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponseDTO {

    private List<ChatDTO> newChats;
    private List<MessageDTO> newMessages;
    // Reference only messages are messages which are needed for displaying replies but are not visible in the chat as
    // standalone messages until loaded separately as a part of a page of messages
    private List<MessageDTO> referenceOnlyMessages;

    @JsonSerialize(using = CustomDateSerializer.class)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    private Instant serverTime;
}
