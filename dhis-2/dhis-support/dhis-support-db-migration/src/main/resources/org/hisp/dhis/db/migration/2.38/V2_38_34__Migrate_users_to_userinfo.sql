alter table userinfo
    add column if not exists lastupdatedby bigint;
alter table userinfo
    add column if not exists creatoruserid bigint;
alter table userinfo
    add column if not exists username character varying(255);
alter table userinfo
    add column if not exists password character varying(60);
alter table userinfo
    add column if not exists secret text;
alter table userinfo
    add column if not exists twofa boolean;
alter table userinfo
    add column if not exists externalauth boolean;
alter table userinfo
    add column if not exists openid text;
alter table userinfo
    add column if not exists ldapid text;
alter table userinfo
    add column if not exists passwordlastupdated timestamp without time zone;
alter table userinfo
    add column if not exists lastlogin timestamp without time zone;
alter table userinfo
    add column if not exists restoretoken character varying(255);
alter table userinfo
    add column if not exists restoreexpiry timestamp without time zone;
alter table userinfo
    add column if not exists selfregistered boolean;
alter table userinfo
    add column if not exists invitation boolean;
alter table userinfo
    add column if not exists disabled boolean;
alter table userinfo
    add column if not exists "uuid" uuid;
alter table userinfo
    add column if not exists accountexpiry timestamp without time zone default null;
alter table userinfo
    add column if not exists idtoken character varying(255);

alter table userinfo drop column if exists restorecode;

alter table only userinfo
    add constraint uk_userinfo_username unique (username);


alter table only userrolemembers
    drop constraint if exists fk_userrolemembers_userid;
alter table only userrolemembers
    add constraint fk_userrolemembers_userid foreign key (userid) references userinfo (userinfoid);

alter table only users_catdimensionconstraints
    drop constraint if exists fk_users_catconstraints_userid;
alter table only users_catdimensionconstraints
    add constraint fk_users_catconstraints_userid foreign key (userid) references userinfo (userinfoid);

alter table only users_cogsdimensionconstraints
    drop constraint if exists fk_users_cogsconstraints_userid;
alter table only users_cogsdimensionconstraints
    add constraint fk_users_cogsconstraints_userid foreign key (userid) references userinfo (userinfoid);

alter table only previouspasswords
    drop constraint if exists fkg6n5kwuhypwdvkn15ke824kpb;
alter table only previouspasswords
    add constraint fkg6n5kwuhypwdvkn15ke824kpb foreign key (userid) references userinfo (userinfoid);

alter table only programstageinstance
    drop constraint if exists fk_programstageinstance_assigneduserid;
alter table programstageinstance
    add constraint fk_programstageinstance_assigneduserid foreign key (assigneduserid) references userinfo (userinfoid);

alter table only potentialduplicate
    drop constraint if exists potentialduplicate_lastupdatedby_user;
alter table potentialduplicate
    add constraint potentialduplicate_lastupdatedby_user foreign key (lastupdatebyusername) references userinfo (username);

alter table only programtempowner
    drop constraint if exists fk_programtempowner_userid;
alter table only programtempowner
    add constraint fk_programtempowner_userid foreign key (userid) references userinfo (userinfoid);

alter table only metadataproposal
    drop constraint if exists fk_createdby_userid;
alter table only metadataproposal
    add constraint fk_createdby_userid foreign key (createdby) references userinfo (userinfoid);

alter table only metadataproposal
    drop constraint if exists fk_finalisedby_userid;

alter table only metadataproposal
    add constraint fk_finalisedby_userid foreign key (finalisedby) references userinfo (userinfoid);

