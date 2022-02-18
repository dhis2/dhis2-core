drop table if exists users;

create unique index in_userinfo_username
    on userinfo (username);

create unique index in_userinfo_openid
    on userinfo (openid);

create unique index in_userinfo_ldapid
    on userinfo (ldapid);

create unique index in_userinfo_uuid
    on userinfo (uuid);
