-- Migration related to DHIS2-20837.

insert into periodtype (periodtypeid, name) values (nextval('hibernate_sequence'), 'WeeklyFriday');

insert into configuration_dataoutputperiodtype (periodtypeid, configurationid) select p.periodtypeid, c.configurationid from periodtype p, configuration c where p.name = 'WeeklyFriday';
