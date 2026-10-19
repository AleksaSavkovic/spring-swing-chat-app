package org.chatapp.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.MessageType;
import org.chatapp.common.model.PageResponse;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.mappers.PageMapper;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.service.ImageStorageService;
import org.chatapp.server.service.MessageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
public class MessageController {

    public static final String MESSAGE_PATH = "/api/v1/message";
    public static final String MESSAGE_PATH_ID = MESSAGE_PATH + "/{messageId}";

    private final MessageService messageService;
    private final ImageStorageService imageStorageService;

    // Method that returns a page of chat messages for the requested chatId sorted
    @GetMapping(MESSAGE_PATH)
    public PageResponse<MessageDTO> listChatMessages(@RequestParam UUID chatId,
                                                     @RequestParam(required = false) Integer pageNumber,
                                                     @RequestParam(required = false) Integer pageSize) {
        // Get the active user for this session (should never be null as this endpoint is authenticated)
        UUID activeId = ((CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getId();

        return PageMapper.from(messageService.getChatMessages(chatId, pageNumber, pageSize, activeId));
    }

    // Returns the image linked to given message id
    @GetMapping(MESSAGE_PATH_ID + "/image")
    public ResponseEntity<?> getMessageImage(@PathVariable("messageId") UUID messageId) {
        MessageDTO messageDTO = messageService.findMessageById(messageId);

        if (!messageDTO.getMessageType().equals(MessageType.FILE_MESSAGE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The requested message does not contain an image");
        }
        Resource file = imageStorageService.loadAsResource(messageDTO.getContent());

        // Check if file exists, if not throw exception
        if(file == null) {
            throw new NotFoundException("Requested file is not found");
        }

        // Return file as resource
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    // Uploads image
    @PostMapping(MESSAGE_PATH + "/image")
    public ResponseEntity<String> uploadImage(@RequestParam("file")MultipartFile file) {
        Path createdFilePath = imageStorageService.store(file);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/" + createdFilePath.getFileName());

        return new ResponseEntity<String>(headers, HttpStatus.CREATED);
    }

    // Get single message by id
    @GetMapping(MESSAGE_PATH_ID)
    public MessageDTO findMessageById(@PathVariable("messageId") UUID messageId) {
        return messageService.findMessageById(messageId);
    }

    // Method which returns a list of messages found with an id in the given id list
    @PostMapping(MESSAGE_PATH + "/by-ids")
    public List<MessageDTO> getMessagesByIdIn(@RequestBody Set<UUID> messageIds) {
        List<MessageDTO> foundMessages = messageService.findMessagesByIdIn(messageIds);
        // Remove all the reply to id's as currently this is being used only for getting reference only messages
        foundMessages.forEach(messageDTO -> messageDTO.setReplyToId(null));
        return foundMessages;
    }
}
