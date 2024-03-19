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

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hisp.dhis.eventdatavalue.EventDataValue;

/**
 * @author David Katuscak
 */
public class JsonEventDataValueSetBinaryType extends JsonBinaryType {
  public JsonEventDataValueSetBinaryType() {
    super();
    writer = MAPPER.writerFor(new TypeReference<Map<String, EventDataValue>>() {});
    reader = MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});
    returnedClass = EventDataValue.class;
  }

  @Override
  protected void init(Class<?> klass) {
    returnedClass = klass;
    reader = MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});
    writer = MAPPER.writerFor(new TypeReference<Map<String, EventDataValue>>() {});
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    String json = convertObjectToJson(value);
    return convertJsonToObject(json);
  }

  /**
   * Serializes an object to JSON.
   *
   * @param object the object to convert.
   * @return JSON content.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected String convertObjectToJson(Object object) {
    try {
      Set<EventDataValue> eventDataValues =
          object == null ? Collections.emptySet() : (Set<EventDataValue>) object;

      Map<String, EventDataValue> tempMap = new HashMap<>();

      for (EventDataValue eventDataValue : eventDataValues) {
        tempMap.put(eventDataValue.getDataElement(), eventDataValue);
      }

      return writer.writeValueAsString(tempMap);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Deserializes JSON content to an object.
   *
   * @param content the JSON content.
   * @return an object.
   */
  @Override
  public Object convertJsonToObject(String content) {
    try {
      Map<String, EventDataValue> data = reader.readValue(content);

      return convertEventDataValuesMapIntoSet(data);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static Set<EventDataValue> convertEventDataValuesMapIntoSet(
      Map<String, EventDataValue> data) {

    Set<EventDataValue> eventDataValues = new HashSet<>();

    for (Map.Entry<String, EventDataValue> entry : data.entrySet()) {

      EventDataValue eventDataValue = entry.getValue();
      eventDataValue.setDataElement(entry.getKey());
      eventDataValues.add(eventDataValue);
    }

    return eventDataValues;
  }
}
