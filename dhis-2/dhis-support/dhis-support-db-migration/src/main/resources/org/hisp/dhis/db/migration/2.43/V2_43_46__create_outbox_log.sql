create table if not exists outboxlog (
    outboxtablename character varying(255) primary key,
    lastprocessedid int8 not null,
    eventhookid int8 not null unique,
    constraint fk_outboxlog_eventhookid foreign key (eventhookid) references eventhook(eventhookid) on delete cascade
);