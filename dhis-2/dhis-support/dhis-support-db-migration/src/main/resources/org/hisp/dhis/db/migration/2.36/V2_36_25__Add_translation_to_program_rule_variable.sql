alter table programrulevariable
    add column if not exists translations jsonb default '[]';
