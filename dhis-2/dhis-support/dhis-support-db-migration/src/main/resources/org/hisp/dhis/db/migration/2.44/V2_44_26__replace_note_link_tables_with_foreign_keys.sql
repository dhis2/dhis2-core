-- A note belongs to exactly one enrollment, tracker event or single event. Replace the
-- enrollment_notes, trackerevent_notes and singleevent_notes link tables with a foreign key to
-- each of them on note. The link tables were a leftover of the Hibernate list mapping; their
-- sort_order made concurrent appends to the same enrollment or event collide on the primary key.
-- DHIS2-22155

alter table note add column if not exists enrollmentid int8;
alter table note add column if not exists trackereventid int8;
alter table note add column if not exists singleeventid int8;

do $$
begin
    if to_regclass('enrollment_notes') is null
        or to_regclass('trackerevent_notes') is null
        or to_regclass('singleevent_notes') is null then
        return;
    end if;

    -- A note references one enrollment or event, but the link tables allow a note to be linked
    -- more than once. The update below would silently keep an arbitrary one of those links, so
    -- fail and let the admin decide which to keep.
    if exists (
        select 1
        from (
            select noteid from enrollment_notes
            union all select noteid from trackerevent_notes
            union all select noteid from singleevent_notes
        ) links
        group by noteid
        having count(*) > 1
    ) then
        raise exception 'There is inconsistent data in your DB. Please check https://github.com/dhis2/dhis2-releases/blob/master/releases/2.44/migration-notes.md#notes-linked-to-more-than-one-enrollment-or-event to have more information on the issue and to find ways to fix it.';
    end if;

    -- the check above guarantees at most one link per note, so each note matches one row
    update note n
    set enrollmentid = links.enrollmentid,
        trackereventid = links.trackereventid,
        singleeventid = links.singleeventid
    from (
        select noteid, enrollmentid, null::int8 as trackereventid, null::int8 as singleeventid
        from enrollment_notes
        union all
        select noteid, null, eventid, null from trackerevent_notes
        union all
        select noteid, null, null, eventid from singleevent_notes
    ) links
    where links.noteid = n.noteid;
end $$;

-- Notes without an enrollment or event cannot be reached through the API. Hard deleting enrollments
-- and events removed the link rows before the notes, and V2_43_21 did not copy the links of the
-- events it moved to inconsistentevent. Move them to inconsistentnote instead of deleting them so
-- admins can decide what to do with them.
do $$
begin
    if exists (select 1 from note where num_nonnulls(enrollmentid, trackereventid, singleeventid) = 0) then
        create table inconsistentnote as
        select noteid, uid, created, lastupdatedby, notetext
        from note
        where num_nonnulls(enrollmentid, trackereventid, singleeventid) = 0;

        delete from note where num_nonnulls(enrollmentid, trackereventid, singleeventid) = 0;

        raise warning 'There is inconsistent data in your DB. Please check https://github.com/dhis2/dhis2-releases/blob/master/releases/2.44/migration-notes.md#notes-without-an-enrollment-or-event to have more information on the issue and to find ways to fix it.';
    end if;
end $$;

drop table if exists enrollment_notes;
drop table if exists trackerevent_notes;
drop table if exists singleevent_notes;

alter table note drop constraint if exists fk_note_enrollmentid;
alter table note add constraint fk_note_enrollmentid
    foreign key (enrollmentid) references enrollment(enrollmentid) on delete cascade;
alter table note drop constraint if exists fk_note_trackereventid;
alter table note add constraint fk_note_trackereventid
    foreign key (trackereventid) references trackerevent(eventid) on delete cascade;
alter table note drop constraint if exists fk_note_singleeventid;
alter table note add constraint fk_note_singleeventid
    foreign key (singleeventid) references singleevent(eventid) on delete cascade;

alter table note drop constraint if exists note_check_enrollment_or_event;
alter table note add constraint note_check_enrollment_or_event
    check (num_nonnulls(enrollmentid, trackereventid, singleeventid) = 1);

-- partial indexes: each only holds the notes of its entity type
create index if not exists in_note_enrollmentid on note (enrollmentid)
    where enrollmentid is not null;
create index if not exists in_note_trackereventid on note (trackereventid)
    where trackereventid is not null;
create index if not exists in_note_singleeventid on note (singleeventid)
    where singleeventid is not null;

-- notes are only written via JDBC, let the DB assign noteid instead of every insert
alter table note alter column noteid set default nextval('note_sequence');
alter sequence note_sequence owned by note.noteid;
