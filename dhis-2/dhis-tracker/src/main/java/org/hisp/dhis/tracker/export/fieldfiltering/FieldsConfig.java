/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.fieldfiltering;

import static org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig.configureMapper;
import static org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig.createJtsModule;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FieldsConfig {

  @Bean
  public ObjectMapper jsonFilterMapper(FieldsPropertyFilter fieldsPropertyFilter) {
    // reuse the same configuration as the primary ObjectMapper bean adding field filter on top of
    // it
    ObjectMapper mapper = configureMapper(new ObjectMapper());
    mapper.registerModule(createJtsModule());

    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(Object.class, FieldFilterMixin.class);
    mapper.registerModule(module);

    mapper.setAnnotationIntrospector(new IgnoreJsonSerializerRefinementAnnotationInspector());

    // pre-configure the filter as its stateless. This means every ObjectWriter will already have
    // the filter setup.
    SimpleFilterProvider filterProvider =
        new SimpleFilterProvider().addFilter(FieldsPropertyFilter.FILTER_ID, fieldsPropertyFilter);
    mapper.setFilterProvider(filterProvider);

    return mapper;
  }

  // this is copied from the current FieldFilterService#configureFieldFilterObjectMapper
  // so this package does not depend on the fieldfiltering package
  static class IgnoreJsonSerializerRefinementAnnotationInspector
      extends JacksonAnnotationIntrospector {
    /**
     * Since the field filter will handle type refinement itself (to avoid recursive loops), we want
     * to ignore any type refinement happening with @JsonSerialize(...). In the future we would want
     * to remove all @JsonSerialize annotations and just use the field filters, but since we still
     * have object mappers without field filtering we can't do this just yet.
     */
    @Override
    public JavaType refineSerializationType(
        MapperConfig<?> config, Annotated a, JavaType baseType) {
      return baseType;
    }
  }

  // This is used to avoid having to annotate every object with the filter annotation.
  @JsonFilter(FieldsPropertyFilter.FILTER_ID)
  interface FieldFilterMixin {}
}
