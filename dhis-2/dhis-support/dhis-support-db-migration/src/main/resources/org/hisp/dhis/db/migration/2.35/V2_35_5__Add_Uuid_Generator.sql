

CREATE OR REPLACE FUNCTION gen_random_uuid() RETURNS uuid
    AS $$
        SELECT uuid_in(
            overlay(
                overlay(md5(random()::text || ':' || clock_timestamp()::text) placing '4' from 13)
                placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17
            )::cstring
        )
    $$
    LANGUAGE SQL;
