package org.chatapp.client.interfaces;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.common.model.socket.transport.SocketEventType;
import org.chatapp.common.model.socket.transport.UserStatus;

import java.util.List;
import java.util.UUID;

public interface ChatClientListener {

    void onMessageReceived(MessageDTO message);

    void onDisconnected();

    void onServerError(String error);

    void onSearchResultReceived(List<UserDTO> result);

    void onChatterStatusChange(UUID userId, UserStatus userStatus);

    void onPresenceSnapshotReceived(List<UUID> onlineUserIds);

    void onSingleMessageStatusChange(MessageDTO message, SocketEventType messageStatus);

    void onChatsSync();

    void onStatusesSync();

    void onBulkSeenStatusChange(UUID chatId);

    void onBulkDeliveredStatusChange(List<UUID> messageList);

    void onGroupCreated(ChatDTO groupChat);

    void onDeleteMessage(MessageDTO messageDTO);

    void onReplyLoaded(MessageDTO messageDTO);
}
