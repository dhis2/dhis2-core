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
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fieldfiltering.FieldPathTransformer;
import org.hisp.dhis.fieldfiltering.FieldTransformer;
import org.hisp.dhis.fieldfiltering.better.Fields.Transformation;
import org.hisp.dhis.fieldfiltering.transformers.IsEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.IsNotEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.RenameFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.SizeFieldTransformer;
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
      Fields children = current.getChildren(writer.getName());
      provider.setAttribute(FIELDS_ATTRIBUTE, children);

      serialize(pojo, jgen, provider, writer, current);

      provider.setAttribute(FIELDS_ATTRIBUTE, current);
    } else if (!jgen.canOmitFields()) { // since 2.3
      writer.serializeAsOmittedField(pojo, jgen, provider);
    }
  }

  /**
   * Serializes a field with optional transformations. Uses hybrid approach: normal serialization
   * for no transformations, TokenBuffer capture + transformation for fields with transformations.
   */
  private void serialize(
      Object pojo,
      JsonGenerator jgen,
      SerializerProvider provider,
      PropertyWriter writer,
      Fields current)
      throws Exception {
    List<Fields.Transformation> transformations = current.getTransformations(writer.getName());
    if (transformations.isEmpty()) {
      writer.serializeAsField(pojo, jgen, provider);
    } else {
      // serialize applying any filters and capture output in buffer for later transformations
      TokenBuffer tokenBuffer = new TokenBuffer(objectMapper, false);
      writer.serializeAsField(pojo, tokenBuffer, provider);
      // create ObjectNode from buffer for transformations to mutate
      JsonNode filteredFieldNode = objectMapper.readTree(tokenBuffer.asParser());
      // transformations are applied on single-field object like {"fieldName": filteredValue}
      String fieldName = filteredFieldNode.fieldNames().next();
      JsonNode fieldValue = filteredFieldNode.get(fieldName);
      ObjectNode result = objectMapper.createObjectNode();
      result.set(fieldName, fieldValue);

      for (Fields.Transformation transformation : transformations) {
        try {
          FieldTransformer transformer = createFieldTransformer(transformation);
          transformer.apply(fieldName, result.get(fieldName), result);
        } catch (Exception e) {
          // TODO(ivo) continue with last valid value or throw?
          break;
        }
      }

      // write the transformed result to the output stream (field name may have been changed by
      // rename transformation)
      String finalFieldName = result.fieldNames().next();
      jgen.writeFieldName(finalFieldName);
      jgen.writeTree(result.get(finalFieldName));
    }
  }

  /**
   * Creates a FieldTransformer instance from a Fields.Transformation. Uses existing transformer
   * implementations for compatibility.
   */
  private FieldTransformer createFieldTransformer(Transformation transformation) {
    return switch (transformation.getName()) {
      case "isEmpty" -> IsEmptyFieldTransformer.INSTANCE;
      case "isNotEmpty" -> IsNotEmptyFieldTransformer.INSTANCE;
      case "rename" -> {
        List<String> parameters = List.of(transformation.getArguments());
        FieldPathTransformer fieldPathTransformer =
            new FieldPathTransformer(transformation.getName(), parameters);
        yield new RenameFieldTransformer(fieldPathTransformer);
      }
      case "size" -> SizeFieldTransformer.INSTANCE;
      default -> null;
    };
  }
}
