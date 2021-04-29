alter table if exists trackedentityinstance
    add column if not exists createdbyuserinfo jsonb;

alter table if exists trackedentityinstance
    add column if not exists lastupdatedbyuserinfo jsonb;

alter table if exists programinstance
    add column if not exists createdbyuserinfo jsonb;

alter table if exists programinstance
    add column if not exists lastupdatedbyuserinfo jsonb;