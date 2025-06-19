/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.external.conf;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import org.hisp.dhis.security.utils.CspConstants;

/**
 * @author Lars Helge Overland
 */
@Getter
public enum ConfigurationKey {
  /** System mode for database read operations only, can be 'off', 'on'. (default: off). */
  SYSTEM_READ_ONLY_MODE("system.read_only_mode", Constants.OFF, false),

  /** Session timeout in seconds. (default: 3600). */
  SYSTEM_SESSION_TIMEOUT("system.session.timeout", "3600", false),

  /** System monitoring URL. */
  SYSTEM_MONITORING_URL("system.monitoring.url"),

  /** System monitoring username. */
  SYSTEM_MONITORING_USERNAME("system.monitoring.username"),

  /** System monitoring password (sensitive). */
  SYSTEM_MONITORING_PASSWORD("system.monitoring.password"),

  /** SQL view protected tables, can be 'on', 'off'. (default: on) */
  SYSTEM_SQL_VIEW_TABLE_PROTECTION("system.sql_view_table_protection", Constants.ON, false),

  /** SQL view write enabled, can be 'on', 'off'. (default: off) */
  SYSTEM_SQL_VIEW_WRITE_ENABLED("system.sql_view_write_enabled", Constants.OFF, false),

  /**
   * Set the maximum size for the cache instance to be built. If set to 0, no caching will take
   * place. Cannot be a negative value. (default: 0).
   */
  SYSTEM_CACHE_MAX_SIZE_FACTOR("system.cache.max_size.factor", "0.5", false),

  /** Node identifier, optional, useful in clusters. */
  NODE_ID("node.id", "", false),

  /**
   * When true, the node will unconditionally set its node ID during leader election causing it to
   * win the election as long as it is alive.
   */
  NODE_PRIMARY_LEADER("node.primary_leader", "false", false),

  /** Encryption password (sensitive). */
  ENCRYPTION_PASSWORD("encryption.password", "", true),

  /** Hibernate SQL dialect. */
  CONNECTION_DIALECT("connection.dialect", "", false),

  /** Analytics database platform. */
  ANALYTICS_DATABASE("analytics.database", "POSTGRESQL", false),

  /** Analytics database JDBC catalog name. Applies to Apache Doris. */
  ANALYTICS_DATABASE_CATALOG("analytics.database.catalog", "pg_dhis", false),

  /** Analytics database JDBC driver filename. Applies to Apache Doris. */
  ANALYTICS_DATABASE_DRIVER_FILENAME("analytics.database.driver_filename", "postgresql.jar", false),

  /** JDBC driver class. */
  CONNECTION_DRIVER_CLASS("connection.driver_class", "org.postgresql.Driver", false),

  /** Analytics JDBC driver class. */
  ANALYTICS_CONNECTION_DRIVER_CLASS("analytics.connection.driver_class", "", false),

  /** Database connection URL. */
  CONNECTION_URL("connection.url", "", false),

  /** Analytics Database connection URL. */
  ANALYTICS_CONNECTION_URL("analytics.connection.url", "", false),

  /** Database username. */
  CONNECTION_USERNAME("connection.username", "", false),

  /** Analytics Database username. */
  ANALYTICS_CONNECTION_USERNAME("analytics.connection.username", "", false),

  /** Database password (sensitive). */
  CONNECTION_PASSWORD("connection.password", "", true),

  /** Database host (hostname or IP). Applies to ClickHouse. */
  CONNECTION_HOST("connection.host", "", false),

  /** Database port number. Applies to ClickHouse. */
  CONNECTION_PORT("connection.port", "5432", false),

  /** Database port number. Applies to ClickHouse. */
  CONNECTION_DATABASE("connection.database", "", false),

  /** Analytics Database password (sensitive). */
  ANALYTICS_CONNECTION_PASSWORD("analytics.connection.password", "", true),

  /** Sets 'hibernate.cache.use_second_level_cache'. (default: true) */
  USE_SECOND_LEVEL_CACHE("hibernate.cache.use_second_level_cache", "true", false),

  /** Sets 'hibernate.cache.use_query_cache'. (default: true) */
  USE_QUERY_CACHE("hibernate.cache.use_query_cache", "true", false),

  /** Max size of connection pool (default: 80). */
  CONNECTION_POOL_MAX_SIZE("connection.pool.max_size", "80", false),

