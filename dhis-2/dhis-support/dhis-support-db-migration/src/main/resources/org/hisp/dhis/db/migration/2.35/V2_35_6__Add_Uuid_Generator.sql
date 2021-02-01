
CREATE OR REPLACE FUNCTION gen_random_uuid() RETURNS uuid
    AS $$
        SELECT md5(random()::text || random()::text)::uuid
    $$
    LANGUAGE SQL;
