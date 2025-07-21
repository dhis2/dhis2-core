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
package org.hisp.dhis.csv;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.intellij.lang.annotations.Language;

/**
 * A utility to read CSV into record POJOs.
 *
 * <p>This assumes all properties of the constructed POJOs are simple in nature.
 *
 * <p>Required properties either use a primitive type or are annotated with {@link Nonnull} or
 * {@link org.hisp.dhis.common.OpenApi.Required}.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CSV {

  @FunctionalInterface
  public interface CsvReader<T> extends Iterable<T> {

    @Nonnull
    default List<T> list() {
      return stream().toList();
    }

    @Nonnull
    default Stream<T> stream() {
      return StreamSupport.stream(spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
  }

  /** Allows to configure the expected CSV formatting before starting to read the input. */
  public interface CsvConfig {

    @Nonnull
    <T extends Record> CsvReader<T> as(Class<T> type);
  }

  public static CsvConfig of(InputStream csv) {
    return new Config(new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8)));
  }

  public static CsvConfig of(@Language("csv") String csv) {
    return new Config(new BufferedReader(new StringReader(csv)));
  }

  private static final Map<Class<?>, Function<String, ?>> DESERIALIZERS = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Columns<?>> MAPPINGS = new ConcurrentHashMap<>();

  private static <C> void addDeserializer(Class<C> type, Function<String, C> deserializer) {
    DESERIALIZERS.put(type, deserializer);
  }

  static {
    addDeserializer(String.class, Function.identity());
    addDeserializer(Character.class, str -> str.isEmpty() ? null : str.charAt(0));
    addDeserializer(char.class, str -> str.charAt(0));
    addDeserializer(Integer.class, Integer::parseInt);
    addDeserializer(int.class, Integer::parseInt);
    addDeserializer(Long.class, Long::parseLong);
    addDeserializer(long.class, Long::parseLong);
    addDeserializer(Float.class, Float::parseFloat);
    addDeserializer(float.class, Float::parseFloat);
    addDeserializer(Double.class, Double::parseDouble);
    addDeserializer(double.class, Double::parseDouble);
    addDeserializer(Boolean.class, Boolean::parseBoolean);
    addDeserializer(boolean.class, Boolean::parseBoolean);
    addDeserializer(UID.class, UID::of);
    // TODO maybe also support that when a map is found it can be marked as target to gobble up all
    // column values that are not mapped otherwise?
    // could that be used for the C=CO thing?
    addDeserializer(
        Map.class,
        map ->
            Stream.of(map.split("\\s+"))
                .collect(
                    toMap(
                        kv -> kv.substring(0, kv.indexOf('=')),
                        kv -> kv.substring(kv.indexOf('=') + 1))));
  }

  private record Column<T>(String name, boolean required, Function<String, T> deserializer) {}

  private record Columns<T extends Record>(Class<T> type, List<Column<?>> columns) {

    Function<List<String>, T> from(List<String> header) {
      int[] compIdxByColIdx = compIdxByColIdx(header);
      checkRequired(compIdxByColIdx);
      Class<?>[] types = getComponentTypes(type);
      try {
        Constructor<T> c = type.getDeclaredConstructor(types);
        return values -> {
          Object[] args = new Object[types.length];
          for (int i = 0; i < values.size(); i++) {
            int compIdx = compIdxByColIdx[i];
            if (compIdx >= 0) {
              Column<?> column = columns.get(compIdx);
              String value = values.get(i);
              if (column.required && value == null)
                throw new IllegalArgumentException(
                    "Column %s is required and cannot be empty".formatted(column.name));
              args[compIdx] = value == null ? null : column.deserializer.apply(value);
            }
          }
          try {
            return c.newInstance(args);
          } catch (Exception ex) {
            // should be impossible as long as there is no custom validation in the constructor
            // throwing an exception, in which case the arguments are not proper, so this wraps as:
            throw new IllegalArgumentException(ex);
          }
        };
      } catch (NoSuchMethodException ex) {
        // should be impossible since it uses the record canonical constructor
        throw new NoSuchElementException(ex);
      }
    }

    private void checkRequired(int[] compIdxByColIdx) {
      List<String> required = columns.stream().filter(Column::required).map(Column::name).toList();
      List<String> present =
          IntStream.of(compIdxByColIdx)
              .filter(i -> i >= 0)
              .mapToObj(columns::get)
              .map(Column::name)
              .toList();
      if (!present.containsAll(required))
        throw new IllegalArgumentException(
            "Required columns missing: "
                + required.stream().filter(not(present::contains)).toList());
    }

    private int[] compIdxByColIdx(List<String> columns) {
      Map<String, Integer> compIdxByName = compIdxByName();
      int[] res = new int[columns.size()];
      for (int i = 0; i < columns.size(); i++) {
        String name = columns.get(i);
        Integer compIdx = compIdxByName.get(name);
        if (compIdx == null) compIdx = compIdxByName.get(name.toLowerCase());
        if (compIdx == null) compIdx = -1;
        res[i] = compIdx;
      }
      return res;
    }

    @Nonnull
    private Map<String, Integer> compIdxByName() {
      RecordComponent[] components = type.getRecordComponents();
      Map<String, Integer> res = new HashMap<>();
      for (int i = 0; i < components.length; i++) {
        RecordComponent c = components[i];
        res.put(c.getName(), i);
        res.put(c.getName().toLowerCase(), i);
      }
      return res;
    }
  }

  private static Class<?>[] getComponentTypes(Class<? extends Record> type) {
    return Stream.of(type.getRecordComponents())
        .map(RecordComponent::getType)
        .toArray(Class<?>[]::new);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Record> Columns<T> toColumnsCached(Class<T> type) {
    return (Columns<T>) MAPPINGS.computeIfAbsent(type, t -> new Columns<>(type, toColumns(type)));
  }

  private static List<Column<?>> toColumns(Class<? extends Record> type) {
    return List.copyOf(Stream.of(type.getRecordComponents()).map(CSV::toColumn).toList());
  }

  @Nonnull
  private static Column<?> toColumn(RecordComponent c) {
    Class<?> type = c.getType();
    Function<String, ?> deserializer = DESERIALIZERS.get(type);
    if (deserializer == null)
      throw new UnsupportedOperationException("%s is not supported".formatted(type));
    boolean required =
        type.isPrimitive()
            || c.isAnnotationPresent(Nonnull.class)
            || c.isAnnotationPresent(OpenApi.Required.class);
    return new Column<>(c.getName(), required, deserializer);
  }

  private record Config(BufferedReader csv) implements CsvConfig {

    @Nonnull
    @Override
    public <T extends Record> CsvReader<T> as(Class<T> type) {
      return new Reader<>(csv, toColumnsCached(type));
    }
  }

  private record Reader<T extends Record>(BufferedReader csv, Columns<T> as)
      implements CsvReader<T> {

    @Nonnull
    @Override
    public Iterator<T> iterator() {
      String header = null;
      try {
        header = csv.readLine();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (header == null) throw new IllegalArgumentException("No header line provided.");
      List<String> columns = List.of(header.split(","));
      Function<List<String>, T> creator = as.from(columns);
      LineBuffer buf = LineBuffer.of(columns);
      return new Iterator<>() {
        Boolean next;

        @Override
        public boolean hasNext() {
          try {
            next = buf.readLine(csv);
            return next;
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }

        @Override
        public T next() {
          if (next == null) next = hasNext();
          if (!next) throw new NoSuchElementException("No more rows in the CSV.");
          return creator.apply(buf.split());
        }
      };
    }
  }
}
