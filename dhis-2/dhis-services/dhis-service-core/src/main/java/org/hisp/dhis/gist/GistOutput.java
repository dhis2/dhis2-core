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

import static org.hisp.dhis.json.JsonOutputUtils.addAsJsonObjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.csv.CsvBuilder;
import org.hisp.dhis.json.JsonOutputUtils.JsonObjectAdder;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonBuilder.JsonObjectBuilder;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.DateUtils;

/**
 * Utility class responsible for all details concerning the serialisation of Gist API data as JSON.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GistOutput {

  private static final JsonBuilder.PrettyPrint LIST_FORMAT =
      new JsonBuilder.PrettyPrint(0, 0, false, true, true);
  private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper();

  private static final Map<Class<?>, JsonObjectAdder<Object[]>> ADDERS_BY_TYPE =
      new ConcurrentHashMap<>();

  private static <T> void register(Class<T> type, Function<? super T, String> toString) {
    register(type, (obj, name, value) -> obj.addString(name, toString.apply(value)));
  }

  private static <T> void register(Class<T> type, BiConsumer<T, JsonObjectBuilder> toObj) {
    register(type, (obj, name, value) -> obj.addObject(name, o -> toObj.accept(value, o)));
  }

  private static <T> void register(Class<T> type, JsonObjectBuilder.AddMember<? super T> adder) {
    register(type, (e, name, i, obj) -> adder.add(obj, name, type.cast(e[i])));
  }

  private static <T> void register(Class<T> type, JsonObjectAdder<Object[]> adder) {
    ADDERS_BY_TYPE.put(type, adder);
  }

  static {
    register(String.class, JsonObjectBuilder::addString);
    register(UID.class, UID::getValue);
    register(Date.class, DateUtils::toIso8601);
    register(Enum.class, Enum::name);
    register(Period.class, Period::getIsoDate);
    register(PeriodType.class, PeriodType::getName);
    register(Integer.class, JsonObjectBuilder::addNumber);
    register(int.class, JsonObjectBuilder::addNumber);
    register(Long.class, JsonObjectBuilder::addNumber);
    register(long.class, JsonObjectBuilder::addNumber);
    register(Double.class, JsonObjectBuilder::addNumber);
    register(double.class, JsonObjectBuilder::addNumber);
    register(Float.class, JsonObjectBuilder::addNumber);
    register(float.class, JsonObjectBuilder::addNumber);
    register(Boolean.class, JsonObjectBuilder::addBoolean);
    register(boolean.class, JsonObjectBuilder::addBoolean);
    register(JsonBuilder.JsonEncodable.class, GistOutput::addJsonEncodable);
    register(Object.class, GistOutput::addJacksonMapped);
  }

  public static void toCsv(@Nonnull GistObjectList.Output list, @Nonnull OutputStream out) {
    try (PrintWriter csv = new PrintWriter(out)) {
      new CsvBuilder(csv)
        .skipHeaders(list.headless())
        .toRows(list.paths(), list.values());
    }
  }

  public static void toJson(@Nonnull GistObjectList.Output list, @Nonnull OutputStream out) {
    List<String> paths = list.paths();
    List<JsonObjectAdder<Object[]>> adders = toAdders(list.valueTypes());
    Stream<Object[]> values = list.values();
    if (list.headless()) {
      JsonBuilder.streamArray(
          LIST_FORMAT, out, arr -> addAsJsonObjects(paths, adders, values, arr));
      return;
    }
    GistPager pager = list.pager();
    JsonBuilder.streamObject(
        LIST_FORMAT,
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
          obj.addArray(list.collectionName(), arr -> addAsJsonObjects(paths, adders, values, arr));
        });
  }

  private static List<JsonObjectAdder<Object[]>> toAdders(List<GistObjectList.Type> valueTypes) {
    return valueTypes.stream().map(GistOutput::toAdder).toList();
  }

  private static JsonObjectAdder<Object[]> toAdder(GistObjectList.Type valueType) {
    Class<?> type = valueType.rawType();
    JsonObjectAdder<Object[]> adder = ADDERS_BY_TYPE.get(type);
    if (adder != null) return adder;
    if (type.isEnum()) return ADDERS_BY_TYPE.get(Enum.class);
    if (JsonBuilder.JsonEncodable.class.isAssignableFrom(type))
      return ADDERS_BY_TYPE.get(JsonBuilder.JsonEncodable.class);
    Class<?> elementType = valueType.elementType();
    if (Collection.class.isAssignableFrom(type) && elementType != null) {
      if (Number.class.isAssignableFrom(elementType))
        return toAdder(
            Number.class, c -> c.toArray(Number[]::new), JsonBuilder.JsonArrayBuilder::addNumbers);
      if (elementType == String.class)
        return toAdder(
            String.class, c -> c.toArray(String[]::new), JsonBuilder.JsonArrayBuilder::addStrings);
      if (elementType.isEnum())
        return toAdder(
            Enum.class,
            c -> c.stream().map(Enum::name).toArray(String[]::new),
            JsonBuilder.JsonArrayBuilder::addStrings);
    }
    return ADDERS_BY_TYPE.get(Object.class);
  }

  @SuppressWarnings("unchecked")
  private static <T, E> JsonObjectAdder<Object[]> toAdder(
      Class<T> elementType,
      Function<Collection<T>, E[]> map,
      BiConsumer<JsonBuilder.JsonArrayBuilder, E[]> add) {
    return (e, name, i, obj) ->
        obj.addArray(name, arr -> add.accept(arr, map.apply((Collection<T>) e[i])));
  }

  private static void addJsonEncodable(
      Object[] data, String name, int valueAt, JsonObjectBuilder obj) {
    obj.addMember(name, (JsonBuilder.JsonEncodable) data[valueAt]);
  }

  private static void addJacksonMapped(
      Object[] data, String name, int valueAt, JsonObjectBuilder obj) {
    Object value = data[valueAt];
    try {
      String json = FALLBACK_MAPPER.writeValueAsString(value);
      obj.addMember(name, JsonNode.of(json));
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

}
