CREATE or replace FUNCTION has_user_id (jsonb, text )
RETURNS bool
AS $$
select  $1->'users' ? $2
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE
;

CREATE or replace FUNCTION has_user_group_ids (jsonb, text[] )
RETURNS bool
AS $$
select  $1->'userGroups' ?| $2 
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE
;

CREATE or replace FUNCTION check_user_access (jsonb, text, text )
RETURNS bool
AS $$
select  $1->'users'->$2->>'access' like $3
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE
;

CREATE or replace FUNCTION check_user_group_access (jsonb, text, text )
RETURNS bool
AS $$
select  $1->'userGroups'->$2->>'access' like $3
$$
LANGUAGE SQL IMMUTABLE PARALLEL SAFE
;
