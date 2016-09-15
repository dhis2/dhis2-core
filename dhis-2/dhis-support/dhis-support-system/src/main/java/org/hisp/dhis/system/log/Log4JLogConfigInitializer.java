package org.hisp.dhis.system.log;

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

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author Lars Helge Overland
 */
public class Log4JLogConfigInitializer
    implements LogConfigInitializer
{    
    private static final PatternLayout PATTERN_LAYOUT = new PatternLayout( "* %-5p %d{ISO8601} %m (%F [%t])%n" );
    
    private static final String MAX_FILE_SIZE = "50MB";

    private static final String LOG_DIR = "logs";
    private static final String ANALYTICS_TABLE_LOGGER_FILENAME = "dhis-analytics-table.log";
    private static final String DATA_EXCHANGE_LOGGER_FILENAME = "dhis-data-exchange.log";
    private static final String DATA_SYNC_LOGGER_FILENAME = "dhis-data-sync.log";
    private static final String METADATA_SYNC_LOGGER_FILENAME = "dhis-metadata-sync.log";
    private static final String GENERAL_LOGGER_FILENAME = "dhis.log";
    private static final String PUSH_ANALYSIS_LOGGER_FILENAME = "dhis-push-analysis.log";
    private static final String LOG4J_CONF_PROP = "log4j.configuration";

    private static final Log log = LogFactory.getLog( Log4JLogConfigInitializer.class );
    
    @Autowired
    private LocationManager locationManager;
    
    @Override
    public void initConfig()
    {
        if ( !locationManager.externalDirectorySet() )
        {
            log.warn( "Could not initialize additional log configuration, external home directory not set" );
            return;
        }
        
        if ( isNotBlank( System.getProperty( LOG4J_CONF_PROP ) ) )
        {
            log.info( "Aborting default log config, external config set through system prop " + LOG4J_CONF_PROP + ": " + System.getProperty( LOG4J_CONF_PROP ) );
            return;
        }
        
        locationManager.buildDirectory( LOG_DIR );

        configureLoggers( ANALYTICS_TABLE_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.resourcetable", "org.hisp.dhis.analytics.table" ) );
        
        configureLoggers( DATA_EXCHANGE_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2" ) );
        
        configureLoggers( DATA_SYNC_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2.synch" ) );

        configureLoggers( METADATA_SYNC_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2.metadata" ) );

        configureLoggers( PUSH_ANALYSIS_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.pushanalysis" ) );
        
        configureRootLogger( GENERAL_LOGGER_FILENAME );
    }
    
    /**
     * Configures rolling file loggers.
     * 
     * @param filename the filename to output logging to.
     * @param loggers the logger names.
     */
    private void configureLoggers( String filename, List<String> loggers )
    {
        String file = getLogFile( filename );
        
        RollingFileAppender appender = getRollingFileAppender( file );

        for ( String loggerName : loggers )
        {
            Logger logger = Logger.getRootLogger().getLoggerRepository().getLogger( loggerName );
            
            logger.addAppender( appender );
            
            log.info( "Added logger: " + loggerName + " using file: " + file );     
        }  
    }
    
    /**
     * Configures a root file logger.
     * 
     * @param filename the filename to output logging to.
     */
    private void configureRootLogger( String filename )
    {
        String file = getLogFile( filename );
        
        RollingFileAppender appender = getRollingFileAppender( file );
        
        Logger.getRootLogger().addAppender( appender );
        
        log.info( "Added root logger using file: " + file );
    }
    
    /**
     * Returns a rolling file appender.
     * 
     * @param file the file to output to, including path and filename.
     */
    private RollingFileAppender getRollingFileAppender( String file )
    {
        RollingFileAppender appender = new RollingFileAppender();
        
        appender.setThreshold( Level.INFO );
        appender.setFile( file );
        appender.setMaxFileSize( MAX_FILE_SIZE );
        appender.setLayout( PATTERN_LAYOUT );
        appender.activateOptions();
        
        return appender;        
    }
    
    /**
     * Returns a file including path and filename.
     * 
     * @param filename the filename to use for the file path.
     */
    private String getLogFile( String filename )
    {
        return locationManager.getExternalDirectoryPath() + File.separator + LOG_DIR + File.separator + filename;
    }
}
