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
package org.hisp.dhis.webapi;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A helper to build jackson {@link ObjectNode} and {@link ArrayNode}s from java collections and
 * POJO objects.
 *
 * @author Jan Bernitt
 */
public final class JsonBuilder {
  public enum Preference {
    SKIP_NULL_MEMBERS,
    SKIP_NULL_ELEMENTS,
    SKIP_EMPTY_ARRAYS
  }

  private final ObjectMapper jackson;

  private final EnumSet<Preference> preferences = EnumSet.noneOf(Preference.class);

  public JsonBuilder(ObjectMapper jackson) {
    this.jackson = jackson;
  }

  public JsonBuilder with(Preference preference) {
    preferences.add(preference);
    return this;
  }

  public JsonBuilder not(Preference preference) {
    preferences.remove(preference);
    return this;
  }

  public boolean is(Preference preference) {
    return preferences.contains(preference);
  }

  public JsonBuilder skipNullMembers() {
    return with(Preference.SKIP_NULL_MEMBERS);
  }

  public JsonBuilder skipNullElements() {
    return with(Preference.SKIP_NULL_ELEMENTS);
  }

  public JsonBuilder skipNulls() {
    return skipNullElements().skipNullMembers();
  }

  public JsonBuilder skipEmptyArrays() {
    return with(Preference.SKIP_EMPTY_ARRAYS);
  }

  public JsonBuilder skipNullOrEmpty() {
    return skipNulls().skipEmptyArrays();
  }

  public ArrayNode toArray(List<String> fields, List<?> values) {
    ArrayNode arr = jackson.createArrayNode();
    for (Object e : values) {
      if (fields.size() == 1) {
        arr.add(toElement(e));
      } else {
        arr.add(toElement(fields, e));
      }
    }
    return arr;
  }

  private JsonNode toElement(List<String> fields, Object e) {
    if (e instanceof Object[]) {
      return toObject(fields, (Object[]) e);
    } else if (e instanceof Collection) {
      return toObject(fields, (Collection<?>) e);
    }
    // assume the element e is already an object with all fields
    return jackson.valueToTree(e);
  }

  private JsonNode toElement(Object e) {
    if (e instanceof Object[]) {
      return toJsonNode(((Object[]) e)[0]);
    } else if (e instanceof Collection && ((Collection<?>) e).size() == 1) {
      return jackson.valueToTree(((Collection<?>) e).iterator().next());
    }
    return jackson.valueToTree(e);
  }

  public ObjectNode toObject(List<String> fields, Object... values) {
    return toObject(fields, asList(values));
  }

  public ObjectNode toObject(List<String> fields, Collection<?> values) {
    ObjectNode obj = jackson.createObjectNode();
    Map<String, ObjectNode> memberObjectsByName = new HashMap<>();
    Iterator<?> iter = values.iterator();
    for (String field : fields) {
      Object value = iter.hasNext() ? iter.next() : null;
      addMember(obj, field, value, memberObjectsByName);
    }
    return obj;
  }

  private void addMember(
      ObjectNode obj, String name, Object value, Map<String, ObjectNode> memberObjectsByName) {
    if (!skipValue(value)) {
      JsonNode node = toJsonNode(value);
      if (name.contains(".")) {
        String member = name.substring(0, name.indexOf('.'));
        ObjectNode memberNode =
            memberObjectsByName.computeIfAbsent(member, key -> jackson.createObjectNode());
        obj.set(member, memberNode);
        memberNode.set(name.substring(name.indexOf('.') + 1), node);
      } else {
        if (memberObjectsByName.containsKey(name) && node.isObject()) {
          ObjectNode memberNode = memberObjectsByName.get(name);
          memberNode.setAll((ObjectNode) node);
        } else {
          obj.set(name, node);
        }
      }
    }
  }

  public ObjectNode toObject(Map<String, ?> members) {
    ObjectNode obj = jackson.createObjectNode();
    Map<String, ObjectNode> memberObjectsByName = new HashMap<>();
    for (Entry<String, ?> member : members.entrySet()) {
      addMember(obj, member.getKey(), member.getValue(), memberObjectsByName);
    }
    return obj;
  }

  private Object cleanValue(Object value) {
    if (value instanceof Object[] && is(Preference.SKIP_NULL_ELEMENTS)) {
      long nulls = Arrays.stream((Object[]) value).filter(Objects::isNull).count();
      if (nulls > 0) {
        return Arrays.stream((Object[]) value).filter(Objects::isNull).toArray();
      }
    }
    return value;
  }

  private boolean skipValue(Object value) {
    if (value == null) {
      return is(Preference.SKIP_NULL_MEMBERS);
    }
    if (value instanceof Object[]) {
      return ((Object[]) value).length == 0 && is(Preference.SKIP_EMPTY_ARRAYS);
    }
    return false;
  }

  private JsonNode toJsonNode(Object value) {
    if (value instanceof org.hisp.dhis.jsontree.JsonNode[] nodes) {
      ArrayNode arr = jackson.createArrayNode();
      Stream.of(nodes).forEach(node -> arr.add(readNode(node.getDeclaration())));
      return arr;
    }
    if (value instanceof org.hisp.dhis.jsontree.JsonNode node) {
      return readNode(node.getDeclaration());
    }
    return jackson.valueToTree(cleanValue(value));
  }

  private JsonNode readNode(String json) {
    try {
      return jackson.readTree(json);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