  /** Analytics Max size of connection pool (default: 80). */
  ANALYTICS_CONNECTION_POOL_MAX_SIZE("analytics.connection.pool.max_size", "80", false),

  /** Minimum number of Connections a pool will maintain at any given time (default: 5). */
  CONNECTION_POOL_MIN_SIZE("connection.pool.min_size", "5", false),

  /**
   * Analytics Minimum number of Connections a pool will maintain at any given time (default: 5).
   */
  ANALYTICS_CONNECTION_POOL_MIN_SIZE("analytics.connection.pool.min_size", "5", false),

  /**
   * Number of Connections a pool will try to acquire upon startup. Should be between minPoolSize
   * and maxPoolSize. (default: 5).
   */
  CONNECTION_POOL_INITIAL_SIZE("connection.pool.initial_size", "5", false),

  /**
   * Number of Connections a pool will try to acquire upon startup. Should be between minPoolSize
   * and maxPoolSize. (default: 5).
   */
  ANALYTICS_CONNECTION_POOL_INITIAL_SIZE("analytics.connection.pool.initial_size", "5", false),

  /**
   * Determines how many connections at a time will try to acquire when the pool is exhausted.
   * (default: 5).
   */
  CONNECTION_POOL_ACQUIRE_INCR("connection.pool.acquire_incr", "5", false),

  /**
   * Determines how many times the system will try to acquire a connection before giving up. If this
   * value is less than or equal to zero, the system will keep trying indefinitely. (default: 30).
   */
  CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS("connection.pool.acquire_retry_attempts", "30", false),

  /**
   * Determines the delay in milliseconds, c3p0 will wait between acquire attempts. (default: 1000)
   */
  CONNECTION_POOL_ACQUIRE_RETRY_DELAY("connection.pool.acquire_retry_delay", "1", false),

  /**
   * Determines how many connections at a time will try to acquire when the pool is exhausted.
   * (default: 5).
   */
  ANALYTICS_CONNECTION_POOL_ACQUIRE_INCR("analytics.connection.pool.acquire_incr", "5", false),

  /**
   * Determines how many times the system will try to acquire a connection before giving up. If this
   * value is less than or equal to zero, the system will keep trying indefinitely. (default: 30).
   */
  ANALYTICS_CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS(
      "analytics.connection.pool.acquire_retry_attempts", "30", false),

  /**
   * Determines the delay in milliseconds, c3p0 will wait between acquire attempts. (default: 1000)
   */
  ANALYTICS_CONNECTION_POOL_ACQUIRE_RETRY_DELAY(
      "analytics.connection.pool.acquire_retry_delay", "1", false),

  /**
   * Seconds a Connection can remain pooled but unused before being discarded. Zero means idle
   * connections never expire (default: 7200).
   */
  CONNECTION_POOL_MAX_IDLE_TIME("connection.pool.max_idle_time", "7200", false),

  /**
   * Seconds a Connection can remain pooled but unused before being discarded. Zero means idle
   * connections never expire (default: 7200).
   */
  ANALYTICS_CONNECTION_POOL_MAX_IDLE_TIME("analytics.connection.pool.max_idle_time", "7200", false),

  /** Minimum number of idle connections to maintain (default: 10). */
  CONNECTION_POOL_MIN_IDLE("connection.pool.min_idle", "10", false),

  /** Minimum number of idle connections to maintain (default: 10). */
  ANALYTICS_CONNECTION_POOL_MIN_IDLE("analytics.connection.pool.min_idle", "10", false),

  /**
   * Number of seconds that Connections in excess of minPoolSize should be permitted to remain idle
   * in the pool before being culled (default: 0).
   */
  CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON("connection.pool.max_idle_time_excess_con", "0", false),

  /**
   * Number of seconds that Connections in excess of minPoolSize should be permitted to remain idle
   * in the pool before being culled (default: 0).
   */
  ANALYTICS_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON(
      "analytics.connection.pool.max_idle_time_excess_con", "0", false),

  /**
   * If this is a number greater than 0, dhis2 will test all idle, pooled but unchecked-out
   * connections, every this number of seconds (default: 0).
   */
  CONNECTION_POOL_IDLE_CON_TEST_PERIOD("connection.pool.idle.con.test.period", "0", false),

