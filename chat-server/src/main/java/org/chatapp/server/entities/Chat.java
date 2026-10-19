package org.chatapp.server.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.chatapp.common.model.ChatType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Builder.Default
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatMember> users = new HashSet<>();

    // Helper methods for handling saving chat members together with the chat
    public void addUsers(Set<ChatMember> users) {
        this.users = users;
        users.forEach(user -> user.setChat(this));
    }

    // Chat type information, can be a group chat or regular chat
    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    @Builder.Default
    @OneToMany(mappedBy = "chat")
    private Set<Message> messages = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updateDate;

    // Group chat specific fields
    @Size(max = 50)
    private String chatName;

    @Size(max = 255)
    private String password;

}
