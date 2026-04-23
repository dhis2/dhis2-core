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
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.Text;
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
    return of((CharSequence) json);
  }

  @Nonnull
  static AttributeValues of(@Nonnull CharSequence json) {
    if (json.isEmpty() || (json.charAt(0) != '{' && json.charAt(0) != '[')) throw illegalJson(json);
    if (json.length() == 2
        && ((json.charAt(0) == '{' && json.charAt(1) == '}')
            || (json.charAt(0) == '[' && json.charAt(1) == ']'))) return empty();
    return new LazyAttributeValues(json, null, null);
  }

  static AttributeValues of(@Nonnull Map<CharSequence, CharSequence> values) {
    if (values.isEmpty()) return empty();
    LazyAttributeValues res = new LazyAttributeValues(null, null, null);
    res.init(values);
    return res;
  }

  private void init(@Nonnull Map<CharSequence, CharSequence> from) {
    keys = new Text[from.size()];
    values = new Text[keys.length];
    int i = 0;
    for (Map.Entry<CharSequence, CharSequence> e : from.entrySet()) {
      keys[i] = Text.of(e.getKey());
      values[i++] = Text.of(e.getValue());
    }
    sort2(keys, values);
  }

  private CharSequence json;

  private Text[] keys;
  private Text[] values;

  private static final Text[] NOTHING = new Text[0];

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
  private synchronized void initSync(@Nonnull CharSequence json) {
    if (keys != null) return;
    JsonMixed objOrArr = JsonMixed.of(json);
    if (objOrArr.isNull() || objOrArr.isEmpty()) {
      initEmpty();
      return;
    }
    int i = 0;
    keys = new Text[objOrArr.size()];
    values = new Text[keys.length];
    if (objOrArr.isObject()) {
      for (JsonNode e : objOrArr.node().members(JsonNode.Index.SKIP)) {
        keys[i] = e.getKey();
        values[i++] = extractValue(e.getIfExists("value"));
      }
    } else if (objOrArr.isArray()) {
      for (JsonNode e : objOrArr.node().elements(JsonNode.Index.SKIP)) {
        keys[i] = e.get("attribute").get("id").textValue();
        values[i++] = extractValue(e.getIfExists("value"));
      }
    } else throw illegalJson(json);
    sort2(keys, values);
  }

  private static void sort2(Text[] keys, Text[] values) {
    // Use a simple bubble sort for clarity
    for (int i = 0; i < keys.length - 1; i++) {
      for (int j = 0; j < keys.length - i - 1; j++) {
        if (keys[j].compareTo(keys[j + 1]) > 0) {
          // Swap keys
          Text tempKey = keys[j];
          keys[j] = keys[j + 1];
          keys[j + 1] = tempKey;
          // Swap values
          Text tempVal = values[j];
          values[j] = values[j + 1];
          values[j + 1] = tempVal;
        }
      }
    }
  }

  @Nonnull
  private static IllegalArgumentException illegalJson(@Nonnull CharSequence json) {
    return new IllegalArgumentException("Not a valid attribute value JSON: " + json);
  }

  @Nonnull
  private static Text extractValue(JsonNode value) {
    if (value == null) return Text.EMPTY;
    return switch (value.type()) {
      case NULL -> Text.EMPTY;
      case STRING -> value.textValue();
      default -> value.getDeclaration();
    };
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
  public Stream<UID> keys() {
    init();
    return Stream.of(keys).map(UID::of);
  }

  @Nonnull
  @Override
  public Stream<Text> values() {
    init();
    return Stream.of(values);
  }

  @CheckForNull
  @Override
  public Text getText(CharSequence attributeId) {
    init();
    Text key = Text.of(attributeId);
    int i = binarySearch(keys, key);
    return i < 0 ? null : values[i];
  }

  @Nonnull
  @Override
  public AttributeValues added(
      @Nonnull CharSequence attributeId, @CheckForNull CharSequence value) {
    if (value == null) return removed(attributeId);
    init();
    Text key = Text.of(attributeId);
    int i = binarySearch(keys, key);
    if (i >= 0) { // replace existing value
      if (values[i].contentEquals(value)) return this;
      Text[] newValues = values.clone();
      newValues[i] = Text.of(value);
      return new LazyAttributeValues(null, keys, newValues);
    }
    int insert = -(i + 1);
    int size = keys.length;
    if (insert == size) { // append
      Text[] newKeys = copyOf(keys, size + 1);
      Text[] newValues = copyOf(values, size + 1);
      newKeys[size] = key;
      newValues[size] = Text.of(value);
      return new LazyAttributeValues(null, newKeys, newValues);
    }
    Text[] newKeys = new Text[size + 1];
    Text[] newValues = new Text[size + 1];
    if (insert > 0) {
      arraycopy(keys, 0, newKeys, 0, insert);
      arraycopy(values, 0, newValues, 0, insert);
    }
    newKeys[insert] = key;
    newValues[insert] = Text.of(value);
    int remaining = keys.length - insert;
    arraycopy(keys, insert, newKeys, insert + 1, remaining);
    arraycopy(values, insert, newValues, insert + 1, remaining);
    return new LazyAttributeValues(null, newKeys, newValues);
  }

  @Nonnull
  @Override
  public AttributeValues removed(@Nonnull CharSequence attributeId) {
    init();
    int i = binarySearch(keys, Text.of(attributeId));
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
    Text[] newKeys = new Text[keys.length - 1];
    Text[] newValues = new Text[newKeys.length];
    arraycopy(keys, 0, newKeys, 0, i);
    arraycopy(values, 0, newValues, 0, i);
    int remaining = keys.length - i - 1;
    arraycopy(keys, i + 1, newKeys, i, remaining);
    arraycopy(values, i + 1, newValues, i, remaining);
    return new LazyAttributeValues(null, newKeys, newValues);
  }

  @Override
  public void forEach(BiConsumer<Text, Text> action) {
    init();
    for (int i = 0; i < keys.length; i++) action.accept(keys[i], values[i]);
  }

  @Nonnull
  @Override
  public Iterator<Map.Entry<Text, Text>> iterator() {
    init();
    return new Iterator<>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Map.Entry<Text, Text> next() {
        if (i >= size())
          throw new NoSuchElementException("Next called without checking via hasNext");
        return Map.entry(keys[i], values[i++]);
      }
    };
  }

  @Override
  public void addTo(JsonBuilder.JsonArrayBuilder arr) {
    init();
    arr.addObject(this::asJsonObject);
  }

  @Override
  public void addTo(CharSequence name, JsonBuilder.JsonObjectBuilder parent) {
    init();
    parent.addObject(name, this::asJsonObject);
  }

  private void asJsonObject(JsonBuilder.JsonObjectBuilder self) {
    for (int i = 0; i < size(); i++) if (values[i] != null) self.addString(keys[i], values[i]);
  }

  @Nonnull
  @Override
  public String toObjectJson() {
    if (isEmpty()) return "{}";
    return createObject(
            map ->
                forEach(
                    (key, value) -> {
                      if (!value.isEmpty())
                        map.addObject(key, obj -> obj.addString("value", value));
                    }))
        .getDeclaration()
        .toString();
  }

  @Nonnull
  @Override
  public String toArrayJson() {
    if (isEmpty()) return "[]";
    return createArray(
            arr ->
                forEach(
                    (key, value) ->
                        arr.addObject(
                            obj ->
                                obj.addString("value", value)
                                    .addObject("attribute", attr -> attr.addString("id", key)))))
        .getDeclaration()
        .toString();
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
