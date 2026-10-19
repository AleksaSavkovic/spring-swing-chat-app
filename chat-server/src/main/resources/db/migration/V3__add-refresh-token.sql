DROP TABLE IF EXISTS refresh_token;

CREATE TABLE refresh_token(
    	id VARCHAR(36) NOT NULL,
    	user_id VARCHAR(36) NOT NULL,
    	expires_at TIMESTAMP NOT NULL,
    	revoked BOOLEAN NOT NULL DEFAULT FALSE,
        created_date TIMESTAMP(6),
	    update_date TIMESTAMP(6),
    	PRIMARY KEY(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

ALTER TABLE refresh_token
	ADD CONSTRAINT fk_token_user
	FOREIGN KEY (user_id) REFERENCES users(id)
	ON DELETE CASCADE;
