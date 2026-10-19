
-- Disable Foreign Key checks so we can drop tables in any order
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS message_status;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS chat_member;
DROP TABLE IF EXISTS chat;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users(
	id VARCHAR(36) NOT NULL,
	username VARCHAR(50) NOT NULL,
	password VARCHAR(255) NOT NULL,
	created_date TIMESTAMP(6),
	update_date TIMESTAMP(6),
	PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat (
	id VARCHAR(36) NOT NULL,
	chat_name VARCHAR(50),
	chat_type ENUM(
		'GROUP',
		'REGULAR'
	),
	password VARCHAR(255),
	created_date TIMESTAMP(6),
	update_date TIMESTAMP(6),
	PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_member (
	id VARCHAR(36) NOT NULL,
	user_id VARCHAR(36) NOT NULL,
	chat_id VARCHAR(36) NOT NULL,
	blocked BOOLEAN NOT NULL,
	pinned BOOLEAN NOT NULL,
	role ENUM (
		'CREATOR',
		'ADMIN',
		'MEMBER'
	),
	created_date TIMESTAMP(6),
	update_date TIMESTAMP(6),
	muted_until TIMESTAMP(6),
	PRIMARY KEY(id),
	UNIQUE KEY uq_chat_user (chat_id, user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE message (
	id VARCHAR(36) NOT NULL,
	chat_id VARCHAR(36),
	sender_id VARCHAR(36) NOT NULL,
	content TEXT NOT NULL,
	message_type ENUM (
		'CHAT_MESSAGE',
		'JOIN_ROOM',
		'LEAVE_ROOM',
		'RECEIVED_MESSAGE',
		'SEARCH',
		'SEEN_MESSAGE'
	),
	created_date TIMESTAMP(6),
	update_date TIMESTAMP(6),
	PRIMARY KEY (id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE message_status (
	id VARCHAR(36) NOT NULL,
	message_id VARCHAR(36) NOT NULL,
	user_id VARCHAR(36) NOT NULL,
	status ENUM (
		'DELIVERED',
		'SEEN',
		'SENT'
	),
	created_date TIMESTAMP(6),
	update_date TIMESTAMP(6),
	PRIMARY KEY (id),
	UNIQUE KEY uq_message_user(message_id, user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

ALTER TABLE chat_member
	ADD CONSTRAINT fk_chat_member_chat
	FOREIGN KEY (chat_id) REFERENCES chat(id)
	ON DELETE CASCADE;

ALTER TABLE chat_member
	ADD CONSTRAINT fk_chat_member_user
	FOREIGN KEY (user_id) REFERENCES users(id)
	ON DELETE CASCADE;

ALTER TABLE message
	ADD CONSTRAINT fk_message_chat
	FOREIGN KEY (chat_id) REFERENCES chat(id)
	ON DELETE CASCADE;

ALTER TABLE message_status
	ADD CONSTRAINT fk_message_status_message
	FOREIGN KEY (message_id) REFERENCES message(id)
	ON DELETE CASCADE;