  /**
   * If this is a number greater than 0, dhis2 will test all idle, pooled but unchecked-out
   * connections, every this number of seconds (default: 0).
   */
  ANALYTICS_CONNECTION_POOL_IDLE_CON_TEST_PERIOD(
      "analytics.connection.pool.idle.con.test.period", "0", false),

  /**
   * If true, an operation will be performed at every connection checkout to verify that the
   * connection is valid (default: false).
   */
  CONNECTION_POOL_TEST_ON_CHECKOUT("connection.pool.test.on.checkout", Constants.OFF, false),

  /**
   * If true, an operation will be performed at every connection checkout to verify that the
   * connection is valid (default: false).
   */
  ANALYTICS_CONNECTION_POOL_TEST_ON_CHECKOUT(
      "analytics.connection.pool.test.on.checkout", Constants.OFF, false),

  /**
   * If true, an operation will be performed asynchronously at every connection checkin to verify
   * that the connection is valid (default: true).
   */
  CONNECTION_POOL_TEST_ON_CHECKIN("connection.pool.test.on.checkin", Constants.ON, false),

  /**
   * If true, an operation will be performed asynchronously at every connection checkin to verify
   * that the connection is valid (default: true).
   */
  ANALYTICS_CONNECTION_POOL_TEST_ON_CHECKIN(
      "analytics.connection.pool.test.on.checkin", Constants.ON, false),

  /**
   * Hikari DB pool feature. Connection pool timeout: Set the maximum number of milliseconds that a
   * client will wait for a connection from the pool. (default: 30s)
   */
  CONNECTION_POOL_TIMEOUT("connection.pool.timeout", String.valueOf(SECONDS.toMillis(30)), false),

  /**
   * Hikari DB pool feature. Connection leak detection threshold: Set the maximum number of
   * milliseconds that a connection can be out of the pool before a message is logged. (default: 0 -
   * no leak detection)
   */
  CONNECTION_POOL_WARN_MAX_AGE("connection.pool.warn_max_age", "0", false),
  /**
   * Analytics Hikari DB pool feature. Connection pool timeout: Set the maximum number of
   * milliseconds that a client will wait for a connection from the pool. (default: 30s)
   */
  ANALYTICS_CONNECTION_POOL_TIMEOUT(
      "analytics.connection.pool.timeout", String.valueOf(SECONDS.toMillis(30)), false),

  /**
   * Sets the maximum number of milliseconds that the Hikari pool will wait for a connection to be
   * validated as alive. (default: 5ms)
   */
  CONNECTION_POOL_VALIDATION_TIMEOUT(
      "connection.pool.validation_timeout", String.valueOf(SECONDS.toMillis(5)), false),

  /**
   * Sets the maximum number of milliseconds that the Analytics Hikari pool will wait for a
   * connection to be validated as alive. (default: 5ms)
   */
  ANALYTICS_CONNECTION_POOL_VALIDATION_TIMEOUT(
      "analytics.connection.pool.validation_timeout", String.valueOf(SECONDS.toMillis(5)), false),

  /** Configure the number of helper threads used by C3P0 pool for jdbc operations (default: 3). */
  CONNECTION_POOL_NUM_THREADS("connection.pool.num.helper.threads", "3", false),

  /**
   * Configure the number of helper threads used by Analytics C3P0 pool for jdbc operations
   * (default: 3).
   */
  ANALYTICS_CONNECTION_POOL_NUM_THREADS("analytics.connection.pool.num.helper.threads", "3", false),

  /**
   * Defines the query that will be executed for all connection tests. Ideally this config is not
   * needed as postgresql driver already provides an efficient test query. The config is exposed
   * simply for evaluation, do not use it unless there is a reason to.
   */
  CONNECTION_POOL_TEST_QUERY("connection.pool.preferred.test.query"),

  /** Defines the query that will be executed for all Analytics connection tests. */
  ANALYTICS_CONNECTION_POOL_TEST_QUERY("analytics.connection.pool.preferred.test.query"),

  /** LDAP server URL. (default: ldaps://0:1) */
  LDAP_URL("ldap.url", "ldaps://0:1", false),

  /** LDAP manager user distinguished name. */
  LDAP_MANAGER_DN("ldap.manager.dn", "", false),

  /** LDAP manager user password (sensitive). */
  LDAP_MANAGER_PASSWORD("ldap.manager.password", "", true),

  /** LDAP entry distinguished name search base. */
  LDAP_SEARCH_BASE("ldap.search.base", "", false),

