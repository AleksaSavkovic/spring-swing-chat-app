package org.chatapp.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusDTO {
    private UUID id;
    private UUID userId;
    private StatusType status;
}
