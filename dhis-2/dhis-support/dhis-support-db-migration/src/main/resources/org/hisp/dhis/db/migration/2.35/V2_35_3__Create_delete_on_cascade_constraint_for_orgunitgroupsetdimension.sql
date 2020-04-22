alter table orgunitgroupsetdimension_items drop constraint fk_dimension_items_orgunitgroupid;

alter table orgunitgroupsetdimension_items
    add constraint fk_dimension_items_orgunitgroupid
        foreign key (orgunitgroupid) references orgunitgroup
            on delete cascade;