package org.chatapp.common.model.socket.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {

    private UUID senderId;
    private String searchQuery;

}
