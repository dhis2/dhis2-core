
------------------------------------------------------------------------
DISTRICT HEALTH INFORMATION SOFTWARE 2
------------------------------------------------------------------------

------------------------------------------------------------------------
HIBERNATE PROPERTIES
------------------------------------------------------------------------

# PostgreSQL

hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
hibernate.connection.driver_class = org.postgresql.Driver
hibernate.connection.url = jdbc:postgresql:dhis2
hibernate.connection.username = dhis
hibernate.connection.password = dhis

# MySQL

#hibernate.dialect = org.hibernate.dialect.MySQLDialect
#hibernate.connection.driver_class = com.mysql.jdbc.Driver
#hibernate.connection.url = jdbc:mysql://localhost/dhis2
#hibernate.connection.username = dhis
#hibernate.connection.password = dhis

# H2

#hibernate.dialect = org.hisp.dhis.dialect.H2Dialect
#hibernate.connection.driver_class = org.h2.Driver
#hibernate.connection.url = jdbc:h2:dhis2
#hibernate.connection.username = sa
#hibernate.connection.password =

# Derby

#hibernate.dialect = org.hisp.dhis.dialect.IdentityDerbyDialect
#hibernate.connection.url = jdbc:derby:dhis2;create=true
#hibernate.connection.driver_class = org.apache.derby.jdbc.EmbeddedDriver
#hibernate.connection.username = sa
#hibernate.connection.password =

# Autogenerate and update schema

hibernate.hbm2ddl.auto = update

# Various options

#hibernate.show_sql = true
#hibernate.format_sql = true

# File store configuration

# Supported providers are 'filesystem' and 'aws-s3'
filestore.provider = filesystem

# Bucket on AWS S3, root files folder within DHIS2 home on file system.
filestore.container = files

# Location/region, unapplicable if using AWS S3 standard region or filesystem
# NOTE: eu-central-1 is currently not supported
#filestore.location = eu-west-1

# Access key id on AWS, does not apply to file system
#filestore.identity = YOURIDENTITY

# Does not apply to file system
#filestore.secret = YOURSECRET

