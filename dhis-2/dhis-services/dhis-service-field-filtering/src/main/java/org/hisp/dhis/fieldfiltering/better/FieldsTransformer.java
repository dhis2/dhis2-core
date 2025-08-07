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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.util.StringUtils;

public class FieldsTransformer {
  public static Map<String, Function> TRANSFORMERS =
      Map.of(
          "isEmpty",
          FieldsTransformer::isEmpty,
          "isNotEmpty",
          FieldsTransformer::isNotEmpty,
          "keyBy",
          FieldsTransformer::keyBy,
          "pluck",
          FieldsTransformer::pluck,
          "rename",
          FieldsTransformer::rename,
          "size",
          FieldsTransformer::size);
  public static Map<String, Validator> TRANSFORMERS_VALIDATOR =
      Map.of("rename", FieldsTransformer::requireOneArg);

  public interface Function {
    void apply(
        @Nonnull String field,
        @Nonnull JsonNode parent,
        @Nonnull JsonNode value,
        @Nullable String argument);
  }

  /**
   * Validates the arguments to a field transformer.
   *
   * <p>Return an error message if the argument does not conform to the transformer and {@code null}
   * if it does.
   */
  public interface Validator {
    @Nullable
    String validate(@Nonnull String transformer, String field, @Nullable String... arguments);
  }

  // TODO(ivo) can I get rid of the guard as all will be called with an object, I mean these are
  // field transformers, as such they will always be applied to a field/property
  // TODO(ivo) how about passing in ObjectNode only, instead of parent/value
  public static void isEmpty(String field, JsonNode parent, JsonNode value, String argument) {
    if (!parent.isObject()) {
      return;
    }

    if (value.isArray()) {
      // overwrite array node with true/false depending on empty status
      ((ObjectNode) parent).put(field, value.isEmpty());
    }
  }

  public static void isNotEmpty(String field, JsonNode parent, JsonNode value, String argument) {
    if (!parent.isObject()) {
      return;
    }

    if (value.isArray()) {
      // overwrite array node with true/false depending on empty status
      ((ObjectNode) parent).put(field, !value.isEmpty());
    }
  }

  public static void keyBy(String field, JsonNode parent, JsonNode value, String key) {
    if (!parent.isObject()) {
      return;
    }

    if (key == null) {
      key = "id";
    }

    if (value.isArray()) {
      ObjectNode objectNode = ((ArrayNode) value).arrayNode().objectNode();

      for (JsonNode node : value) {
        if (node.isObject() && node.has(key)) {
          String propertyName = node.get(key).asText();

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

      ((ObjectNode) parent).replace(field, objectNode);
    }
  }

  public static void pluck(String field, JsonNode parent, JsonNode value, String pluckField) {
    if (!parent.isObject()) {
      return;
    }

    if (pluckField == null) {
      pluckField = "id";
    }

    if (value.isArray()) {
      ArrayNode arrayNode = ((ArrayNode) value).arrayNode();

      for (JsonNode node : value) {
        if (node.isObject() && node.has(pluckField)) {
          arrayNode.add(node.get(pluckField));
        } else {
          arrayNode.add(node);
        }
      }

      ((ObjectNode) parent).replace(field, arrayNode);
    }
  }

  public static void rename(String field, JsonNode parent, JsonNode value, String argument) {
    if (!parent.isObject()) {
      return;
    }

    value = ((ObjectNode) parent).remove(field);
    ((ObjectNode) parent).set(argument, value);
  }

  public static void size(String field, JsonNode parent, JsonNode value, String argument) {
    if (!parent.isObject()) {
      return;
    }

    if (value.isArray()) {
      ((ObjectNode) parent).put(field, value.size());
    } else if (value.isTextual()) {
      ((ObjectNode) parent).put(field, value.asText().length());
    } else if (value.isInt()) {
      ((ObjectNode) parent).put(field, value.asInt());
    } else if (value.isLong()) {
      ((ObjectNode) parent).put(field, value.asLong());
    } else if (value.isDouble() || value.isFloat()) {
      ((ObjectNode) parent).put(field, value.asDouble());
    }
  }

  public static String requireOneArg(
      @Nonnull String transformer, String field, @Nullable String... arguments) {
    if (arguments == null) {
      return transformer
          + " applied to field "
          + field
          + " requires exactly one non-blank argument";
    }
    if (arguments.length != 1) {
      return transformer
          + " applied to field "
          + field
          + " requires exactly one non-blank argument";
    }
    if (arguments[0].isBlank()) {
      return transformer
          + " applied to field "
          + field
          + " requires exactly one non-blank argument";
    }

    return null;
  }
}
