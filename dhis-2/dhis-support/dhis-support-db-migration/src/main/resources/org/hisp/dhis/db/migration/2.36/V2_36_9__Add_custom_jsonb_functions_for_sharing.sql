-- This will use the index gin ( (sharing->'users')  ) ;
CREATE or replace FUNCTION jsonb_has_user_id(jsonb, text )
RETURNS bool
AS $$
select  $1->'users' ? $2
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

CREATE or replace FUNCTION jsonb_check_user_access(jsonb, text, text)
RETURNS bool
AS $$
select  $1->'users'->$2->>'access' like $3
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

-- Second parameter is an array of userGroup uuid. e.g. '{248ed37f-31a4-37ac-9bd8-0273bfb566ac,09c0b68f-1fb7-40a0-3575-bf4687a1d9dd}'
-- This function uses the index  gin ( (sharing->'userGroups')  ) ;
CREATE OR replace FUNCTION jsonb_has_user_group_ids(jsonb,  text)
RETURNS bool
AS $$
SELECT   $1->'userGroups' ?| $2::text[];
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

CREATE OR replace FUNCTION jsonb_check_user_groups_access(jsonb, text, text)
RETURNS bool
AS $$
SELECT exists(
         SELECT 1
         FROM  jsonb_each($1->'userGroups') je
         WHERE je.key = ANY ($3::text[])
         AND je.value->>'access' LIKE $2
     );
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

CREATE OR REPLACE FUNCTION regexp_search(character varying, character varying)
RETURNS boolean
AS 'select $1 ~* $2;'
LANGUAGE sql;


