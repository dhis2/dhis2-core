
-- Add column 'noDataColor' to table 'mapview'

alter table mapview add column if not exists nodatacolor varchar(7);
