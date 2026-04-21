-- ========================
-- ENUMS
-- ========================
CREATE TYPE user_role     AS ENUM ('USER', 'ADMIN');
CREATE TYPE draw_status   AS ENUM ('ACTIVE', 'CLOSED', 'COMPLETED');
CREATE TYPE ticket_status AS ENUM ('AVAILABLE', 'SOLD', 'WIN', 'LOSE');

-- ========================
-- TABLES
-- ========================
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    login         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE draws (
    id            BIGSERIAL    PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    status        draw_status  NOT NULL DEFAULT 'ACTIVE',
    end_date      TIMESTAMPTZ  NOT NULL,
    total_tickets INT          NOT NULL CHECK (total_tickets > 0),
    created_by    BIGINT       NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE tickets (
    id            BIGSERIAL     PRIMARY KEY,
    draw_id       BIGINT        NOT NULL REFERENCES draws (id),
    owner_id      BIGINT        REFERENCES users (id),
    ticket_number INT           NOT NULL,
    status        ticket_status NOT NULL DEFAULT 'AVAILABLE',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ticket_number UNIQUE (draw_id, ticket_number),
    CONSTRAINT chk_owner_status CHECK (
        (status = 'AVAILABLE' AND owner_id IS NULL) OR
        (status IN ('SOLD', 'LOSE') AND owner_id IS NOT NULL)
        OR (status = 'WIN')
    )
);

CREATE TABLE draw_results (
    id                BIGSERIAL   PRIMARY KEY,
    draw_id           BIGINT      NOT NULL UNIQUE REFERENCES draws (id),
    winning_ticket_id BIGINT      NOT NULL REFERENCES tickets (id),
    drawn_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ========================
-- INDEXES
-- ========================
CREATE INDEX idx_tickets_draw_status ON tickets (draw_id, status);
CREATE INDEX idx_tickets_owner_id    ON tickets (owner_id);
CREATE INDEX idx_draws_status        ON draws (status);
CREATE INDEX idx_draws_created_by    ON draws (created_by);
CREATE INDEX idx_draws_end_date      ON draws (end_date);
