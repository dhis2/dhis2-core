/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.hibernate.jsonb.type;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Date;
import java.util.Map;
import javax.persistence.AttributeConverter;
import org.hisp.dhis.commons.jackson.config.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.commons.jackson.config.ParseDateStdDeserializer;
import org.hisp.dhis.commons.jackson.config.WriteDateStdSerializer;

public class JsonBinaryTypeConverter implements AttributeConverter<Object, Object> {
  public static final ObjectMapper MAPPER = new ObjectMapper();

  public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE_REFERENCE =
      new TypeReference<>() {};

  static {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new EmptyStringToNullStdDeserializer());
    module.addDeserializer(Date.class, new ParseDateStdDeserializer());
    module.addSerializer(Date.class, new WriteDateStdSerializer());

    MAPPER.registerModules(module, new JtsModule(), new JavaTimeModule(), new Jdk8Module());
    MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  ObjectWriter writer;

  ObjectReader reader;

  Class<?> returnedClass;

  ObjectMapper resultingMapper;

  JavaType resultingJavaType;

  @Override
  public Object convertToDatabaseColumn(Object attribute) {
    return null;
  }

  @Override
  public Object convertToEntityAttribute(Object dbData) {
    return null;
  }

  public void setReturnedClass(Class<?> returnedClass) {
    this.returnedClass = returnedClass;
  }

  public Class<?> getReturnedClass() {
    return returnedClass;
  }
}
