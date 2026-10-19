ALTER TABLE message
MODIFY COLUMN chat_id VARCHAR(36) NOT NULL;

ALTER TABLE message
	ADD CONSTRAINT fk_message_user
	FOREIGN KEY (sender_id) REFERENCES users(id)
	ON DELETE CASCADE;

ALTER TABLE message_status
	ADD CONSTRAINT fk_message_status_user
	FOREIGN KEY (user_id) REFERENCES users(id)
	ON DELETE CASCADE;