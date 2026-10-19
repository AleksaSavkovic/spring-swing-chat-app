package org.chatapp.common.model.socket.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkMessageStatusDTO {

    // Used for bulk SEEN and DELIVERED status update
    private UUID senderId;
    // SEEN status specific
    private UUID chatId;
    // DELIVERED status specific
    private List<UUID> messageIds;

}
