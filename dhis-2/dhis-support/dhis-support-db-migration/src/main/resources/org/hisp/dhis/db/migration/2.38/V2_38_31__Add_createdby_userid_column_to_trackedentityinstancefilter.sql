alter table trackedentityinstancefilter add column if not exists userid bigint;
alter table trackedentityinstancefilter drop constraint if exists fk_trackedentityinstancefilter_userid;
alter table trackedentityinstancefilter add constraint fk_trackedentityinstancefilter_userid foreign key (userid) references userinfo(userinfoid);
