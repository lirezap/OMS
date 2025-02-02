-- order request table definition.
CREATE TYPE order_request_side AS ENUM ('BUY', 'SELL');

CREATE TABLE order_request (
    id        BIGINT NOT NULL,
    symbol    VARCHAR(16) NOT NULL,
    side      order_request_side NOT NULL,
    quantity  VARCHAR(32) NOT NULL,
    price     VARCHAR(64) NOT NULL,
    remaining VARCHAR(32) NOT NULL,
    canceled  BOOLEAN NOT NULL DEFAULT FALSE,
    ts        TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (id, symbol)
) PARTITION BY HASH (id, symbol);

CREATE TABLE order_request_p1 PARTITION OF order_request FOR VALUES WITH (MODULUS 5, REMAINDER 0);
CREATE TABLE order_request_p2 PARTITION OF order_request FOR VALUES WITH (MODULUS 5, REMAINDER 1);
CREATE TABLE order_request_p3 PARTITION OF order_request FOR VALUES WITH (MODULUS 5, REMAINDER 2);
CREATE TABLE order_request_p4 PARTITION OF order_request FOR VALUES WITH (MODULUS 5, REMAINDER 3);
CREATE TABLE order_request_p5 PARTITION OF order_request FOR VALUES WITH (MODULUS 5, REMAINDER 4);

-- trade table definition.
CREATE TABLE trade (
    buy_order_id  BIGINT NOT NULL,
    sell_order_id BIGINT NOT NULL,
    symbol        VARCHAR(16) NOT NULL,
    quantity      VARCHAR(32) NOT NULL,
    buy_price     VARCHAR(64) NOT NULL,
    sell_price    VARCHAR(64) NOT NULL,
    metadata      VARCHAR(256),
    ts            TIMESTAMPTZ NOT NULL
) PARTITION BY RANGE (ts);

CREATE TABLE trade_p2025 PARTITION OF trade FOR VALUES FROM ('2025-01-01T00:00:00') TO ('2026-01-01T00:00:00');
CREATE TABLE trade_p2026 PARTITION OF trade FOR VALUES FROM ('2026-01-01T00:00:00') TO ('2027-01-01T00:00:00');
CREATE TABLE trade_p2027 PARTITION OF trade FOR VALUES FROM ('2027-01-01T00:00:00') TO ('2028-01-01T00:00:00');
CREATE TABLE trade_p2028 PARTITION OF trade FOR VALUES FROM ('2028-01-01T00:00:00') TO ('2029-01-01T00:00:00');
CREATE TABLE trade_p2029 PARTITION OF trade FOR VALUES FROM ('2029-01-01T00:00:00') TO ('2030-01-01T00:00:00');
