-- Cross-client user activity: one row per user per calendar day, upserted from a request
-- interceptor (cache-guarded, so lastactive is refreshed at most every few minutes per user).
-- Covers every authenticated client (web apps, Android, API integrations), unlike
-- datastatisticsevent which only sees analytics-app view events.
create table if not exists useractivity (
    username     varchar(255) not null,
    activitydate date not null,
    lastactive   timestamp without time zone not null,
    constraint useractivity_pkey primary key (username, activitydate)
);

create index if not exists in_useractivity_activitydate on useractivity (activitydate);
create index if not exists in_useractivity_lastactive on useractivity (lastactive);

-- Daily rollup produced by the cleanup job before raw rows older than the retention window
-- are pruned. Preserves historical daily active users indefinitely.
create table if not exists useractivitydaily (
    activitydate date primary key,
    activeusers  integer not null
);
