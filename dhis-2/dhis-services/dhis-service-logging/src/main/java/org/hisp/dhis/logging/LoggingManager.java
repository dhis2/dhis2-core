package org.hisp.dhis.logging;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class LoggingManager implements ApplicationEventPublisherAware, InitializingBean
{
    public final static ObjectMapper objectMapper = new ObjectMapper();
    private static LoggingManager instance;

    static
    {
        objectMapper.setSerializationInclusion( JsonInclude.Include.NON_EMPTY );
        objectMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        objectMapper.enable( SerializationFeature.WRAP_EXCEPTIONS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_FIELDS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_CREATORS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_GETTERS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_SETTERS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );
        objectMapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );

        objectMapper.registerModule( new Jdk8Module() );
        objectMapper.registerModule( new JavaTimeModule() );

    }

    private final SystemSettingManager systemSettingManager;

    private ApplicationEventPublisher publisher;

    public LoggingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    public void log( Log log )
    {
        if ( StringUtils.isEmpty( log.getUsername() ) )
        {
            SecurityContext context = SecurityContextHolder.getContext();

            if ( context.getAuthentication() != null )
            {
                if ( context.getAuthentication().getPrincipal() instanceof String )
                {
                    log.setUsername( (String) context.getAuthentication().getPrincipal() );
                }
            }
        }

        // if username is still null, this is probably an internal process.. so mark it as such
        if ( StringUtils.isEmpty( log.getUsername() ) )
        {
            log.setUsername( "system-process" );
        }

        publisher.publishEvent( new LogEvent( this, log, getLoggingConfig() ) );
    }

    @Override
    public void setApplicationEventPublisher( ApplicationEventPublisher publisher )
    {
        this.publisher = publisher;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        instance = this;
    }

    public static LoggingManager getInstance()
    {
        return instance;
    }

    public LoggingConfig getLoggingConfig()
    {
        return new LoggingConfig(
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
        );
    }

    public static String toJson( Log log )
    {
        try
        {
            return objectMapper.writeValueAsString( log );
        }
        catch ( JsonProcessingException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    public static Logger createLogger( Class<?> source )
    {
        return new Logger( source );
    }

    public static class Logger
    {
        private final Class<?> source;

        public Logger( Class<?> source )
        {
            this.source = source;
        }

        public void log( String message )
        {
            log( new Log( message ) );
        }

        public void fatal( String message )
        {
            log( new Log( message ).setLogLevel( LogLevel.FATAL ) );
        }

        public void error( String message )
        {
            log( new Log( message ).setLogLevel( LogLevel.ERROR ) );
        }

        public void warn( String message )
        {
            log( new Log( message ).setLogLevel( LogLevel.WARN ) );
        }

        public void info( String message )
        {
            log( new Log( message ).setLogLevel( LogLevel.INFO ) );
        }

        public void debug( String message )
        {
            log( new Log( message ).setLogLevel( LogLevel.DEBUG ) );
        }

        public void trace( String message )
        {
            log( new Log( message ).setLogLevel( LogLevel.TRACE ) );
        }

        public void log( Log log )
        {
            if ( log.getSource() == null )
            {
                log.setSource( source );
            }

            getInstance().log( log );
        }

        public boolean isDebugEnabled()
        {
            return true;
        }
    }
}
