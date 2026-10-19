package org.chatapp.common.model.socket.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketEvent<T> {

    private SocketEventType type;
    private T payload;
}
