-- order message table definition.
CREATE TYPE order_message_side AS ENUM ('BUY', 'SELL');
CREATE TYPE order_message_type AS ENUM ('LIMIT', 'MARKET', 'FILL_OR_KILL', 'STOP', 'STOP_LIMIT');
CREATE TYPE order_message_state AS ENUM ('ACTIVE', 'CANCELED', 'EXECUTED');

CREATE TABLE order_message (
    id        BIGINT NOT NULL,
    symbol    VARCHAR(16) NOT NULL,
    side      order_message_side NOT NULL,
    type      order_message_type NOT NULL,
    quantity  VARCHAR(32) NOT NULL,
    price     VARCHAR(32),
    remaining VARCHAR(32),
    state     order_message_state NOT NULL DEFAULT 'ACTIVE',
    ts        TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (id, symbol)
) PARTITION BY HASH (id, symbol);

CREATE TABLE order_message_p1 PARTITION OF order_message FOR VALUES WITH (MODULUS 5, REMAINDER 0);
CREATE TABLE order_message_p2 PARTITION OF order_message FOR VALUES WITH (MODULUS 5, REMAINDER 1);
CREATE TABLE order_message_p3 PARTITION OF order_message FOR VALUES WITH (MODULUS 5, REMAINDER 2);
CREATE TABLE order_message_p4 PARTITION OF order_message FOR VALUES WITH (MODULUS 5, REMAINDER 3);
CREATE TABLE order_message_p5 PARTITION OF order_message FOR VALUES WITH (MODULUS 5, REMAINDER 4);

CREATE INDEX order_message_state_ts ON order_message (state, ts) WHERE state IN ('ACTIVE');

-- trade table definition.
CREATE TABLE trade (
    buy_order_id  BIGINT NOT NULL,
    sell_order_id BIGINT NOT NULL,
    symbol        VARCHAR(16) NOT NULL,
    quantity      VARCHAR(32) NOT NULL,
    buy_price     VARCHAR(32) NOT NULL,
    sell_price    VARCHAR(32) NOT NULL,
    metadata      VARCHAR(256),
    ts            TIMESTAMPTZ NOT NULL
) PARTITION BY RANGE (ts);

CREATE TABLE trade_p2025 PARTITION OF trade FOR VALUES FROM ('2025-01-01T00:00:00') TO ('2026-01-01T00:00:00');
CREATE TABLE trade_p2026 PARTITION OF trade FOR VALUES FROM ('2026-01-01T00:00:00') TO ('2027-01-01T00:00:00');
CREATE TABLE trade_p2027 PARTITION OF trade FOR VALUES FROM ('2027-01-01T00:00:00') TO ('2028-01-01T00:00:00');
CREATE TABLE trade_p2028 PARTITION OF trade FOR VALUES FROM ('2028-01-01T00:00:00') TO ('2029-01-01T00:00:00');
CREATE TABLE trade_p2029 PARTITION OF trade FOR VALUES FROM ('2029-01-01T00:00:00') TO ('2030-01-01T00:00:00');
