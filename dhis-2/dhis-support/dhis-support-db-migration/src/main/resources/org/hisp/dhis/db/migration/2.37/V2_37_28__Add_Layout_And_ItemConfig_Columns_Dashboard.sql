-- Add new JSONB columns "layout" and "itemconfig" to table dashboard

alter table dashboard add column if not exists "layout" jsonb;
alter table dashboard add column if not exists "itemconfig" jsonb;
