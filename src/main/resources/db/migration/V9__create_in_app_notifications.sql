CREATE TABLE in_app_notifications (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    type        VARCHAR(50) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT        NOT NULL,
    auction_id  UUID,
    read        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_in_app_notification_type CHECK (
        type IN ('AUCTION_WON', 'PAYMENT_DECLARED', 'PAYMENT_CONFIRMED', 'PAYMENT_DISPUTED')
    )
);

CREATE INDEX idx_in_app_notif_user_id  ON in_app_notifications (user_id, created_at DESC);
CREATE INDEX idx_in_app_notif_unread   ON in_app_notifications (user_id) WHERE read = FALSE;
