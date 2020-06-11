alter table dataelementoperand drop constraint fk_dataelementoperand_dataelement;

alter table dataelementoperand
    add constraint fk_dataelementoperand_dataelement
        foreign key (dataelementid)
            references dataelement (dataelementid)
            on delete cascade;
