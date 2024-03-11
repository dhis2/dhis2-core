package org.hisp.dhis.system;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.datasource.DataSourceManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.kafka.KafkaConfig;
import org.hisp.dhis.logging.LogFormat;
import org.hisp.dhis.logging.LogLevel;
import org.hisp.dhis.logging.LoggingConfig;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.util.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Lars Helge Overland
 */
public class DefaultSystemService
    implements SystemService, InitializingBean
{
    private static final Log log = LogFactory.getLog( DefaultSystemService.class );

    @Autowired
    private LocationManager locationManager;

    @Autowired
    private DatabaseInfo databaseInfo;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DhisConfigurationProvider dhisConfig;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private DataSourceManager dataSourceManager;

    /**
     * Variable holding fixed system info state.
     */
    private SystemInfo systemInfo = null;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        systemInfo = getFixedSystemInfo();

        List<String> info = ImmutableList.<String>builder()
            .add( "Version: " + systemInfo.getVersion() )
            .add( "Revision: " + systemInfo.getRevision() )
            .add( "Build date: " + systemInfo.getBuildTime() )
            .add( "Database name: " + systemInfo.getDatabaseInfo().getName() )
            .add( "Java version: " + systemInfo.getJavaVersion() )
            .build();

        log.info( StringUtils.join( info, ", " ) );
    }

    // -------------------------------------------------------------------------
    // SystemService implementation
    // -------------------------------------------------------------------------

    @Override
    public SystemInfo getSystemInfo()
    {
        SystemInfo info = systemInfo != null ? systemInfo.instance() : null;

        if ( info == null )
        {
            return null;
        }

        Date lastAnalyticsTableSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        String lastAnalyticsTableRuntime = (String) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME );
        Date lastSystemMonitoringSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_SYSTEM_MONITORING_PUSH );
        String systemName = (String) systemSettingManager.getSystemSetting( SettingKey.APPLICATION_TITLE );
        String instanceBaseUrl = (String) systemSettingManager.getSystemSetting( SettingKey.INSTANCE_BASE_URL );

        Date now = new Date();

        info.setCalendar( calendarService.getSystemCalendar().name() );
        info.setDateFormat( calendarService.getSystemDateFormat().getJs() );
        info.setServerDate( new Date() );
        info.setLastAnalyticsTableSuccess( lastAnalyticsTableSuccess );
        info.setIntervalSinceLastAnalyticsTableSuccess( DateUtils.getPrettyInterval( lastAnalyticsTableSuccess, now ) );
        info.setLastSystemMonitoringSuccess( lastSystemMonitoringSuccess );
        info.setLastAnalyticsTableRuntime( lastAnalyticsTableRuntime );
        info.setSystemName( systemName );
        info.setInstanceBaseUrl( instanceBaseUrl );
        info.setEmailConfigured( systemSettingManager.emailConfigured() );

        setSystemMetadataVersionInfo( info );

        // ---------------------------------------------------------------------
        // Kafka
        // ---------------------------------------------------------------------

        KafkaConfig kafka = new KafkaConfig(
            dhisConfig.getProperty( ConfigurationKey.KAFKA_BOOTSTRAP_SERVERS ),
            dhisConfig.getProperty( ConfigurationKey.KAFKA_CLIENT_ID ),
            Integer.valueOf( dhisConfig.getProperty( ConfigurationKey.KAFKA_RETRIES ) ),
            Integer.valueOf( dhisConfig.getProperty( ConfigurationKey.KAFKA_MAX_POLL_RECORDS ) )
        );

        if ( kafka.isValid() )
        {
            info.setKafka( kafka );
        }

        info.setLogging( new LoggingConfig(
            LogLevel.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_LEVEL )).toUpperCase() ),
            LogFormat.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_FORMAT )).toUpperCase() ),
            (Boolean) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_CONSOLE ),
            LogLevel.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_CONSOLE_LEVEL )).toUpperCase() ),
            LogFormat.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_CONSOLE_FORMAT )).toUpperCase() ),
            (Boolean) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_FILE ),
            ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_FILE_NAME )),
            LogLevel.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_FILE_LEVEL )).toUpperCase() ),
            LogFormat.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_FILE_FORMAT )).toUpperCase() ),
            (Boolean) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_KAFKA ),
            LogLevel.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_KAFKA_LEVEL )).toUpperCase() ),
            LogFormat.valueOf( ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_KAFKA_FORMAT )).toUpperCase() ),
            ((String) systemSettingManager.getSystemSetting( SettingKey.LOGGING_ADAPTER_KAFKA_TOPIC ))
        ) );

        return info;
    }

    private SystemInfo getFixedSystemInfo()
    {
        SystemInfo info = new SystemInfo();

        Configuration config = configurationService.getConfiguration();

        // ---------------------------------------------------------------------
        // Version
        // ---------------------------------------------------------------------

        ClassPathResource resource = new ClassPathResource( "build.properties" );

        if ( resource.isReadable() )
        {
            try ( InputStream in = resource.getInputStream() )
            {
                Properties properties = new Properties();

                properties.load( in );

                info.setVersion( properties.getProperty( "build.version" ) );
                info.setRevision( properties.getProperty( "build.revision" ) );
                info.setJasperReportsVersion( properties.getProperty( "jasperreports.version" ) );

                String buildTime = properties.getProperty( "build.time" );

                DateTimeFormatter dateFormat = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ss" );

                info.setBuildTime( new DateTime( dateFormat.parseDateTime( buildTime ) ).toDate() );
            }
            catch ( IOException ex )
            {
                // Do nothing
            }
        }

        // ---------------------------------------------------------------------
        // External directory
        // ---------------------------------------------------------------------

        info.setEnvironmentVariable( locationManager.getEnvironmentVariable() );

        try
        {
            File directory = locationManager.getExternalDirectory();

            info.setExternalDirectory( directory.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            info.setExternalDirectory( "Not set" );
        }

        info.setFileStoreProvider( dhisConfig.getProperty( ConfigurationKey.FILESTORE_PROVIDER ) );
        info.setCacheProvider( dhisConfig.getProperty( ConfigurationKey.CACHE_PROVIDER ) );
        info.setReadOnlyMode( dhisConfig.getProperty( ConfigurationKey.SYSTEM_READ_ONLY_MODE ) );
        info.setNodeId( dhisConfig.getProperty( ConfigurationKey.NODE_ID ) );
        info.setSystemMonitoringUrl( dhisConfig.getProperty( ConfigurationKey.SYSTEM_MONITORING_URL ) );
        info.setSystemId( config.getSystemId() );
        info.setRedisEnabled( Boolean.valueOf( dhisConfig.getProperty( ConfigurationKey.REDIS_ENABLED ) ) );
        
        if ( info.isRedisEnabled() )
        {
            info.setRedisHostname( dhisConfig.getProperty( ConfigurationKey.REDIS_HOST ) );
        }

        // ---------------------------------------------------------------------
        // Database
        // ---------------------------------------------------------------------

        info.setDatabaseInfo( databaseInfo );
        info.setReadReplicaCount( dataSourceManager.getReadReplicaCount() );

        // ---------------------------------------------------------------------
        // Metadata Audit
        // ---------------------------------------------------------------------

        info.setMetadataAudit( new MetadataAudit(
            Objects.equals( dhisConfig.getProperty( ConfigurationKey.METADATA_AUDIT_PERSIST ), "on" ),
            Objects.equals( dhisConfig.getProperty( ConfigurationKey.METADATA_AUDIT_LOG ), "on" )
        ) );

        // ---------------------------------------------------------------------
        // RabbitMQ
        // ---------------------------------------------------------------------

        RabbitMQ rabbitMQ = new RabbitMQ(
            dhisConfig.getProperty( ConfigurationKey.RABBITMQ_HOST ),
            Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.RABBITMQ_PORT ) ),
            dhisConfig.getProperty( ConfigurationKey.RABBITMQ_USERNAME ),
            dhisConfig.getProperty( ConfigurationKey.RABBITMQ_PASSWORD )
        );

        rabbitMQ.setExchange( dhisConfig.getProperty( ConfigurationKey.RABBITMQ_EXCHANGE ) );
        rabbitMQ.setAddresses( dhisConfig.getProperty( ConfigurationKey.RABBITMQ_ADDRESSES ) );
        rabbitMQ.setVirtualHost( dhisConfig.getProperty( ConfigurationKey.RABBITMQ_VIRTUAL_HOST ) );
        rabbitMQ.setConnectionTimeout( Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.RABBITMQ_CONNECTION_TIMEOUT ) ) );

        if ( rabbitMQ.isValid() )
        {
            info.setRabbitMQ( rabbitMQ );
        }

        // ---------------------------------------------------------------------
        // System env variables and properties
        // ---------------------------------------------------------------------

        try
        {
            info.setJavaOpts( System.getenv( "JAVA_OPTS" ) );
        }
        catch ( SecurityException ex )
        {
            info.setJavaOpts( "Unknown" );
        }

        Properties props = System.getProperties();

        info.setJavaVersion( props.getProperty( "java.version" ) );
        info.setJavaVendor( props.getProperty( "java.vendor" ) );
        info.setOsName( props.getProperty( "os.name" ) );
        info.setOsArchitecture( props.getProperty( "os.arch" ) );
        info.setOsVersion( props.getProperty( "os.version" ) );

        info.setMemoryInfo( SystemUtils.getMemoryString() );
        info.setCpuCores( SystemUtils.getCpuCores() );
        info.setEncryption( dhisConfig.getEncryptionStatus().isOk() );

        return info;
    }

    private void setSystemMetadataVersionInfo( SystemInfo info )
    {
        Boolean isMetadataVersionEnabled = (boolean) systemSettingManager.getSystemSetting( SettingKey.METADATAVERSION_ENABLED );
        Date lastSuccessfulMetadataSync = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_METADATA_SYNC );
        Date metadataLastFailedTime = (Date) systemSettingManager.getSystemSetting( SettingKey.METADATA_LAST_FAILED_TIME );
        String systemMetadataVersion = (String) systemSettingManager.getSystemSetting( SettingKey.SYSTEM_METADATA_VERSION );
        Date lastMetadataVersionSyncAttempt = getLastMetadataVersionSyncAttempt( lastSuccessfulMetadataSync, metadataLastFailedTime );

        info.setIsMetadataVersionEnabled( isMetadataVersionEnabled );
        info.setSystemMetadataVersion( systemMetadataVersion );
        info.setLastMetadataVersionSyncAttempt( lastMetadataVersionSyncAttempt );
    }

    private Date getLastMetadataVersionSyncAttempt( Date lastSuccessfulMetadataSyncTime, Date lastFailedMetadataSyncTime )
    {
        if ( lastSuccessfulMetadataSyncTime == null && lastFailedMetadataSyncTime == null )
        {
            return null;
        }
        else if ( lastSuccessfulMetadataSyncTime == null || lastFailedMetadataSyncTime == null )
        {
            return (lastFailedMetadataSyncTime != null ? lastFailedMetadataSyncTime : lastSuccessfulMetadataSyncTime);
        }

        return (lastSuccessfulMetadataSyncTime.compareTo( lastFailedMetadataSyncTime ) < 0) ? lastFailedMetadataSyncTime : lastSuccessfulMetadataSyncTime;
    }
}
