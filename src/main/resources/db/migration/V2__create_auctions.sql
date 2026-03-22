CREATE TABLE auctions (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    seller_id               UUID        NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    description             TEXT,
    initial_price_amount    INTEGER     NOT NULL,
    current_price_amount    INTEGER     NOT NULL,
    min_increment_amount    INTEGER     NOT NULL,
    duration_seconds        INTEGER     NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    started_at              TIMESTAMPTZ,
    ends_at                 TIMESTAMPTZ,
    approved_by_user_id     UUID,
    approved_at             TIMESTAMPTZ,
    rejected_by_user_id     UUID,
    rejected_at             TIMESTAMPTZ,
    rejection_reason        TEXT,
    cancelled_by_user_id    UUID,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    leading_bid_id          UUID,
    winner_user_id          UUID,
    finished_at             TIMESTAMPTZ,
    version                 BIGINT      NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_auctions PRIMARY KEY (id),
    CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_auctions_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_auctions_rejected_by FOREIGN KEY (rejected_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_auctions_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_auctions_winner FOREIGN KEY (winner_user_id) REFERENCES users (id),
    CONSTRAINT chk_auctions_status CHECK (
        status IN (
            'DRAFT', 'PENDING_APPROVAL', 'REJECTED',
            'READY_TO_START', 'CANCELLED',
            'ACTIVE', 'FINISHED_WITH_WINNER', 'FINISHED_NO_BIDS'
        )
    ),
    CONSTRAINT chk_auctions_initial_price CHECK (initial_price_amount > 0),
    CONSTRAINT chk_auctions_min_increment CHECK (min_increment_amount > 0),
    CONSTRAINT chk_auctions_duration CHECK (duration_seconds >= 60)
);

CREATE INDEX idx_auctions_seller_id ON auctions (seller_id);
CREATE INDEX idx_auctions_status ON auctions (status);
CREATE INDEX idx_auctions_ends_at ON auctions (ends_at) WHERE status = 'ACTIVE';

CREATE TABLE auction_images (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    auction_id  UUID        NOT NULL,
    file_key    VARCHAR(512) NOT NULL,
    file_url    VARCHAR(1024) NOT NULL,
    position    INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_auction_images PRIMARY KEY (id),
    CONSTRAINT fk_auction_images_auction FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE
);

CREATE INDEX idx_auction_images_auction_id ON auction_images (auction_id);

CREATE TABLE auction_status_history (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    auction_id          UUID        NOT NULL,
    from_status         VARCHAR(30),
    to_status           VARCHAR(30) NOT NULL,
    changed_by_user_id  UUID,
    reason              TEXT,
    metadata_json       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_auction_status_history PRIMARY KEY (id),
    CONSTRAINT fk_auction_status_history_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_auction_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_auction_status_history_auction_id ON auction_status_history (auction_id);
