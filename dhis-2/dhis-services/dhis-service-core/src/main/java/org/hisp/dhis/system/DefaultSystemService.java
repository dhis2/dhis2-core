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
package org.hisp.dhis.system;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.util.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.system.SystemService" )
public class DefaultSystemService
    implements SystemService, InitializingBean
{
    private final LocationManager locationManager;

    private final DatabaseInfo databaseInfo;

    private final ConfigurationService configurationService;

    private final DhisConfigurationProvider dhisConfig;

    private final CalendarService calendarService;

    private final SystemSettingManager systemSettingManager;

    public DefaultSystemService( LocationManager locationManager, DatabaseInfo databaseInfo,
        ConfigurationService configurationService, DhisConfigurationProvider dhisConfig,
        CalendarService calendarService, SystemSettingManager systemSettingManager )
    {
        checkNotNull( locationManager );
        checkNotNull( databaseInfo );
        checkNotNull( configurationService );
        checkNotNull( dhisConfig );
        checkNotNull( calendarService );
        checkNotNull( systemSettingManager );

        this.locationManager = locationManager;
        this.databaseInfo = databaseInfo;
        this.configurationService = configurationService;
        this.dhisConfig = dhisConfig;
        this.calendarService = calendarService;
        this.systemSettingManager = systemSettingManager;
    }

    /**
     * Variable holding fixed system info state.
     */
    private SystemInfo systemInfo = null;

    @Override
    public void afterPropertiesSet()
    {
        systemInfo = getFixedSystemInfo();

        List<String> info = ImmutableList.of(
            "DHIS 2 Version: " + systemInfo.getVersion(),
            "Revision: " + systemInfo.getRevision(),
            "Build date: " + systemInfo.getBuildTime(),
            "Database name: " + systemInfo.getDatabaseInfo().getName(),
            "Java version: " + systemInfo.getJavaVersion() );

        log.info( StringUtils.join( info, ", " ) );
    }

    // -------------------------------------------------------------------------
    // SystemService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public SystemInfo getSystemInfo()
    {
        SystemInfo info = systemInfo != null ? systemInfo.instance() : null;
        TimeZone tz = Calendar.getInstance().getTimeZone();

        if ( info == null )
        {
            return null;
        }

        Date lastAnalyticsTableSuccess = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        String lastAnalyticsTableRuntime = (String) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME );
        Date lastAnalyticsTablePartitionSuccess = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE );
        String lastAnalyticsTablePartitionRuntime = (String) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_RUNTIME );
        Date lastSystemMonitoringSuccess = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_SYSTEM_MONITORING_PUSH );
        String systemName = (String) systemSettingManager.getSystemSetting( SettingKey.APPLICATION_TITLE );
        String instanceBaseUrl = dhisConfig.getServerBaseUrl();

        Date now = new Date();

        info.setCalendar( calendarService.getSystemCalendar().name() );
        info.setDateFormat( calendarService.getSystemDateFormat().getJs() );
        info.setServerDate( new Date() );
        info.setServerTimeZoneId( tz.getID() );
        info.setServerTimeZoneDisplayName( tz.getDisplayName() );

        info.setLastAnalyticsTableSuccess( lastAnalyticsTableSuccess );
        info.setIntervalSinceLastAnalyticsTableSuccess( DateUtils.getPrettyInterval( lastAnalyticsTableSuccess, now ) );
        info.setLastAnalyticsTableRuntime( lastAnalyticsTableRuntime );

        info.setLastAnalyticsTablePartitionSuccess( lastAnalyticsTablePartitionSuccess );
        info.setIntervalSinceLastAnalyticsTablePartitionSuccess(
            DateUtils.getPrettyInterval( lastAnalyticsTablePartitionSuccess, now ) );
        info.setLastAnalyticsTablePartitionRuntime( lastAnalyticsTablePartitionRuntime );

        info.setLastSystemMonitoringSuccess( lastSystemMonitoringSuccess );

        info.setSystemName( systemName );
        info.setInstanceBaseUrl( instanceBaseUrl );
        info.setEmailConfigured( systemSettingManager.emailConfigured() );

        setSystemMetadataVersionInfo( info );

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
        info.setReadOnlyMode( dhisConfig.getProperty( ConfigurationKey.SYSTEM_READ_ONLY_MODE ) );
        info.setNodeId( dhisConfig.getProperty( ConfigurationKey.NODE_ID ) );
        info.setSystemMonitoringUrl( dhisConfig.getProperty( ConfigurationKey.SYSTEM_MONITORING_URL ) );
        info.setSystemId( config.getSystemId() );
        info.setClusterHostname( dhisConfig.getProperty( ConfigurationKey.CLUSTER_HOSTNAME ) );
        info.setRedisEnabled( Boolean.parseBoolean( dhisConfig.getProperty( ConfigurationKey.REDIS_ENABLED ) ) );

        if ( info.isRedisEnabled() )
        {
            info.setRedisHostname( dhisConfig.getProperty( ConfigurationKey.REDIS_HOST ) );
        }

        // ---------------------------------------------------------------------
        // Database
        // ---------------------------------------------------------------------

        info.setDatabaseInfo( databaseInfo.instance() );
        info.setReadReplicaCount( Integer.valueOf( dhisConfig.getProperty( ConfigurationKey.ACTIVE_READ_REPLICAS ) ) );

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
        Boolean isMetadataVersionEnabled = (boolean) systemSettingManager
            .getSystemSetting( SettingKey.METADATAVERSION_ENABLED );
        Date lastSuccessfulMetadataSync = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_METADATA_SYNC );
        Date metadataLastFailedTime = (Date) systemSettingManager
            .getSystemSetting( SettingKey.METADATA_LAST_FAILED_TIME );
        String systemMetadataVersion = (String) systemSettingManager
            .getSystemSetting( SettingKey.SYSTEM_METADATA_VERSION );
        Date lastMetadataVersionSyncAttempt = getLastMetadataVersionSyncAttempt( lastSuccessfulMetadataSync,
            metadataLastFailedTime );

        info.setIsMetadataVersionEnabled( isMetadataVersionEnabled );
        info.setSystemMetadataVersion( systemMetadataVersion );
        info.setLastMetadataVersionSyncAttempt( lastMetadataVersionSyncAttempt );
    }

    private Date getLastMetadataVersionSyncAttempt( Date lastSuccessfulMetadataSyncTime,
        Date lastFailedMetadataSyncTime )
    {
        if ( lastSuccessfulMetadataSyncTime == null && lastFailedMetadataSyncTime == null )
        {
            return null;
        }
        else if ( lastSuccessfulMetadataSyncTime == null || lastFailedMetadataSyncTime == null )
        {
            return (lastFailedMetadataSyncTime != null ? lastFailedMetadataSyncTime : lastSuccessfulMetadataSyncTime);
        }

        return (lastSuccessfulMetadataSyncTime.compareTo( lastFailedMetadataSyncTime ) < 0) ? lastFailedMetadataSyncTime
            : lastSuccessfulMetadataSyncTime;
    }
}
