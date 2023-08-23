# Citus Cluster installation and configuration

## 1 - Setup environment
### 1.1 - Install OS
For my tests I used [Ubuntu Server 22.04](https://ubuntu.com/download/server) in a virtualized environment.
The environment is composed by:
- `citus-1` : the coordinator node
- `citus-2` : a worker node
- `citus-3` : a worker node
- `citus-4` : a worker node

I set up each node with a dedicated static IP address, and added entries in `/etc/hosts` in each of them.

For example `@citus-1:/etc/hosts` looks like this:

    127.0.0.1 localhost  
    127.0.1.1 citus-1  
    10.0.2.101 citus-1  
    10.0.2.102 citus-2  
    10.0.2.103 citus-3  
    10.0.2.104 citus-4

### 1.2 - Operations to perform on ALL nodes

#### 1.2.1 - Install `Postgres + Citus extension + Postgis extension`
Execute the following commands on each node:

    curl https://install.citusdata.com/community/deb.sh | sudo bash
    sudo apt install postgresql-15-citus-12.0 postgresql-15-postgis-3
    sudo pg_conftool 15 main set shared_preload_libraries citus
    sudo pg_conftool 15 main set listen_addresses '*'

Edit the postgres `/etc/postgres/15/main/pg_hba.conf` file to allow unrestricted access to nodes in the local network:

    # Allow unrestricted access to nodes in the local network. The following ranges
    # correspond to 24, 20, and 16-bit blocks in Private IPv4 address spaces.
    host  all  all  10.0.0.0/8  trust
    
    # Also allow the host unrestricted access to connect to itself
    host  all  all  127.0.0.1/32  trust
    host  all  all  ::1/128  trust

#### 1.2.2 - Enable postgres service and start postgres server
Execute

    systemctl enable postgresql.service
    systemctl start postgresql.service

#### 1.2.3 - Create DHIS database and DHIS user

With postgres user, using `psql`

    create user  dhis password 'password'
    drop database if exists dhis;
    create database dhis with owner dhis;
    grant all privileges on dhis to dhis;
    \c dhis
    create extension postgis;
    create extension citus;

Import a sample DHIS database like, for example:

    psql dhis -U dhis < dhis2-db-sierra-leone.sql

### 1.3 - Steps to execute on the coordinator (`citus-1`)
With postgres user, using `psql`

    SELECT citus_set_coordinator_host('citus-1', 5432);
    SELECT * from citus_add_node('citus-2', 5432);
    SELECT * from citus_add_node('citus-3', 5432);
    SELECT * from citus_add_node('citus-4', 5432);

### 1.4 - Tests execution
#### 1.4.1 - Setup analytics tables
Run DHIS using `citus-1:5432` connection and run a full analytics export, including TEIs.
To force TEI analytics table creation, it might help to use:

    POST 'http://localhost:8080/dhis/api/resourceTables/analytics?executeTei=true

#### 1.4.2 - Create distributed tables from analytics ones:
In my test I just cloned the analytics TEI table to citus own  tables:
Execute the following `SQL` commands using dhis user:

    create table public.analytics_tei_neenwmsyuep_citus  
    (  
      trackedentityinstanceid integer  not null primary key,  
      trackedentityinstanceuid char(11) not null,  
      created timestamp,  
      lastupdated timestamp,  
      inactive boolean,  
      createdatclient timestamp,  
      lastupdatedatclient timestamp,  
      lastsynchronized timestamp,  
      geometry geometry,  
      longitude double precision,  
      latitude double precision,  
      featuretype varchar(255),  
      coordinates text,  
      storedby varchar(255),  
      potentialduplicate boolean,  
      uidlevel1 char(11),  
      uidlevel2 char(11),  
      uidlevel3 char(11),  
      uidlevel4 char(11),  
      ou char(11),  
      ouname varchar(255),  
      oucode varchar(50),  
      oulevel integer,  
      ounamehierarchy text,  
      createdbyusername varchar(255),  
      createdbyname varchar(255),  
      createdbylastname varchar(255),  
      createdbydisplayname varchar(255),  
      lastupdatedbyusername varchar(255),  
      lastupdatedbyname varchar(255),  
      lastupdatedbylastname varchar(255),  
      lastupdatedbydisplayname varchar(255),  
      "uy2gU8kT1jF" boolean,  
      "ur1Edk5Oe2n" boolean,  
      "IpHINAT79UW" boolean,  
      "fDd25txQckK" boolean,  
      "WSGAb5XwJ3Y" boolean,  
      "ZcBPrXKahq2" varchar(1200),  
      "DODgdr5Oo2v" varchar(1200),  
      "Qo571yj6Zcn" varchar(1200),  
      "ruQQnf6rswq" varchar(1200),  
      "gHGyrwKPzej" varchar(1200),  
      "OvY4VVhSDeJ" varchar(1200),  
      "VqEFza8wbwA" varchar(1200),  
      "A4xFHyieXys" varchar(1200),  
      "Agywv2JGwuq" varchar(1200),  
      "NDXw0cluzSw" varchar(1200),  
      "GUOBQt5K2WI" varchar(1200),  
      "lZGmxYbs97q" varchar(1200),  
      "w75KJ2mc4zz" varchar(1200),  
      "spFvx9FndA4" varchar(1200),  
      "iESIqZ0R0R0" varchar(1200),  
      "zDhUuAYrxNC" varchar(1200),  
      "VHfUeXpawmE" varchar(1200),  
      "n9nUvfpTsxQ" varchar(1200),  
      "FO4sWYJ64LQ" varchar(1200),  
      "ciq2USN94oJ" varchar(1200),  
      "lw1SqmMlnfh" varchar(1200),  
      "KmEUg2hHEtx" varchar(1200),  
      "RG7uGl4w5Jq" varchar(1200),  
      "H9IlTX2X6SL" varchar(1200),  
      "kyIzQsj96BD" varchar(1200),  
      "cejWyOfXge6" varchar(1200),  
      "gu1fqsmoU8r" varchar(1200),  
      "xs8A6tQJY0s" varchar(1200),  
      "P2cwLGskgxn" varchar(1200),  
      "G7vUx908SwP" varchar(1200),  
      "AuPLng5hLbE" varchar(1200),  
      "o9odfev2Ty5" varchar(1200)  
    ) with (autovacuum_enabled = false);  
      
    SELECT create_distributed_table('analytics_tei_neenwmsyuep_citus', 'trackedentityinstanceid');   
    insert into analytics_tei_neenwmsyuep_citus select * from analytics_tei_neenwmsyuep;  
    
    create table public.analytics_tei_enrollments_neenwmsyuep_citus  
    (  
      trackedentityinstanceuid char(11) not null,  
      programuid char(11),  
      programinstanceuid char(11),  
      enrollmentdate timestamp,  
      enddate timestamp,  
      incidentdate timestamp,  
      enrollmentstatus varchar(50),  
      pigeometry geometry,  
      pilongitude double precision,  
      pilatitude double precision,  
      uidlevel1 char(11),  
      uidlevel2 char(11),  
      uidlevel3 char(11),  
      uidlevel4 char(11),  
      ou char(11),  
      ouname varchar(255),  
      oucode char(32),  
      oulevel integer,  
      ounamehierarchy text,  
      primary key (trackedentityinstanceuid, programinstanceuid)  
    ) with (autovacuum_enabled = false);  
      
    SELECT create_distributed_table('analytics_tei_enrollments_neenwmsyuep_citus', 'trackedentityinstanceuid');      
    insert into analytics_tei_enrollments_neenwmsyuep_citus select * from analytics_tei_enrollments_neenwmsyuep;  
    
    create table public.analytics_tei_events_neenwmsyuep_citus  
    (  
      trackedentityinstanceuid char(11) not null,  
      programuid char(11),  
      programinstanceuid char(11),  
      programstageuid char(11),  
      programstageinstanceuid char(11),  
      executiondate timestamp,  
      lastupdated timestamp,  
      created timestamp,  
      duedate timestamp,  
      status varchar(50),  
      psigeometry geometry,  
      psilongitude double precision,  
      psilatitude double precision,  
      uidlevel1 char(11),  
      uidlevel2 char(11),  
      uidlevel3 char(11),  
      uidlevel4 char(11),  
      ou char(11),  
      ouname varchar(255),  
      oucode char(32),  
      oulevel integer,  
      eventdatavalues jsonb,  
      ounamehierarchy text,  
      primary key (trackedentityinstanceuid, programinstanceuid, programstageinstanceuid)  
    ) using columnar with (autovacuum_enabled = false);  
      
    SELECT create_distributed_table('analytics_tei_events_neenwmsyuep_citus', 'trackedentityinstanceuid');       
    insert into analytics_tei_events_neenwmsyuep_citus select * from analytics_tei_events_neenwmsyuep;  
    
    The query I used for testing is:
    
    select t_1."trackedentityinstanceuid" as "trackedentityinstanceuid",  
      t_1."lastupdated" as "lastupdated",  
      t_1."createdbydisplayname" as "createdbydisplayname",  
      t_1."lastupdatedbydisplayname" as "lastupdatedbydisplayname",  
      t_1."geometry" as "geometry",  
      t_1."longitude" as "longitude",  
      t_1."latitude" as "latitude",  
      t_1."ouname" as "ouname",  
      t_1."oucode" as "oucode",  
      t_1."ounamehierarchy" as "ounamehierarchy",  
      t_1."w75KJ2mc4zz" as "IpHINAT79UW.w75KJ2mc4zz",  
      t_1."zDhUuAYrxNC" as "IpHINAT79UW.zDhUuAYrxNC",  
      t_1."cejWyOfXge6" as "IpHINAT79UW.cejWyOfXge6",  
      t_1."lZGmxYbs97q" as "IpHINAT79UW.lZGmxYbs97q",  
      "t_1".lastupdated as "t_1.lastupdated",  
      ("IpHINAT79UW.ZzYYXq4fJie"."eventdatavalues" -> 'cYGaxwK615G' ->> 'value')::TEXT as "IpHINAT79UW.ZzYYXq4fJie.cYGaxwK615G"  
    from analytics_tei_neenwmsyuep_citus t_1  
             left join (select innermost_enr.*  
                        from (select *, row_number()  
                                     over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn  
                              from analytics_tei_enrollments_neenwmsyuep_citus  
                              where programuid = 'IpHINAT79UW') innermost_enr  
                        where innermost_enr.rn = 1) as "IpHINAT79UW"  
                       on t_1."trackedentityinstanceuid" = "IpHINAT79UW"."trackedentityinstanceuid"  
      left join (select innermost_evt.*  
                        from (select *, row_number() over (partition by programinstanceuid order by executiondate desc) as rn  
                              from analytics_tei_events_neenwmsyuep_citus  
                              where programuid = 'IpHINAT79UW'  
      and programstageuid = 'ZzYYXq4fJie') innermost_evt  
                        where innermost_evt.rn = 1) as "IpHINAT79UW.ZzYYXq4fJie"  
                       on "IpHINAT79UW"."programinstanceuid" =  
                          "IpHINAT79UW.ZzYYXq4fJie"."programinstanceuid"  
    where t_1."IpHINAT79UW"  
      and ("IpHINAT79UW.ZzYYXq4fJie"."eventdatavalues" -> 'cYGaxwK615G' ->> 'value')::TEXT = 'Negative-Conf'  
    order by "w75KJ2mc4zz" desc nulls last, "zDhUuAYrxNC" desc nulls last  
    limit 51 offset 0;
