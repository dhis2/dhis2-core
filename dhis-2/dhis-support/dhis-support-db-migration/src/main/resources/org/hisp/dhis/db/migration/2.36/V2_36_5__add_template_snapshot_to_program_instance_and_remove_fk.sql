alter table if exists programnotificationinstance
    add programnotificationtemplatesnapshot jsonb;

alter table if exists programnotificationinstance drop constraint if exists fk_programstagenotification_pnt;

alter table if exists programnotificationinstance alter column programnotificationtemplateid drop not null;