  /** LDAP entry distinguished name filter. (default: (cn={0}) ). */
  LDAP_SEARCH_FILTER("ldap.search.filter", "(cn={0})", false),

  /**
   * File store provider, currently 'filesystem', 'aws-s3' and 's3' are supported. (default:
   * filesystem)
   */
  FILESTORE_PROVIDER("filestore.provider", "filesystem", false),

  /**
   * Directory / bucket name, folder below DHIS2_HOME on file system, 'bucket' on AWS S3. (default:
   * files)
   */
  FILESTORE_CONTAINER("filestore.container", "files", false),

  /** Datacenter location (not required). */
  FILESTORE_LOCATION("filestore.location", "", false),

  /** URL where the S3 compatible API can be accessed (only for provider 's3') */
  FILESTORE_ENDPOINT("filestore.endpoint", "", false),

  /** Public identity / username. */
  FILESTORE_IDENTITY("filestore.identity", "", false),

  /** Secret key / password (sensitive). */
  FILESTORE_SECRET("filestore.secret", "", true),

  /** The Google service account client id. */
  GOOGLE_SERVICE_ACCOUNT_CLIENT_ID("google.service.account.client.id", "", false),

  /**
   * Maximum number of retries (if any of the steps fail) for the metadata sync task. (default: 3)
   */
  META_DATA_SYNC_RETRY("metadata.sync.retry", "3", false),

  /** Sets up {@see RetryTemplate} retry frequency. (default: 30000) */
  META_DATA_SYNC_RETRY_TIME_FREQUENCY_MILLISEC(
      "metadata.sync.retry.time.frequency.millisec", "30000", false),

  /**
   * Remote servers allowed to call. <br>
   * Default is empty. <br>
   * Servers should be in a comma-separated style and always end with '/' for security reasons <br>
   * e.g. metadata.sync.remote_servers_allowed = https://server1.com/,https://server2.com/
   */
  META_DATA_SYNC_SERVERS_ALLOWED("metadata.sync.remote_servers_allowed", "", false),

  /** EHCache replication host. */
  CLUSTER_HOSTNAME("cluster.hostname", "", false),

  /** EHCache replication members. */
  CLUSTER_MEMBERS("cluster.members", "", false),

  /** DEPRECATED EHCache replication port. */
  CLUSTER_CACHE_PORT("cluster.cache.port", "4001", false),

  /** DEPRECATED EHCache replication remote object port. */
  CLUSTER_CACHE_REMOTE_OBJECT_PORT("cluster.cache.remote.object.port", "0", false),

  /** Enable redis cache. (default: false) */
  REDIS_ENABLED("redis.enabled", Constants.OFF, false),

  /** Redis host to use for cache. (default: localhost) */
  REDIS_HOST("redis.host", "localhost", false),

  /** Redis port to use for cache. (default: 6379) */
  REDIS_PORT("redis.port", "6379", false),

  /** Redis password to use for cache. (sensitive) */
  REDIS_PASSWORD("redis.password", "", true),

  /** Use SSL for connecting to redis. (default: false) */
  REDIS_USE_SSL("redis.use.ssl", Constants.OFF, false),

  /**
   * Allows Flyway migrations to be run "out of order".
   *
   * <p>If you already have versions 1 and 3 applied, and now a version 2 is found, it will be
   * applied too instead of being ignored.
   */
  FLYWAY_OUT_OF_ORDER_MIGRATION("flyway.migrate_out_of_order", Constants.OFF, false),

  /**
   * Repairs the Flyway schema history table on startup before Flyway migrations is applied.
   * (default: false).
   */
  FLYWAY_REPAIR_BEFORE_MIGRATION("flyway.repair_before_migration", Constants.OFF, false),

  /** Whether to skip Flyway migration on startup. (default: false). */
  FLYWAY_SKIP_MIGRATION("flyway.skip_migration", Constants.OFF, false),

  PROGRAM_TEMPORARY_OWNERSHIP_TIMEOUT("tracker.temporary.ownership.timeout", "3", false),

  /** Use unlogged tables during analytics export. (default: ON) */
  ANALYTICS_TABLE_UNLOGGED("analytics.table.unlogged", Constants.ON),

  /**
   * Skip building indexes for dimensional columns on analytics tables for the comma-separated list
   * of dimension identifiers. Experimental.
   */
  ANALYTICS_TABLE_SKIP_INDEX("analytics.table.skip_index", "", false),

