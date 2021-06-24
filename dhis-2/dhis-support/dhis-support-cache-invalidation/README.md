# How to use:

## Configuration


debezium.enabled = on
debezium.db.name = dhis2_2206_3
debezium.db.port = 5432
debezium.db.hostname = localhost
debezium.connection.username = dhis2_2206_3
debezium.connection.password = dhis
debezium.shutdown_on.connector_stop = on

### Postgres.conf

############ REPLICATION ##############
# pgoutput
# MODULES
#shared_preload_libraries = 'wal2json'

# Mandatory config variables
wal_level = logical              
max_wal_senders = 20              
max_replication_slots = 20


#https://gist.github.com/alexhwoods/4c4c90d83db3c47d9303cb734135130d

# Grant replication access to the DHIS2 database user
ALTER ROLE dhis2_2206_1 WITH replication;


#https://debezium.io/documentation/reference/postgres-plugins.html

#https://debezium.io/documentation/reference/connectors/postgresql.html
#https://debezium.io/documentation/reference/connectors/postgresql.html#setting-up-postgresql
#https://medium.com/@lmramos.usa/debezium-cdc-postgres-c9ce4da05ce1

# Clean/delete replication slots
SELECT * FROM pg_replication_slots ;
select pg_drop_replication_slot('bottledwater');

#
#CREATE ROLE replication WITH REPLICATION PASSWORD 'admin123' LOGIN
#CREATE ROLE replication WITH REPLICATION

# Grant replication access to the DHIS2 database user
ALTER ROLE dhis2_2206_1 WITH replication;

#Problems
* Datavalue objects dont get catched txid


* deletedobject missing in tx log
* audit missing ---

* add to cache on create event?
  
* delay/chunk full eviction on create.