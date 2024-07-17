# Citus Cluster installation and configuration

## 1 - Setup environment
### 1.1 - Install OS in the VM
For my tests I used [Ubuntu Server 24.04](https://ubuntu.com/download/server) in a virtualized environment.
The environment is composed by:

- `citus-1` : the coordinator node
- `citus-2` : a worker node
- `citus-3` : a worker node
- `citus-4` : a worker node

Each node is setuo with a static IP address. Also each node has proper entries in `/etc/hosts` like the following example:

`@citus-1:/etc/hosts` looks like this:

    127.0.0.1 localhost
    127.0.1.1 citus-1
    10.0.2.101 citus-1
    10.0.2.102 citus-2
    10.0.2.103 citus-3
    10.0.2.104 citus-4

the other nodes have similar entries, with the corresponding IP addresses.

### 1.2 - Operations to perform on ALL nodes

#### 1.2.1 - Install `Postgres + Citus extension + Postgis extension`
Execute the following commands on each node:

    curl https://install.citusdata.com/community/deb.sh | sudo bash
    sudo apt install postgresql-16-citus-12.0 postgresql-16-postgis-3
    sudo pg_conftool 16 main set shared_preload_libraries citus
    sudo pg_conftool 16 main set listen_addresses '*'

(at the time of writing this document, it's necessary to edit the `/etc/apt/sources.list.d/citusdata_community.list `
and replace `noble` with `jammy`, since citus has not released an official version for Ubuntu 24.04 yet)

#### 1.2.2 - Configure `Postgres` to allow unrestricted access to nodes in the local network

Edit the postgres `/etc/postgres/16/main/pg_hba.conf` file to allow unrestricted access to nodes in the local network:

    local all  all                trust

    # Allow unrestricted access to nodes in the local network. The following ranges
    # correspond to 24, 20, and 16-bit blocks in Private IPv4 address spaces.
    host  all  all  10.0.0.0/8    trust
    
    # Also allow the host unrestricted access to connect to itself
    host  all  all  127.0.0.1/32  trust
    host  all  all  ::1/128       trust

#### 1.2.3 - Enable postgres service and start postgres server
Execute

    systemctl enable postgresql.service
    systemctl start postgresql.service

#### 1.2.4 - Create DHIS database and DHIS user

With postgres user, using `psql`

    create user dhis password 'password'
    drop database if exists dhis;
    create database dhis with owner dhis;
    grant all privileges on database dhis to dhis;
    \c dhis
    create extension postgis;
    create extension citus;

> **_NOTE:_** if you want to clone a VM instance to create the other nodes, 
> remember to execute the following command to change the `Postgres` user password:<br/><br/>
> `drop extension citus;`<br/>
> `create extension citus;`

Import a sample DHIS database like, for example:

    wget -O - https://databases.dhis2.org/sierra-leone/dev/dhis2-db-sierra-leone.sql.gz | gunzip -c > dhis2-db-sierra-leone.sql
    psql dhis -U dhis < dhis2-db-sierra-leone.sql

### 1.3 - Steps to execute on the coordinator (in our example `citus-1`)
With postgres user, using `psql`

    SELECT citus_set_coordinator_host('citus-1', 5432);
    SELECT * from citus_add_node('citus-2', 5432);
    SELECT * from citus_add_node('citus-3', 5432);
    SELECT * from citus_add_node('citus-4', 5432);

### 1.4 - Running DHIS using Citus distributed tables:
Run DHIS using `citus-1:5432` as database connection and setting `analytics.citus.enabled` to `true` in `dhis.conf` file.
Then run a full analytics export.
