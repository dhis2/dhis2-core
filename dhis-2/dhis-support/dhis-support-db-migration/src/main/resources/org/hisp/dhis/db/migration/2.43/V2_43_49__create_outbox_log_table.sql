create table if not exists eventhookoutboxlog (
    outboxtablename character varying(255) primary key,
    nextoutboxmessageid int8 not null,
    eventhookid int8 not null unique,
    constraint fk_eventhookoutboxlog_eventhookid foreign key (eventhookid) references eventhook(eventhookid) on delete cascade
);