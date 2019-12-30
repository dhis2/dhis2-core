package org.hisp.dhis.system.log;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Component( "logInitializer" )
public class Log4JLogConfigInitializer
    implements LogConfigInitializer
{
    private static final String PATTERN_LAYOUT = "* %-5p %d{ISO8601} %m (%F [%t])%n";
    private static final Level DEFAULT_ROLLINGFILEAPPENDER_LEVEL = Level.INFO;
    private static final String LOG_DIR = "logs";
    private static final String ANALYTICS_TABLE_LOGGER_FILENAME = "dhis-analytics-table.log";
    private static final String DATA_EXCHANGE_LOGGER_FILENAME = "dhis-data-exchange.log";
    private static final String DATA_SYNC_LOGGER_FILENAME = "dhis-data-sync.log";
    private static final String METADATA_SYNC_LOGGER_FILENAME = "dhis-metadata-sync.log";
    private static final String GENERAL_LOGGER_FILENAME = "dhis.log";
    private static final String PUSH_ANALYSIS_LOGGER_FILENAME = "dhis-push-analysis.log";
    private static final String LOG4J_CONF_PROP = "log4j.configuration";

    private static final Log log = LogFactory.getLog( Log4JLogConfigInitializer.class );

    private final LocationManager locationManager;

    private final DhisConfigurationProvider config;

    public Log4JLogConfigInitializer( LocationManager locationManager, DhisConfigurationProvider config )
    {
        checkNotNull( locationManager );
        checkNotNull( config );
        this.locationManager = locationManager;
        this.config = config;
    }

    @PostConstruct
    @Override
    public void initConfig()
    {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

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

        log.info( String.format( "Initializing Log4j, max file size: '%s', max file archives: %s",
            config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_SIZE ),
            config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_ARCHIVES ) ) );

        locationManager.buildDirectory( LOG_DIR );

        configureLoggers( builder, ANALYTICS_TABLE_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.resourcetable", "org.hisp.dhis.analytics.table" ) );

        configureLoggers( builder, DATA_EXCHANGE_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2" ) );

        configureLoggers( builder, DATA_SYNC_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2.synch" ) );

        configureLoggers( builder, METADATA_SYNC_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2.metadata" ) );

        configureLoggers( builder, PUSH_ANALYSIS_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.pushanalysis" ) );

        configureRootLogger( builder, GENERAL_LOGGER_FILENAME );

        Configurator.initialize( builder.build() );
    }
    
    private void configureLoggers( ConfigurationBuilder<BuiltConfiguration> builder, String filename,
        List<String> loggers )
    {
        String file = getLogFile( filename );
        String appenderName = filename.substring( 0, filename.lastIndexOf( "." ) );

        createRollingFileAppender( builder, appenderName, file );

        for ( String loggerName : loggers )
        {
            LoggerComponentBuilder logger = builder.newLogger( loggerName );
            logger.add( builder.newAppenderRef( appenderName ).addAttribute( "level", DEFAULT_ROLLINGFILEAPPENDER_LEVEL ) );
            logger.addAttribute( "additivity", true );
            builder.add( logger );

            log.info( "Added logger: " + loggerName + " using file: " + file );
        }
    }

    /**
     * Configures a root file logger.
     *
     * @param filename the filename to output logging to.
     */
    private void configureRootLogger( ConfigurationBuilder<BuiltConfiguration> builder, String filename )
    {
        String file = getLogFile( filename );

        createRollingFileAppender( builder, "rootAppender", file );

        builder.newRootLogger().add(builder.newAppenderRef( "rootAppender") );

        log.info( "Added root logger using file: " + file );
    }

    /**
     *
     * @param builder a Log4J ConfigurationBuilder
     * @param appenderName the name of the Appender
     * @param file the file to output to, including path and filename.
     */
    private void createRollingFileAppender(ConfigurationBuilder<BuiltConfiguration> builder, String appenderName, String file )
    {
        @SuppressWarnings("rawtypes")
        ComponentBuilder triggeringPolicy = builder.newComponent( "Policies" )
            .addComponent( builder.newComponent( "SizeBasedTriggeringPolicy" ).addAttribute( "size",
                config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_SIZE ) ) );

        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", PATTERN_LAYOUT);

        // DefaultRolloverStrategy Component
        @SuppressWarnings("rawtypes")
        ComponentBuilder defaultRolloverStrategy = builder.newComponent("DefaultRolloverStrategy")
                .addAttribute("max", config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_ARCHIVES ));

        AppenderComponentBuilder rollingFile = builder.newAppender(appenderName, "RollingFile")
            .addAttribute( "fileName", file )
            .addAttribute( "filePattern", file + "%i")
            .addComponent( triggeringPolicy )
            .addComponent( defaultRolloverStrategy )
            .add( layoutBuilder );

        builder.add( rollingFile );
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