  /**
   * Skip creating columns for analytics tables for the comma-separated list of dimensional
   * identifiers. Experimental.
   */
  ANALYTICS_TABLE_SKIP_COLUMN("analytics.table.skip_column", "", false),

  /**
   * Artemis support mode, 2 modes supported: EMBEDDED (starts up an embedded Artemis which lives in
   * the same process as your DHIS2 instance), NATIVE (connects to an external Artemis instance,
   * remember to set username / password if required). (default: EMBEDDED)
   */
  ARTEMIS_MODE("artemis.mode", "EMBEDDED"),

  /** Artemis host to use for connection (only relevant for NATIVE mode). (default: 127.0.0.1) */
  ARTEMIS_HOST("artemis.host", "127.0.0.1"),

  /** Artemis port to use for connection (only relevant for NATIVE mode). (default: 25672) */
  ARTEMIS_PORT("artemis.port", "25672"),

  /**
   * Artemis username to use for connection (only relevant for NATIVE mode). (default: guest)
   * (sensitive)
   */
  ARTEMIS_USERNAME("artemis.username", "guest", true),

  /**
   * Artemis password to use for connection (only relevant for NATIVE mode). (default: guest)
   * (sensitive)
   */
  ARTEMIS_PASSWORD("artemis.password", "guest", true),

  /**
   * Enable/disable security for the embedded Artemis. (default: false).
   *
   * <p>If enabled will use {@link #ARTEMIS_USERNAME} and {@link #ARTEMIS_PASSWORD}.
   */
  ARTEMIS_EMBEDDED_SECURITY("artemis.embedded.security", Constants.OFF),

  /**
   * Enable/disable Artemis persistence. (default. false).
   *
   * <p>We do not currently support any kind of recovery on failure, so this should only be set if
   * the volume on the queue is too high, and we want to fall back to disk storage.
   */
  ARTEMIS_EMBEDDED_PERSISTENCE("artemis.embedded.persistence", Constants.OFF),

  /**
   * Number of threads to use for processing incoming packets. (default: 5).
   *
   * <p>This can also be set to "-1" which means use the Artemis default, which is number of cores
   * times 3.
   */
  ARTEMIS_EMBEDDED_THREADS("artemis.embedded.threads", "5"),

  /**
   * Max filesize for log files in "HOME/logs/" directory. Does not affect size of audit logs.
   * (default: 100MB).
   */
  LOGGING_FILE_MAX_SIZE("logging.file.max_size", "100MB"),

  /** Number of log file archives to keep around. Does not affect audit logs. (default: 1). */
  LOGGING_FILE_MAX_ARCHIVES("logging.file.max_archives", "1"),

  /**
   * Adds a hashed (SHA-256) session_id to each log line. Useful for tracking which user is
   * responsible for the logging line.
   */
  LOGGING_REQUEST_ID_ENABLED("logging.request_id.enabled", Constants.ON, false),

  /** Base URL to the DHIS 2 instance. */
  SERVER_BASE_URL("server.base.url", "", false),

  /**
   * @deprecated use META_DATA_SYNC_SERVERS_ALLOWED instead
   */
  @Deprecated
  REMOTE_SERVERS_ALLOWED("system.remote_servers_allowed", "", false),

  /** Enable secure settings if system is deployed on HTTPS, can be 'off', 'on'. */
  SERVER_HTTPS("server.https", Constants.OFF),

  /** DHIS2 API monitoring. */
  MONITORING_API_ENABLED("monitoring.api.enabled", Constants.OFF, false),

  /** JVM monitoring. */
  MONITORING_JVM_ENABLED("monitoring.jvm.enabled", Constants.OFF, false),

  /** Database connection pool monitoring. (default: off) */
  MONITORING_DBPOOL_ENABLED("monitoring.dbpool.enabled", Constants.OFF, false),

  /** Hibernate monitoring, do not use in production. (default: off) */
  MONITORING_HIBERNATE_ENABLED("monitoring.hibernate.enabled", Constants.OFF, false),

  /** Uptime monitoring. (default: off) */
  MONITORING_UPTIME_ENABLED("monitoring.uptime.enabled", Constants.OFF, false),

  /** CPU monitoring. (default: off) */
  MONITORING_CPU_ENABLED("monitoring.cpu.enabled", Constants.OFF, false),

