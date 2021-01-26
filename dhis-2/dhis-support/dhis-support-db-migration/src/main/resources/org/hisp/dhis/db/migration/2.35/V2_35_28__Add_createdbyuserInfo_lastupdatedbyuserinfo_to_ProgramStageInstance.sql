alter table if exists programstageinstance
    add column if not exists createdbyuserinfo jsonb;

alter table if exists programstageinstance
    add column if not exists lastupdatedbyuserinfo jsonb;
