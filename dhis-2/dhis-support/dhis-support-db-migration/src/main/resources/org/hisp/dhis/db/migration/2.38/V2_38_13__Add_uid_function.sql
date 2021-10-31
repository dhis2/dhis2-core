CREATE OR REPLACE FUNCTION uid()
 RETURNS text
 LANGUAGE sql
AS $function$ 
  SELECT substring('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ' FROM (random()*51)::int + 1 FOR 1) || 
  array_to_string(ARRAY(SELECT substring('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789' FROM (random()*61)::int + 1 FOR 1) FROM generate_series(1,10)), '') 
$function$
;
