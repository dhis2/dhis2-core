### Hibernate cache invalidation with Debezium

DHIS2 has the ability to invalidate its cache by listening to replication events emitted by the Postgres database, this
feature makes it possible to add new instances without having individual configuration for each "node".

The automatic cache invalidation is based on the open source project [Debezium](https://debezium.io/), it works by
listening the replication stream from the Postgres database and detect changes made by other nodes.

## Prerequisites:

* Postgres 10+
* Logical replication enabled
* A Postgres user with replication access

## Postgres configuration

To enable replication in your database, the following variables needs to be set in your postgres.conf file:

```
wal_level = logical              

# This number has to be the same or bigger as the number of 
# DHIS2 nodes/instances you intend to run at the same time.  
max_wal_senders = 20              

# This number has to be the same or bigger as the number of 
# DHIS2 nodes/instances you intend to run at the same time.
max_replication_slots = 20
```

> **Note**
>
> The database needs to be restarted for the replication feature to start.

## DHIS2 configuration

The following variables is required in the DHIS2 conf file.

```
debezium.enabled = on 
debezium.db.name = DHIS2_DATABASE_NAME
debezium.db.port = DHIS2_DATABASE_POST_NUMBER
debezium.db.hostname = DHIS2_DATABASE_HOSTNAME
debezium.connection.username = DHIS2_DATABASE_USER 
debezium.connection.password = DHIS2_DATABASE_PASSWORD

# If you want the server to shutdown if the replication connection is lost. (defaults off)
debezium.shutdown_on.connector_stop = off
```

## Enable replication access on the database user

Execute the following statement with an admin user to give replication access to your user.

```
ALTER ROLE [YOUR DHIS2 DATABASE USER] WITH replication;
```

## Potential issues

### Running out of available replication slots

Every time an instance is started a new replication slot is created in the database. On every normal shutdown of the
instance, the replication created on startup is automatically removed. However, if the server is not shutdown normally,
like for example on a power outage the replication slot will remain in the database until it is manually removed. Each
replication slot name includes the date it was created and a random string, such that no replication slot will ever have
the same name. The number of available replication slots is fixed and is determined by the postgres config variables:
'max_wal_senders' and 'max_replication_slots'. You can see that if you have a lot of stale replication slots you might
run out of available slots.

To manually remove old stale and unused replication slots you can use the following statements:

#### List all replication slots

```
SELECT * FROM pg_replication_slots;
```

#### Remove a stale replication slot

```
SELECT pg_drop_replication_slot('dhis2_1624530654__a890ba555e634f50983d4d6ad0fd63f1');
```

###             

### The Debezium engine lose its connection to the database

In the event that the replication connection fails and can not recover, the node will eventually be out of sync with
other nodes caches. To prevent this from happening you can enable the server to shut down if it detects it has lost
connection. This feature is disabled by default since it can take down the instance without warning. If however you have
several nodes in a typical load balanced setup and can tolerate that some nodes are down, and you have adequate
monitoring and alerting of your implementation you might consider enabling this. It can be enabled by setting the
"dhis.conf" file variable below to "on".

```
debezium.shutdown_on.connector_stop = on
```
