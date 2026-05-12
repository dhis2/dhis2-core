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
package org.hisp.dhis.common.validation;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.Validation;
import org.springframework.stereotype.Service;

@Service
public class DefaultInputValidationService implements InputValidationService {

  @Override
  @NonTransactional
  public JsonObject decode(
      Class<? extends Record> schema, Function<String, String[]> propertyLookup) {
    List<JsonObject.Property> properties = JsonObject.collapsedProperties(schema);
    JsonNode object =
        JsonBuilder.createObject(
            obj -> {
              for (JsonObject.Property p : properties) {
                Text name = p.jsonName();
                String key = name.toString();
                String[] values = propertyLookup.apply(key);
                if (values == null) continue;
                Set<Validation.NodeType> types = p.types();
                if (values.length == 0) {
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
                      case STRING, OBJECT ->
                          obj.addString(
                              name, values.length == 1 ? values[0] : String.join(",", values));
                      case ARRAY ->
                          obj.addArray(
                              name,
                              arr -> {
                                for (String v : values) arr.addString(v);
                              });
                    }
                  }
                }
              }
            });
    return JsonMixed.of(object);
  }

  @Override
  @NonTransactional
  public void validate(Class<?> schema, JsonObject input) throws BadRequestException {
    Validation.Result result = input.validate(schema, Validation.Mode.PROBE);
    if (!result.errors().isEmpty()) {
      // TODO do we want to have different error codes per validation Rule?
      Validation.Error e0 = result.errors().get(0);
      throw new BadRequestException(
          "URL parameter `"
              + e0.path().segment()
              + "` "
              + e0.template().formatted(e0.args().toArray()));
    }
  }
}
