-- event trigger function that prevents some drop commands to be executed.
CREATE OR REPLACE FUNCTION abort_drop_commands() RETURNS event_trigger AS $abort_drop_commands$
BEGIN
    IF tg_tag = 'DROP SCHEMA' THEN
        RAISE EXCEPTION 'command % is disabled', tg_tag;
    END IF;

    IF tg_tag = 'DROP TABLE' THEN
        RAISE EXCEPTION 'command % is disabled', tg_tag;
    END IF;

    IF tg_tag = 'DROP TRIGGER' THEN
        RAISE EXCEPTION 'command % is disabled', tg_tag;
    END IF;

    IF tg_tag = 'DROP FUNCTION' THEN
        RAISE EXCEPTION 'command % is disabled', tg_tag;
    END IF;

    IF tg_tag = 'DROP PROCEDURE' THEN
        RAISE EXCEPTION 'command % is disabled', tg_tag;
    END IF;

    IF tg_tag = 'DROP ROUTINE' THEN
        RAISE EXCEPTION 'command % is disabled', tg_tag;
    END IF;
END;

$abort_drop_commands$ LANGUAGE plpgsql;

CREATE EVENT TRIGGER abort_drop_commands_event_trigger ON ddl_command_start EXECUTE FUNCTION abort_drop_commands();
