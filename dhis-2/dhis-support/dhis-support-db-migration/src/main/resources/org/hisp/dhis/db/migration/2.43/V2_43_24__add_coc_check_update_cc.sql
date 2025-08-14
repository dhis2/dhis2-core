-- Create function if not exists
CREATE OR REPLACE FUNCTION prevent_categorycomboid_update()
RETURNS trigger AS $$
BEGIN
    IF NEW.categorycomboid <> OLD.categorycomboid THEN
        RAISE EXCEPTION 'The CategoryOptionCombo CategoryCombo relationship cannot be updated once set (old=%, new=%)',
            OLD.categorycomboid, NEW.categorycomboid;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Only create trigger if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'no_categorycomboid_update'
    ) THEN
CREATE TRIGGER no_categorycomboid_update
    BEFORE UPDATE ON categorycombos_optioncombos
    FOR EACH ROW
    EXECUTE FUNCTION prevent_categorycomboid_update();
END IF;
END
$$;