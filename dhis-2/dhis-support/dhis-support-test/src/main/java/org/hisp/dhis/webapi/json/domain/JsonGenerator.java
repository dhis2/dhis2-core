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
package org.hisp.dhis.webapi.json.domain;

import static java.util.Collections.emptyMap;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;

/**
 * Generates JSON object declarations as {@link String} based on {@link JsonSchema}.
 *
 * @author Jan Bernitt
 */
public class JsonGenerator {
  private final Map<String, JsonSchema> schemasByEndpoint = new HashMap<>();

  private final Map<String, JsonSchema> schemasByKlass = new HashMap<>();

  public JsonGenerator(JsonList<JsonSchema> schemas) {
    for (JsonSchema s : schemas) {
      String endpoint = s.getRelativeApiEndpoint();
      if (endpoint != null) {
        schemasByEndpoint.put(endpoint, s);
      }
      // avoid triggering class loading by using getString
      schemasByKlass.put(s.getString("klass").string(), s);
    }
  }

  /**
   * Generates a mapping between a relative API endpoint path (the path used with POST to create
   * objects) and a JSON payload to POST to the endpoint to create the generated object.
   *
   * <p>When objects have mandatory references to other objects these are also contained in the
   * returned map.
   *
   * <p>The iteration order of the map entries is such that the first entry has no dependencies, the
   * second might depend on the first, the thrid on the first and second and so forth until finally
   * the last entry is the entry for the given {@link JsonSchema} that we actually want to create in
   * the first place.
   *
   * @param schema a DHIS2 schema definition as returned by the server
   * @return a ordered mapping for relative API endpoint path to JSON body for that endpoint (POST
   *     in iteration order)
   */
  public Map<String, String> generateObjects(JsonSchema schema) {
    Map<String, String> objects = new LinkedHashMap<>();
    addObject(schema, false, objects);
    return objects;
  }

  private String addObject(JsonSchema schema, boolean addId, Map<String, String> objects) {
    String object = createObject(schema, addId, objects);
    objects.put(schema.getRelativeApiEndpoint(), object);
    return object;
  }

  private String createObject(JsonSchema schema, boolean addId, Map<String, String> objects) {
    StringBuilder json = new StringBuilder();
    json.append('{');
    int i = 0;
    if (addId) {
      json.append('"')
          .append("id")
          .append('"')
          .append(':')
          .append('"')
          .append(CodeGenerator.generateUid())
          .append('"');
      i++;
    }
    for (JsonProperty property : schema.getRequiredProperties()) {
      if (property.getPropertyType() == PropertyType.REFERENCE
          && property.getRelativeApiEndpoint() == null) {
        continue;
      }
      if (i > 0) {
        json.append(',');
      }
      json.append('"').append(property.getName()).append('"').append(':');
      appendValue(json, property, objects);
      i++;
    }
    json.append('}');
    return json.toString();
  }

  private void appendValue(StringBuilder json, JsonProperty property, Map<String, String> objects) {
    switch (property.getPropertyType()) {
      case COMPLEX:
        JsonSchema schema = schemasByKlass.get(property.getString("klass").string());
        if (schema == null) {
          json.append("null");
        } else {
          json.append(createObject(schema, false, emptyMap()));
        }
        break;
      case TEXT:
        json.append('"').append(generateString(property)).append('"');
        break;
      case DATE:
        json.append('"').append(generateDateString()).append('"');
        break;
      case CONSTANT:
        json.append('"').append(property.getConstants().get(0)).append('"');
        break;
      case BOOLEAN:
        json.append("true");
        break;
      case INTEGER:
      case NUMBER:
        json.append(getSmallestPositiveValue(property));
        break;
      case IDENTIFIER:
        json.append('"').append(generateId(property)).append('"');
        break;
      case REFERENCE:
        String object = objects.get(property.getRelativeApiEndpoint());
        if (object == null) {
          schema = schemasByEndpoint.get(property.getRelativeApiEndpoint());
          object = addObject(schema, true, objects);
        }
        if (object.isEmpty()) {
          // we are already trying to compute an object of this type
          json.append("null");
          return;
        }
        int idStart = object.indexOf("\"id\":") + 6;
        int idEnd = object.indexOf('"', idStart);
        json.append("{\"id\":\"").append(object, idStart, idEnd).append("\"}");
        break;
      default:
        throw new IllegalArgumentException(property.getName() + " " + property.getPropertyType());
    }
  }

  private static String generateId(JsonProperty property) {
    switch (property.getName()) {
      case "id":
      case "uid":
      case "code":
      case "cid":
        return CodeGenerator.generateUid();
      default:
        throw new UnsupportedOperationException("id type not supported: " + property.getName());
    }
  }

  private static String generateDateString() {
    return DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDateTime.now());
  }

  private static String generateString(JsonProperty property) {
    switch (property.getName()) {
      case "url":
        return "http://example.com";
      case "cronExpression":
        return "* * * * * *";
      case "periodType":
        return PeriodType.PERIOD_TYPES.get(0).getName();

      case "name":
      case "shortName":
        // there are often unique constrains on TEXT attributes called name,
        // so...
        return getUniqueString(property);
      default:
        return getRandomString(property);
    }
  }

  private static String getRandomString(JsonProperty property) {
    int length = getSmallestPositiveValue(property);
    StringBuilder str = new StringBuilder(length);
    str.append(property.getName());
    char c = 'a';
    while (str.length() < length) {
      str.append(c++);
    }
    return str.toString();
  }

  private static int getSmallestPositiveValue(JsonProperty property) {
    int min = property.getMin().intValue();
    int max = property.getMax().intValue();
    return Math.min(Math.max(1, min), Math.max(1, max));
  }

  private static String getUniqueString(JsonProperty property) {
    int min = property.getMin().intValue();
    int max = property.getMax().intValue();
    return min < 11 && (max < 0 || max >= 11)
        ? CodeGenerator.generateUid()
        : CodeGenerator.generateCode(max);
  }
}
