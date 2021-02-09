alter table dataelementcategory alter column shortname set not null;
alter table if exists dataelementcategory
        add constraint unique_shortname unique (shortname);

