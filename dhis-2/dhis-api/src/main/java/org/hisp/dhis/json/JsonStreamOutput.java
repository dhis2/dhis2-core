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
package org.hisp.dhis.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonBuilder.JsonObjectBuilder.AddMember;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.object.ObjectOutput;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.DateUtils;

/**
 * A utility to support {@link Stream}-serialisation as JSON for varying property paths.
 *
 * <p>This makes sure that independent of the paths order given the created JSON follows that order
 * as close as possible without tearing objects apart.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonStreamOutput {

  /**
   * @see #addArrayElements(JsonBuilder.JsonArrayBuilder, List, List, Stream)
   */
  public static void addArrayElements(
      @Nonnull JsonBuilder.JsonArrayBuilder arr,
      @Nonnull List<ObjectOutput.Property> properties,
      @Nonnull Stream<IntFunction<Object>> elements) {
    List<String> paths = properties.stream().map(ObjectOutput.Property::path).toList();
    List<AddMember<Object>> adders =
        properties.stream()
            .map(ObjectOutput.Property::type)
            .map(JsonStreamOutput::getAdder)
            .toList();
    addArrayElements(arr, paths, adders, elements);
  }

  /**
   * Stream-adds JSON object nodes to an JSON array parent. Each object is created with the same
   * properties provided by their paths. For each path also a function is given that can add it to
   * the current object added.
   *
   * @implNote The elements stream is abstracted so that different types of sources can be processed
   *     without the need to allocate an intermediate representation. Instead, an adopter function
   *     is provided in form of an {@link IntFunction} that resolves the property value by index
   *     (the index her refers to the sequence of the given paths)
   * @param arr the target parent the objects are added to
   * @param paths the fully qualified properties (e.g. {@code address.street}, nested paths
   *     auto-create the intermediate parent objects) The output contains the properties in the
   *     given order, unless a property has to be reordered to avoid tearing intermediate parents.
   * @param adders a function (for each path) that is capable to add a name-value pair to the object
   * @param elements a stream of "objects" where each object is represented by an {@link
   *     IntFunction} that given the index of the provided path returns the value for that property
   *     in the object (which then is given to the adder for the corresponding path to perform the
   *     adding to the target object)
   */
  public static void addArrayElements(
      @Nonnull JsonBuilder.JsonArrayBuilder arr,
      @Nonnull List<String> paths,
      @Nonnull List<AddMember<Object>> adders,
      @Nonnull Stream<IntFunction<Object>> elements) {
    checkCardinality(paths, adders);
    JsonLayoutNode.ObjectLayoutNode element = createLayoutTree(paths, adders);
    elements.forEach(e -> arr.addObject(obj -> element.add(obj, e)));
  }

  public static void addObjectMembers(
      @Nonnull JsonBuilder.JsonObjectBuilder obj,
      @Nonnull List<ObjectOutput.Property> properties,
      @Nonnull IntFunction<Object> values) {
    List<String> paths = properties.stream().map(ObjectOutput.Property::path).toList();
    List<AddMember<Object>> adders =
        properties.stream()
            .map(ObjectOutput.Property::type)
            .map(JsonStreamOutput::getAdder)
            .toList();
    addObjectMembers(obj, paths, adders, values);
  }

  public static void addObjectMembers(
      @Nonnull JsonBuilder.JsonObjectBuilder obj,
      @Nonnull List<String> paths,
      @Nonnull List<AddMember<Object>> adders,
      @Nonnull IntFunction<Object> values) {
    checkCardinality(paths, adders);
    JsonLayoutNode.ObjectLayoutNode self = createLayoutTree(paths, adders);
    self.add(obj, values);
  }

  private static void checkCardinality(
      @Nonnull List<String> paths, @Nonnull List<AddMember<Object>> adders) {
    if (paths.size() != adders.size())
      throw new IllegalArgumentException("A adder must be provided for each path");
  }

  /**
   * When users make their {@code fields} selection the extracted properties do not necessarily have
   * an order that when understood in a hierarchy would not tear parents apart. To allow using the
   * properties to write JSON that does not tear parents the order is altered to prevent tearing.
   *
   * <pre>
   * foo.a, bar, foo.b => foo.a, foo.b, bar
   * </pre>
   *
   * @implNote Reordering by classic sorting might seem more straight forward at first glance but is
   *     actually surprisingly difficult for this problem.
   * @param paths the list of fields/properties path in the order requested by the user
   */
  public static List<String> reorderNoParentTearing(List<String> paths) {
    int n = paths.size();
    if (n <= 1) return paths;
    // build tree layout (maintains insert order)
    JsonLayoutNode.ObjectLayoutNode root = createLayoutTree(paths, List.of());
    // flatten the tree into a list of leaf properties
    List<String> sorted = new ArrayList<>(n);
    root.flatten(sorted);
    return List.copyOf(sorted);
  }

  @Nonnull
  private static JsonLayoutNode.ObjectLayoutNode createLayoutTree(
      List<String> paths, List<AddMember<Object>> adders) {
    JsonLayoutNode.ObjectLayoutNode root = new JsonLayoutNode.ObjectLayoutNode("");
    int i = 0;
    for (String path : paths) {
      JsonLayoutNode.ObjectLayoutNode parent = root;
      List<String> parentPath = ObjectOutput.Property.parentPath(path);
      if (!parentPath.isEmpty()) {
        for (String name : parentPath) {
          int j = parent.indexOf(name);
          if (j < 0) {
            JsonLayoutNode.ObjectLayoutNode p = new JsonLayoutNode.ObjectLayoutNode(name);
            parent.members.add(p);
            parent = p;
          } else {
            parent = (JsonLayoutNode.ObjectLayoutNode) parent.members.get(j);
          }
        }
      }
      String name = ObjectOutput.Property.name(path);
      parent.members.add(
          new JsonLayoutNode.PropertyLayoutNode(
              i, path, name, i < adders.size() ? adders.get(i) : null));
      i++;
    }
    return root;
  }

  /**
   * A layout describes the "schema" of a JSON object. In this layout there are only objects and
   * property members of objects.
   */
  private sealed interface JsonLayoutNode {

    String name();

    void flatten(List<String> into);

    void add(JsonBuilder.JsonObjectBuilder obj, IntFunction<java.lang.Object> valueAtPathIndex);

    /** The layout (schema) eqivalent of a JSON object (data) */
    record ObjectLayoutNode(String name, List<JsonLayoutNode> members) implements JsonLayoutNode {
      ObjectLayoutNode(String name) {
        this(name, new ArrayList<>());
      }

      int indexOf(String name) {
        if (members.isEmpty()) return -1;
        for (int i = 0; i < members.size(); i++) if (name.equals(members.get(i).name())) return i;
        return -1;
      }

      @Override
      public void flatten(List<String> into) {
        members.forEach(m -> m.flatten(into));
      }

      @Override
      public void add(
          JsonBuilder.JsonObjectBuilder to, IntFunction<java.lang.Object> valueAtPathIndex) {
        if (name.isEmpty()) { // root
          members.forEach(m -> m.add(to, valueAtPathIndex));
        } else {
          to.addObject(name, obj -> members.forEach(m -> m.add(obj, valueAtPathIndex)));
        }
      }
    }

    /** The layout (schema) eqivalent of a JSON object member (data) */
    record PropertyLayoutNode(
        int index, String path, String name, AddMember<java.lang.Object> adder)
        implements JsonLayoutNode {

      @Override
      public void flatten(List<String> into) {
        into.add(path);
      }

      @Override
      public void add(
          JsonBuilder.JsonObjectBuilder obj, IntFunction<java.lang.Object> getValueAtPathIndex) {
        adder.add(obj, name, getValueAtPathIndex.apply(index));
      }
    }
  }

  /*
  Implementation of value type conversion
   */

  private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper();

  private static final Map<Class<?>, AddMember<Object>> ADDERS_BY_TYPE = new ConcurrentHashMap<>();

  private static <T> void register(Class<T> type, Function<? super T, String> toString) {
    register(type, (obj, name, value) -> obj.addString(name, toString.apply(value)));
  }

  private static <T> void register(Class<T> type, AddMember<? super T> adder) {
    AddMember<Object> guarded =
        (obj, name, val) -> {
          if (val != null) {
            try {
              adder.add(obj, name, type.cast(val));
            } catch (ClassCastException ex) {
              addJacksonMapped(obj, name, val);
            }
          }
        };
    ADDERS_BY_TYPE.put(type, guarded);
  }

  static {
    register(String.class, JsonBuilder.JsonObjectBuilder::addString);
    register(UID.class, UID::getValue);
    register(Date.class, DateUtils::toIso8601NoTz);
    register(Timestamp.class, DateUtils::toIso8601NoTz);
    register(Enum.class, Enum::name);
    register(Period.class, Period::getIsoDate);
    register(PeriodType.class, PeriodType::getName);
    register(Integer.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(int.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(Long.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(long.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(Double.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(double.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(Float.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(float.class, JsonBuilder.JsonObjectBuilder::addNumber);
    register(Boolean.class, JsonBuilder.JsonObjectBuilder::addBoolean);
    register(boolean.class, JsonBuilder.JsonObjectBuilder::addBoolean);
    register(JsonBuilder.JsonEncodable.class, JsonBuilder.JsonObjectBuilder::addMember);
    register(Object.class, JsonStreamOutput::addJacksonMapped);
    register(
        String[].class,
        (obj, name, val) ->
            obj.addArray(
                name,
                arr -> {
                  // uses for loop to avoid wrapping => defensive copying
                  for (String s : val) arr.addString(s);
                }));
    register(
        JsonBuilder.JsonEncodable[].class,
        (obj, name, val) ->
            obj.addArray(
                name,
                arr -> {
                  // uses for loop to avoid wrapping => defensive copying
                  for (JsonBuilder.JsonEncodable e : val)
                    if (e == null) {
                      arr.addElement(JsonNode.NULL);
                    } else e.addTo(arr);
                }));
  }

  public static AddMember<Object> getAdder(ObjectOutput.Type valueType) {
    Class<?> type = valueType.rawType();
    AddMember<Object> adder = ADDERS_BY_TYPE.get(type);
    if (adder != null) return adder;
    if (type.isEnum()) return ADDERS_BY_TYPE.get(Enum.class);
    if (JsonBuilder.JsonEncodable.class.isAssignableFrom(type))
      return ADDERS_BY_TYPE.get(JsonBuilder.JsonEncodable.class);
    Class<?> elementType = valueType.elementType();
    if (Collection.class.isAssignableFrom(type) && elementType != null) {
      AddMember<Object> obj = createCollectionAdder(elementType);
      if (obj != null) return obj;
    }
    if (Map.class.isAssignableFrom(type) && elementType == String.class)
      return createStringMapAdder();
    return ADDERS_BY_TYPE.get(Object.class);
  }

  @SuppressWarnings("unchecked")
  private static AddMember<Object> createStringMapAdder() {
    return (obj, name, val) ->
        obj.addObject(name, map -> ((Map<String, String>) val).forEach(map::addString));
  }

  @CheckForNull
  @SuppressWarnings("unchecked")
  private static AddMember<Object> createCollectionAdder(Class<?> elementType) {
    if (Number.class.isAssignableFrom(elementType))
      return (obj, name, val) ->
          obj.addArray(name, arr -> ((Collection<Number>) val).forEach(arr::addNumber));
    if (elementType == String.class)
      return (obj, name, val) ->
          obj.addArray(name, arr -> ((Collection<String>) val).forEach(arr::addString));
    if (elementType.isEnum())
      return (obj, name, val) ->
          obj.addArray(
              name, arr -> ((Collection<Enum<?>>) val).forEach(e -> arr.addString(e.name())));
    if (JsonBuilder.JsonEncodable.class.isAssignableFrom(elementType))
      return (obj, name, val) ->
          obj.addArray(
              name,
              arr ->
                  ((Collection<? extends JsonBuilder.JsonEncodable>) val)
                      .forEach(e -> e.addTo(arr)));
    return null;
  }

  private static void addJacksonMapped(
      JsonBuilder.JsonObjectBuilder obj, String name, Object value) {
    try {
      String json = FALLBACK_MAPPER.writeValueAsString(value);
      obj.addMember(name, JsonNode.of(json));
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
