alter table dataapproval
    add column if not exists lastupdated timestamp;

alter table dataapproval
    add column if not exists lastupdatedby bigint;

alter table dataapproval
    add constraint fk_dataapproval_lastupdateby foreign key (lastupdatedby) references userinfo(userinfoid);
