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

import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class LogManager implements ApplicationEventPublisherAware, ApplicationListener<LogEvent>, InitializingBean
{
    private static final long serialVersionUID = 1L;
    private static LogManager instance;

    private final SystemSettingManager systemSettingManager;

    private ApplicationEventPublisher publisher;

    public LogManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    public void log( Log log )
    {
        if ( log.getUsername() != null )
        {
            return;
        }

        SecurityContext context = SecurityContextHolder.getContext();

        if ( context.getAuthentication() != null )
        {
            if ( context.getAuthentication().getPrincipal() instanceof String )
            {
                log.setUsername( (String) context.getAuthentication().getPrincipal() );
            }
        }

        publisher.publishEvent( new LogEvent( this, log ) );
    }

    @Override
    public void onApplicationEvent( LogEvent event )
    {
        System.err.println( event.getLog() );
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

    public static LogManager getInstance()
    {
        return instance;
    }

    public static Logger logger( Class<?> source )
    {
        return new Logger( LogManager.getInstance(), source );
    }

    public static class Logger
    {
        private final LogManager logManager;
        private final Class<?> source;

        public Logger( LogManager logManager, Class<?> source )
        {
            Assert.notNull( logManager, "LogManager is required, check if the logger is called within a spring context." );
            this.logManager = logManager;
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

            logManager.log( log );
        }
    }
}
