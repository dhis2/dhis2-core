/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionTrackingMode;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.hisp.dhis.system.startup.StartupListener;
import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

@Slf4j
@Order(10)
public class DhisWebApiWebAppInitializer implements WebApplicationInitializer {
  @Override
  public void onStartup(ServletContext context) {
    context.getSessionCookieConfig().setName("JSESSIONID");
    context.getSessionCookieConfig().setHttpOnly(true);

    boolean httpsOnly = getConfig().isEnabled(ConfigurationKey.SERVER_HTTPS);
    log.info(String.format("Configuring cookies, HTTPS only: %b", httpsOnly));
    if (httpsOnly) {
      context.getSessionCookieConfig().setSecure(true);
      log.info("HTTPS only is enabled, cookies configured as secure");
    }

    String sameSite = getConfig().getProperty(ConfigurationKey.SESSION_COOKIE_SAME_SITE);
    log.info("SameSite cookie attribute set to: " + sameSite);
    if (sameSite != null) {
      context.getSessionCookieConfig().setAttribute("SameSite", sameSite);
    }

    context.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));

    AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext =
        new AnnotationConfigWebApplicationContext();
    annotationConfigWebApplicationContext.register(WebMvcConfig.class);

    context.addListener(new ContextLoaderListener(annotationConfigWebApplicationContext));
    context.addListener(new StartupListener());
    context.addListener(new HttpSessionEventPublisher());

    setupServlets(context, annotationConfigWebApplicationContext);
  }

  private DhisConfigurationProvider getConfig() {
    DefaultLocationManager locationManager = DefaultLocationManager.getDefault();
    locationManager.init();

    DefaultDhisConfigurationProvider configProvider =
        new DefaultDhisConfigurationProvider(locationManager);
    configProvider.init();

    return configProvider;
  }

  public static void setupServlets(
      ServletContext context, AnnotationConfigWebApplicationContext webApplicationContext) {

    context
        .addFilter(
            "SpringSessionRepositoryFilter",
            new DelegatingFilterProxy("springSessionRepositoryFilter"))
        .addMappingForUrlPatterns(null, false, "/*");

    DispatcherServlet servlet = new DispatcherServlet(webApplicationContext);
    ServletRegistration.Dynamic dispatcher = context.addServlet("dispatcher", servlet);
    dispatcher.setAsyncSupported(true);
    dispatcher.setLoadOnStartup(1);
    dispatcher.addMapping("/*");
    dispatcher.setMultipartConfig(new MultipartConfigElement(""));

    context
        .addServlet("TempGetAppMenuServlet", TempGetAppMenuServlet.class)
        .addMapping("/dhis-web-commons/menu/getModules.action");

    context
        .addFilter("webMetricsFilter", new DelegatingFilterProxy("webMetricsFilter"))
        .addMappingForUrlPatterns(null, false, "/api/*");

    FilterRegistration.Dynamic openSessionInViewFilter =
        context.addFilter("openSessionInViewFilter", OpenEntityManagerInViewFilter.class);
    openSessionInViewFilter.setInitParameter(
        "entityManagerFactoryBeanName", "entityManagerFactory");
    openSessionInViewFilter.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "/*");
    openSessionInViewFilter.addMappingForServletNames(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "dispatcher");

    FilterRegistration.Dynamic characterEncodingFilter =
        context.addFilter("characterEncodingFilter", CharacterEncodingFilter.class);
    characterEncodingFilter.setInitParameter("encoding", "UTF-8");
    characterEncodingFilter.setInitParameter("forceEncoding", "true");
    characterEncodingFilter.addMappingForUrlPatterns(null, false, "/*");
    characterEncodingFilter.addMappingForServletNames(null, false, "dispatcher");

    context
        .addFilter(
            "springSecurityFilterChain", new DelegatingFilterProxy("springSecurityFilterChain"))
        .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

    context
        .addFilter("ApiVersionFilter", new DelegatingFilterProxy("apiVersionFilter"))
        .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/api/*");

    context
        .addFilter("RequestIdentifierFilter", new DelegatingFilterProxy("requestIdentifierFilter"))
        .addMappingForUrlPatterns(null, true, "/*");

    /* Intercept index.html, plugin.html, and other html requests to inject no-cache
      headers using ContextUtils.setNoStore(response).
    */
    context
        .addFilter("AppHtmlNoCacheFilter", new DelegatingFilterProxy("appHtmlNoCacheFilter"))
        .addMappingForUrlPatterns(null, true, "/*");

    context
        .addFilter("GlobalShellFilter", new DelegatingFilterProxy("globalShellFilter"))
        .addMappingForUrlPatterns(null, true, "/*");

    context
        .addFilter("AppOverrideFilter", new DelegatingFilterProxy("appOverrideFilter"))
        .addMappingForUrlPatterns(null, true, "/*");

    String profile = System.getProperty("spring.profiles.active");
    if (profile == null || !profile.equals("embeddedJetty")) {
      RequestContextListener requestContextListener = new RequestContextListener();
      context.addListener(requestContextListener);
    }
  }
}
