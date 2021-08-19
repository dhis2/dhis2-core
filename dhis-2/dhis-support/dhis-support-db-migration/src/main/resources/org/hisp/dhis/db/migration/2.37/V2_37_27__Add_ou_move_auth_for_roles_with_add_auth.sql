insert into userroleauthorities (userroleid, authority)
select userroleid, 'F_ORGANISATIONUNIT_MOVE'
from userroleauthorities a
where a.authority = 'F_ORGANISATIONUNIT_ADD' and not exists
    (select 1
    from userroleauthorities b
    where a.userroleid = b.userroleid and b.authority = 'F_ORGANISATIONUNIT_MOVE'
    );
