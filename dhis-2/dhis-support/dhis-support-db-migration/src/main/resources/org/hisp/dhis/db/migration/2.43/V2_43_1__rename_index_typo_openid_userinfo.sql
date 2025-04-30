drop index if exists "create index in_userinfo_openid";
drop index if exists "in_userinfo_openid";

create index "in_userinfo_openid"
    on userinfo (openid);