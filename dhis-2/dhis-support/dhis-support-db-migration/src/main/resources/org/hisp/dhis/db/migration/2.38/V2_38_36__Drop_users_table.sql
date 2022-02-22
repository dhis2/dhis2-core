drop table if exists users CASCADE;

create unique index in_userinfo_username
    on userinfo (username);

create unique index in_userinfo_openid
    on userinfo (openid);

create unique index in_userinfo_ldapid
    on userinfo (ldapid);

create unique index in_userinfo_uuid
    on userinfo (uuid);

-- Clear all users without usernames, this can happen if there is no mach between the userinfo table and users table, these users will be useless anyway, aka. they would not work in the first place.

DELETE FROM usermembership WHERE userinfoid IN (SELECT userinfoid FROM userinfo where username is NULL);
DELETE FROM interpretation_comments WHERE interpretationid IN (SELECT interpretationid FROM interpretation WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM interpretation WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL);
DELETE FROM interpretation_comments WHERE interpretationcommentid IN (SELECT interpretationcommentid FROM interpretationcomment WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM interpretationcomment WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL);
DELETE FROM userdatavieworgunits WHERE userinfoid IN (SELECT userinfoid FROM userinfo where username is NULL);
DELETE FROM usergroupmembers WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL);

DELETE FROM messageconversation_usermessages WHERE usermessageid IN (SELECT usermessage.usermessageid FROM usermessage WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM usermessage WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL);

DELETE FROM visualization_rows WHERE visualizationid IN (SELECT visualization.visualizationid  FROM visualization WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM visualization_organisationunits WHERE visualizationid IN (SELECT visualization.visualizationid  FROM visualization WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM visualization_filters WHERE visualizationid IN (SELECT visualization.visualizationid  FROM visualization WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM visualization_datadimensionitems WHERE visualizationid IN (SELECT visualization.visualizationid  FROM visualization WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM visualization_columns WHERE visualizationid IN (SELECT visualization.visualizationid  FROM visualization WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL));
DELETE FROM visualization WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL);

DELETE FROM dashboarditem_users WHERE userid IN (SELECT userinfoid FROM userinfo where username is NULL);

delete from userinfo where username is NULL ;
