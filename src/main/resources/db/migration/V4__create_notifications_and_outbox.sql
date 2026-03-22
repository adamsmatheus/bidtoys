CREATE TABLE notifications (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    auction_id          UUID,
    type                VARCHAR(50) NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payload_json        TEXT,
    provider_message_id VARCHAR(255),
    error_message       TEXT,
    retry_count         INTEGER     NOT NULL DEFAULT 0,
    sent_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT chk_notifications_status CHECK (
        status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT chk_notifications_channel CHECK (
        channel IN ('WHATSAPP', 'EMAIL', 'PUSH')
    )
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_auction_id ON notifications (auction_id);
CREATE INDEX idx_notifications_status ON notifications (status);

CREATE TABLE admin_alerts (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    type                VARCHAR(50) NOT NULL,
    auction_id          UUID,
    notification_id     UUID,
    message             TEXT        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_at         TIMESTAMPTZ,
    resolved_by_user_id UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_admin_alerts PRIMARY KEY (id),
    CONSTRAINT fk_admin_alerts_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_admin_alerts_notification FOREIGN KEY (notification_id) REFERENCES notifications (id),
    CONSTRAINT fk_admin_alerts_resolver FOREIGN KEY (resolved_by_user_id) REFERENCES users (id),
    CONSTRAINT chk_admin_alerts_status CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX idx_admin_alerts_status ON admin_alerts (status);
CREATE INDEX idx_admin_alerts_auction_id ON admin_alerts (auction_id);

CREATE TABLE outbox_events (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    UUID        NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload_json    TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    available_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    retry_count     INTEGER     NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT chk_outbox_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')
    )
);

CREATE INDEX idx_outbox_events_status ON outbox_events (status, available_at)
    WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX idx_outbox_events_aggregate ON outbox_events (aggregate_type, aggregate_id);

CREATE TABLE audit_logs (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    actor_user_id   UUID,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID        NOT NULL,
    action          VARCHAR(50) NOT NULL,
    metadata_json   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
