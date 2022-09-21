
-- Adds "colorset" property to "visualization" table

alter table visualization add column if not exists "colorset" varchar(255);
