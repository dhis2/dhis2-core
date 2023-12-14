drop table if exists metadataaudit;

update trackedentityaudit set audittype = 'CREATE' where audittype = 'create';
update trackedentityaudit set audittype = 'UPDATE' where audittype = 'update';
update trackedentityaudit set audittype = 'DELETE' where audittype = 'delete';
update trackedentityaudit set audittype = 'READ' where audittype = 'read';
update trackedentityaudit set audittype = 'SEARCH' where audittype = 'search';

update datavalueaudit set audittype = 'CREATE' where audittype = 'create';
update datavalueaudit set audittype = 'UPDATE' where audittype = 'update';
update datavalueaudit set audittype = 'DELETE' where audittype = 'delete';
update datavalueaudit set audittype = 'READ' where audittype = 'read';
update datavalueaudit set audittype = 'SEARCH' where audittype = 'search';

update trackedentityattributevalueaudit set audittype = 'CREATE' where audittype = 'create';
update trackedentityattributevalueaudit set audittype = 'UPDATE' where audittype = 'update';
update trackedentityattributevalueaudit set audittype = 'DELETE' where audittype = 'delete';
update trackedentityattributevalueaudit set audittype = 'READ' where audittype = 'read';
update trackedentityattributevalueaudit set audittype = 'SEARCH' where audittype = 'search';