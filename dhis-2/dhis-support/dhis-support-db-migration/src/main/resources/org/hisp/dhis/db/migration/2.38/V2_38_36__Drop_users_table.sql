
-- Drop users table

drop table if exists users CASCADE;

-- Add unique indexes on userinfo

create unique index in_userinfo_username
    on userinfo (username);

create unique index in_userinfo_openid
    on userinfo (openid);

create unique index in_userinfo_ldapid
    on userinfo (ldapid);

create unique index in_userinfo_uuid
    on userinfo (uuid);

-- Set primitive user properties to false
-- Required to avoid startup failure for users with no match in userinfo table

update userinfo set twofa = false, externalauth = false, selfregistered = false, invitation = false, disabled = false
where username is null;
