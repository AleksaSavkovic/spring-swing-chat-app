package org.chatapp.client.interfaces;

import java.util.UUID;

public interface GetMessageAttachmentExecutor {
    boolean getMessageAttachment(UUID messageId);
}
