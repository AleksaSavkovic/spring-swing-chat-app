package org.chatapp.server.socket;


import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {
    // For unauthenticated clients
    private final Set<ClientHandler> pendingClients = ConcurrentHashMap.newKeySet();
    // For authenticated clients
    private final ConcurrentHashMap<UUID, ClientHandler> authenticatedClients = new ConcurrentHashMap<>();

    public ConnectionManager() {

    }

    public Map<UUID, ClientHandler> getClients() {
        return authenticatedClients;
    }

    public void addClient(ClientHandler client) {
        pendingClients.add(client);
    }

    public void addAuthenticatedClient(ClientHandler client) {
        authenticatedClients.put(client.getUserId(), client);
    }

    public boolean removePendingClient(ClientHandler client) {
        return pendingClients.remove(client);
    }

    public ClientHandler removeAuthenticatedClient(ClientHandler client){
        return authenticatedClients.remove(client.getUserId());
    }

    // Iterates through each id from the given set and checks if they are online (authenticatedClients map contains them)
    public Set<UUID> getOnlineFromUsers(Set<UUID> userIdSet) {
        Set<UUID> online = new HashSet<>();
        for (UUID id : userIdSet) {
            if (authenticatedClients.containsKey(id)) {
                online.add(id);
            }
        }
        return online;
    }
}
