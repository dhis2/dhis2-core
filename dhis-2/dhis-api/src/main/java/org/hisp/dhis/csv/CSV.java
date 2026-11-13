/*
 * Copyright (c) 2004-2026, University of Oslo
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
import lombok.RequiredArgsConstructor;
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
 * <h2>RFC 4180 conformance</h2>
 *
 * <p>Well-formed <a href="https://www.rfc-editor.org/rfc/rfc4180">RFC 4180</a> CSV is read as
 * specified. The following behaviors, listed for the avoidance of doubt, are all standard.
 *
 * <pre>{@code
 * +-----------------------------+-------------------------------------------------+
 * | Behavior                    | Notes                                           |
 * +-----------------------------+-------------------------------------------------+
 * | Comma delimiter             | , separates fields; CRLF ends records.          |
 * | Double-quote quoting        | "..." wraps a field; "" parses to ".            |
 * | Line breaks in quoted field | Preserved verbatim; record continues next line. |
 * | Quoted empty value          | "" parses to ""; distinct from unquoted (null). |
 * | Last record w/o trailing NL | Accepted; file need not end in CRLF.            |
 * +-----------------------------+-------------------------------------------------+
 * }</pre>
 *
 * <p>The parser additionally accepts input that deviates from RFC 4180. The deviations are
 * deliberate, driven by the real-world inputs this reader consumes (Excel exports, hand-edited
 * files, data value import).
 *
 * <pre>{@code
 * +--------------------+--------------------------------+------------------------------------+
 * | Deviation          | RFC 4180                       | This parser                        |
 * +--------------------+--------------------------------+------------------------------------+
 * | Whitespace         | "Spaces are considered part    | Leading/trailing space and tab are |
 * |                    | of a field and should not be   | stripped from unquoted fields and  |
 * |                    | ignored." (§2.6)               | header names. Quote to preserve.   |
 * | Line endings       | CRLF only.                     | CRLF, LF and CR are accepted.      |
 * | Blank lines        | Not specified.                 | Skipped, along with rows of only   |
 * |                    |                                | delimiters and whitespace.         |
 * | Empty unquoted     | Empty string.                  | null. A quoted "" is still "".     |
 * | Header row         | Optional.                      | Required; names map CSV columns to |
 * |                    |                                | record components.                 |
 * | Column count       | "Each line should contain the  | Short rows accepted (missing       |
 * |                    | same number of fields" (§2.4). | fields are null); long rows are    |
 * |                    |                                | rejected.                          |
 * | Header names       | Not addressed.                 | Matched case-insensitively.        |
 * | Whitespace after   | A quoted field must be         | Space and tab between the closing  |
 * | closing quote      | followed by a comma or line    | quote and the delimiter or line    |
 * |                    | break.                         | end are tolerated.                 |
 * +--------------------+--------------------------------+------------------------------------+
 * }</pre>
 *
 * <h2>Extensions</h2>
 *
 * <ul>
 *   <li><b>Required columns</b>: a record component is required when its type is primitive or it is
 *       annotated {@link Nonnull} or {@link OpenApi.Required}. A missing required column is
 *       reported before any data rows are read.
 *   <li><b>{@link Any}</b>: a {@link java.util.Map} component annotated {@link Any} receives every
 *       CSV column not mapped to another component, with optional {@link Any#ignore()} and {@link
 *       Any#ifAbsent()} filters.
 * </ul>
 *
 * <h2>Malformed input</h2>
 *
 * <p>Input that cannot have been produced by a conforming writer is rejected with an {@link
 * IllegalArgumentException}. The message identifies the line and column, for example {@code "CSV
 * input error at line 2 column 4: Quoted value is not terminated"}. The following are rejected:
 *
 * <ul>
 *   <li>a quote appearing inside an unquoted field ({@code ab"cd,ef}),
 *   <li>a non-whitespace, non-delimiter character after a closing quote ({@code "abc"def}),
 *   <li>an unterminated quoted value,
 *   <li>a value longer than {@link CsvConfig#maxValueBufferLength(int)},
 *   <li>a record with more fields than the header, and
 *   <li>a missing required column.
 * </ul>
 *
 * @author Jan Bernitt
 * @since 2.43
 * @see CsvConfig
 * @see CsvReader
 * @see Any
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CSV {

  /**
   * 50000 for no reason other than this is the longest any aggregate data value property allows.
   * Can be adjusted using {@link CsvConfig#maxValueBufferLength(int)}.
   */
  static final int DEFAULT_MAX_VALUE_LENGTH = 50000;

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

  /**
   * The API for the different terminal operations that read the data from the file.
   *
   * @param <T> row type
   */
  @FunctionalInterface
  public interface CsvReader<T> extends Iterable<T> {

    /**
     * @return reads CSV data into a list
     */
    @Nonnull
    default List<T> list() {
      return stream().toList();
    }

    /**
     * @return reads CSV data into a stream
     */
    @Nonnull
    default Stream<T> stream() {
      return StreamSupport.stream(spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
  }

  /** Allows to configure the expected CSV formatting before starting to read the input. */
  public interface CsvConfig {

    /**
     * Note the maximum length of the value itself is somewhat shorter depending on situation. At
     * the very least it is 1 character shorter to be able to find the comma or end of line after
     * the value. When quoted the maximum is 2 characters shorter for the quotes. If escaped quoted
     * are contained via pairs of quotes the actual value itself is shorter again as 2 characters
     * fold to 1 for each pair.
     *
     * @param maxLength the maximum buffer size to hold a single value
     * @return a new updated configuration for chaining
     */
    CsvConfig maxValueBufferLength(int maxLength);

    /**
     * Terminal operation of the configuration builder that creates the reader with the current
     * configuration
     *
     * @param type each row becomes an instance of this {@link Record} type
     * @return a reader
     * @param <T> row type
     */
    @Nonnull
    <T extends Record> CsvReader<T> as(Class<T> type);
  }

  public static CsvConfig of(InputStream csv) {
    return of(new InputStreamReader(csv, StandardCharsets.UTF_8));
  }

  public static CsvConfig of(@Language("csv") String csv) {
    return of(new StringReader(csv));
  }

  static CsvConfig of(java.io.Reader csv) {
    return new Config(csv, DEFAULT_MAX_VALUE_LENGTH);
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
    addDeserializer(org.hisp.dhis.common.Locale.class, org.hisp.dhis.common.Locale::of);
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

    Function<String[], T> from(String[] header) {
      int[] columnToCompIndex = columnToComponentIndexMapping(header);
      checkRequired(columnToCompIndex);
      Class<?>[] types = getComponentTypes(type);
      boolean skipAny = isSkipAny(header);
      try {
        Constructor<T> c = type.getDeclaredConstructor(types);
        return values -> {
          Object[] args = new Object[types.length];
          Map<String, String> anyValues = null;
          int maxColumns = Math.min(columnToCompIndex.length, values.length);
          for (int col = 0; col < maxColumns; col++) {
            int compIdx = columnToCompIndex[col];
            if (compIdx >= 0) {
              String value = values[col];
              Component<?> comp = components.get(compIdx);
              if (comp.any) {
                if (!skipAny) {
                  if (anyValues == null) {
                    anyValues = new HashMap<>(header.length);
                    args[compIdx] = anyValues;
                  }
                  anyValues.put(header[col], value);
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

    private boolean isSkipAny(String[] header) {
      Set<String> headers = Set.of(header);
      return !any
          || components.stream()
              .map(Component::anyIfAbsent)
              .filter(not(String::isEmpty))
              .anyMatch(headers::contains);
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

    private int[] columnToComponentIndexMapping(String[] columns) {
      Map<String, Integer> compIdxByName = compIdxByName();
      int[] res = new int[columns.length];
      for (int i = 0; i < columns.length; i++) {
        String column = columns[i];
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

  private record Config(java.io.Reader csv, int maxValueLength) implements CsvConfig {

    @Override
    public CsvConfig maxValueBufferLength(int maxLength) {
      return new Config(csv, maxLength);
    }

    @Nonnull
    @Override
    public <T extends Record> CsvReader<T> as(Class<T> type) {
      return new CSV.Reader<>(csv, getComponentsCached(type), maxValueLength);
    }
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

  @RequiredArgsConstructor
  private static final class Reader<T extends Record> implements CsvReader<T> {

    final java.io.Reader csv;
    final Components<T> as;
    final int maxValueLength;

    char[] buffer = new char[2048];
    boolean eoi;
    boolean twinQuotes;
    int len = 0;
    int pos = 0;
    // track lines and columns
    int line = 1;
    int col = 1;

    @Nonnull
    @Override
    public Iterator<T> iterator() {
      String[] headers = readHeaderRow();
      if (headers == null) throw new IllegalArgumentException("No header line provided.");
      Function<String[], T> newRecord = as.from(headers);
      return new Iterator<>() {
        final String[] row = new String[headers.length];
        Boolean next;

        @Override
        public boolean hasNext() {
          next = readDataRow(row);
          return next;
        }

        @Override
        public T next() {
          if (next == null || !next) throw new NoSuchElementException("No more rows in the CSV.");
          return newRecord.apply(row);
        }
      };
    }

    private void fillBufferTail(int keepLen) {
      try {
        int tailLen = buffer.length - keepLen;
        int readLen = csv.read(buffer, keepLen, tailLen);
        eoi = readLen == -1;
        len = eoi ? keepLen : keepLen + readLen;
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    private String[] readHeaderRow() {
      fillBufferTail(0);
      int from = skipLineBreak(skipIndent(0));
      List<String> header = new ArrayList<>();
      while (from < len && !isLineBreak(buffer[from])) {
        int to = skipValueGrowAndRefill(from);
        from = pos; // in case we did move/grow
        header.add(value(from, to));
        from = skipIndent(to);
        if (from < len && buffer[from] == ',') from++;
        col++;
      }
      pos = skipLineBreak(from);
      if (header.isEmpty()) throw error("No header row found");
      return header.toArray(String[]::new);
    }

    private boolean readDataRow(String[] row) {
      do {
        if (eoi && pos >= len) return false;
        int from = pos;
        col = 1;
        for (int i = 0; i < row.length; i++) {
          int to = skipValueGrowAndRefill(from);
          from = pos; // in case we did move/grow
          row[i] = value(from, to);
          from = skipIndent(to);
          if (from < len && buffer[from] == ',') from++;
          col++;
        }
        if (from < len && !isLineBreak(buffer[from])) throw error("Additional columns found");
        pos = skipLineBreak(from);
      } while (isEmpty(row));
      return true;
    }

    private int skipValueGrowAndRefill(int from) {
      pos = from;
      int to = -1;
      while (to == -1) {
        to = skipValue(pos);
        if (to == -1) {
          int keepLen = len - pos;
          if (keepLen == buffer.length) {
            // grow
            if (buffer.length >= maxValueLength) throw error("Value longer than " + maxValueLength);
            char[] b2 = new char[Math.min(DEFAULT_MAX_VALUE_LENGTH, buffer.length * 2)];
            System.arraycopy(buffer, pos, b2, 0, keepLen);
            buffer = b2;
          } else {
            // move
            System.arraycopy(buffer, pos, buffer, 0, keepLen);
          }
          pos = 0;
          // and fill
          fillBufferTail(keepLen);
        }
      }
      return to;
    }

    private boolean isEmpty(String[] row) {
      for (String s : row) if (s != null) return false;
      return true;
    }

    /**
     * @param p start pos
     * @return the pos after the value (, or line-break) or -1 if we ran out of buffer before we
     *     could find it
     */
    private int skipValue(int p) {
      boolean nonIndent = false;
      while (p < len) {
        char c = buffer[p];
        if (isValueSeparator(c)) return p;
        if (c == '"') {
          if (nonIndent) throw error("Quote inside unquoted value");
          p = skipQuotes(p + 1);
          if (p != -1) {
            p = skipIndent(p);
            if (p == len && !eoi) return -1; // refill to see what follows
            if (p < len && !isValueSeparator(buffer[p]))
              throw error("Unexpected character after closing quote");
          }
          return p;
        }
        nonIndent |= !isIndent(c);
        p++;
      }
      return eoi ? p : -1;
    }

    /**
     * Scans a quoted value starting immediately after its opening quote.
     *
     * @param p buffer position immediately after the opening quote
     * @return the position after the closing quote, or -1 if the buffer was exhausted before the
     *     closing quote could be found
     */
    private int skipQuotes(int p) {
      twinQuotes = false;
      while (p < len) {
        char c = buffer[p++];
        if (c == '"') {
          if (p < len && buffer[p] == '"') {
            twinQuotes = true;
            p++;
          } else if (p == len && !eoi) {
            return -1; // might still be a twin quote; refill and rescan
          } else {
            return p; // closing quote, either mid-buffer or at true EOI
          }
        }
      }
      if (eoi) throw error("Quoted value is not terminated");
      // closing quote not found in this buffer
      return -1;
    }

    private int skipIndent(int p) {
      while (p < len && isIndent(buffer[p])) p++;
      return p;
    }

    private int skipLineBreak(int p) {
      while (p < len && isLineBreak(buffer[p])) {
        if (buffer[p] == '\r') {
          p++;
          line++;
          if (p < len && buffer[p] == '\n') p++;
        } else if (buffer[p] == '\n') {
          p++;
          line++;
        }
      }
      return p;
    }

    private String value(int from, int to) {
      int f = skipIndent(from);
      int t = to;
      while (t > f && isIndent(buffer[t - 1])) t--;
      if (f >= t || (f < len && isValueSeparator(buffer[f]))) return null;
      if (buffer[f] == '"' && buffer[t - 1] == '"') {
        String str = new String(buffer, f + 1, t - f - 2);
        return twinQuotes ? str.replace("\"\"", "\"") : str;
      }
      return new String(buffer, f, t - f);
    }

    private IllegalArgumentException error(String msg) {
      return new IllegalArgumentException(
          "CSV input error at line %d column %d: %s".formatted(line, col, msg));
    }
  }

  private static boolean isValueSeparator(char c) {
    return c == ',' || isLineBreak(c);
  }

  private static boolean isIndent(char c) {
    return c == ' ' || c == '\t';
  }

  private static boolean isLineBreak(char c) {
    return c == '\r' || c == '\n';
  }
}
