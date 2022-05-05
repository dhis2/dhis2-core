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
package org.hisp.dhis.system.log;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.util.List;
import java.util.zip.Deflater;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CronTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * This class adds new Logger(s) and RollingFileAppender(s) to the XML-based,
 * default Log4J configuration. The goal is to create a number of scoped log
 * files, each for different areas of the application. The scope is defined by
 * package name.
 *
 * Additionally this class also attach a RollingFileAppender to the Root logger.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component( "logInitializer" )
public class Log4JLogConfigInitializer
    implements LogConfigInitializer
{
    private PatternLayout PATTERN_LAYOUT = PatternLayout.newBuilder().withPattern( "* %-5p %d{ISO8601} %m (%F [%t])%n" )
        .build();

    private static final String LOG_DIR = "logs";

    private static final String ANALYTICS_TABLE_LOGGER_FILENAME = "dhis-analytics-table.log";

    private static final String DATA_EXCHANGE_LOGGER_FILENAME = "dhis-data-exchange.log";

    private static final String DATA_SYNC_LOGGER_FILENAME = "dhis-data-sync.log";

    private static final String METADATA_SYNC_LOGGER_FILENAME = "dhis-metadata-sync.log";

    private static final String GENERAL_LOGGER_FILENAME = "dhis.log";

    private static final String PUSH_ANALYSIS_LOGGER_FILENAME = "dhis-push-analysis.log";

    private static final String AUDIT_LOGGER_FILENAME = "dhis-audit.log";

    private static final String LOG4J_CONF_PROP = "log4j2.configurationFile";

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
        if ( isNotBlank( System.getProperty( LOG4J_CONF_PROP ) ) )
        {
            log.info( "Aborting default log config, external config set through system prop " + LOG4J_CONF_PROP + ": "
                + System.getProperty( LOG4J_CONF_PROP ) );
            return;
        }

        if ( !locationManager.externalDirectorySet() )
        {
            log.warn( "Could not initialize additional log configuration, external home directory not set" );
            return;
        }

        log.info( String.format( "Initializing Log4j, max file size: '%s', max file archives: %s",
            config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_SIZE ),
            config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_ARCHIVES ) ) );

        locationManager.buildDirectory( LOG_DIR );

        configureLoggers( ANALYTICS_TABLE_LOGGER_FILENAME,
            Lists.newArrayList( "org.hisp.dhis.resourcetable", "org.hisp.dhis.analytics.table" ) );

        configureLoggers( DATA_EXCHANGE_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2" ) );

        configureLoggers( DATA_SYNC_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2.sync" ) );

        configureLoggers( METADATA_SYNC_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.dxf2.metadata" ) );

        configureLoggers( PUSH_ANALYSIS_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.pushanalysis" ) );

        configureAuditLogger( AUDIT_LOGGER_FILENAME, Lists.newArrayList( "org.hisp.dhis.audit" ) );

        configureRootLogger( GENERAL_LOGGER_FILENAME );

        final LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
        ctx.updateLoggers();
    }

    /**
     * Configures rolling audit file loggers.
     *
     * @param filename the filename to output logging to.
     * @param packages the logger names.
     */
    private void configureAuditLogger( String filename, List<String> packages )
    {
        String file = getLogFile( filename );

        RollingFileAppender appender = RollingFileAppender.newBuilder()
            .withFileName( file )
            .setName( "appender_" + file )
            .withFilePattern( file + ".%i" )
            .setLayout( PATTERN_LAYOUT )
            .withPolicy( CronTriggeringPolicy.createPolicy( getLogConfiguration(), "true", "0 0 * * *" ) )
            .withStrategy( DefaultRolloverStrategy.newBuilder()
                .withCompressionLevelStr( String.valueOf( Deflater.BEST_COMPRESSION ) )
                .withFileIndex( "nomax" )
                .build() )
            .build();

        appender.start();

        getLogConfiguration().addAppender( appender );

        AppenderRef[] refs = createAppenderRef( "Ref_" + filename );

        for ( String loggerName : packages )
        {
            LoggerConfig loggerConfig = LoggerConfig.createLogger( true, Level.INFO, loggerName, "true", refs, null,
                getLogConfiguration(), null );

            loggerConfig.addAppender( appender, null, null );

            getLogConfiguration().addLogger( loggerName, loggerConfig );

            log.info( "Added logger: " + loggerName + " using file: " + file );
        }
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

        getLogConfiguration().addAppender( appender );

        AppenderRef[] refs = createAppenderRef( "Ref_" + filename );

        for ( String loggerName : loggers )
        {
            LoggerConfig loggerConfig = LoggerConfig.createLogger( true, Level.INFO, loggerName, "true", refs, null,
                getLogConfiguration(), null );

            loggerConfig.addAppender( appender, null, null );

            getLogConfiguration().addLogger( loggerName, loggerConfig );

            log.info( "Added logger: " + loggerName + " using file: " + file );
        }
    }

    private AppenderRef[] createAppenderRef( String refName )
    {
        AppenderRef ref = AppenderRef.createAppenderRef( refName, Level.INFO, null );
        return new AppenderRef[] { ref };
    }

    private Configuration getLogConfiguration()
    {

        final LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
        return ctx.getConfiguration();
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

        getLogConfiguration().addAppender( appender );

        getLogConfiguration().getRootLogger().addAppender( getLogConfiguration().getAppender( appender.getName() ),
            Level.INFO, null );

        log.info( "Added root logger using file: " + file );
    }

    /**
     * Returns a rolling file appender.
     *
     * @param file the file to output to, including path and filename.
     */
    private RollingFileAppender getRollingFileAppender( String file )
    {
        RollingFileAppender appender = RollingFileAppender.newBuilder().withFileName( file )
            .setName( "appender_" + file )
            .withFilePattern( file + "%i" )
            .setLayout( PATTERN_LAYOUT )
            .withPolicy(
                SizeBasedTriggeringPolicy.createPolicy( config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_SIZE ) ) )
            .withStrategy( DefaultRolloverStrategy.newBuilder()
                .withMax( config.getProperty( ConfigurationKey.LOGGING_FILE_MAX_ARCHIVES ) ).build() )
            .build();

        appender.start();
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
