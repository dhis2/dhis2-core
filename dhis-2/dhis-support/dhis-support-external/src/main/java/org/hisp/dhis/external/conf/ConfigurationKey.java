/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import org.hisp.dhis.security.utils.CspConstants;

/**
 * @author Lars Helge Overland
 */
public enum ConfigurationKey
{
    /**
     * System mode for database read operations only, can be 'off', 'on'.
     * (default: off).
     */
    SYSTEM_READ_ONLY_MODE( "system.read_only_mode", Constants.OFF, false ),

    /**
     * Session timeout in seconds. (default: 3600).
     */
    SYSTEM_SESSION_TIMEOUT( "system.session.timeout", "3600", false ),

    /**
     * System monitoring URL.
     */
    SYSTEM_MONITORING_URL( "system.monitoring.url" ),

    /**
     * System monitoring username.
     */
    SYSTEM_MONITORING_USERNAME( "system.monitoring.username" ),

    /**
     * System monitoring password (sensitive).
     */
    SYSTEM_MONITORING_PASSWORD( "system.monitoring.password" ),

    /**
     * SQL view protected tables, can be 'on', 'off'. (default: on)
     */
    SYSTEM_SQL_VIEW_TABLE_PROTECTION( "system.sql_view_table_protection", Constants.ON, false ),

    /**
     * Disable server-side program rule execution, can be 'on', 'off'. <br />
     * (default: on)
     */
    SYSTEM_PROGRAM_RULE_SERVER_EXECUTION( "system.program_rule.server_execution", Constants.ON, false ),

    /**
     * Set the maximum size for the cache instance to be built. If set to 0, no
     * caching will take place. Cannot be a negative value. (default: 0).
     */
    SYSTEM_CACHE_MAX_SIZE_FACTOR( "system.cache.max_size.factor", "0.5", false ),

    /**
     * Percentage cap limit for all cache memory usages. If set to 0, no limit
     * is set. (default: 0).
     */
    SYSTEM_CACHE_CAP_PERCENTAGE( "system.cache.cap.percentage", "0", false ),

    /**
     * Node identifier, optional, useful in clusters.
     */
    NODE_ID( "node.id", "", false ),

    /**
     * Encryption password (sensitive).
     */
    ENCRYPTION_PASSWORD( "encryption.password", "", true ),

    /**
     * Hibernate SQL dialect.
     */

    CONNECTION_DIALECT( "connection.dialect", "", false ),

    /**
     * JDBC driver class.
     */
    CONNECTION_DRIVER_CLASS( "connection.driver_class", "org.postgresql.Driver", false ),

    /**
     * Database connection URL.
     */
    CONNECTION_URL( "connection.url", "", false ),

    /**
     * Database username.
     */
    CONNECTION_USERNAME( "connection.username", "", false ),

    /**
     * Database password (sensitive).
     */
    CONNECTION_PASSWORD( "connection.password", "", true ),

    /**
     * Sets 'hibernate.cache.use_second_level_cache'. (default: true)
     */
    USE_SECOND_LEVEL_CACHE( "hibernate.cache.use_second_level_cache", "true", false ),

    /**
     * Sets 'hibernate.cache.use_query_cache'. (default: true)
     */
    USE_QUERY_CACHE( "hibernate.cache.use_query_cache", "true", false ),

    /**
     * Sets 'hibernate.hbm2ddl.auto', used in tests only. (default: none)
     */
    CONNECTION_SCHEMA( "connection.schema", "none", false ),

    /**
     * Max size of connection pool (default: 80).
     */
    CONNECTION_POOL_MAX_SIZE( "connection.pool.max_size", "80", false ),

    /**
     * Minimum number of Connections a pool will maintain at any given time
     * (default: 5).
     */
    CONNECTION_POOL_MIN_SIZE( "connection.pool.min_size", "5", false ),

    /**
     * Number of Connections a pool will try to acquire upon startup. Should be
     * between minPoolSize and maxPoolSize. (default: 5).
     */
    CONNECTION_POOL_INITIAL_SIZE( "connection.pool.initial_size", "5", false ),

    /**
     * Determines how many connections at a time will try to acquire when the
     * pool is exhausted. (default: 5).
     */
    CONNECTION_POOL_ACQUIRE_INCR( "connection.pool.acquire_incr", "5", false ),

    /**
     * Seconds a Connection can remain pooled but unused before being discarded.
     * Zero means idle connections never expire (default: 7200).
     */
    CONNECTION_POOL_MAX_IDLE_TIME( "connection.pool.max_idle_time", "7200", false ),

