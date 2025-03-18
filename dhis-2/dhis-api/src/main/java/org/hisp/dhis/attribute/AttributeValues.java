/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.attribute;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.intellij.lang.annotations.Language;

/**
 * An immutable set of {@link Attribute} values.
 *
 * <p>{@link AttributeValues} variables and fields should never be initialized to {@code null}.
 * Always prefer to use {@link #empty()} as it has virtually no costs.
 *
 * <p>This should prevent usages to get dependent on the exact representation of attribute values as
 * well as prevent issues with managing the state such as the backing collection being
 * uninitialised/null.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public sealed interface AttributeValues extends Iterable<Map.Entry<String, String>>, Serializable
    permits LazyAttributeValues {

  /**
   * @return an empty collection of attribute values
   */
  @Nonnull
  static AttributeValues empty() {
    return LazyAttributeValues.empty();
  }

  @Nonnull
  static AttributeValues of(@Nonnull Map<String, String> values) {
    return LazyAttributeValues.of(values);
  }

  /**
   * Possible input shapes:
   *
   * <pre>
   * { "{uid}": { "value": "..." } }
   * [ { "value": "...", "attribute": { "id": "{uid}" }} ]
   * null
   * </pre>
   *
   * Both, array and object shape may contain more properties that are of no interest as long as
   * they at least provide the shape outlined.
   *
   * @param json either an object with keys being the attribute ID and value being an object with a
   *     "value" property or an array of objects with a string "value" and an object "attribute"
   *     property or null.
   * @return a new instance of {@link AttributeValues} with same mapping as given by the JSON
   */
  @Nonnull
  static AttributeValues of(@Nonnull @Language("json") String json) {
    return LazyAttributeValues.of(json);
  }

  boolean isEmpty();

  int size();

  @Nonnull
  Set<String> keys();

  @Nonnull
  Set<String> values();

  /**
   * @param attributeId the attribute to read
   * @return the value or null if the attribute is not contained (undefined)
   */
  @CheckForNull
  String get(String attributeId);

  default boolean contains(String attributeId) {
    return get(attributeId) != null;
  }

  void forEach(BiConsumer<String, String> action);

  @Nonnull
  Stream<Map.Entry<String, String>> stream();

  /**
   * JSON of the shape
   *
   * <pre>
   * {
   *   "{uid}": { "value": "..." }
   * }
   * </pre>
   *
   * @return The JSON as stored in DB
   */
  @Nonnull
  @Language("json")
  String toObjectJson();

  /**
   * JSON of the shape
   *
   * <pre>
   * [
   *   { "value": "...", "attribute": { "id": "{uid}" }}
   * ]
   * </pre>
   *
   * @return the JSON as returned by the API
   */
  @Nonnull
  @Language("json")
  String toArrayJson();

  /*
  Modification
   */

  /**
   * @param attributeId the attribute UID
   * @param value the value for the attribute, null to remove
   * @return a new set with the attribute value added or changed to the provided value
   */
  @Nonnull
  AttributeValues added(@Nonnull String attributeId, @CheckForNull String value);

  /**
   * @param uid the attribute to remove
   * @return a new set with the attribute removed
   */
  @Nonnull
  AttributeValues removed(@Nonnull String uid);

  /**
   * @param attributeIdMatches filter for keys
   * @return a new set with all attributes removed that had a matching attribute ID (match true)
   */
  @Nonnull
  AttributeValues removedAll(Predicate<String> attributeIdMatches);

  /**
   * @param mapper mapping function for values
   * @return a new set with same keys but all values transformed by the mapper function
   */
  @Nonnull
  AttributeValues mapValues(@Nonnull UnaryOperator<String> mapper);

  /**
   * @param mapper mapping function for keys
   * @return a new set with same values but all keys transformed by the mapper function
   */
  @Nonnull
  AttributeValues mapKeys(@Nonnull UnaryOperator<String> mapper);

  default Map<String, String> toMap() {
    return isEmpty()
        ? Map.of()
        : stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
