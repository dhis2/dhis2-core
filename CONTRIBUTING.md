# Contributing

## Requirements

You'll need the following software to run DHIS2 on your machine:

- Java 8
- Maven
- Tomcat

## Fork

Go to [GitHub](https://github.com/dhis2/dhis2-core) and fork the repository under your account.

Clone the repository:

    git clone git@github.com:dhis2/dhis2-core.git

## Install

Go in the repo and run maven:

    cd dhis-2
    mvn install
    cd dhis-web
    mvn install -U

Each project in the /dhis-2/dhis-web directory is an individual web module. The dhis-web-portal project is an assembly of all the individual web modules.

This should create a ready to use war in /dhis-2/dhis-web/dhis-web-portal/target

## Run locally

Deploy the war in Tomcat using either the manager or a simple copy to the webapp directory:

    cp dhis.war /opt/tomcat/webapps

Before starting tomcat, you need to create a DHIS2_HOME pointing to a folder on your machine:

     export DHIS2_HOME=~/dhis2_home 

this folder will host the `dhis.conf` file with at minimum your database settings. To start as fast as possible, you can use the H2 in memory database using the following configuration:

```properties
connection.dialect = org.hibernate.dialect.H2Dialect
connection.driver_class = org.h2.Driver
connection.url = jdbc:h2:./database/dhis2;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE
connection.username = sa
connection.password =
```

You can get such a file in your DHIS2_HOME folder with a simple call:

     wget -O $DHIS2_HOME/dhis.conf https://gist.githubusercontent.com/vanakenm/87b729fbf78ec52ca4c5da7856c62584/raw/9554680c8ab62d7f2bbecc3847406fa17d551a2e/dhis.conf 

You can now start tomcat and go to localhost:8080/dhis - get in with admin/district as user/password.