    /**
     * Number of seconds that Connections in excess of minPoolSize should be
     * permitted to remain idle in the pool before being culled (default: 0).
     */
    CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON( "connection.pool.max_idle_time_excess_con", "0", false ),

    /**
     * If this is a number greater than 0, dhis2 will test all idle, pooled but
     * unchecked-out connections, every this number of seconds (default: 0).
     */
    CONNECTION_POOL_IDLE_CON_TEST_PERIOD( "connection.pool.idle.con.test.period", "0", false ),

    /**
     * If true, an operation will be performed at every connection checkout to
     * verify that the connection is valid (default: false).
     */
    CONNECTION_POOL_TEST_ON_CHECKOUT( "connection.pool.test.on.checkout", Constants.OFF, false ),

    /**
     * If true, an operation will be performed asynchronously at every
     * connection checkin to verify that the connection is valid (default:
     * true).
     */
    CONNECTION_POOL_TEST_ON_CHECKIN( "connection.pool.test.on.checkin", Constants.ON, false ),

    /**
     * Hikari DB pool feature. Connection pool timeout: Set the maximum number
     * of milliseconds that a client will wait for a connection from the pool.
     * (default: 30s)
     */
    CONNECTION_POOL_TIMEOUT( "connection.pool.timeout", String.valueOf( SECONDS.toMillis( 30 ) ), false ),

    /**
     * Sets the maximum number of milliseconds that the Hikari pool will wait
     * for a connection to be validated as alive. (default: 5ms)
     */
    CONNECTION_POOL_VALIDATION_TIMEOUT( "connection.pool.validation_timeout", String.valueOf( SECONDS.toMillis( 5 ) ),
        false ),

    /**
     * Configure the number of helper threads used by C3P0 pool for jdbc
     * operations (default: 3).
     */
    CONNECTION_POOL_NUM_THREADS( "connection.pool.num.helper.threads", "3", false ),

    /**
     * Defines the query that will be executed for all connection tests. Ideally
     * this config is not needed as postgresql driver already provides an
     * efficient test query. The config is exposed simply for evaluation, do not
     * use it unless there is a reason to.
     */
    CONNECTION_POOL_TEST_QUERY( "connection.pool.preferred.test.query" ),

    /**
     * LDAP server URL. (default: ldaps://0:1)
     */
    LDAP_URL( "ldap.url", "ldaps://0:1", false ),

    /**
     * LDAP manager user distinguished name.
     */
    LDAP_MANAGER_DN( "ldap.manager.dn", "", false ),

    /**
     * LDAP manager user password (sensitive).
     */
    LDAP_MANAGER_PASSWORD( "ldap.manager.password", "", true ),

    /**
     * LDAP entry distinguished name search base.
     */
    LDAP_SEARCH_BASE( "ldap.search.base", "", false ),

    /**
     * LDAP entry distinguished name filter. (default: (cn={0}) ).
     */
    LDAP_SEARCH_FILTER( "ldap.search.filter", "(cn={0})", false ),

    /**
     * File store provider, currently 'filesystem' and 'aws-s3' are supported.
     * (default: filesystem)
     */
    FILESTORE_PROVIDER( "filestore.provider", "filesystem", false ),

    /**
     * Directory / bucket name, folder below DHIS2_HOME on file system, 'bucket'
     * on AWS S3. (default: files)
     */
    FILESTORE_CONTAINER( "filestore.container", "files", false ),

    /**
     * Datacenter location (not required).
     */
    FILESTORE_LOCATION( "filestore.location", "", false ),

    /**
     * Public identity / username.
     */
    FILESTORE_IDENTITY( "filestore.identity", "", false ),

    /**
     * Secret key / password (sensitive).
     */
    FILESTORE_SECRET( "filestore.secret", "", true ),

    GOOGLE_SERVICE_ACCOUNT_CLIENT_ID( "google.service.account.client.id", "", false ),

    META_DATA_SYNC_RETRY( "metadata.sync.retry", "3", false ),

    /**
     * Sets up {@see RetryTemplate} retry frequency.
     */
    META_DATA_SYNC_RETRY_TIME_FREQUENCY_MILLISEC( "metadata.sync.retry.time.frequency.millisec", "30000", false ),

    /**
     * EHCache replication host.
     */
    CLUSTER_HOSTNAME( "cluster.hostname", "", false ),

    /**
     * EHCache replication members.
     */
    CLUSTER_MEMBERS( "cluster.members", "", false ),

