package org.chatapp.client.service;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.ChatMemberDTO;

import java.util.*;
import java.util.stream.Collectors;


// Class used to track which user is online based on the information provided by the socket server
public class OnlineUserTracker {
    private final HashMap<UUID, Boolean> userStatuses;

    public OnlineUserTracker(List<ChatDTO> chats) {
        userStatuses = new HashMap<UUID, Boolean>();

        Set<UUID> users =  chats.stream().flatMap(chatDTO -> chatDTO
                                                            .getUsers()
                                                            .stream()
                                                            .map(ChatMemberDTO::getUserId)).collect(Collectors.toSet());
        for(UUID key: users) {
            userStatuses.put(key, false);
        }
    }

    // Adds all the new users from the chat list to the user statuses map if they aren't there yet and returns the newly added UUIDs
    public Set<UUID> updateUserSet(List<ChatDTO> newChats){
        Set<UUID> addedIds = new HashSet<>();
        Set <UUID> newUsers = newChats.stream().flatMap(chatDTO -> chatDTO
                                                            .getUsers()
                                                            .stream()
                                                            .map(ChatMemberDTO::getUserId)).collect(Collectors.toSet());

        for(UUID userId : newUsers) {
            if(!userStatuses.containsKey(userId)){
                addedIds.add(userId);
                userStatuses.put(userId, false);
            }
        }

        return addedIds;
    }

    // Returns the user status for the requested user. If there is no user with that id will return false as well
    public boolean getUserStatus(UUID userId){
        return userStatuses.get(userId) != null && userStatuses.get(userId);
    }

    // Set new user status for the user
    public void setUserStatus(UUID userId, boolean status) {
        userStatuses.put(userId, status);
    }

    // Checks if the user is added to the online tracker
    public boolean checkIfUserStatusAvailable(UUID userId) {
        return userStatuses.containsKey(userId);
    }

    // Adds new user to the status tracker
    public void addNewUser(UUID userId) {
        userStatuses.put(userId, false);
    }

    // Change user status to the opposite of existing status
    public void toggleUserStatus(UUID userId){
        userStatuses.put(userId,!userStatuses.get(userId));
    }

    // Returns all the online users for the specified chat
    public List<ChatMemberDTO> getOnlineUsersForChat(ChatDTO chat){
        return chat.getUsers()
                .stream()
                // Filter out all users in the chat which are not in the userStatuses and whose status is not true (online)
                .filter(user ->
                        userStatuses.containsKey(user.getUserId()) && userStatuses.get(user.getUserId()))
                .toList();
    }

    public Set<UUID> getUserIds() {
        return userStatuses.keySet();
    }
}
