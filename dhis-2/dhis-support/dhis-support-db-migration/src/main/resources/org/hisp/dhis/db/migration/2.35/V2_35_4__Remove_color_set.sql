
-- Remove colorset references

alter table visualization drop column if exists colorsetid;

-- Remove colorset tables

drop table if exists colorset_colors;
drop table if exists color;
drop table if exists colorset;
