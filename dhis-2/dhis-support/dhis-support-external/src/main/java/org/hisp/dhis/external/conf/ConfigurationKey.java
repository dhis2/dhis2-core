/*
 * Copyright (c) 2004-2021, University of Oslo
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

import org.hisp.dhis.security.utils.CspUtils;

/**
 * @author Lars Helge Overland
 */
public enum ConfigurationKey
{
    SYSTEM_READ_ONLY_MODE( "system.read_only_mode", Constants.OFF, false ),
    SYSTEM_SESSION_TIMEOUT( "system.session.timeout", "3600", false ),
    SYSTEM_MONITORING_URL( "system.monitoring.url" ),
    SYSTEM_MONITORING_USERNAME( "system.monitoring.username" ),
    SYSTEM_MONITORING_PASSWORD( "system.monitoring.password" ),
    SYSTEM_SQL_VIEW_TABLE_PROTECTION( "system.sql_view_table_protection", Constants.ON, false ),
    SYSTEM_PROGRAM_RULE_SERVER_EXECUTION( "system.program_rule.server_execution", Constants.ON, false ),
    SYSTEM_CACHE_MAX_SIZE_FACTOR( "system.cache.max_size.factor", "0.5", false ),
    SYSTEM_CACHE_CAP_PERCENTAGE( "system.cache.cap.percentage", "0", false ),
    NODE_ID( "node.id", "", false ),
    ENCRYPTION_PASSWORD( "encryption.password", "", true ),
    CONNECTION_DIALECT( "connection.dialect", "", false ),
    CONNECTION_DRIVER_CLASS( "connection.driver_class", "", false ),
    CONNECTION_URL( "connection.url", "", false ),
    CONNECTION_USERNAME( "connection.username", "", false ),
    CONNECTION_PASSWORD( "connection.password", "", true ),
    CONNECTION_SCHEMA( "connection.schema", "", false ),
    CONNECTION_POOL_MAX_SIZE( "connection.pool.max_size", "80", false ),
    CONNECTION_POOL_MIN_SIZE( "connection.pool.min_size", "5", false ),
    CONNECTION_POOL_INITIAL_SIZE( "connection.pool.initial_size", "5", false ),
    CONNECTION_POOL_ACQUIRE_INCR( "connection.pool.acquire_incr", "5", false ),
    CONNECTION_POOL_MAX_IDLE_TIME( "connection.pool.max_idle_time", "7200", false ),
    CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON( "connection.pool.max_idle_time_excess_con", "0", false ),
    CONNECTION_POOL_IDLE_CON_TEST_PERIOD( "connection.pool.idle.con.test.period", "0", false ),
    CONNECTION_POOL_TEST_ON_CHECKOUT( "connection.pool.test.on.checkout", Constants.FALSE, false ),
    CONNECTION_POOL_TEST_ON_CHECKIN( "connection.pool.test.on.checkin", Constants.TRUE, false ),
    CONNECTION_POOL_TIMEOUT( "connection.pool.timeout", String.valueOf( SECONDS.toMillis( 30 ) ), false ),
    CONNECTION_POOL_VALIDATION_TIMEOUT( "connection.pool.validation_timeout", String.valueOf( SECONDS.toMillis( 5 ) ),
        false ),
    CONNECTION_POOL_NUM_THREADS( "connection.pool.num.helper.threads", "3", false ),
    CONNECTION_POOL_TEST_QUERY( "connection.pool.preferred.test.query" ),
    LDAP_URL( "ldap.url", "ldaps://0:1", false ),
    LDAP_MANAGER_DN( "ldap.manager.dn", "", false ),
    LDAP_MANAGER_PASSWORD( "ldap.manager.password", "", true ),
    LDAP_SEARCH_BASE( "ldap.search.base", "", false ),
    LDAP_SEARCH_FILTER( "ldap.search.filter", "(cn={0})", false ),
    FILESTORE_PROVIDER( "filestore.provider", "filesystem", false ),
    FILESTORE_CONTAINER( "filestore.container", "files", false ),
    FILESTORE_LOCATION( "filestore.location", "", false ),
    FILESTORE_IDENTITY( "filestore.identity", "", false ),
    FILESTORE_SECRET( "filestore.secret", "", true ),
    GOOGLE_SERVICE_ACCOUNT_CLIENT_ID( "google.service.account.client.id", "", false ),
    META_DATA_SYNC_RETRY( "metadata.sync.retry", "3", false ),
    META_DATA_SYNC_RETRY_TIME_FREQUENCY_MILLISEC( "metadata.sync.retry.time.frequency.millisec", "30000", false ),
    CLUSTER_HOSTNAME( "cluster.hostname", "", false ),
    CLUSTER_MEMBERS( "cluster.members", "", false ),
    CLUSTER_CACHE_PORT( "cluster.cache.port", "4001", false ),
    CLUSTER_CACHE_REMOTE_OBJECT_PORT( "cluster.cache.remote.object.port", "0", false ),
    REDIS_HOST( "redis.host", "localhost", false ),
    REDIS_PORT( "redis.port", "6379", false ),
    REDIS_PASSWORD( "redis.password", "", true ),
    REDIS_ENABLED( "redis.enabled", Constants.FALSE, false ),
    REDIS_USE_SSL( "redis.use.ssl", Constants.FALSE, false ),
    FLYWAY_OUT_OF_ORDER_MIGRATION( "flyway.migrate_out_of_order", Constants.FALSE, false ),
    FLYWAY_REPAIR_BEFORE_MIGRATION( "flyway.repair_before_migration", Constants.FALSE, false ),
    PROGRAM_TEMPORARY_OWNERSHIP_TIMEOUT( "tracker.temporary.ownership.timeout", "3", false ),
    LEADER_TIME_TO_LIVE( "leader.time.to.live.minutes", "2", false ),
    ANALYTICS_CACHE_EXPIRATION( "analytics.cache.expiration", "0" ),