    /**
     * EHCache replication port.
     */
    CLUSTER_CACHE_PORT( "cluster.cache.port", "4001", false ),

    /**
     * EHCache replication remote object port.
     */
    CLUSTER_CACHE_REMOTE_OBJECT_PORT( "cluster.cache.remote.object.port", "0", false ),

    /**
     * Redis host to use for cache. (default: localhost)
     */
    REDIS_HOST( "redis.host", "localhost", false ),

    /**
     * Redis port to use for cache. (default: 6379)
     */
    REDIS_PORT( "redis.port", "6379", false ),

    /**
     * Redis password to use for cache.
     */
    REDIS_PASSWORD( "redis.password", "", true ),

    /**
     * Use SSL for connecting to redis. (default: false)
     */
    REDIS_USE_SSL( "redis.use.ssl", Constants.OFF, false ),

    /**
     * Enable redis cache. (default: false)
     */
    REDIS_ENABLED( "redis.enabled", Constants.OFF, false ),

    /**
     * Allows Flyway migrations to be run "out of order".
     * <p>
     * If you already have versions 1 and 3 applied, and now a version 2 is
     * found, it will be applied too instead of being ignored.
     * </p>
     */
    FLYWAY_OUT_OF_ORDER_MIGRATION( "flyway.migrate_out_of_order", Constants.OFF, false ),

    /**
     * Repairs the Flyway schema history table on startup before Flyway
     * migrations is applied. (default: false).
     */
    FLYWAY_REPAIR_BEFORE_MIGRATION( "flyway.repair_before_migration", Constants.OFF, false ),

    PROGRAM_TEMPORARY_OWNERSHIP_TIMEOUT( "tracker.temporary.ownership.timeout", "3", false ),

    LEADER_TIME_TO_LIVE( "leader.time.to.live.minutes", "2", false ),

    /**
     * Analytics server-side cache expiration in seconds. (default: 0)
     */
    ANALYTICS_CACHE_EXPIRATION( "analytics.cache.expiration", "0" ),

    /**
     * Use unlogged tables during analytics export. (default: off)
     */
    ANALYTICS_TABLE_UNLOGGED( "analytics.table.unlogged", Constants.OFF ),

    /**
     * Artemis support mode, 2 modes supported: EMBEDDED (starts up an embedded
     * Artemis which lives in the same process as your DHIS2 instance), NATIVE
     * (connects to an external Artemis instance, remember to set username /
     * password if required). (default: EMBEDDED)
     */
    ARTEMIS_MODE( "artemis.mode", "EMBEDDED" ),

    /**
     * Artemis host to use for connection (only relevant for NATIVE mode).
     * (default: 127.0.0.1)
     */
    ARTEMIS_HOST( "artemis.host", "127.0.0.1" ),

    /**
     * Artemis port to use for connection (only relevant for NATIVE mode).
     * (default: 25672)
     */
    ARTEMIS_PORT( "artemis.port", "25672" ),

    /**
     * Artemis username to use for connection (only relevant for NATIVE mode).
     * (default: guest)
     */
    ARTEMIS_USERNAME( "artemis.username", "guest", true ),

    /**
     * Artemis password to use for connection (only relevant for NATIVE mode).
     * (default: guest)
     */
    ARTEMIS_PASSWORD( "artemis.password", "guest", true ),

    /**
     * Enable/disable security for the embedded Artemis. (default: false).
     * <p>
     * If enabled will use {@link #ARTEMIS_USERNAME} and
     * {@link #ARTEMIS_PASSWORD}.
     */
    ARTEMIS_EMBEDDED_SECURITY( "artemis.embedded.security", Constants.OFF ),

    /**
     * Enable/disable Artemis persistence. (default. false).
     * <p>
     * We do not currently support any kind of recovery on failure, so this
     * should only be set if the volume on the queue is too high, and we want to
     * fall back to disk storage.
     */
    ARTEMIS_EMBEDDED_PERSISTENCE( "artemis.embedded.persistence", Constants.OFF ),

    /**
     * Number of threads to use for processing incoming packets. (default: 5).
     * <p>
     * This can also be set to "-1" which means use the Artemis default, which
     * is number of cores times 3.
     */
    ARTEMIS_EMBEDDED_THREADS( "artemis.embedded.threads", "5" ),

    /**
     * Max filesize for log files in "HOME/logs/" directory. Does not affect
     * size of audit logs. (default: 100MB).
     */
    LOGGING_FILE_MAX_SIZE( "logging.file.max_size", "100MB" ),

