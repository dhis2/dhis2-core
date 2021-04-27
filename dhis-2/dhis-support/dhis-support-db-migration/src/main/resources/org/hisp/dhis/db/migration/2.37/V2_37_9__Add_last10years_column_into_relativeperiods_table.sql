alter table public.relativeperiods add column if not exists last10years boolean;
update public.relativeperiods set last10years=false where last10years is null;
alter table public.relativeperiods alter column last10years set not null;