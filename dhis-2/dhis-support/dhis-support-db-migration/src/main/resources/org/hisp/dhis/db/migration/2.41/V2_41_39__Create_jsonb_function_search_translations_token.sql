/**
  Find translated value that matches given token and given locale.
    @param $1 the translations column name
    @param $2 the properties to search for (array of strings such as '{NAME,SHORT_NAME}')
    @param $3 the locale language to search for
    @param $4 the token to search (example : '(?=.*année)')
 */
CREATE OR replace FUNCTION jsonb_search_translated_token(jsonb, text, text, text)
RETURNS bool
AS $$
SELECT exists(
    SELECT 1
    FROM  jsonb_array_elements($1) trans
    WHERE trans->>'property' = ANY ($2::text[])
         AND trans->>'locale' = $3
         AND trans->>'value' ~* $4
);
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE;