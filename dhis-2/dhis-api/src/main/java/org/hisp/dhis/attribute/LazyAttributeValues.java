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

import static java.lang.System.arraycopy;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static org.hisp.dhis.jsontree.JsonBuilder.createArray;
import static org.hisp.dhis.jsontree.JsonBuilder.createObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.intellij.lang.annotations.Language;

/**
 * Represents {@link AttributeValues} as memory efficient as possible without compromising lookup
 * performance.
 *
 * <p>If values are constructed from JSON the parsing occurs lazily on first access as in many
 * instances this might never happen until the instance is discarded.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class LazyAttributeValues implements AttributeValues {

  private static final AttributeValues EMPTY = new LazyAttributeValues("{}", null, null);

  static AttributeValues empty() {
    return EMPTY;
  }

  @Nonnull
  static AttributeValues of(@Nonnull @Language("json") String json) {
    if ("{}".equals(json) || "[]".equals(json)) return empty();
    if (json.isEmpty() || (json.charAt(0) != '{' && json.charAt(0) != '[')) throw illegalJson(json);
    return new LazyAttributeValues(json, null, null);
  }

  static AttributeValues of(@Nonnull Map<String, String> values) {
    if (values.isEmpty()) return empty();
    LazyAttributeValues entries = new LazyAttributeValues(null, null, null);
    if (values instanceof TreeMap<String, String> tm) {
      entries.init(tm);
    } else {
      entries.init(new TreeMap<>(values));
    }
    return entries;
  }

  @Language("json")
  private String json;

  private String[] keys;
  private String[] values;

  private static final String[] NOTHING = new String[0];

  private void init() {
    if (keys != null) return;
    if (json == null) {
      initEmpty();
      return;
    }
    initSync(json);
    json = null; // free memory
  }

  private void initEmpty() {
    keys = NOTHING;
    values = NOTHING;
  }

  /**
   * Since the implementation is immutable it is generally thread-safe except when initialized lazy
   * from JSON multiple threads might concurrently invoke the initialisation wherefore this is
   * synchronized.
   */
  private synchronized void initSync(@Nonnull String json) {
    if (keys != null) return;
    JsonMixed objOrArr = JsonMixed.of(json);
    if (objOrArr.isNull() || objOrArr.isEmpty()) {
      initEmpty();
      return;
    }
    // Note that this needs to go via TreeMap to ensure alphabetic ordering;
    // using JSON value API directly is difficult due to incompatibility of lambdas and i-counters
    if (objOrArr.isObject()) {
      init(parseObjectJson(objOrArr));
    } else if (objOrArr.isArray()) {
      init(parseArrayJson(objOrArr));
    } else throw illegalJson(json);
  }

  @Nonnull
  private static IllegalArgumentException illegalJson(@Nonnull String json) {
    return new IllegalArgumentException("Not a valid attribute value JSON: " + json);
  }

  @Nonnull
  private static TreeMap<String, String> parseObjectJson(JsonObject map) {
    return map.entries()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> parseValue(e.getValue().asObject().get("value")),
                (a, b) -> a,
                TreeMap::new));
  }

  @Nonnull
  private static TreeMap<String, String> parseArrayJson(JsonArray arr) {
    return arr.stream()
        .map(JsonValue::asObject)
        .collect(
            Collectors.toMap(
                obj -> obj.getObject("attribute").getString("id").string(),
                obj -> parseValue(obj.get("value")),
                (a, b) -> a,
                TreeMap::new));
  }

  @Nonnull
  private static String parseValue(JsonValue value) {
    if (value.isUndefined()) return "";
    return switch (value.type()) {
      case NULL -> "";
      case STRING -> value.as(JsonString.class).string();
      default -> value.toJson();
    };
  }

  private void init(TreeMap<String, String> from) {
    keys = new String[from.size()];
    values = new String[keys.length];
    int i = 0;
    for (Map.Entry<String, String> e : from.entrySet()) {
      // Note: intern is used so all keys of the same attribute share a String instance
      keys[i] = e.getKey().intern();
      values[i++] = e.getValue();
    }
  }

  @Override
  public boolean isEmpty() {
    init();
    return keys.length == 0;
  }

  @Override
  public int size() {
    init();
    return keys.length;
  }

  @Nonnull
  @Override
  public Set<String> keys() {
    init();
    return Set.of(keys);
  }

  @Nonnull
  @Override
  public Set<String> values() {
    init();
    return Set.of(values);
  }

  @CheckForNull
  @Override
  public String get(String attributeId) {
    init();
    int i = binarySearch(keys, attributeId);
    return i < 0 ? null : values[i];
  }

  @Nonnull
  @Override
  public AttributeValues added(@Nonnull String attributeId, @CheckForNull String value) {
    if (value == null) return removed(attributeId);
    init();
    int i = binarySearch(keys, attributeId);
    if (i >= 0) { // replace existing value
      if (values[i].equals(value)) return this;
      String[] newValues = values.clone();
      newValues[i] = value;
      return new LazyAttributeValues(null, keys, newValues);
    }
    int insert = -(i + 1);
    int size = keys.length;
    if (insert == size) { // append
      String[] newKeys = copyOf(keys, size + 1);
      String[] newValues = copyOf(values, size + 1);
      newKeys[size] = attributeId;
      newValues[size] = value;
      return new LazyAttributeValues(null, newKeys, newValues);
    }
    String[] newKeys = new String[size + 1];
    String[] newValues = new String[size + 1];
    if (insert > 0) {
      arraycopy(keys, 0, newKeys, 0, insert);
      arraycopy(values, 0, newValues, 0, insert);
    }
    newKeys[insert] = attributeId;
    newValues[insert] = value;
    int remaining = keys.length - insert;
    arraycopy(keys, insert, newKeys, insert + 1, remaining);
    arraycopy(values, insert, newValues, insert + 1, remaining);
    return new LazyAttributeValues(null, newKeys, newValues);
  }

  @Nonnull
  @Override
  public AttributeValues removed(@Nonnull String uid) {
    init();
    int i = binarySearch(keys, uid);
    if (i < 0) return this; // not contained
    if (size() == 1) {
      return empty();
    }
    if (i == 0) { // remove head special case
      return new LazyAttributeValues(
          null, copyOfRange(keys, 1, keys.length), copyOfRange(values, 1, values.length));
    }
    if (i == keys.length - 1) { // remove tail special case
      return new LazyAttributeValues(
          null, copyOf(keys, keys.length - 1), copyOf(values, values.length - 1));
    }
    // remove in the middle
    String[] newKeys = new String[keys.length - 1];
    String[] newValues = new String[newKeys.length];
    arraycopy(keys, 0, newKeys, 0, i);
    arraycopy(values, 0, newValues, 0, i);
    int remaining = keys.length - i - 1;
    arraycopy(keys, i + 1, newKeys, i, remaining);
    arraycopy(values, i + 1, newValues, i, remaining);
    return new LazyAttributeValues(null, newKeys, newValues);
  }

  @Nonnull
  @Override
  public AttributeValues removedAll(Predicate<String> attributeIdMatches) {
    if (Stream.of(keys).noneMatch(attributeIdMatches)) return this;
    int remove = (int) Stream.of(keys).filter(attributeIdMatches).count();
    if (remove == size()) return empty();
    int newSize = keys.length - remove;
    String[] newKeys = new String[newSize];
    String[] newValues = new String[newKeys.length];
    int j = 0;
    for (int i = 0; i < keys.length; i++) {
      if (!attributeIdMatches.test(keys[i])) {
        newKeys[j] = keys[i];
        newValues[j++] = values[i];
      }
    }
    return new LazyAttributeValues(null, newKeys, newValues);
  }

  @Nonnull
  @Override
  public AttributeValues mapValues(@Nonnull UnaryOperator<String> mapper) {
    init();
    return map(values, mapper);
  }

  @Nonnull
  @Override
  public AttributeValues mapKeys(@Nonnull UnaryOperator<String> mapper) {
    init();
    return map(keys, mapper);
  }

  @Nonnull
  private AttributeValues map(String[] elements, @Nonnull UnaryOperator<String> f) {
    String[] newElements = null;
    for (int i = 0; i < elements.length; i++) {
      String element = elements[i];
      String mapped = f.apply(element);
      if (!element.equals(mapped)) {
        if (newElements == null) newElements = elements.clone();
        newElements[i] = mapped;
      }
    }
    if (newElements == null) return this; // no change happened
    return elements == keys
        ? new LazyAttributeValues(null, newElements, values)
        : new LazyAttributeValues(null, keys, newElements);
  }

  @Override
  public void forEach(BiConsumer<String, String> action) {
    init();
    for (int i = 0; i < keys.length; i++) action.accept(keys[i], values[i]);
  }

  @Nonnull
  @Override
  public Stream<Map.Entry<String, String>> stream() {
    init();
    if (isEmpty()) return Stream.empty();
    return StreamSupport.stream(spliterator(), false);
  }

  @Nonnull
  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    init();
    return new Iterator<>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Map.Entry<String, String> next() {
        if (i >= size())
          throw new NoSuchElementException("Next called without checking via hasNext");
        return Map.entry(keys[i], values[i++]);
      }
    };
  }

  @Nonnull
  @Override
  public String toObjectJson() {
    if (json != null && json.charAt(0) == '{') return json;
    init();
    if (isEmpty()) return "{}";
    return createObject(
            map ->
                forEach((key, value) -> map.addObject(key, obj -> obj.addString("value", value))))
        .getDeclaration();
  }

  @Nonnull
  @Override
  public String toArrayJson() {
    init();
    if (isEmpty()) return "[]";
    return createArray(
            arr ->
                forEach(
                    (key, value) ->
                        arr.addObject(
                            obj ->
                                obj.addString("value", value)
                                    .addObject("attribute", attr -> attr.addString("id", key)))))
        .getDeclaration();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LazyAttributeValues other)) return false;
    if (this == obj) return true;
    init();
    other.init();
    return Arrays.equals(keys, other.keys) && Arrays.equals(values, other.values);
  }

  @Override
  public int hashCode() {
    init();
    return Arrays.hashCode(keys) ^ Arrays.hashCode(values);
  }

  @Override
  public String toString() {
    return toObjectJson();
  }
}
