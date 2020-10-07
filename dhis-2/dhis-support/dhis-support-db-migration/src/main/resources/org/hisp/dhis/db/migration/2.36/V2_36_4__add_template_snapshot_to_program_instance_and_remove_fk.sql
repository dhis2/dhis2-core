alter table programnotificationinstance
    add programnotificationtemplatesnapshot jsonb;

alter table programnotificationinstance drop constraint fk_programstagenotification_pnt;

alter table programnotificationinstance alter column programnotificationtemplateid drop not null;
