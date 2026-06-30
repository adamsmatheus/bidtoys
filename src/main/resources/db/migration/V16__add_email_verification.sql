ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Usuários existentes já foram verificados manualmente, não bloquear
UPDATE users SET email_verified = TRUE;
