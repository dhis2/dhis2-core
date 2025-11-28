/**
  Extract a translated value from the translations JSONB column for a given locale and property key.
  If no translation exists for the given locale and property, returns NULL.

  @param $1 the translations JSONB column
  @param $2 the property key to search for (e.g., 'NAME', 'DESCRIPTION', 'SHORT_NAME')
  @param $3 the locale to search for (e.g., 'en', 'fr', 'es')
  @return the translated value as text, or NULL if not found
 */
CREATE OR REPLACE FUNCTION jsonb_get_translated_value(jsonb, text, text)
RETURNS text
AS $$
SELECT trans->>'value'
FROM jsonb_array_elements($1) trans
WHERE trans->>'property' = $2
  AND trans->>'locale' = $3
LIMIT 1;
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE;
