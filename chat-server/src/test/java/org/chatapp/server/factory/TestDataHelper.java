package org.chatapp.server.factory;

import org.chatapp.common.model.StatusType;
import org.chatapp.server.entities.*;
import org.chatapp.server.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@TestComponent
public class TestDataHelper {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private ChatMemberRepository chatMemberRepository;
    @Autowired
    private MessageStatusRepository messageStatusRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public void clearUserRepository() {
        userRepository.deleteAll();
    }

    public User saveUser() {
        return userRepository.save(UserTestFactory.createUser());
    }

    public User saveUser(String username) {
        return userRepository.save(UserTestFactory.createUser(username));
    }

    public Chat saveRegularChat() {
        return chatRepository.save(ChatTestFactory.createRegularChat());
    }

    public Chat saveGroupChat() {
        return chatRepository.save(ChatTestFactory.createGroupChat());
    }

    public Chat saveChatWithTimeStamps(Instant created, Instant updated) {
        return chatRepository.save(ChatTestFactory.createChatWithTimeStamps(created, updated));
    }

    public ChatMember saveChatMember(Chat chat, User user) {
        return chatMemberRepository.save(ChatMemberTestFactory.createChatMember(chat, user));
    }

    public ChatMember saveChatMemberWithTimeStamps(Chat chat, User user, Instant created, Instant updated) {
        return chatMemberRepository.save(ChatMemberTestFactory.createChatMemberWithTimestamps(chat, user, created, updated));
    }

    public Message saveMessage() {
        return messageRepository.save(MessageTestFactory.createMessage());
    }

    public Message saveMessage(User user, Chat chat) {
        return messageRepository.save(MessageTestFactory.createMessage(user, chat));
    }

    public Message saveMessage(User user, Chat chat, String content) {
        return messageRepository.save(MessageTestFactory.createMessage(user, chat, content));
    }

    public MessageStatus saveMessageStatus() {
        return messageStatusRepository.save(MessageStatusTestFactory.createMessageStatus());
    }

    public MessageStatus saveMessageStatus(Message message, User user, StatusType status) {
        return messageStatusRepository.save(MessageStatusTestFactory.createMessageStatus(message, user, status));
    }

    public RefreshTokenData saveRefreshTokenData(User user) {
        return refreshTokenRepository.save(RefreshTokenDataTestFactory.createRefreshTokenData(user));
    }

    public RefreshTokenData saveRefreshTokenData(User user, boolean revoked) {
        return refreshTokenRepository.save(RefreshTokenDataTestFactory.createRefreshTokenData(user, revoked));
    }

    public List<Message> saveMessagesFromSender(Chat chat, User sender, int count){
        List<Message> savedMessages = new ArrayList<>();
        for(int i = 0; i<count; i++) {
            savedMessages.add(saveMessage(sender, chat, "Message " + i));
        }
        return savedMessages;
    }

    public List<Message> saveMessagesFromSenderWithStatuses(Chat chat, User sender, int count){
        List<Message> messages = saveMessagesFromSender(chat, sender, count);
        for(Message message : messages) {
            Set<ChatMember> recipients = message.getChat().getUsers();
            for(ChatMember recipient : recipients) {
                if(!recipient.getUser().getId().equals(sender.getId())){
                    message.getMessageStatuses().add(saveMessageStatus(message, recipient.getUser(), StatusType.SEEN));
                }
            }
        }
        return messages;
    }
}
