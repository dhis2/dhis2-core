alter table trackedentity drop column if exists storedby;
alter table enrollment drop column if exists storedby;
alter table trackerevent drop column if exists storedby;
alter table singleevent drop column if exists storedby;