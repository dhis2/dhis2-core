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
package org.hisp.dhis.webapi.security.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_API_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.hisp.dhis.condition.PropertiesAwareConfigurationCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.monitoring.metrics.MetricsEnabler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.util.UrlPathHelper;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class WebMvcMetricsConfig implements WebMvcConfigurer {
  private static final String ATTRIBUTE_START_TIME =
      WebMvcMetricsFilter.class.getName() + ".START_TIME";

  /**
   * Interface defining how to generate Tags to be associated with metrics for Spring MVC requests.
   */
  public interface WebMvcTagsProvider {
    /**
     * Provides tags to be associated with metrics for the given request.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param handler the handler for the request or {@code null} if the handler is not known
     * @param exception the exception, if any, thrown during request handling
     * @return tags to associate with metrics for the request
     */
    Iterable<Tag> getTags(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Throwable exception);
  }

  /** Default implementation of {@link WebMvcTagsProvider}. */
  public static class DefaultWebMvcTagsProvider implements WebMvcTagsProvider {

    private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");
    private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");
    private static final Tag URI_ROOT = Tag.of("uri", "root");
    private static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");
    private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");
    private static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");

    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Override
    public Iterable<Tag> getTags(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Throwable exception) {
      return Tags.of(
          getUriTag(request, response),
          getMethodTag(request),
          getStatusTag(response),
          getExceptionTag(exception),
          getOutcomeTag(response));
    }

    private Tag getUriTag(HttpServletRequest request, HttpServletResponse response) {
      if (response != null) {
        HttpStatus status = HttpStatus.resolve(response.getStatus());
        if (status != null) {
          if (status.is3xxRedirection()) {
            return URI_REDIRECTION;
          }
          if (status == HttpStatus.NOT_FOUND) {
            return URI_NOT_FOUND;
          }
        }
      }

      String uri = getPathWithinApplication(request);
      if (uri.isEmpty()) {
        return URI_ROOT;
      }

      // Use the handler mapping pattern if possible
      Object bestMatchingPattern =
          request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      if (bestMatchingPattern != null) {
        return Tag.of("uri", bestMatchingPattern.toString());
      }

      return Tag.of("uri", uri.startsWith("/") ? uri : "/" + uri);
    }

    private String getPathWithinApplication(HttpServletRequest request) {
      try {
        return urlPathHelper.getPathWithinApplication(request);
      } catch (Exception ex) {
        return URI_UNKNOWN.getValue();
      }
    }

    private Tag getMethodTag(HttpServletRequest request) {
      return Tag.of("method", request.getMethod());
    }

    private Tag getStatusTag(HttpServletResponse response) {
      return (response != null)
          ? Tag.of("status", String.valueOf(response.getStatus()))
          : STATUS_UNKNOWN;
    }

    private Tag getExceptionTag(Throwable exception) {
      if (exception != null) {
        String simpleName = exception.getClass().getSimpleName();
        return Tag.of(
            "exception", simpleName.isEmpty() ? exception.getClass().getName() : simpleName);
      }
      return EXCEPTION_NONE;
    }

    private Tag getOutcomeTag(HttpServletResponse response) {
      if (response != null) {
        HttpStatus status = HttpStatus.resolve(response.getStatus());
        if (status != null) {
          return Tag.of("outcome", status.series().name());
        }
      }
      return Tag.of("outcome", "UNKNOWN");
    }
  }

  @Bean
  public WebMvcTagsProvider servletTagsProvider() {
    return new DefaultWebMvcTagsProvider();
  }

  /** Replacement for the legacy WebMvcMetricsFilter that works with Jakarta EE. */
  public static class WebMvcMetricsFilter extends PushUsageMetricsWebMvcMetricsFilter {
    private final MeterRegistry registry;
    private final WebMvcTagsProvider tagsProvider;
    private final String metricName;
    private final boolean autoTimeRequests;
    private final HandlerMappingIntrospector handlerMappingIntrospector;

    public WebMvcMetricsFilter(
        MeterRegistry meterRegistry,
        OpenTelemetrySdk otelSdk,
        WebMvcTagsProvider tagsProvider,
        String metricName,
        boolean autoTimeRequests,
        HandlerMappingIntrospector handlerMappingIntrospector) {
      super(otelSdk, tagsProvider);
      this.registry = meterRegistry;
      this.tagsProvider = tagsProvider;
      this.metricName = metricName;
      this.autoTimeRequests = autoTimeRequests;
      this.handlerMappingIntrospector = handlerMappingIntrospector;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
      return false;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      if (!this.autoTimeRequests) {
        super.doFilterInternal(request, response, filterChain);
        return;
      }

      long startTime = System.nanoTime();
      request.setAttribute(ATTRIBUTE_START_TIME, startTime);

      try {
        super.doFilterInternal(request, response, filterChain);
        Object handler = getHandler(request);
        recordTimerSample(startTime, request, response, handler, null);
      } catch (Exception exception) {
        Object handler = getHandler(request);
        recordTimerSample(startTime, request, response, handler, exception);
        throw exception;
      }
    }

    private Object getHandler(HttpServletRequest request) {
      try {
        return this.handlerMappingIntrospector
            .getMatchableHandlerMapping(request)
            .getHandler(request);
      } catch (Exception ex) {
        return null;
      }
    }

    private void recordTimerSample(
        long startTime,
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception exception) {
      long endTime = System.nanoTime();
      HandlerMethod handlerMethod = (handler instanceof HandlerMethod method) ? method : null;
      Iterable<Tag> tags = this.tagsProvider.getTags(request, response, handlerMethod, exception);
      Timer.builder(this.metricName)
          .tags(tags)
          .register(this.registry)
          .record(endTime - startTime, TimeUnit.NANOSECONDS);
    }
  }

  @Bean(name = "webMetricsFilter")
  @Conditional(WebMvcMetricsConfig.WebMvcMetricsEnabledCondition.class)
  public WebMvcMetricsFilter webMetricsFilter(
      MeterRegistry metricsRegistry,
      OpenTelemetrySdk otelSdk,
      WebMvcTagsProvider tagsProvider,
      HandlerMappingIntrospector handlerMappingIntrospector) {
    return new WebMvcMetricsFilter(
        metricsRegistry,
        otelSdk,
        tagsProvider,
        "http_server_requests",
        true,
        handlerMappingIntrospector);
  }

  @Bean(name = "webMetricsFilter")
  @Conditional(WebMvcMetricsDisabledCondition.class)
  public PushUsageMetricsWebMvcMetricsFilter pushUsageMetricsFilter(
      OpenTelemetrySdk otelSdk, WebMvcTagsProvider tagsProvider) {
    return new PushUsageMetricsWebMvcMetricsFilter(otelSdk, tagsProvider);
  }

  public static class PushUsageMetricsWebMvcMetricsFilter extends OncePerRequestFilter {
    private final LongCounter counter;
    private final WebMvcTagsProvider tagsProvider;

    public PushUsageMetricsWebMvcMetricsFilter(
        OpenTelemetrySdk otelSdk, WebMvcTagsProvider tagsProvider) {

      this.counter =
          otelSdk
              .getMeter("usage-metrics")
              .counterBuilder("http_errors")
              .setDescription("HTTP Errors")
              .build();
      this.tagsProvider = tagsProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      try {
        filterChain.doFilter(request, response);
        if (HttpStatus.resolve(response.getStatus()).is5xxServerError()) {
          counter.add(
              1,
              collectAttributes(this.tagsProvider.getTags(request, response, null, null), request));
        }
      } catch (Exception exception) {
        counter.add(
            1,
            collectAttributes(this.tagsProvider.getTags(request, response, null, null), request));
        throw exception;
      }
    }

    protected Attributes collectAttributes(Iterable<Tag> tags, HttpServletRequest request) {
      AttributesBuilder attributesBuilder = Attributes.builder();
      StreamSupport.stream(tags.spliterator(), false)
          .forEach(
              t -> {
                String attrValue;
                if (t.getKey().equals("uri")
                    && request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                        == null) {
                  attrValue = "no-uri-pattern";
                } else {
                  attrValue = t.getValue();
                }
                attributesBuilder.put(t.getKey(), attrValue);
              });
      return attributesBuilder.build();
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
