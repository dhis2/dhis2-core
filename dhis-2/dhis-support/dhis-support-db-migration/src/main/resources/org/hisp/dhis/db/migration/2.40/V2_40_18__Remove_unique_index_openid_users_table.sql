drop index if exists in_userinfo_openid;

create index "create index in_userinfo_openid"
    on userinfo (openid);