  /** AppHub base URL. (default: https://apps.dhis2.org). */
  APPHUB_BASE_URL("apphub.base.url", "https://apps.dhis2.org", false),

  /** AppHub api URL. (default: https://apps.dhis2.org/api). */
  APPHUB_API_URL("apphub.api.url", "https://apps.dhis2.org/api", false),

  /**
   * Enable/disable changelog/history log of aggregate data values. <br>
   * (default: on)
   */
  CHANGELOG_AGGREGATE("changelog.aggregate", Constants.ON),

  /**
   * Enable/disable changelog/history log of tracker data values. <br>
   * (default: on)
   */
  CHANGELOG_TRACKER("changelog.tracker", Constants.ON),

  /** Use in-memory queue before sending audits into the Artemis queue. (default: off). */
  AUDIT_USE_IN_MEMORY_QUEUE_ENABLED(
      "audit.in_memory-queue.enabled",
      Constants.OFF,
      false,
      new String[] {"audit.inmemory_queue.enabled"}),

  /** Send audits to "logs/dhis-audit.log". (default: on). */
  AUDIT_LOGGER("audit.logger", Constants.ON, false),

  /** Save audits to database table "audit". (default: off). */
  AUDIT_DATABASE("audit.database", Constants.OFF, false),

  /** Sets the audit matrix for metadata. (default: none). */
  AUDIT_METADATA_MATRIX("audit.metadata", "", false),

  /** Sets the audit matrix for aggregate. (default: none). */
  AUDIT_AGGREGATE_MATRIX("audit.aggregate", "", false),

  /** Sets the audit matrix for tracker. (default: none). */
  AUDIT_TRACKER_MATRIX("audit.tracker", "", false),

  /** Enable OIDC. (default: off). */
  OIDC_OAUTH2_LOGIN_ENABLED("oidc.oauth2.login.enabled", Constants.OFF, false),

  /**
   * DHIS 2 instance URL, do not end with a slash, not all IdPs support logout (Where to end up
   * after calling end_session_endpoint on the IdP).
   */
  OIDC_LOGOUT_REDIRECT_URL("oidc.logout.redirect_url", "", false),

  /**
   * Google IdP specific parameters. Provider client ID: This is the identifier that the IdP
   * assigned to your application. (sensitive)
   */
  OIDC_PROVIDER_GOOGLE_CLIENT_ID("oidc.provider.google.client_id", "", true),

  /**
   * Google IdP specific parameters. Provider client secret: This value is a secret and should be
   * kept secure. (sensitive)
   */
  OIDC_PROVIDER_GOOGLE_CLIENT_SECRET("oidc.provider.google.client_secret", "", true),

  /** Google IdP specific parameters. Mapping claim: *Optional. (default: email). (sensitive) */
  OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM("oidc.provider.google.mapping_claim", "email", true),

  /**
   * Google IdP specific parameters. Redirect URL: DHIS 2 instance URL, do not end with a slash.
   * (sensitive) <br>
   * e.g. https://dhis2.org/demo.
   */
  OIDC_PROVIDER_GOOGLE_REDIRECT_URI("oidc.provider.google.redirect_url", "", true),

  /**
   * WSO2 IdP specific parameters. Provider client ID: This is the identifier that the IdP assigned
   * to your application.
   */
  OIDC_PROVIDER_WSO2_CLIENT_ID("oidc.provider.wso2.client_id", "", false),

  /**
   * WSO2 IdP specific parameters. Provider client secret: This value is a secret and should be kept
   * secure.
   */
  OIDC_PROVIDER_WSO2_CLIENT_SECRET("oidc.provider.wso2.client_secret", "", false),

  /** WSO2 IdP specific parameters. Mapping claim: *Optional. (default: email). */
  OIDC_PROVIDER_WSO2_MAPPING_CLAIM("oidc.provider.wso2.mapping_claim", "email", false),

  /** WSO2 IdP specific parameters. Server URL */
  OIDC_PROVIDER_WSO2_SERVER_URL("oidc.provider.wso2.server_url", "", false),

  /**
   * WSO2 IdP specific parameters. Redirect URL: DHIS 2 instance URL, do not end with a slash, <br>
   * e.g. https://dhis2.org/demo.
   */
  OIDC_PROVIDER_WSO2_REDIRECT_URI("oidc.provider.wso2.redirect_url", "", false),

