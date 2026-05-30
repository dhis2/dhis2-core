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
package org.hisp.dhis.hibernate.jsonb.type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hisp.dhis.eventdatavalue.EventDataValue;

/**
 * @author David Katuscak
 */
public class JsonEventDataValueSetBinaryType extends JsonBinaryType {
  private static final ObjectWriter EVENT_DATA_VALUE_WRITER =
      MAPPER.writerFor(EventDataValue.class);

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

  @SuppressWarnings("unchecked")
  @Override
  public Object deepCopy(Object value) throws HibernateException {
    if (value == null) {
      return null;
    }

    Set<EventDataValue> original = (Set<EventDataValue>) value;
    Set<EventDataValue> copy = new HashSet<>(original.size());
    for (EventDataValue edv : original) {
      copy.add(new EventDataValue(edv));
    }
    return copy;
  }

  /**
   * Serializes a {@code Set<EventDataValue>} to JSON, writing the Map-shaped structure ({@code
   * {"dataElementUid": {...}, ...}}) directly from the Set without allocating an intermediate Map.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected String convertObjectToJson(Object object) {
    try {
      Set<EventDataValue> eventDataValues =
          object == null ? Set.of() : (Set<EventDataValue>) object;

      StringWriter sw = new StringWriter();
      try (JsonGenerator gen = MAPPER.getFactory().createGenerator(sw)) {
        gen.writeStartObject();
        for (EventDataValue edv : eventDataValues) {
          gen.writeFieldName(edv.getDataElement());
          EVENT_DATA_VALUE_WRITER.writeValue(gen, edv);
        }
        gen.writeEndObject();
      }
      return sw.toString();
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
