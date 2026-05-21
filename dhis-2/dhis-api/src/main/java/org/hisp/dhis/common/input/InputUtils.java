/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.common.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.jsontree.JsonAccess;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.Jurl;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.Validation;
import org.hisp.dhis.period.Period;

/**
 * Utilities around generic input decoding and formal (static context) validation.
 *
 * @author Jan Bernitt
 * @since 2.44
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputUtils {

  static {
    JsonAccess global = JsonAccess.GLOBAL;
    global.addStringAs(UID.class, UID::of);
    global.addStringAs(Period.class, Period::of);
  }

  /**
   * Formal input validation against a schema {@link Class} which has {@link
   * org.hisp.dhis.jsontree.Validation} annotations for restrictions.
   *
   * <p>Formal input validation is all the validation that can be performed solely based on a target
   * schema. In other words, it is a validation of input within the static (type) context without
   * checking validity in the context the value will exist. Such semantic validation is performed in
   * later stages.
   *
   * @param schema the target type the input should conform to
   * @param input typically user input such as request bodies
   * @throws BadRequestException in case the input is not valid
   */
  public static void validateInput(@Nonnull Class<?> schema, @Nonnull JsonObject input)
      throws BadRequestException {
    Validation.Result result = input.validate(schema, Validation.Mode.PROBE);
    if (!result.errors().isEmpty()) {
      Validation.Error e0 = result.errors().get(0);
      throw new BadRequestException(
          "URL parameter `"
              + e0.path().segment()
              + "` "
              + e0.template().formatted(e0.args().toArray()));
    }
  }

  @Nonnull
  public static <T extends Record> T decodeInput(
      @Nonnull Class<T> schema, @Nonnull Map<String, Object> properties) {
    return decodeInput(
            schema,
            name -> {
              Object value = properties.get(name);
              return value == null ? null : new String[] {value.toString()};
            })
        .to(schema);
  }

  @Nonnull
  public static <T extends Record> T decodeInput(
      @Nonnull Class<T> schema, @Nonnull String properties) {
    Map<String, List<String>> map = new HashMap<>();
    for (String p : properties.split("&")) {
      int eqIndex = p.indexOf('=');
      String name = p.substring(0, eqIndex);
      String value = p.substring(eqIndex + 1);
      map.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
    }
    return decodeInput(
            schema,
            name -> {
              List<String> values = map.get(name);
              return values == null ? null : values.toArray(String[]::new);
            })
        .to(schema);
  }

  /**
   * Decodes key-value input such as request parameters into a JSON value based on the target
   * schema.
   *
   * @param schema the target the key-value data should conform to
   * @param propertyLookup a lookup function to return the values for a given key
   * @return a JSON object with the key-value data found in the given schema as provided by the
   *     values lookup-function
   */
  @Nonnull
  public static JsonObject decodeInput(
      @Nonnull Class<? extends Record> schema, @Nonnull Function<String, String[]> propertyLookup) {
    List<JsonObject.Property> properties = JsonObject.properties(schema);
    JsonNode object =
        JsonBuilder.createObject(
            obj -> {
              for (JsonObject.Property p : properties) {
                Text name = p.jsonName();
                String key = name.toString();
                String[] values = propertyLookup.apply(key);
                if (values == null) continue;
                Set<Validation.NodeType> types = p.types();
                if (types.isEmpty()) {
                  // default behaviour
                  addAutoComplex(name, values, obj);
                } else if (values.length == 0) {
                  if (types.contains(Validation.NodeType.BOOLEAN)) {
                    obj.addBoolean(name, true);
                  } else if (types.contains(Validation.NodeType.ARRAY)) {
                    obj.addArray(name, arr -> {});
                  }
                } else {
                  Validation.NodeType type = types.iterator().next();
                  if (types.contains(Validation.NodeType.ARRAY)) type = Validation.NodeType.ARRAY;
                  if (types.size() == 1) {
                    switch (type) {
                      case INTEGER, NUMBER, BOOLEAN, NULL ->
                          obj.addMember(name, JsonNode.of(values[0]));
                      case STRING ->
                          obj.addString(
                              name, values.length == 1 ? values[0] : String.join(",", values));
                      case ARRAY, OBJECT -> addAutoComplex(name, values, obj);
                    }
                  }
                }
              }
            });
    return JsonMixed.of(object);
  }

  private static void addAutoComplex(
      Text name, String[] values, JsonBuilder.JsonObjectBuilder obj) {
    if (values.length == 1) {
      // assume JURL
      if (values[0].startsWith("(")) {
        obj.addMember(name, Jurl.of(values[0]).node());
      } else {
        obj.addString(name, values[0]);
      }
    } else {
      obj.addArray(
          name,
          arr -> {
            for (String v : values) arr.addString(v);
          });
    }
  }
}
