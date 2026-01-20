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
import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
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

  /**
   * Can be put on a {@link Map} component that will receive all colum values in the input that are
   * not mapped otherwise
   */
  @Target(ElementType.RECORD_COMPONENT)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Any {
    /**
     * @return lists names of columns that explicitly should not be put in the component otherwise
     *     collecting all names not mapped otherwise.
     */
    String[] ignore() default {};

    /**
     * @return the name of the column that when it is absent the property should receive all not
     *     mapped values. If the named column is present unmapped source columns will be ignored
     *     similar to not defining an any target component
     */
    String ifAbsent() default "";
  }

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
  private static final Map<Class<?>, Components<?>> MAPPINGS = new ConcurrentHashMap<>();

  private static <C> void addDeserializer(Class<C> type, Function<String, C> deserializer) {
    DESERIALIZERS.put(type, deserializer);
  }

  static {
    // "primitive" types supported
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
    addDeserializer(Locale.class, Locale::new);
  }

  private static Function<String, ?> getDeserializer(RecordComponent c) {
    Class<?> rawType = c.getType();
    Type type = c.getGenericType();
    if (type instanceof ParameterizedType pt) {
      if (rawType == Map.class)
        return getMapDeserializer((Class<?>) pt.getActualTypeArguments()[1]);
      if (rawType == Set.class)
        return getSetDeserializer((Class<?>) pt.getActualTypeArguments()[0]);
      if (rawType == List.class || rawType == Collection.class)
        return getListDeserializer((Class<?>) pt.getActualTypeArguments()[0]);
    }
    return getDeserializer(rawType);
  }

  @CheckForNull
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <E> Function<String, E> getDeserializer(Class<E> rawType) {
    if (rawType.isEnum()) return getEnumDeserializer((Class) rawType);
    return (Function<String, E>) DESERIALIZERS.get(rawType);
  }

  private static <E extends Enum<E>> Function<String, E> getEnumDeserializer(Class<E> valueType) {
    return enumValue -> Enum.valueOf(valueType, enumValue);
  }

  private static <E> Function<String, Map<String, E>> getMapDeserializer(Class<E> valueType) {
    Function<String, E> valueDeserializer = getDeserializer(valueType);
    if (valueDeserializer == null) return null;
    return mapValue ->
        splitOnIndent(mapValue).stream()
            .collect(
                toMap(
                    kv -> kv.substring(0, Math.max(0, kv.indexOf('='))),
                    kv -> valueDeserializer.apply(kv.substring(kv.indexOf('=') + 1))));
  }

  private static <E> Function<String, List<E>> getListDeserializer(Class<E> valueType) {
    Function<String, E> valueDeserializer = getDeserializer(valueType);
    if (valueDeserializer == null) return null;
    return listValue -> splitOnIndent(listValue).stream().map(valueDeserializer).toList();
  }

  private static <E> Function<String, Set<E>> getSetDeserializer(Class<E> valueType) {
    Function<String, E> valueDeserializer = getDeserializer(valueType);
    if (valueDeserializer == null) return null;
    return setValue -> splitOnIndent(setValue).stream().map(valueDeserializer).collect(toSet());
  }

  /**
   * A record component that is a target for a CSV column
   *
   * @param target the component set by a CSV column value
   * @param column name of the column that is stored in this component
   * @param required if the value is required
   * @param any if the component should receive all unmapped CSV columns as a {@link Map}
   * @param anyIfAbsent name of the column that when present disables the any-component as a target
   *     for unmapped columns
   * @param deserializer the function that deserializes the CSV string value into the type expected
   *     by the {@link #target()} component
   * @param <T> type of the value the target {@link RecordComponent} accepts
   */
  private record Component<T>(
      RecordComponent target,
      String column,
      boolean required,
      boolean any,
      String anyIfAbsent,
      Function<String, T> deserializer) {}

  /**
   * @param type row record type
   * @param any true if the type has an "any" component column
   * @param components the type's component info (index match their corresponding {@link
   *     RecordComponent})
   * @param <T> type of the created row record
   */
  private record Components<T extends Record>(
      Class<T> type, boolean any, List<Component<?>> components) {

    Components(Class<T> type, List<Component<?>> components) {
      this(type, components.stream().anyMatch(Component::any), components);
    }

    Function<List<String>, T> from(List<String> header) {
      int[] columnToCompIndex = columnToComponentIndexMapping(header);
      checkRequired(columnToCompIndex);
      Class<?>[] types = getComponentTypes(type);
      boolean skipAny = isSkipAny(header);
      try {
        Constructor<T> c = type.getDeclaredConstructor(types);
        return values -> {
          Object[] args = new Object[types.length];
          Map<String, String> anyValues = null;
          int maxColumns = Math.min(columnToCompIndex.length, values.size());
          for (int col = 0; col < maxColumns; col++) {
            int compIdx = columnToCompIndex[col];
            if (compIdx >= 0) {
              String value = values.get(col);
              Component<?> comp = components.get(compIdx);
              if (comp.any) {
                if (!skipAny) {
                  if (anyValues == null) {
                    anyValues = new HashMap<>(header.size());
                    args[compIdx] = anyValues;
                  }
                  anyValues.put(header.get(col), value);
                }
              } else {
                if (comp.required && value == null)
                  throw new IllegalArgumentException(
                      "Column %s is required and cannot be empty".formatted(comp.column));
                args[compIdx] =
                    value == null || "null".equals(value) ? null : comp.deserializer.apply(value);
              }
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

    private boolean isSkipAny(List<String> header) {
      return !any
          || components.stream()
              .map(Component::anyIfAbsent)
              .filter(not(String::isEmpty))
              .anyMatch(header::contains);
    }

    private void checkRequired(int[] compIdxByColIdx) {
      List<String> required =
          components.stream().filter(Component::required).map(Component::column).toList();
      List<String> present =
          IntStream.of(compIdxByColIdx)
              .filter(i -> i >= 0)
              .mapToObj(components::get)
              .map(Component::column)
              .toList();
      if (!present.containsAll(required))
        throw new IllegalArgumentException(
            "Required columns missing: "
                + required.stream().filter(not(present::contains)).toList());
    }

    private int[] columnToComponentIndexMapping(List<String> columns) {
      Map<String, Integer> compIdxByName = compIdxByName();
      int[] res = new int[columns.size()];
      for (int i = 0; i < columns.size(); i++) {
        String column = columns.get(i);
        Integer compIdx = compIdxByName.get(column);
        if (compIdx == null) compIdx = compIdxByName.get(column.toLowerCase());
        if (compIdx == null) compIdx = compIdxByName.get("*");
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
        if (isAnyTarget(c)) {
          res.put("*", i);
          Any info = c.getAnnotation(Any.class);
          if (info != null)
            for (String ignore : info.ignore()) {
              // explicitly map to -1 (not mapped)
              res.put(ignore, -1);
              res.put(ignore.toLowerCase(), -1);
            }
        } else {
          res.put(c.getName(), i);
          res.put(c.getName().toLowerCase(), i);
        }
      }
      return res;
    }
  }

  private static boolean isAnyTarget(RecordComponent c) {
    return c.getType() == Map.class && c.isAnnotationPresent(Any.class);
  }

  private static Class<?>[] getComponentTypes(Class<? extends Record> type) {
    return Stream.of(type.getRecordComponents())
        .map(RecordComponent::getType)
        .toArray(Class<?>[]::new);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Record> Components<T> getComponentsCached(Class<T> type) {
    return (Components<T>)
        MAPPINGS.computeIfAbsent(type, t -> new Components<>(type, getComponents(type)));
  }

  private static List<Component<?>> getComponents(Class<? extends Record> type) {
    return List.copyOf(Stream.of(type.getRecordComponents()).map(CSV::toComponent).toList());
  }

  @Nonnull
  private static Component<?> toComponent(RecordComponent c) {
    if (isAnyTarget(c)) return new Component<>(c, "*", false, true, getAnyIfAbsentName(c), null);
    Function<String, ?> deserializer = getDeserializer(c);
    if (deserializer == null)
      throw new UnsupportedOperationException("%s is not supported".formatted(c.getGenericType()));
    boolean required =
        c.getType().isPrimitive()
            || c.isAnnotationPresent(Nonnull.class)
            || c.isAnnotationPresent(OpenApi.Required.class);
    return new Component<>(c, c.getName(), required, false, "", deserializer);
  }

  private static String getAnyIfAbsentName(RecordComponent c) {
    Any any = c.getAnnotation(Any.class);
    return any == null ? "" : any.ifAbsent();
  }

  private record Config(BufferedReader csv) implements CsvConfig {

    @Nonnull
    @Override
    public <T extends Record> CsvReader<T> as(Class<T> type) {
      return new Reader<>(csv, getComponentsCached(type));
    }
  }

  private static String unquote(String str) {
    return str == null || !str.startsWith("\"") || !str.endsWith("\"")
        ? str
        : str.substring(1, str.length() - 1);
  }

  /**
   * @implNote a regex would work fine but might be used as attack vector, this is to make it safe
   *     to handle large crafted inputs
   */
  private static List<String> splitOnComma(String line) {
    if (line == null || line.isEmpty()) return List.of();

    int from = 0;
    int n = line.length();
    List<String> elems = new ArrayList<>();
    while (from < n) {
      // Skip leading indent
      while (from < n && isIndent(line.charAt(from))) from++;

      // Find the next comma or end of string
      int to = from;
      while (to < n && line.charAt(to) != ',') to++;

      // Trim trailing indent
      int toB = to;
      while (toB > from && isIndent(line.charAt(toB - 1))) toB--;

      elems.add(line.substring(from, toB));
      from = to + 1;
    }
    return elems;
  }

  /**
   * @implNote a regex would work fine but might be used as attack vector, this is to make it safe
   *     to handle large crafted inputs
   */
  private static List<String> splitOnIndent(String value) {
    if (value == null) return List.of();
    if (value.indexOf(' ') < 0 && value.indexOf('\t') < 0) return List.of(value);
    int from = 0;
    int n = value.length();
    List<String> elems = new ArrayList<>();
    while (from < n) {
      // Skip leading indent
      while (from < n && isIndent(value.charAt(from))) from++;

      int to = from;
      while (to < n && !isIndent(value.charAt(to))) to++;
      elems.add(value.substring(from, to));
      from = to + 1;
    }
    return elems;
  }

  private static boolean isIndent(char c) {
    return c == ' ' || c == '\t';
  }

  private record Reader<T extends Record>(BufferedReader csv, Components<T> as)
      implements CsvReader<T> {

    @Nonnull
    @Override
    public Iterator<T> iterator() {
      String header;
      try {
        header = csv.readLine();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (header == null) throw new IllegalArgumentException("No header line provided.");
      List<String> columns = splitOnComma(header).stream().map(CSV::unquote).toList();
      Function<List<String>, T> newRecord = as.from(columns);
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
          return newRecord.apply(buf.split());
        }
      };
    }
  }
}