  /** WSO2 IdP specific parameters. Display alias */
  OIDC_PROVIDER_WSO2_DISPLAY_ALIAS("oidc.provider.wso2.display_alias", "", false),

  /** WSO2 IdP specific parameters. Enable logout */
  OIDC_PROVIDER_WSO2_ENABLE_LOGOUT("oidc.provider.wso2.enable_logout", Constants.ON, false),

  /** Database debugging feature. Defines threshold for logging of slow queries in the log. */
  SLOW_QUERY_LOGGING_THRESHOLD_TIME_MS(
      "slow.query.logging.threshold.time", String.valueOf(SECONDS.toMillis(1)), false),

  /** Database debugging feature. Enables logging of all SQL queries to the log. */
  ENABLE_QUERY_LOGGING("enable.query.logging", Constants.OFF, false),

  /** Database debugging feature. Defines database logging before/after methods */
  METHOD_QUERY_LOGGING_ENABLED("method.query.logging.enabled", Constants.OFF, false),

  /** Database debugging feature. Enable time query logging. */
  ELAPSED_TIME_QUERY_LOGGING_ENABLED("elapsed.time.query.logging.enabled", Constants.OFF, false),

  /** Database datasource pool type. Supported pool types are: c3p0 (default), hikari, unpooled */
  DB_POOL_TYPE("db.pool.type", "c3p0", false),

  /**
   * @TODO
   */
  ACTIVE_READ_REPLICAS("active.read.replicas", "0", false),

  /**
   * Allows enabling/disabling audits system-wide (without configuring the audit matrix). (default:
   * true)
   */
  AUDIT_ENABLED("system.audit.enabled", Constants.ON, false),

  /** JWT OIDC token authentication feature. Enable or disable. */
  ENABLE_JWT_OIDC_TOKEN_AUTHENTICATION(
      "oidc.jwt.token.authentication.enabled", Constants.OFF, false),

  /** API authentication feature. Enable or disable personal access tokens. */
  ENABLE_API_TOKEN_AUTHENTICATION("enable.api_token.authentication", Constants.ON, false),

  /** System update notifications system. Enable or disable the feature. */
  SYSTEM_UPDATE_NOTIFICATIONS_ENABLED("system.update_notifications_enabled", Constants.ON, false),

  /**
   * Number of possible concurrent sessions on different computers or browsers for each user. If
   * configured to 1, the user will be logged out from any other session when a new session is
   * started.
   */
  MAX_SESSIONS_PER_USER("max.sessions.per_user", "10", false),

  /** Redis based cache invalidation feature. Enable or disable. */
  REDIS_CACHE_INVALIDATION_ENABLED("redis.cache.invalidation.enabled", Constants.OFF, false),

  /** Content Security Policy feature. Enable or disable the feature. (sensitive) */
  CSP_ENABLED("csp.enabled", Constants.ON, true),

  /** CSP upgrade insecure connections. Enable or disable the feature. (sensitive) */
  CSP_UPGRADE_INSECURE_ENABLED("csp.upgrade.insecure.enabled", Constants.OFF, true),

  /** CSP default header value/string. Enable or disable the feature. */
  CSP_HEADER_VALUE("csp.header.value", CspConstants.SCRIPT_SOURCE_DEFAULT, false),

  /** Event hooks for system events. Enable or disable the feature. */
  EVENT_HOOKS_ENABLED("event_hooks.enabled", Constants.OFF, false),

  /** Linked accounts via OpenID mapping. Enable or disable the feature. */
  LINKED_ACCOUNTS_ENABLED("linked_accounts.enabled", Constants.OFF, false),

  /**
   * @TODO
   */
  LINKED_ACCOUNTS_RELOGIN_URL("linked_accounts.relogin_url", "", false),

  LINKED_ACCOUNTS_LOGOUT_URL("linked_accounts.logout_url", "", false),

  /** User impersonation, also known as user switching. */
  SWITCH_USER_FEATURE_ENABLED("switch_user_feature.enabled", Constants.OFF, false),

  /** The list of IP address from which you will be calling the user impersonation feature. */
  SWITCH_USER_ALLOW_LISTED_IPS(
      "switch_user_allow_listed_ips", "localhost,127.0.0.1,[0:0:0:0:0:0:0:1]", false),

  /** Maximun size for files uploaded as fileResources. */
  MAX_FILE_UPLOAD_SIZE_BYTES("max.file_upload_size", Integer.toString(10_000_000), false),

