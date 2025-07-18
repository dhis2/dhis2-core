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

import static org.springframework.http.MediaType.parseMediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.DefaultRequestInfoService;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPathConverter;
import org.hisp.dhis.fieldfiltering.better.FieldsConverter;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.webapi.mvc.CurrentSystemSettingsHandlerMethodArgumentResolver;
import org.hisp.dhis.webapi.mvc.CurrentUserHandlerMethodArgumentResolver;
import org.hisp.dhis.webapi.mvc.CustomRequestMappingHandlerMapping;
import org.hisp.dhis.webapi.mvc.DhisApiVersionHandlerMethodArgumentResolver;
import org.hisp.dhis.webapi.mvc.interceptor.AuthorityInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.RequestInfoInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.SystemSettingsInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.TrailingSlashInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.UserContextInterceptor;
import org.hisp.dhis.webapi.mvc.messageconverter.FilteredPageHttpMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.MetadataExportParamsMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.StreamingJsonRootMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.XmlMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.XmlPathMappingJackson2XmlHttpMessageConverter;
import org.hisp.dhis.webapi.view.CustomPathExtensionContentNegotiationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order(1000)
@ComponentScan(basePackages = {"org.hisp.dhis"})
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebMvcConfig extends DelegatingWebMvcConfiguration {
  // Paths where XML should still be allowed.
  public static final List<Pattern> XML_PATTERNS =
      List.of(
          Pattern.compile("/api/(\\d\\d/)?dataValueSets(.xml)?(.+)?"),
          Pattern.compile("/api/(\\d\\d/)?completeDataSetRegistrations(.xml)?(.+)?"));

  @Autowired
  private CurrentUserHandlerMethodArgumentResolver currentUserHandlerMethodArgumentResolver;

  @Autowired
  private CurrentSystemSettingsHandlerMethodArgumentResolver
      currentSystemSettingsHandlerMethodArgumentResolver;

  @Autowired private DefaultRequestInfoService requestInfoService;

  @Autowired private AuthorityInterceptor authorityInterceptor;

  @Autowired private SystemSettingsInterceptor settingsInterceptor;

  @Autowired private NodeService nodeService;

  @Autowired
  @Qualifier("jsonMapper")
  private ObjectMapper jsonMapper;

  @Qualifier("jsonFilterMapper")
  @Autowired
  private ObjectMapper jsonFilterMapper;

  @Autowired
  @Qualifier("xmlMapper")
  private ObjectMapper xmlMapper;

  @Autowired private MetadataExportService metadataExportService;

  @Autowired private FieldFilterService fieldFilterService;

  static class IndexFallbackResourceResolver extends PathResourceResolver {
    @Override
    protected Resource resolveResourceInternal(
        HttpServletRequest request,
        String requestPath,
        List<? extends Resource> locations,
        ResourceResolverChain chain) {
      Resource resource = super.resolveResourceInternal(request, requestPath, locations, chain);
      if (resource == null) {
        // try with /index.html
        resource =
            super.resolveResourceInternal(request, requestPath + "/index.html", locations, chain);
      }
      return resource;
    }
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .setOrder(Ordered.LOWEST_PRECEDENCE)
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/", "file:./dhis-web-apps/target/dhis-web-apps/")
        // .setCachePeriod(3600)
        .resourceChain(false)
        .addResolver(new IndexFallbackResourceResolver());
  }

  @Bean
  public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
  }

  @Bean
  public DhisApiVersionHandlerMethodArgumentResolver dhisApiVersionHandlerMethodArgumentResolver() {
    return new DhisApiVersionHandlerMethodArgumentResolver();
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(dhisApiVersionHandlerMethodArgumentResolver());
    resolvers.add(currentUserHandlerMethodArgumentResolver);
    resolvers.add(currentSystemSettingsHandlerMethodArgumentResolver);
  }

  @Bean
  public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
    DefaultMethodSecurityExpressionHandler expressionHandler =
        new DefaultMethodSecurityExpressionHandler();
    expressionHandler.setDefaultRolePrefix("");
    return expressionHandler;
  }

  @Bean
  public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
    return new MappingJackson2HttpMessageConverter(jsonMapper);
  }

  @Bean
  public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter() {
    XmlPathMappingJackson2XmlHttpMessageConverter messageConverter =
        new XmlPathMappingJackson2XmlHttpMessageConverter(xmlMapper);

    messageConverter.setSupportedMediaTypes(
        Arrays.asList(
            new MediaType("application", "xml", StandardCharsets.UTF_8),
            new MediaType("text", "xml", StandardCharsets.UTF_8)));

    return messageConverter;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    Arrays.stream(Compression.values())
        .forEach(compression -> converters.add(new JsonMessageConverter(nodeService, compression)));
    Arrays.stream(Compression.values())
        .forEach(compression -> converters.add(new XmlMessageConverter(nodeService, compression)));

    Arrays.stream(Compression.values())
        .forEach(
            compression ->
                converters.add(
                    new MetadataExportParamsMessageConverter(metadataExportService, compression)));

    Arrays.stream(Compression.values())
        .forEach(
            compression ->
                converters.add(
                    new StreamingJsonRootMessageConverter(fieldFilterService, compression)));
    converters.add(new FilteredPageHttpMessageConverter(jsonFilterMapper));

    converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new FormHttpMessageConverter());

    converters.add(mappingJackson2HttpMessageConverter());
    converters.add(mappingJackson2XmlHttpMessageConverter());
    converters.add(new ResourceHttpMessageConverter());
  }

  @Override
  protected void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new FieldPathConverter());
    registry.addConverter(new FieldsConverter());
  }

  @Primary
  @Bean
  @Override
  public ContentNegotiationManager mvcContentNegotiationManager() {
    CustomPathExtensionContentNegotiationStrategy pathExtensionNegotiationStrategy =
        new CustomPathExtensionContentNegotiationStrategy(mediaTypeMap);
    pathExtensionNegotiationStrategy.setUseRegisteredExtensionsOnly(true);

    return new ContentNegotiationManager(
        Arrays.asList(
            pathExtensionNegotiationStrategy,
            new HeaderContentNegotiationStrategy(),
            new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)));
  }

  @Override
  protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
    CustomRequestMappingHandlerMapping mapping = new CustomRequestMappingHandlerMapping();
    mapping.setOrder(0);
    mapping.setContentNegotiationManager(mvcContentNegotiationManager());
    mapping.setUseTrailingSlashMatch(true);
    mapping.setUseSuffixPatternMatch(true);
    mapping.setUseRegisteredSuffixPatternMatch(true);
    return mapping;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new UserContextInterceptor());
    registry.addInterceptor(new RequestInfoInterceptor(requestInfoService));
    registry.addInterceptor(authorityInterceptor);
    registry.addInterceptor(settingsInterceptor);
    registry.addInterceptor(new TrailingSlashInterceptor()).excludePathPatterns("/api/**");
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer config) {
    config
        .favorPathExtension(true)
        .favorParameter(false)
        .ignoreAcceptHeader(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("json", MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML);
  }

  private Map<String, MediaType> mediaTypeMap =
      new ImmutableMap.Builder<String, MediaType>()
          .put("json", MediaType.APPLICATION_JSON)
          .put("json.gz", parseMediaType("application/json+gzip"))
          .put("json.zip", parseMediaType("application/json+zip"))
          .put("jsonp", parseMediaType("application/javascript"))
          .put("xml", MediaType.APPLICATION_XML)
          .put("xml.gz", parseMediaType("application/xml+gzip"))
          .put("xml.zip", parseMediaType("application/xml+zip"))
          .put("png", MediaType.IMAGE_PNG)
          .put("pdf", MediaType.APPLICATION_PDF)
          .put("xls", parseMediaType("application/vnd.ms-excel"))
          .put("xlsx", parseMediaType("application/vnd.ms-excel"))
          .put("csv", parseMediaType("text/csv"))
          .put("csv.gz", parseMediaType("application/csv+gzip"))
          .put("csv.zip", parseMediaType("application/csv+zip"))
          .put("adx.xml", parseMediaType("application/adx+xml"))
          .put("adx.xml.gz", parseMediaType("application/adx+xml+gzip"))
          .put("adx.xml.zip", parseMediaType("application/adx+xml+zip"))
          .put("geojson", parseMediaType("application/json+geojson"))
          .build();
}
