package org.chatapp.client.ui.records;

import org.chatapp.common.model.StatusType;
import org.chatapp.common.model.UserDTO;

import java.util.UUID;

public record MessageStatusDisplay(
        UserDTO user,
        StatusType status,
        UUID statusId
) {}
