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
package org.hisp.dhis.webapi.security.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_API_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.spring.web.servlet.DefaultWebMvcTagsProvider;
import io.micrometer.spring.web.servlet.WebMvcMetricsFilter;
import io.micrometer.spring.web.servlet.WebMvcTagsProvider;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.condition.PropertiesAwareConfigurationCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.monitoring.metrics.MetricsEnabler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * @author Luciano Fiandesio
 */
@Configuration
@Conditional(WebMvcMetricsConfig.WebMvcMetricsEnabledCondition.class)
public class WebMvcMetricsConfig {
  @Bean
  public DefaultWebMvcTagsProvider servletTagsProvider() {
    return new DefaultWebMvcTagsProvider();
  }

  @Bean
  public WebMvcMetricsFilter webMetricsFilter(
      MeterRegistry registry,
      WebMvcTagsProvider tagsProvider,
      HandlerMappingIntrospector handlerMappingIntrospector) {
    return new WebMvcMetricsFilter(
        registry, tagsProvider, "http_server_requests", true, handlerMappingIntrospector);
  }

  @Configuration
  @Conditional(WebMvcMetricsDisabledCondition.class)
  static class DataSourcePoolMetadataMetricsConfiguration {
    @Bean
    public PassThroughWebMvcMetricsFilter webMetricsFilter() {
      return new PassThroughWebMvcMetricsFilter();
    }
  }

  // If API metrics are disabled, system still expects a filter named
  // 'webMetricsFilter' to be available

  static class PassThroughWebMvcMetricsFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      filterChain.doFilter(request, response);
    }
  }

  static class WebMvcMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_API_ENABLED;
    }
  }

  static class WebMvcMetricsDisabledCondition extends PropertiesAwareConfigurationCondition {
    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    public boolean matches(
        ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
      return isTestRun(conditionContext) || !getBooleanValue(MONITORING_API_ENABLED);
    }
  }
}
