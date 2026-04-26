CREATE TABLE rifas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    ticket_price_amount INT NOT NULL,
    total_tickets INT NOT NULL,
    draw_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    winner_ticket_number INT,
    winner_user_id UUID REFERENCES users(id),
    approved_by_user_id UUID,
    approved_at TIMESTAMPTZ,
    rejected_by_user_id UUID,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rifa_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rifa_id UUID NOT NULL REFERENCES rifas(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rifa_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rifa_id UUID NOT NULL REFERENCES rifas(id) ON DELETE CASCADE,
    ticket_number INT NOT NULL,
    buyer_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (rifa_id, ticket_number)
);

CREATE INDEX idx_rifas_seller_id ON rifas(seller_id);
CREATE INDEX idx_rifas_status ON rifas(status);
CREATE INDEX idx_rifa_tickets_rifa_id ON rifa_tickets(rifa_id);
CREATE INDEX idx_rifa_tickets_buyer_id ON rifa_tickets(buyer_id);
