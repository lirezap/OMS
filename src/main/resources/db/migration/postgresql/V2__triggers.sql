-- trigger function that prevents order_request records to be deleted.
CREATE OR REPLACE FUNCTION prevent_order_request_delete() RETURNS trigger AS $prevent_order_request_delete$
    BEGIN
        RETURN NULL;
    END;

$prevent_order_request_delete$ LANGUAGE plpgsql;
CREATE OR REPLACE TRIGGER prevent_order_request_delete_trigger BEFORE DELETE ON order_request
    FOR EACH ROW EXECUTE FUNCTION prevent_order_request_delete();

-- trigger function that prevents trade records to be updated.
CREATE OR REPLACE FUNCTION prevent_trade_update() RETURNS trigger AS $prevent_trade_update$
    BEGIN
        RETURN NULL;
    END;

$prevent_trade_update$ LANGUAGE plpgsql;
CREATE OR REPLACE TRIGGER prevent_trade_update_trigger BEFORE UPDATE ON trade
    FOR EACH ROW EXECUTE FUNCTION prevent_trade_update();

-- trigger function that prevents trade records to be deleted.
CREATE OR REPLACE FUNCTION prevent_trade_delete() RETURNS trigger AS $prevent_trade_delete$
    BEGIN
        RETURN NULL;
    END;

$prevent_trade_delete$ LANGUAGE plpgsql;
CREATE OR REPLACE TRIGGER prevent_trade_delete_trigger BEFORE DELETE ON trade
    FOR EACH ROW EXECUTE FUNCTION prevent_trade_delete();
