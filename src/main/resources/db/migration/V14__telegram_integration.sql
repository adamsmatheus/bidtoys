-- Substituir whatsapp_enabled por telegram_chat_id na tabela de usuários
ALTER TABLE users ADD COLUMN telegram_chat_id BIGINT;
ALTER TABLE users DROP COLUMN whatsapp_enabled;

-- Atualizar constraint do canal de notificações para incluir TELEGRAM
ALTER TABLE notifications DROP CONSTRAINT chk_notifications_channel;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_channel
    CHECK (channel IN ('WHATSAPP', 'EMAIL', 'PUSH', 'TELEGRAM'));

-- Migrar alertas WHATSAPP_FAILED para TELEGRAM_FAILED
UPDATE admin_alerts SET type = 'TELEGRAM_FAILED' WHERE type = 'WHATSAPP_FAILED';
