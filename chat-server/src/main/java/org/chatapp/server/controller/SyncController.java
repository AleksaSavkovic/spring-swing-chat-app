package org.chatapp.server.controller;

import lombok.RequiredArgsConstructor;
import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.SyncResponseDTO;
import org.chatapp.server.service.SyncService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class SyncController {

    public final static String SYNC_PATH = "/api/v1/sync";
    private final SyncService syncService;

    // Method for syncing new chats, chat members and unread (or one page of) messages
    @GetMapping(SYNC_PATH)
    @PreAuthorize("#userId == authentication.principal.id")
    public SyncResponseDTO syncUserData(@RequestParam UUID userId, @RequestParam(required = false) Instant lastSync){
        return syncService.syncUserData(userId, lastSync);
    }

    // Method for syncing message status updates for a chats messages
    @GetMapping(SYNC_PATH + "/statuses")
    @PreAuthorize("@chatService.isChatMember(#chatId, authentication.principal.id)")
    public List<MessageStatusChangeDTO> syncChatMessageStatuses(@RequestParam UUID chatId,
                                                                @RequestParam Instant firstMsg,
                                                                @RequestParam(required = false) Instant lastSync){
        return syncService.syncChatMsgStatuses(chatId, firstMsg, lastSync);
    }
}
