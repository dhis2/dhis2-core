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
package org.hisp.dhis.fieldfiltering.better;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fieldfiltering.FieldPathTransformer;
import org.hisp.dhis.fieldfiltering.FieldTransformer;
import org.hisp.dhis.fieldfiltering.better.Fields.Transformation;
import org.hisp.dhis.fieldfiltering.transformers.IsEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.RenameFieldTransformer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FieldsPropertyFilter extends SimpleBeanPropertyFilter {

  public static final String FILTER_ID = "better-fields-filter";

  /** Key under which fields are stored for filtering during serialization. */
  public static final String FIELDS_ATTRIBUTE = "fields";

  private final ObjectMapper objectMapper;

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    Fields current = (Fields) provider.getAttribute(FIELDS_ATTRIBUTE);

    if (current == null) {
      throw new IllegalStateException(
          "No fields attribute found in SerializerProvider there must be a bug in field filtering.");
    }

    if (current.test(writer.getName())) {
      // Standard field serialization
      Fields children = current.getChildren(writer.getName());
      provider.setAttribute(FIELDS_ATTRIBUTE, children);

      List<Fields.Transformation> transformations = current.getTransformations(writer.getName());
      if (transformations.isEmpty()) {
        writer.serializeAsField(pojo, jgen, provider);
      } else {
        serializeUsingTransformations(pojo, jgen, provider, writer, transformations);
      }

      provider.setAttribute(FIELDS_ATTRIBUTE, current);
    } else if (!jgen.canOmitFields()) { // since 2.3
      writer.serializeAsOmittedField(pojo, jgen, provider);
    }
  }

  // TODO(ivo) docs
  // Apply transformations using existing FieldTransformer instances
  // Use hybrid approach: JsonNode transformation for compatibility
  // Only pay the double serialization cost when transformations are used
  private void serializeUsingTransformations(
      Object pojo,
      JsonGenerator jgen,
      SerializerProvider provider,
      PropertyWriter writer,
      List<Fields.Transformation> transformations)
      throws Exception {
    // Create an ObjectNode with only the field and its value for the transformers to mutate
    Object fieldValue = extractFieldValue(pojo, writer);
    JsonNode fieldNode = objectMapper.valueToTree(fieldValue);
    ObjectNode result = objectMapper.createObjectNode();
    result.set(writer.getName(), fieldNode);

    for (Fields.Transformation transformation : transformations) {
      try {
        FieldTransformer transformer = createFieldTransformer(transformation);
        transformer.apply(writer.getName(), result.get(writer.getName()), result);
      } catch (Exception e) {
        // TODO(ivo) continue with last valid value or throw?
        break;
      }
    }

    jgen.writeFieldName(result.fieldNames().next());
    jgen.writeTree(result.values().next());
  }

  // TODO(ivo) error handling
  private Object extractFieldValue(Object pojo, PropertyWriter writer) {
    if (writer instanceof BeanPropertyWriter beanWriter) {
      try {
        return beanWriter.get(pojo);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Creates a FieldTransformer instance from a Fields.Transformation. Uses existing transformer
   * implementations for compatibility.
   */
  private FieldTransformer createFieldTransformer(Transformation transformation) {
    return switch (transformation.getName()) {
      case "isEmpty" -> IsEmptyFieldTransformer.INSTANCE;
      case "rename" -> {
        List<String> parameters = List.of(transformation.getArguments());
        FieldPathTransformer fieldPathTransformer =
            new FieldPathTransformer(transformation.getName(), parameters);
        yield new RenameFieldTransformer(fieldPathTransformer);
      }
      default -> null;
    };
  }
}
