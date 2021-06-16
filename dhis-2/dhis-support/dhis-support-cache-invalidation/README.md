# How to use:

## Configuration

### Postgres.conf

############ REPLICATION ##############
# pgoutput
# MODULES
shared_preload_libraries = 'wal2json'

# REPLICATION
wal_level = logical              
max_wal_senders = 20              
max_replication_slots = 20


#https://debezium.io/documentation/reference/postgres-plugins.html

#https://debezium.io/documentation/reference/connectors/postgresql.html
#https://debezium.io/documentation/reference/connectors/postgresql.html#setting-up-postgresql
#https://medium.com/@lmramos.usa/debezium-cdc-postgres-c9ce4da05ce1

# Clean/delete replication slots
SELECT * FROM pg_replication_slots ;
select pg_drop_replication_slot('bottledwater');


#Problems
* Datavalue objects dont get catched txid

FIXED* Do we need to reset replication slot on each restart? YES!!
    - get rid of WARN in log about it

* All <set> in hbm files needs to be explicitly mapped

* deletedobject missing in tx log
* audit missing ---

* add to cache on create event?
* delay/chunk full eviction on create.