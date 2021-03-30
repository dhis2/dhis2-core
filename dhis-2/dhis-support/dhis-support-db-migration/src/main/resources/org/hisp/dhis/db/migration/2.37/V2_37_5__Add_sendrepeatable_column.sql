-- Add sendrepeatable column to programnotificationtemplate table.
-- For backward compatibility set sendrepeatable to false where null.

alter table programnotificationtemplate add column if not exists sendrepeatable boolean;
update programnotificationtemplate set sendrepeatable = false where sendrepeatable is null;
alter table programnotificationtemplate alter column sendrepeatable set not null;
