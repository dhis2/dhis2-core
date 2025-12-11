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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.jsontree.JsonBuilder;

/**
 * A utility to support {@link Stream}-serialisation as JSON for generic inputs of property paths.
 *
 * <p>This makes sure that independent of the paths order given the created JSON follows that order
 * as close as possible without tearing objects appart.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonOutputUtils {

  /**
   * @apiNote The main goal of this interface is to decouple any conversion logic between Java types
   *     and JSON values from the algorithm used to write structured JSON. As an extra benefit this
   *     also makes sure the logic to find the right conversion for each property only has to run
   *     once.
   * @param <T> type of one element in the list that becomes a JSON object in an array
   */
  public interface JsonObjectAdder<T> {

    /**
     * Adds a value extracted from given element to the given parent JSON object.
     *
     * @param element an element in the stream converted to an array of JSON objects
     * @param name name of the property (simple name) in the parent
     * @param valueAt the index of the path (property) that should be extracted from the given
     *     element and written to the parent
     * @param parent the target of the addition for the value extracted from element for the
     *     property indicated by index
     */
    void add(T element, String name, int valueAt, JsonBuilder.JsonObjectBuilder parent);
  }

  public static <T> void addAsJsonObjects(
      List<String> paths,
      List<JsonObjectAdder<T>> adders,
      Stream<T> data,
      JsonBuilder.JsonArrayBuilder arr) {
    JsonObjectLayout.Object<T> element = createLayoutTree(paths, adders);
    data.forEach(e -> arr.addObject(obj -> element.add(obj, e)));
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
    JsonObjectLayout.Object<?> root = createLayoutTree(paths, List.of());
    // flatten the tree into a list of leaf properties
    List<String> sorted = new ArrayList<>(n);
    root.flatten(sorted);
    return List.copyOf(sorted);
  }

  @Nonnull
  private static <T> JsonObjectLayout.Object<T> createLayoutTree(
      List<String> paths, List<JsonObjectAdder<T>> adders) {
    JsonObjectLayout.Object<T> root = new JsonObjectLayout.Object<>("");
    int i = 0;
    for (String path : paths) {
      JsonObjectLayout.Object<T> parent = root;
      List<String> parentPath = parentPath(path);
      if (!parentPath.isEmpty()) {
        for (String name : parentPath) {
          int j = parent.indexOf(name);
          if (j < 0) {
            JsonObjectLayout.Object<T> p = new JsonObjectLayout.Object<>(name);
            parent.members.add(p);
            parent = p;
          } else {
            parent = (JsonObjectLayout.Object<T>) parent.members.get(j);
          }
        }
      }
      String name = propertyName(path);
      parent.members.add(
          new JsonObjectLayout.Property<>(i, path, name, i < adders.size() ? adders.get(i) : null));
      i++;
    }
    return root;
  }

  @Nonnull
  private static List<String> parentPath(String path) {
    List<String> segments = List.of(path.split("\\."));
    return segments.subList(0, segments.size() - 1);
  }

  @Nonnull
  private static String propertyName(String path) {
    return path.substring(path.lastIndexOf('.') + 1);
  }

  private sealed interface JsonObjectLayout<T> {

    String name();

    void flatten(List<String> into);

    void add(JsonBuilder.JsonObjectBuilder to, T data);

    record Object<T>(String name, List<JsonObjectLayout<T>> members)
        implements JsonObjectLayout<T> {
      Object(String name) {
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
      public void add(JsonBuilder.JsonObjectBuilder to, T data) {
        if (name.isEmpty()) {
          members.forEach(m -> m.add(to, data));
        } else {
          to.addObject(name, obj -> members.forEach(m -> m.add(obj, data)));
        }
      }
    }

    record Property<T>(int index, String path, String name, JsonObjectAdder<T> adder)
        implements JsonObjectLayout<T> {

      @Override
      public void flatten(List<String> into) {
        into.add(path);
      }

      @Override
      public void add(JsonBuilder.JsonObjectBuilder to, T data) {
        adder.add(data, name, index, to);
      }
    }
  }
}
