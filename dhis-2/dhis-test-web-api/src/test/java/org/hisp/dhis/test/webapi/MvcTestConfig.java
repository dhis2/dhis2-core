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
package org.hisp.dhis.test.webapi;

import static org.hisp.dhis.webapi.security.config.WebMvcConfig.MEDIA_TYPE_MAP;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.DefaultRequestInfoService;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPathConverter;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.test.message.DefaultFakeMessageSender;
import org.hisp.dhis.webapi.fields.FieldsConverter;
import org.hisp.dhis.webapi.mvc.CurrentSystemSettingsHandlerMethodArgumentResolver;
import org.hisp.dhis.webapi.mvc.CurrentUserHandlerMethodArgumentResolver;
import org.hisp.dhis.webapi.mvc.CustomRequestMappingHandlerMapping;
import org.hisp.dhis.webapi.mvc.interceptor.AuthorityInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.RequestInfoInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.SystemSettingsInterceptor;
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
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@EnableWebMvc
@EnableMethodSecurity
public class MvcTestConfig implements WebMvcConfigurer {
  @Autowired public DefaultRequestInfoService requestInfoService;

  @Autowired private MetadataExportService metadataExportService;

  @Autowired private AuthorityInterceptor authorityInterceptor;

  @Autowired private SystemSettingsInterceptor settingsInterceptor;

  @Autowired private NodeService nodeService;

  @Autowired
  private CurrentUserHandlerMethodArgumentResolver currentUserHandlerMethodArgumentResolver;

  @Autowired
  private CurrentSystemSettingsHandlerMethodArgumentResolver
      currentSystemSettingsHandlerMethodArgumentResolver;

  @Autowired private FieldsConverter fieldsConverter;

  @Autowired
  @Qualifier("jsonMapper")
  private ObjectMapper jsonMapper;

  @Qualifier("jsonFilterMapper")
  @Autowired
  private ObjectMapper jsonFilterMapper;

  @Autowired
  @Qualifier("xmlMapper")
  private ObjectMapper xmlMapper;

  @Autowired private FieldFilterService fieldFilterService;

  @Autowired private FormattingConversionService mvcConversionService;

  @Autowired private ResourceUrlProvider mvcResourceUrlProvider;

  private static MessageSender messageSender = new DefaultFakeMessageSender();

  @Bean
  @Primary
  public CustomRequestMappingHandlerMapping requestMappingHandlerMapping(
      FormattingConversionService mvcConversionService,
      ResourceUrlProvider mvcResourceUrlProvider) {
    CustomRequestMappingHandlerMapping mapping = new CustomRequestMappingHandlerMapping();
    mapping.setOrder(0);
    TestInterceptorRegistry registry = new TestInterceptorRegistry();
    addInterceptors(registry);
    registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));
    registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));
    registry.addInterceptor(new UserContextInterceptor());
    registry.addInterceptor(new RequestInfoInterceptor(requestInfoService));
    registry.addInterceptor(authorityInterceptor);
    registry.addInterceptor(settingsInterceptor);
    mapping.setInterceptors(registry.getInterceptors().toArray());

    CustomPathExtensionContentNegotiationStrategy pathExtensionNegotiationStrategy =
        new CustomPathExtensionContentNegotiationStrategy(MEDIA_TYPE_MAP);
    pathExtensionNegotiationStrategy.setUseRegisteredExtensionsOnly(true);

    mapping.setContentNegotiationManager(
        new ContentNegotiationManager(
            Arrays.asList(
                pathExtensionNegotiationStrategy,
                new HeaderContentNegotiationStrategy(),
                new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON))));

    mapping.setUseTrailingSlashMatch(true);
    mapping.setUseSuffixPatternMatch(true);
    mapping.setUseRegisteredSuffixPatternMatch(true);

    return mapping;
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    CustomPathExtensionContentNegotiationStrategy pathExtensionNegotiationStrategy =
        new CustomPathExtensionContentNegotiationStrategy(MEDIA_TYPE_MAP);

    configurer.strategies(
        Arrays.asList(
            pathExtensionNegotiationStrategy,
            new HeaderContentNegotiationStrategy(),
            new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)));
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.setUseSuffixPatternMatch(false);
    configurer.setUseRegisteredSuffixPatternMatch(true);
  }

  @Bean
  public DatabaseInfoProvider databaseInfoProvider() {
    return () -> DatabaseInfo.builder().build();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));
    registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));

    registry.addInterceptor(new UserContextInterceptor());
    registry.addInterceptor(new RequestInfoInterceptor(requestInfoService));
    registry.addInterceptor(authorityInterceptor);
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
            new MediaType("application", "*+xml", StandardCharsets.UTF_8)));

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

    converters.add(new StringHttpMessageConverter());
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new FormHttpMessageConverter());

    converters.add(mappingJackson2HttpMessageConverter());
    converters.add(mappingJackson2XmlHttpMessageConverter());
    converters.add(new ResourceHttpMessageConverter());
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new FieldPathConverter());
    registry.addConverter(fieldsConverter);
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
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

  @Bean("smsMessageSender")
  public MessageSender smsMessageSender() {
    return messageSender;
  }

  @Bean("emailMessageSender")
  public MessageSender emailMessageSender() {
    return messageSender;
  }

  static final class TestInterceptorRegistry extends InterceptorRegistry {
    @Override
    public List<Object> getInterceptors() {
      return super.getInterceptors();
    }
  }
}