  /** CSRF feature. Enable or disable the feature. (sensitive) */
  CSRF_ENABLED("http.security.csrf.enabled", Constants.OFF, true),

  /** The maximum number of category options in a single category */
  METADATA_CATEGORIES_MAX_OPTIONS("metadata.categories.max_options", "50", false),

  /** The maximum number of categories per category combo */
  METADATA_CATEGORIES_MAX_PER_COMBO("metadata.categories.max_per_combo", "5", false),
  /**
   * The maximum number of possible category combination. This is computed by multiplying the number
   * of options in each category in a category combo with each other.
   */
  METADATA_CATEGORIES_MAX_COMBINATIONS("metadata.categories.max_combinations", "500", false),

  /** Enable email-based 2FA authentication. (default: false) */
  EMAIL_2FA_ENABLED("login.security.email_2fa.enabled", Constants.OFF, false),

  /** Enable TOTP-based 2FA authentication. (default: true) */
  TOTP_2FA_ENABLED("login.security.totp_2fa.enabled", Constants.ON, false),

  SESSION_COOKIE_SAME_SITE("session.cookie.samesite", "Lax", false),

  /**
   * Remote servers allowed to call from the Route endpoint. <br>
   * Default is 'https://*'. <br>
   * Servers should be in a comma-separated style and always end with '/' for security reasons <br>
   * e.g. route.remote_servers_allowed = https://server1.com/,https://server2.com/
   */
  ROUTE_REMOTE_SERVERS_ALLOWED("route.remote_servers_allowed", "https://*", false),

  /** Enable OAuth2 authentication server. (default: off) */
  OAUTH2_SERVER_ENABLED("oauth2.server.enabled", Constants.OFF, false),

  /** Path to the JWT keystore file. */
  OAUTH2_JWT_KEYSTORE_PATH("oauth2.server.jwt.keystore.path", "", false),

  /** Password for the JWT keystore. (sensitive) */
  OAUTH2_JWT_KEYSTORE_PASSWORD("oauth2.server.jwt.keystore.password", "", true),

  /** Alias for the JWT key in the keystore. */
  OAUTH2_JWT_KEYSTORE_ALIAS("oauth2.server.jwt.keystore.alias", "", false),

  /** Password for the JWT key in the keystore. (sensitive) */
  OAUTH2_JWT_KEYSTORE_KEY_PASSWORD("oauth2.server.jwt.keystore.key-password", "", true),

  /** Whether to generate a new JWT key if the keystore is missing. */
  OAUTH2_JWT_KEYSTORE_GENERATE_IF_MISSING(
      "oauth2.server.jwt.keystore.generate-if-missing", "true", false),

  /** Ehcache monitoring. (default: off) */
  MONITORING_EHCACHE_ENABLED("monitoring.ehcache.enabled", Constants.OFF, false),

  CACHE_EHCACHE_CONFIG_FILE("cache.ehcache.config.file", "classpath:ehcache.xml", false),

  // Enable saved requests, this will save the URL the user tries to access before they are logged
  // in, and redirect to that URL after they are logged in.
  LOGIN_SAVED_REQUESTS_ENABLE("login.saved.requests.enable", Constants.ON, false);

  private final String key;

  private final String defaultValue;

  /**
   * Confidential means that the system setting will be encrypted and not visible through the API.
   * The system setting will be used internally in the backend, but cannot be used by web apps and
   * clients.
   */
  private final boolean confidential;

  private final String[] aliases;

  ConfigurationKey(String key) {
    this.key = key;
    this.defaultValue = null;
    this.confidential = false;
    this.aliases = new String[] {};
  }

  ConfigurationKey(String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.confidential = false;
    this.aliases = new String[] {};
  }

  ConfigurationKey(String key, String defaultValue, boolean confidential) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.confidential = confidential;
    this.aliases = new String[] {};
  }

  ConfigurationKey(String key, String defaultValue, boolean confidential, String[] aliases) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.confidential = confidential;
    this.aliases = aliases;
  }

  public static Optional<ConfigurationKey> getByKey(String key) {
    return Arrays.stream(ConfigurationKey.values()).filter(k -> k.key.equals(key)).findFirst();
  }

  private static final class Constants {
    public static final String OFF = "off";

    public static final String ON = "on";
  }
}
