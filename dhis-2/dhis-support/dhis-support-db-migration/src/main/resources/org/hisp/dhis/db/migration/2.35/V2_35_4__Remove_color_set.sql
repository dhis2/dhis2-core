
-- Remove colorset references

alter table visualization drop column if exists colorsetid;

-- Remove colorset tables

drop table colorset_colors;
drop table color;
drop table colorset;
