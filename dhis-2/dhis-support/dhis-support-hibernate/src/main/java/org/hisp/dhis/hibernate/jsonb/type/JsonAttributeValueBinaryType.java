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
package org.hisp.dhis.hibernate.jsonb.type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;

public class JsonAttributeValueBinaryType extends JsonBinaryType {
  public static final ObjectMapper MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  @Override
  protected JavaType getResultingJavaType(Class<?> returnedClass) {
    return MAPPER.getTypeFactory().constructMapLikeType(Map.class, String.class, returnedClass);
  }

  @Override
  @SuppressWarnings("unchecked")
  public String convertObjectToJson(Object object) {
    try {
      Set<AttributeValue> attributeValues =
          object == null ? Collections.emptySet() : (Set<AttributeValue>) object;

      Map<String, AttributeValue> attrValueMap = new HashMap<>();

      for (AttributeValue attributeValue : attributeValues) {
        if (attributeValue.getAttribute() != null) {
          attributeValue.setAttribute(new Attribute(attributeValue.getAttribute().getUid()));
          attrValueMap.put(attributeValue.getAttribute().getUid(), attributeValue);
        }
      }

      return writer.writeValueAsString(attrValueMap);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    String json = convertObjectToJson(value);
    return convertJsonToObject(json);
  }

  @Override
  public Object convertJsonToObject(String content) {
    try {
      Map<String, AttributeValue> data = reader.readValue(content);

      return convertAttributeValueMapIntoSet(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Set<AttributeValue> convertAttributeValueMapIntoSet(
      Map<String, AttributeValue> data) {
    Set<AttributeValue> attributeValues = new HashSet<>();

    for (Map.Entry<String, AttributeValue> entry : data.entrySet()) {
      AttributeValue attributeValue = entry.getValue();
      attributeValues.add(attributeValue);
    }

    return attributeValues;
  }
}
