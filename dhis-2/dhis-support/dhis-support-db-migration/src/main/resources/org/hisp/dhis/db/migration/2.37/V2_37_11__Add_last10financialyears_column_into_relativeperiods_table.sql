alter table public.relativeperiods add column if not exists last10financialyears boolean;
update public.relativeperiods set last10financialyears=false where last10financialyears is null;
alter table public.relativeperiods alter column last10financialyears set not null;