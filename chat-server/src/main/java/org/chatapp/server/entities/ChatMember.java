package org.chatapp.server.entities;

import jakarta.persistence.*;
import lombok.*;
import org.chatapp.common.model.ChatterRole;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}))
public class ChatMember {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant mutedUntil;

    private boolean pinned;

    private boolean blocked;

    @Enumerated(EnumType.STRING)
    private ChatterRole role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updateDate;
}
