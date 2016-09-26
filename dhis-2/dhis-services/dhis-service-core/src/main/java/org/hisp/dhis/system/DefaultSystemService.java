package org.hisp.dhis.system;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.datasource.DataSourceManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.SystemUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Lars Helge Overland
 */
public class DefaultSystemService
    implements SystemService
{
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

    // -------------------------------------------------------------------------
    // SystemService implementation
    // -------------------------------------------------------------------------

    @Override
    public SystemInfo getSystemInfo()
    {
        if ( systemInfo == null )
        {
            systemInfo = getFixedSystemInfo();
        }

        // ---------------------------------------------------------------------
        // Set volatile properties
        // ---------------------------------------------------------------------

        Date lastAnalyticsTableSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        String lastAnalyticsTableRuntime = (String) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME );
        
        Date now = new Date();
        
        SystemInfo info = systemInfo.instance();
        
        info.setCalendar( calendarService.getSystemCalendar().name() );
        info.setDateFormat( calendarService.getSystemDateFormat().getJs() );
        info.setServerDate( new Date() );
        info.setLastAnalyticsTableSuccess( lastAnalyticsTableSuccess );
        info.setIntervalSinceLastAnalyticsTableSuccess( DateUtils.getPrettyInterval( lastAnalyticsTableSuccess, now ) );
        info.setLastAnalyticsTableRuntime( lastAnalyticsTableRuntime );
        
        return info;
    }
    
    private SystemInfo getFixedSystemInfo()
    {
        SystemInfo info = new SystemInfo();

        // ---------------------------------------------------------------------
        // Version
        // ---------------------------------------------------------------------

        ClassPathResource resource = new ClassPathResource( "build.properties" );

        if ( resource.isReadable() )
        {
            InputStream in = null;

            try
            {
                in = resource.getInputStream();

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
            finally
            {
                IOUtils.closeQuietly( in );
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
        
        // ---------------------------------------------------------------------
        // Database
        // ---------------------------------------------------------------------

        info.setDatabaseInfo( databaseInfo );
        info.setReadReplicaCount( dataSourceManager.getReadReplicaCount() );
        
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

        Configuration config = configurationService.getConfiguration();

        info.setSystemId( config.getSystemId() );

        return info;
    }
}