    /**
     * Number of log file archives to keep around. Does not affect audit logs.
     * (default: 1).
     */
    LOGGING_FILE_MAX_ARCHIVES( "logging.file.max_archives", "1" ),

    /**
     * Adds a hashed (SHA-256) session_id to each log line. Useful for tracking
     * which user is responsible for the logging line.
     */
    LOGGING_REQUEST_ID_ENABLED( "logging.request_id.enabled", Constants.ON, false ),

    /**
     * Base URL to the DHIS 2 instance.
     */
    SERVER_BASE_URL( "server.base.url", "", false ),

    /**
     * Enable secure settings if system is deployed on HTTPS, can be 'off',
     * 'on'.
     */
    SERVER_HTTPS( "server.https", Constants.OFF ),

    /**
     * DHIS2 API monitoring.
     */
    MONITORING_API_ENABLED( "monitoring.api.enabled", Constants.OFF, false ),

    /**
     * JVM monitoring.
     */
    MONITORING_JVM_ENABLED( "monitoring.jvm.enabled", Constants.OFF, false ),

    /**
     * Database connection pool monitoring. (default: off)
     */
    MONITORING_DBPOOL_ENABLED( "monitoring.dbpool.enabled", Constants.OFF, false ),

    /**
     * Hibernate monitoring, do not use in production. (default: off)
     */
    MONITORING_HIBERNATE_ENABLED( "monitoring.hibernate.enabled", Constants.OFF, false ),

    /**
     * Uptime monitoring. (default: off)
     */
    MONITORING_UPTIME_ENABLED( "monitoring.uptime.enabled", Constants.OFF, false ),

    /**
     * CPU monitoring. (default: off)
     */
    MONITORING_CPU_ENABLED( "monitoring.cpu.enabled", Constants.OFF, false ),

    /**
     * AppHub base URL. (default: https://apps.dhis2.org).
     */
    APPHUB_BASE_URL( "apphub.base.url", "https://apps.dhis2.org", false ),

    /**
     * AppHub api URL. (default: https://apps.dhis2.org/api).
     */
    APPHUB_API_URL( "apphub.api.url", "https://apps.dhis2.org/api", false ),

    /**
     * Enable/disable changelog/history log of aggregate data values. <br/>
     * (default: on)
     */
    CHANGELOG_AGGREGATE( "changelog.aggregate", Constants.ON ),

    /**
     * Enable/disable changelog/history log of tracker data values. <br/>
     * (default: on)
     */
    CHANGELOG_TRACKER( "changelog.tracker", Constants.ON ),

    /**
     * Use in-memory queue before sending audits into the Artemis queue.
     * (default: off).
     */
    AUDIT_USE_IN_MEMORY_QUEUE_ENABLED( "audit.in_memory-queue.enabled", Constants.OFF, false,
        new String[] { "audit.inmemory_queue.enabled" } ),

    /**
     * Send audits to "logs/dhis-audit.log". (default: on).
     */
    AUDIT_LOGGER( "audit.logger", Constants.ON, false ),

    /**
     * Save audits to database table "audit". (default: off).
     */
    AUDIT_DATABASE( "audit.database", Constants.OFF, false ),

    /**
     * Sets the audit matrix for metadata. (default: none).
     */
    AUDIT_METADATA_MATRIX( "audit.metadata", "", false ),

    /**
     * Sets the audit matrix for aggregate. (default: none).
     */
    AUDIT_AGGREGATE_MATRIX( "audit.aggregate", "", false ),

    /**
     * Sets the audit matrix for tracker. (default: none).
     */
    AUDIT_TRACKER_MATRIX( "audit.tracker", "", false ),

    /**
     * Enable OIDC. (default: off).
     */
    OIDC_OAUTH2_LOGIN_ENABLED( "oidc.oauth2.login.enabled", Constants.OFF, false ),

    /**
     * DHIS 2 instance URL, do not end with a slash, not all IdPs support logout
     * (Where to end up after calling end_session_endpoint on the IdP).
     */
    OIDC_LOGOUT_REDIRECT_URL( "oidc.logout.redirect_url", "", false ),

    /**
     * Google IdP specific parameters. Provider client ID: This is the
     * identifier that the IdP assigned to your application.
     */
    OIDC_PROVIDER_GOOGLE_CLIENT_ID( "oidc.provider.google.client_id", "", true ),

