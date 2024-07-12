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
package org.hisp.dhis.web.jetty;

import static org.hisp.dhis.webapi.servlet.DhisWebApiWebAppInitializer.setupServlets;
import static org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME;

import java.security.Security;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hisp.dhis.system.startup.StartupListener;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * This class is the main entry point for the embedded server. It starts an embedded Jetty server
 * and deploys the DHIS2 API web application.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Main extends EmbeddedJettyBase {

  private static final int DEFAULT_HTTP_PORT = 9090;
  private static final String SERVER_HOSTNAME_OR_IP = "localhost";
  private static final Long ELAPSED_SINCE_START = System.currentTimeMillis();

  public Main() {
    super();
  }

  public static void main(String[] args) throws Exception {
    Security.setProperty("crypto.policy", "unlimited");
    Security.setProperty("networkaddress.cache.ttl", "FOREVER");
    Security.setProperty("networkaddress.cache.negative.ttl", "10");

    setDefaultPropertyValue("jetty.host", SERVER_HOSTNAME_OR_IP);
    setDefaultPropertyValue("jetty.http.port", String.valueOf(DEFAULT_HTTP_PORT));

    Main jettyEmbeddedCoreWeb = new Main();
    jettyEmbeddedCoreWeb.printBanner("DHIS2 API Server");
    jettyEmbeddedCoreWeb.startJetty();
  }

  public ServletContextHandler getServletContextHandler() {
    ServletContextHandler contextHandler =
        new ServletContextHandler(ServletContextHandler.SESSIONS);
    contextHandler.setErrorHandler(null);

    AnnotationConfigWebApplicationContext webApplicationContext = getWebApplicationContext();
    contextHandler.addEventListener(new ContextLoaderListener(webApplicationContext));
    contextHandler.addEventListener(new StartupListener());
    contextHandler.addEventListener(new HttpSessionEventPublisher());

    // Spring Security Filter
    contextHandler.addFilter(
        new FilterHolder(new DelegatingFilterProxy(DEFAULT_FILTER_NAME)),
        "/*",
        EnumSet.allOf(DispatcherType.class));

    ContextHandler.Context context = contextHandler.getServletContext();

    setupServlets(context, webApplicationContext);

    return contextHandler;
  }

  private static AnnotationConfigWebApplicationContext getWebApplicationContext() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(JettyStartupTimerSpringConfiguration.class);
    return context;
  }

  public static Long getElapsedMsSinceStart() {
    return System.currentTimeMillis() - ELAPSED_SINCE_START;
  }
}
