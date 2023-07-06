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
package org.hisp.dhis.fieldfiltering.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hisp.dhis.fieldfiltering.FieldPathTransformer;
import org.hisp.dhis.fieldfiltering.FieldTransformer;
import org.springframework.util.StringUtils;

/**
 * Field transformer that plucks out a property as a key, and transform the array into an object
 * with a value = payload.
 *
 * <p>Usage: "?fields=id,name,dataElementGroups::keyBy(id)"
 *
 * @author Morten Olav Hansen
 */
public class KeyByFieldTransformer implements FieldTransformer {
  private final FieldPathTransformer fieldPathTransformer;

  public KeyByFieldTransformer(FieldPathTransformer fieldPathTransformer) {
    this.fieldPathTransformer = fieldPathTransformer;
  }

  @Override
  public JsonNode apply(String path, JsonNode value, JsonNode parent) {
    if (!parent.isObject()) {
      return value;
    }

    String keyByFieldName = "id";

    if (!fieldPathTransformer.getParameters().isEmpty()) {
      keyByFieldName = fieldPathTransformer.getParameters().get(0);
    }

    String fieldName = getFieldName(path);

    if (value.isArray()) {
      ObjectNode objectNode = ((ArrayNode) value).arrayNode().objectNode();

      for (JsonNode node : value) {
        if (node.isObject() && node.has(keyByFieldName)) {
          String propertyName = node.get(keyByFieldName).asText();

          if (!objectNode.has(propertyName) && StringUtils.hasText(propertyName)) {
            objectNode.set(propertyName, node);
          } else if (objectNode.has(propertyName)) {
            JsonNode jsonNode = objectNode.get(propertyName);

            if (jsonNode.isArray()) {
              ((ArrayNode) jsonNode).add(node);
            } else {
              ArrayNode arrayNode = objectNode.arrayNode();
              arrayNode.add(jsonNode);
              arrayNode.add(node);

              objectNode.set(propertyName, arrayNode);
            }
          }
        }
      }

      ((ObjectNode) parent).replace(fieldName, objectNode);
    }

    return value;
  }
}
