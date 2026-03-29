CREATE TABLE companies (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url    VARCHAR(1024),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uq_companies_user_id UNIQUE (user_id),
    CONSTRAINT fk_companies_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_companies_user_id ON companies (user_id);