    /**
     * Google IdP specific parameters. Provider client secret: This value is a
     * secret and should be kept secure.
     */
    OIDC_PROVIDER_GOOGLE_CLIENT_SECRET( "oidc.provider.google.client_secret", "", true ),

    /**
     * Google IdP specific parameters. Mapping claim: *Optional. (default:
     * email).
     */
    OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM( "oidc.provider.google.mapping_claim", "email", true ),

    /**
     * Google IdP specific parameters. Redirect URL: DHIS 2 instance URL, do not
     * end with a slash, <br />
     * e.g. https://dhis2.org/demo.
     */
    OIDC_PROVIDER_GOOGLE_REDIRECT_URI( "oidc.provider.google.redirect_url", "", true ),

    /**
     * WSO2 IdP specific parameters. Provider client ID: This is the identifier
     * that the IdP assigned to your application.
     */
    OIDC_PROVIDER_WSO2_CLIENT_ID( "oidc.provider.wso2.client_id", "", false ),

    /**
     * WSO2 IdP specific parameters. Provider client secret: This value is a
     * secret and should be kept secure.
     */
    OIDC_PROVIDER_WSO2_CLIENT_SECRET( "oidc.provider.wso2.client_secret", "", false ),

    /**
     * WSO2 IdP specific parameters. Mapping claim: *Optional. (default: email).
     */
    OIDC_PROVIDER_WSO2_MAPPING_CLAIM( "oidc.provider.wso2.mapping_claim", "email", false ),

    /**
     * WSO2 IdP specific parameters. Server URL
     */
    OIDC_PROVIDER_WSO2_SERVER_URL( "oidc.provider.wso2.server_url", "", false ),

    /**
     * WSO2 IdP specific parameters. Redirect URL: DHIS 2 instance URL, do not
     * end with a slash, <br />
     * e.g. https://dhis2.org/demo.
     */
    OIDC_PROVIDER_WSO2_REDIRECT_URI( "oidc.provider.wso2.redirect_url", "", false ),

    /**
     * WSO2 IdP specific parameters. Display alias
     */
    OIDC_PROVIDER_WSO2_DISPLAY_ALIAS( "oidc.provider.wso2.display_alias", "", false ),

    /**
     * WSO2 IdP specific parameters. Enable logout
     */
    OIDC_PROVIDER_WSO2_ENABLE_LOGOUT( "oidc.provider.wso2.enable_logout", Constants.ON, false ),

    /**
     * Database debugging feature. Defines threshold for logging of slow queries
     * in the log.
     */
    SLOW_QUERY_LOGGING_THRESHOLD_TIME_MS( "slow.query.logging.threshold.time", String.valueOf( SECONDS.toMillis( 1 ) ),
        false ),

    /**
     * Database debugging feature. Enables logging of all SQL queries to the
     * log.
     */
    ENABLE_QUERY_LOGGING( "enable.query.logging", Constants.OFF, false ),

    /**
     * Database debugging feature. Defines database logging before/after methods
     */
    METHOD_QUERY_LOGGING_ENABLED( "method.query.logging.enabled", Constants.OFF, false ),

    /**
     * Database debugging feature. Enable time query logging.
     */
    ELAPSED_TIME_QUERY_LOGGING_ENABLED( "elapsed.time.query.logging.enabled", Constants.OFF, false ),

    /**
     * Database datasource pool type. Supported pool types are: c3p0 (default)
     * or hikari
     */
    DB_POOL_TYPE( "db.pool.type", "c3p0", false ),

    ACTIVE_READ_REPLICAS( "active.read.replicas", "0", false ),

    /**
     * Allows enabling/disabling audits system-wide (without configuring the
     * audit matrix). (default: true)
     */
    AUDIT_ENABLED( "system.audit.enabled", Constants.ON, false ),

    /**
     * OAuth2 authorization server feature. Enable or disable.
     */
    ENABLE_OAUTH2_AUTHORIZATION_SERVER( "oauth2.authorization.server.enabled", Constants.ON, false ),

    /**
     * JWT OIDC token authentication feature. Enable or disable.
     */
    ENABLE_JWT_OIDC_TOKEN_AUTHENTICATION( "oidc.jwt.token.authentication.enabled", Constants.OFF, false ),

    /**
     * Cache invalidation feature. Enable or disable.
     */
    DEBEZIUM_ENABLED( "debezium.enabled", Constants.OFF, false ),

    /**
     * Cache invalidation feature. DB connection username
     */
    DEBEZIUM_CONNECTION_USERNAME( "debezium.connection.username", "", false ),

