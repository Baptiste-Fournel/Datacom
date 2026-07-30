CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    login         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    firstname     VARCHAR(100) NOT NULL,
    lastname      VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('OPERATOR', 'VALIDATOR'))
);

CREATE TABLE products
(
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255),
    reference          VARCHAR(100),
    description        TEXT,
    category           VARCHAR(100),
    subcategory        VARCHAR(100),
    manufacturer       VARCHAR(255),
    country            VARCHAR(100),
    lot                VARCHAR(100),
    certification      VARCHAR(255),
    validation_comment TEXT,
    status             VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_VALIDATION', 'VALIDATED')),
    current_step       INTEGER     NOT NULL DEFAULT 1 CHECK (current_step BETWEEN 1 AND 4),
    created_by         BIGINT      NOT NULL REFERENCES users (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
