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
package org.hisp.dhis.gist;

import static org.hisp.dhis.json.JsonStreamOutput.addArrayElements;
import static org.hisp.dhis.json.JsonStreamOutput.addObjectMembers;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.csv.CsvBuilder;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.object.ObjectOutput;

/**
 * Utility class responsible for all details concerning the serialisation of Gist API data as JSON.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GistOutput {

  private static final JsonBuilder.PrettyPrint FORMAT =
      new JsonBuilder.PrettyPrint(0, 0, false, true, true);

  public static void toCsv(@Nonnull GistObject.Output obj, @Nonnull OutputStream out) {
    try (PrintWriter csv = new PrintWriter(out)) {
      new CsvBuilder(csv).toRows(obj.paths(), Stream.of(obj.values()));
    }
  }

  public static void toCsv(@Nonnull GistObjectList.Output list, @Nonnull OutputStream out) {
    try (PrintWriter csv = new PrintWriter(out)) {
      new CsvBuilder(csv).skipHeaders(list.headless()).toRows(list.paths(), list.values());
    }
  }

  public static void toJson(@Nonnull GistObject.Output obj, @Nonnull OutputStream out) {
    List<ObjectOutput.Property> properties = obj.properties();
    if (properties.size() == 1) {
      // only write the property value itself
      // by first writing the array of objects (to a string buffer)
      // and then navigating to the value,
      // so we use the same code to write the actual values
      Stream<IntFunction<Object>> values = Stream.of(i -> obj.values()[i]);
      JsonNode array =
          JsonBuilder.createArray(FORMAT, arr -> addArrayElements(arr, properties, values));
      JsonValue value = JsonMixed.of(array).getObject(0).get(properties.get(0).path());
      try (PrintWriter json = new PrintWriter(out)) {
        json.append(value.toJson());
      }
    } else {
      IntFunction<Object> values = (i -> obj.values()[i]);
      JsonBuilder.streamObject(FORMAT, out, root -> addObjectMembers(root, properties, values));
    }
  }

  public static void toJson(@Nonnull GistObjectList.Output list, @Nonnull OutputStream out) {
    Stream<IntFunction<Object>> values = list.values().map(arr -> (i -> arr[i]));
    if (list.headless()) {
      JsonBuilder.streamArray(FORMAT, out, addObjectListElements(list, values));
      return;
    }
    GistPager pager = list.pager();
    JsonBuilder.streamObject(
        FORMAT,
        out,
        obj -> {
          if (pager != null)
            obj.addObject(
                "pager",
                p -> {
                  p.addNumber("page", pager.page());
                  p.addNumber("pageSize", pager.pageSize());
                  p.addNumber("total", pager.total());
                  p.addNumber("pageCount", pager.getPageCount());
                  p.addString("prevPage", pager.prevPage());
                  p.addString("nextPage", pager.nextPage());
                });
          obj.addArray(list.collectionName(), addObjectListElements(list, values));
        });
  }

  @Nonnull
  private static Consumer<JsonBuilder.JsonArrayBuilder> addObjectListElements(
      GistObjectList.Output list, Stream<IntFunction<Object>> values) {
    List<ObjectOutput.Property> properties = list.properties();
    return arr -> {
      if (properties.size() == 1) {
        // list each only as the property itself
        // to do that, first list objects but to written to a string
        // then iterate the values from the array
        String path = properties.get(0).path();
        JsonNode array =
            JsonBuilder.createArray(temp -> addArrayElements(temp, properties, values));
        array.elements().forEach(e -> arr.addElement(e.get(path)));
      } else {
        // list each as an object with the given properties
        addArrayElements(arr, properties, values);
      }
    };
  }
}