    /**
     * Use unlogged tables during analytics export. (default: off)
     */
    ANALYTICS_TABLE_UNLOGGED( "analytics.table.unlogged", Constants.OFF ),

    ARTEMIS_MODE( "artemis.mode", "EMBEDDED" ),
    ARTEMIS_HOST( "artemis.host", "127.0.0.1" ),
    ARTEMIS_PORT( "artemis.port", "25672" ),
    ARTEMIS_USERNAME( "artemis.username", "guest", true ),
    ARTEMIS_PASSWORD( "artemis.password", "guest", true ),
    ARTEMIS_EMBEDDED_SECURITY( "artemis.embedded.security", Constants.FALSE ),
    ARTEMIS_EMBEDDED_PERSISTENCE( "artemis.embedded.persistence", Constants.FALSE ),
    ARTEMIS_EMBEDDED_THREADS( "artemis.embedded.threads", "5" ),
    LOGGING_FILE_MAX_SIZE( "logging.file.max_size", "100MB" ),
    LOGGING_FILE_MAX_ARCHIVES( "logging.file.max_archives", "1" ),
    LOGGING_REQUEST_ID_HASH( "logging.request_id.hash", "SHA-256", false ),
    LOGGING_REQUEST_ID_ENABLED( "logging.request_id.enabled", Constants.ON, false ),
    LOGGING_REQUEST_ID_MAX_SIZE( "logging.request_id.max_size", "-1", false ),
    SERVER_BASE_URL( "server.base.url", "", false ),
    SERVER_HTTPS( "server.https", Constants.OFF ),
    MONITORING_PROVIDER( "monitoring.provider", "prometheus" ),
    MONITORING_API_ENABLED( "monitoring.api.enabled", Constants.OFF, false ),
    MONITORING_JVM_ENABLED( "monitoring.jvm.enabled", Constants.OFF, false ),
    MONITORING_DBPOOL_ENABLED( "monitoring.dbpool.enabled", Constants.OFF, false ),
    MONITORING_HIBERNATE_ENABLED( "monitoring.hibernate.enabled", Constants.OFF, false ),
    MONITORING_UPTIME_ENABLED( "monitoring.uptime.enabled", Constants.OFF, false ),
    MONITORING_CPU_ENABLED( "monitoring.cpu.enabled", Constants.OFF, false ),
    APPHUB_BASE_URL( "apphub.base.url", "https://apps.dhis2.org", false ),
    APPHUB_API_URL( "apphub.api.url", "https://apps.dhis2.org/api", false ),
    CHANGELOG_AGGREGATE( "changelog.aggregate", Constants.ON ),
    CHANGELOG_TRACKER( "changelog.tracker", Constants.ON ),
    AUDIT_USE_INMEMORY_QUEUE_ENABLED( "audit.inmemory-queue.enabled", Constants.OFF ),
    AUDIT_USE_IN_MEMORY_QUEUE_ENABLED( "audit.in_memory_queue.enabled", Constants.OFF ),
    AUDIT_LOGGER( "audit.logger", Constants.OFF, false ),
    AUDIT_LOGGER_FILE_MAX_SIZE( "audit.logger.file.max_size", "500MB" ),
    AUDIT_DATABASE( "audit.database", Constants.ON, false ),
    AUDIT_METADATA_MATRIX( "audit.metadata", "", false ),
    AUDIT_TRACKER_MATRIX( "audit.tracker", "", false ),
    AUDIT_AGGREGATE_MATRIX( "audit.aggregate", "", false ),
    OIDC_OAUTH2_LOGIN_ENABLED( "oidc.oauth2.login.enabled", Constants.OFF, false ),
    OIDC_LOGOUT_REDIRECT_URL( "oidc.logout.redirect_url", "", false ),
    OIDC_PROVIDER_GOOGLE_CLIENT_ID( "oidc.provider.google.client_id", "", true ),
    OIDC_PROVIDER_GOOGLE_CLIENT_SECRET( "oidc.provider.google.client_secret", "", true ),
    OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM( "oidc.provider.google.mapping_claim", "", true ),
    OIDC_PROVIDER_GOOGLE_REDIRECT_URI( "oidc.provider.google.redirect_url", "", true ),
    OIDC_PROVIDER_WSO2_CLIENT_ID( "oidc.provider.wso2.client_id", "", false ),
    OIDC_PROVIDER_WSO2_CLIENT_SECRET( "oidc.provider.wso2.client_secret", "", false ),
    OIDC_PROVIDER_WSO2_MAPPING_CLAIM( "oidc.provider.wso2.mapping_claim", "email", false ),
    OIDC_PROVIDER_WSO2_SERVER_URL( "oidc.provider.wso2.server_url", "", false ),
    OIDC_PROVIDER_WSO2_REDIRECT_URI( "oidc.provider.wso2.redirect_url", "", false ),
    OIDC_PROVIDER_WSO2_DISPLAY_ALIAS( "oidc.provider.wso2.display_alias", "", false ),
    OIDC_PROVIDER_WSO2_ENABLE_LOGOUT( "oidc.provider.wso2.enable_logout", Constants.TRUE, false ),
    SLOW_QUERY_LOGGING_THRESHOLD_TIME_MS( "slow.query.logging.threshold.time", String.valueOf( SECONDS.toMillis( 1 ) ),
        false ),
    ENABLE_QUERY_LOGGING( "enable.query.logging", Constants.FALSE, false ),
    METHOD_QUERY_LOGGING_ENABLED( "method.query.logging.enabled", Constants.FALSE, false ),
    ELAPSED_TIME_QUERY_LOGGING_ENABLED( "elapsed.time.query.logging.enabled", Constants.FALSE, false ),
    DB_POOL_TYPE( "db.pool.type", "c3p0", false ),
    ACTIVE_READ_REPLICAS( "active.read.replicas", "0", false ),
    AUDIT_ENABLED( "system.audit.enabled", Constants.TRUE, false ),
    TRACKER_IMPORT_PREHEAT_CACHE_ENABLED( "tracker.import.preheat.cache.enabled", Constants.ON, false ),
    ENABLE_OAUTH2_AUTHORIZATION_SERVER( "oauth2.authorization.server.enabled", Constants.ON, false ),
    ENABLE_JWT_OIDC_TOKEN_AUTHENTICATION( "oidc.jwt.token.authentication.enabled", Constants.OFF, false ),
    DEBEZIUM_ENABLED( "debezium.enabled", Constants.OFF, false ),
    DEBEZIUM_CONNECTION_USERNAME( "debezium.connection.username", "", false ),
    DEBEZIUM_CONNECTION_PASSWORD( "debezium.connection.password", "", false ),
    DEBEZIUM_DB_HOSTNAME( "debezium.db.hostname", "", false ),
    DEBEZIUM_DB_PORT( "debezium.db.port", "", false ),
    DEBEZIUM_DB_NAME( "debezium.db.name", "", false ),
    DEBEZIUM_SLOT_NAME( "debezium.slot.name", "", false ),
    DEBEZIUM_EXCLUDE_LIST( "debezium.exclude.list", "", false ),
    DEBEZIUM_SHUTDOWN_ON_CONNECTOR_STOP( "debezium.shutdown_on.connector_stop", Constants.OFF, false ),
    ENABLE_API_TOKEN_AUTHENTICATION( "enable.api_token.authentication", Constants.OFF, false ),

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
    CSP_HEADER_VALUE( "csp.header.value", CspUtils.DEFAULT_HEADER_VALUE, false ),

    MAX_SESSIONS_PER_USER( "max.sessions.per_user", "10", false);

    private final String key;

    private final String defaultValue;

    private final boolean confidential;

    ConfigurationKey( String key )
    {
        this.key = key;
        this.defaultValue = null;
        this.confidential = false;
    }

    ConfigurationKey( String key, String defaultValue )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.confidential = false;
    }

    ConfigurationKey( String key, String defaultValue, boolean confidential )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.confidential = confidential;
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

    public static Optional<ConfigurationKey> getByKey( String key )
    {
        return Arrays.stream( ConfigurationKey.values() ).filter( k -> k.key.equals( key ) ).findFirst();
    }

    private static final class Constants
    {
        public static final String FALSE = Boolean.FALSE.toString();

        public static final String TRUE = Boolean.TRUE.toString();

        public static final String OFF = "off";

        public static final String ON = "on";
    }
}
