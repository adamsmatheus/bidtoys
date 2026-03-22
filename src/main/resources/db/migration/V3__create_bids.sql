CREATE TABLE bids (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    auction_id  UUID        NOT NULL,
    bidder_id   UUID        NOT NULL,
    amount      INTEGER     NOT NULL,
    request_id  VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_bids PRIMARY KEY (id),
    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users (id),
    CONSTRAINT uq_bids_request_id UNIQUE (request_id),
    CONSTRAINT chk_bids_amount CHECK (amount > 0)
);

CREATE INDEX idx_bids_auction_id ON bids (auction_id);
CREATE INDEX idx_bids_bidder_id ON bids (bidder_id);
CREATE INDEX idx_bids_auction_created ON bids (auction_id, created_at DESC);

-- Adiciona FK de leading_bid_id após criar a tabela bids
ALTER TABLE auctions
    ADD CONSTRAINT fk_auctions_leading_bid
        FOREIGN KEY (leading_bid_id) REFERENCES bids (id) DEFERRABLE INITIALLY DEFERRED;
