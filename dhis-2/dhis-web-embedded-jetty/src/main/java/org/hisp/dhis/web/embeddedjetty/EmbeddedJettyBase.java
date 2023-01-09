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
package org.hisp.dhis.web.embeddedjetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.core.io.ClassPathResource;

import com.google.common.base.Preconditions;

@Slf4j
public abstract class EmbeddedJettyBase
{
    private String resourceBase = "./dhis-web/dhis-web-portal/target/dhis";

    public EmbeddedJettyBase()
    {
        Thread.currentThread().setUncaughtExceptionHandler( EmbeddedJettyUncaughtExceptionHandler.systemExit( log ) );
    }

    public void startJetty()
        throws Exception
    {
        Integer queueSize = getIntSystemProperty( "jetty.thread.queue", 6000 );
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( queueSize );

        Integer maxThreads = getIntSystemProperty( "jetty.threads.max", 200 );
        QueuedThreadPool threadPool = new InstrumentedQueuedThreadPool(
            maxThreads,
            10,
            60000,
            queue );

        threadPool.setDetailedDump( getBooleanSystemProperty( "jetty.detailedDump", false ) );

        Server server = new Server( threadPool );
        server.addBean( new org.eclipse.jetty.util.thread.ScheduledExecutorScheduler() );

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed( false );
        resourceHandler.setResourceBase( resourceBase );

        RewriteHandler rewrite = new RewriteHandler();
        rewrite.setHandler( resourceHandler );
        RedirectPatternRule rewritePatternRule = new RedirectPatternRule();
        rewritePatternRule.setPattern( "" );
        rewritePatternRule.setLocation( "/index.html" );
        rewrite.addRule( rewritePatternRule );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[] { rewrite,
            getServletContextHandler(), new DefaultHandler() } );
        server.setHandler( handlers );

        final HttpConfiguration http_config = getHttpConfiguration();
        addHttpConnector( server, http_config );

        server.setStopAtShutdown( true );
        server.setStopTimeout( 5000 );
        server.setDumpBeforeStop( getBooleanSystemProperty( "jetty.dumpBeforeStop", false ) );
        server.setDumpAfterStart( getBooleanSystemProperty( "jetty.dumpBeforeStart", false ) );

        server.start();
        server.join();

        log.info( "DHIS2 Server stopped!" );
    }

    private void addHttpConnector( Server server, HttpConfiguration http_config )
    {
        setDefaultPropertyValue( "jetty.port", System.getProperty( "jetty.http.port" ) );
        server.addConnector( setupHTTPConnector( server, http_config ) );
    }

    private HttpConfiguration getHttpConfiguration()
    {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize( 32768 );
        httpConfig.setRequestHeaderSize( 8192 );
        httpConfig.setResponseHeaderSize( 8192 );
        httpConfig.setSendServerVersion( true );
        httpConfig.setSendDateHeader( false );
        httpConfig.setHeaderCacheSize( 512 );
        return httpConfig;
    }

    private Connector setupHTTPConnector( Server server, HttpConfiguration http_config )
    {
        ServerConnector httpConnector = new ServerConnector(
            server,
            getIntSystemProperty( "jetty.http.acceptors", -1 ),
            getIntSystemProperty( "jetty.http.selectors", -1 ),
            new HttpConnectionFactory( http_config ) );

        httpConnector.setHost( getStringSystemProperty( "jetty.host", null ) );
        httpConnector.setPort( getIntSystemProperty( "jetty.http.port", -1 ) );
        httpConnector.setIdleTimeout( getIntSystemProperty( "jetty.http.idleTimeout", 300000 ) );

        return httpConnector;
    }

    protected void printBanner( String name )
    {
        String msg = "Starting: " + name;
        try (
            final InputStream resourceStream = new ClassPathResource( "banner.txt" ).getInputStream();
            final InputStreamReader inputStreamReader = new InputStreamReader( resourceStream, "UTF-8" );
            final BufferedReader bufferedReader = new BufferedReader( inputStreamReader ) )
        {
            final String banner = bufferedReader
                .lines()
                .collect( Collectors.joining( String.format( "%n" ) ) );
            msg = String.format( "Starting: %n%s", banner );
        }
        catch ( IllegalArgumentException | IOException ignored )
        {
        }
        log.info( msg );
    }

    public static Boolean getBooleanSystemProperty( String key, Boolean defaultValue )
    {
        Preconditions.checkNotNull( key, "Key can not be NULL!" );
        return Boolean.valueOf( System.getProperty( key, defaultValue.toString() ) );
    }

    public static String getStringSystemProperty( String key, String defaultValue )
    {
        Preconditions.checkNotNull( key, "'key' can not be NULL!" );
        return System.getProperty( key, defaultValue );
    }

    public static Integer getIntSystemProperty( String key, Integer defaultValue )
    {
        Preconditions.checkNotNull( key, "Key can not be NULL!" );
        return Integer.valueOf( System.getProperty( key, String.valueOf( defaultValue ) ) );
    }

    public static void setDefaultPropertyValue( String key, String defaultValue )
    {
        Preconditions.checkNotNull( key );
        Preconditions.checkNotNull( defaultValue );
        String property = System.getProperty( key, defaultValue );
        System.setProperty( key, property );
    }

    public abstract ServletContextHandler getServletContextHandler();
}