    /**
     * Cache invalidation feature. DB connection password
     */
    DEBEZIUM_CONNECTION_PASSWORD( "debezium.connection.password", "", false ),

    /**
     * Cache invalidation feature. DB hostname
     */
    DEBEZIUM_DB_HOSTNAME( "debezium.db.hostname", "", false ),

    /**
     * Cache invalidation feature. DB port number
     */
    DEBEZIUM_DB_PORT( "debezium.db.port", "", false ),

    /**
     * Cache invalidation feature. DB name
     */
    DEBEZIUM_DB_NAME( "debezium.db.name", "", false ),

    /**
     * Cache invalidation feature. Replication slot name
     */
    DEBEZIUM_SLOT_NAME( "debezium.slot.name", "", false ),

    /**
     * Cache invalidation feature. Table exclude list
     */
    DEBEZIUM_EXCLUDE_LIST( "debezium.exclude.list", "", false ),

    /**
     * Cache invalidation feature. Shutdown server if connector loose connection
     */
    DEBEZIUM_SHUTDOWN_ON_CONNECTOR_STOP( "debezium.shutdown_on.connector_stop", Constants.ON, false ),

    /**
     * API authentication feature. Enable or disable personal access tokens.
     */
    ENABLE_API_TOKEN_AUTHENTICATION( "enable.api_token.authentication", Constants.ON, false ),

    /**
     * System update notifications system. Enable or disable the feature.
     */
    SYSTEM_UPDATE_NOTIFICATIONS_ENABLED( "system.update_notifications_enabled", Constants.ON, false ),

    /**
     * Number of possible concurrent sessions on different computers or browsers
     * for each user. If configured to 1, the user will be logged out from any
     * other session when a new session is started.
     */
    MAX_SESSIONS_PER_USER( "max.sessions.per_user", "10", false ),

    /**
     * Redis based cache invalidation feature. Enable or disable.
     */
    REDIS_CACHE_INVALIDATION_ENABLED( "redis.cache.invalidation.enabled", Constants.OFF, false ),

    /**
     * Content Security Policy feature. Enable or disable the feature.
     */
    CSP_ENABLED( "csp.enabled", Constants.ON, true ),

    /**
     * CSP upgrade insecure connections. Enable or disable the feature.
     */
    CSP_UPGRADE_INSECURE_ENABLED( "csp.upgrade.insecure.enabled", Constants.OFF, true ),

    /**
     * CSP default header value/string. Enable or disable the feature.
     */
    CSP_HEADER_VALUE( "csp.header.value", CspConstants.SCRIPT_SOURCE_DEFAULT, false ),

    /**
     * Event hooks for system events. Enable or disable the feature.
     */
    EVENT_HOOKS_ENABLED( "event_hooks.enabled", Constants.OFF, false ),

    /**
     * Linked accounts via OpenID mapping. Enable or disable the feature.
     */
    LINKED_ACCOUNTS_ENABLED( "linked_accounts.enabled", Constants.OFF, false ),

    LINKED_ACCOUNTS_RELOGIN_URL( "linked_accounts.relogin_url", "", false ),
    SWITCH_USER_FEATURE_ENABLED( "switch_user_feature.enabled", Constants.OFF, false ),
    SWITCH_USER_ALLOW_LISTED_IPS( "switch_user_allow_listed_ips", "localhost", false );


    private final String key;

    private final String defaultValue;

    private final boolean confidential;

    private final String[] aliases;

    ConfigurationKey( String key )
    {
        this.key = key;
        this.defaultValue = null;
        this.confidential = false;
        this.aliases = new String[] {};
    }

    ConfigurationKey( String key, String defaultValue )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.confidential = false;
        this.aliases = new String[] {};
    }

    ConfigurationKey( String key, String defaultValue, boolean confidential )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.confidential = confidential;
        this.aliases = new String[] {};
    }

    ConfigurationKey( String key, String defaultValue, boolean confidential, String[] aliases )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.confidential = confidential;
        this.aliases = aliases;
    }

    public String getKey()
    {
        return key;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public boolean isConfidential()
    {
        return confidential;
    }

    public String[] getAliases()
    {
        return aliases;
    }

    public static Optional<ConfigurationKey> getByKey( String key )
    {
        return Arrays.stream( ConfigurationKey.values() ).filter( k -> k.key.equals( key ) ).findFirst();
    }

    private static final class Constants
    {
        public static final String OFF = "off";

        public static final String ON = "on";
    }
}
