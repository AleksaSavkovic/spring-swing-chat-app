package org.chatapp.server.controller;

import lombok.RequiredArgsConstructor;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.service.ChatService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class ChatController {

    public static final String CHAT_PATH = "/api/v1/chat";
    public static final String CHAT_PATH_ID = CHAT_PATH + "/{chatId}";

    private final ChatService chatService;

    @PreAuthorize("#userId == authentication.principal.id")
    @GetMapping(CHAT_PATH)
    public List<ChatDTO> listUserChats(@RequestParam UUID userId) {
        return chatService.listUserChats(userId);
    }

    @PreAuthorize("@chatService.isChatMember(#chatId, authentication.principal.id)")
    @GetMapping(CHAT_PATH_ID)
    public ChatDTO findChatById(@PathVariable UUID chatId) {
        return chatService.findChatById(chatId).orElseThrow(NotFoundException::new);
    }

    @PostMapping(CHAT_PATH)
    public ResponseEntity<String> createChat(@RequestBody ChatDTO chat){
        ChatDTO savedChat = chatService.saveChat(chat);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", CHAT_PATH + "/" + savedChat.getId().toString());

        return new ResponseEntity<String>(headers, HttpStatus.CREATED);
    }
}
