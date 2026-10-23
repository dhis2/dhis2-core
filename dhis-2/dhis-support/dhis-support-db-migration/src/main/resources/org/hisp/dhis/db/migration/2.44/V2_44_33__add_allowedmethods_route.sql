-- Routes created before this column existed accepted all of these HTTP methods, so keep that behaviour
alter table route add column if not exists allowedmethods jsonb not null default '["GET", "POST", "PUT", "PATCH", "DELETE"]'::jsonb;
