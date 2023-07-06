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
package org.hisp.dhis.commons.jackson.config;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Date;
import org.hibernate.SessionFactory;
import org.hisp.dhis.commons.jackson.config.geometry.JtsXmlModule;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Main Jackson Mapper configuration. Any component that requires JSON/XML serialization should use
 * the Jackson mappers configured in this class.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Configuration
public class JacksonObjectMapperConfig {
  /*
   * Standard JSON mapper.
   */
  public static final ObjectMapper jsonMapper = configureMapper(new ObjectMapper());

  /*
   * Standard mapper that have {@link Hibernate5Module} registered.
   */
  public static final ObjectMapper hibernateAwareJsonMapper = configureMapper(new ObjectMapper());

  /*
   * Standard JSON mapper for Program Stage Instance data values.
   */
  public static final ObjectMapper dataValueJsonMapper = configureMapper(new ObjectMapper(), true);

  /*
   * Standard XML mapper.
   */
  public static final ObjectMapper xmlMapper = configureMapper(new XmlMapper());

  /** Standard CSV mapper. */
  public static final CsvMapper csvMapper = configureCsvMapper(new CsvMapper());

  @Primary
  @Bean("jsonMapper")
  public ObjectMapper jsonMapper() {
    return jsonMapper;
  }

  @Bean("hibernateAwareJsonMapper")
  public ObjectMapper hibernateAwareJsonMapper(SessionFactory sessionFactory) {
    Hibernate5Module hibernate5Module = new Hibernate5Module(sessionFactory);
    hibernate5Module.enable(
        Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
    hibernateAwareJsonMapper.registerModule(hibernate5Module);
    return hibernateAwareJsonMapper;
  }

  @Bean("dataValueJsonMapper")
  public ObjectMapper dataValueJsonMapper() {
    return dataValueJsonMapper;
  }

  @Bean("xmlMapper")
  public ObjectMapper xmlMapper() {
    return xmlMapper;
  }

  public static ObjectMapper staticJsonMapper() {
    return jsonMapper;
  }

  public static ObjectMapper staticXmlMapper() {
    return xmlMapper;
  }

  static {
    JtsModule jtsModule = new JtsModule(new GeometryFactory(new PrecisionModel(), 4326));
    jsonMapper.registerModule(jtsModule);
    dataValueJsonMapper.registerModule(jtsModule);
    xmlMapper.registerModule(new JtsXmlModule());
  }

  private static ObjectMapper configureMapper(ObjectMapper objectMapper) {
    return configureMapper(objectMapper, false);
  }

  /**
   * Shared configuration for all Jackson mappers
   *
   * @param objectMapper an {@see ObjectMapper}
   * @param autoDetectGetters if true, enable `autoDetectGetters`
   * @return a configured {@see ObjectMapper}
   */
  private static ObjectMapper configureMapper(
      ObjectMapper objectMapper, boolean autoDetectGetters) {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new EmptyStringToNullStdDeserializer());
    module.addDeserializer(Date.class, new ParseDateStdDeserializer());
    module.addDeserializer(JsonPointer.class, new JsonPointerStdDeserializer());
    module.addSerializer(Date.class, new WriteDateStdSerializer());
    module.addSerializer(JsonPointer.class, new JsonPointerStdSerializer());

    // Registering a custom Instant serializer/deserializer for DTOs using
    // Instant
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(Instant.class, new WriteInstantStdSerializer());
    javaTimeModule.addDeserializer(Instant.class, new ParseInstantStdDeserializer());

    objectMapper.registerModules(module, javaTimeModule, new Jdk8Module());

    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.enable(SerializationFeature.WRAP_EXCEPTIONS);

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
    objectMapper.enable(DeserializationFeature.WRAP_EXCEPTIONS);

    objectMapper.disable(MapperFeature.AUTO_DETECT_FIELDS);
    objectMapper.disable(MapperFeature.AUTO_DETECT_CREATORS);

    if (!autoDetectGetters) {
      objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
    }

    objectMapper.disable(MapperFeature.AUTO_DETECT_SETTERS);
    objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);

    return objectMapper;
  }

  /**
   * Configures the shared CSV mapper.
   *
   * @param mapper the {@link CsvMapper}.
   * @return the {@link CsvMapper}.
   */
  private static CsvMapper configureCsvMapper(CsvMapper mapper) {
    mapper.disable(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS);
    return mapper;
  }
}
