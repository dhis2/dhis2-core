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
package org.hisp.dhis.tracker.export.fieldfiltering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link FieldsTransformer.Function} implement the {@code fields} transformations. Transformer
 * functions work by mutating the {@link ObjectNode} field/value. They reuse most of the logic from
 * {@link org.hisp.dhis.fieldfiltering.transformers}.
 */
public class FieldsTransformer {
  public static final Map<String, Function> TRANSFORMERS =
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
  public static final Map<String, Validator> TRANSFORMERS_VALIDATOR =
      Map.of("rename", FieldsTransformer::requireOneArgument);

  public interface Function {
    void transform(@Nonnull String field, @Nonnull ObjectNode parent, @Nullable String argument);
  }

  /**
   * Validates the argument(s) to a field transformer.
   *
   * <p>Return an error message if the argument does not conform to the transformer and {@code null}
   * if it does.
   */
  public interface Validator {
    @Nullable
    String validate(
        @Nonnull String transformer, @Nonnull String field, @Nullable String... arguments);
  }

  public static void isEmpty(
      @Nonnull String field, @Nonnull ObjectNode parent, @Nullable String argument) {
    JsonNode value = parent.get(field);

    if (value.isArray()) {
      parent.put(field, value.isEmpty());
    }
  }

  public static void isNotEmpty(
      @Nonnull String field, @Nonnull ObjectNode parent, @Nullable String argument) {
    JsonNode value = parent.get(field);

    if (value.isArray()) {
      parent.put(field, !value.isEmpty());
    }
  }

  public static void keyBy(
      @Nonnull String field, @Nonnull ObjectNode parent, @Nullable String key) {
    JsonNode value = parent.get(field);
    if (!value.isArray()) {
      return;
    }

    if (key == null) {
      key = "id";
    }

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

    parent.replace(field, objectNode);
  }

  public static void pluck(
      @Nonnull String field, @Nonnull ObjectNode parent, @Nullable String pluckField) {
    JsonNode value = parent.get(field);
    if (!value.isArray()) {
      return;
    }

    if (pluckField == null) {
      pluckField = "id";
    }

    ArrayNode arrayNode = ((ArrayNode) value).arrayNode();

    for (JsonNode node : value) {
      if (node.isObject() && node.has(pluckField)) {
        arrayNode.add(node.get(pluckField));
      } else {
        arrayNode.add(node);
      }
    }

    parent.replace(field, arrayNode);
  }

  public static void rename(
      @Nonnull String field, @Nonnull ObjectNode parent, @Nonnull String newField) {
    JsonNode value = parent.get(field);

    parent.remove(field);
    parent.set(newField, value);
  }

  public static void size(
      @Nonnull String field, @Nonnull ObjectNode parent, @Nullable String argument) {
    JsonNode value = parent.get(field);

    if (value.isArray()) {
      parent.put(field, value.size());
    } else if (value.isTextual()) {
      parent.put(field, value.asText().length());
    } else if (value.isInt()) {
      parent.put(field, value.asInt());
    } else if (value.isLong()) {
      parent.put(field, value.asLong());
    } else if (value.isDouble() || value.isFloat()) {
      parent.put(field, value.asDouble());
    }
  }

  public static String requireOneArgument(
      @Nonnull String transformer, @Nonnull String field, @Nullable String... arguments) {
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
