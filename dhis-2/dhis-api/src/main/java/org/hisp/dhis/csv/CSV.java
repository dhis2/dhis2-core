package org.hisp.dhis.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.UID;
import org.intellij.lang.annotations.Language;

/**
 * A utility to read CSV into record POJOs.
 *
 * <p>This assumes all properties of the constructed POJOs are simple in nature.
 *
 * @author Jan Bernitt
 *
 * @since 2.43
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CSV {

  @FunctionalInterface
  public interface CsvReader<T> {

    @Nonnull
    List<T> list();

    @Nonnull
    default Stream<T> stream() {
      return list().stream();
    }
  }

  /**
   * Allows to configure the expected CSV formatting before starting to read the input.
   */
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
  private static final Map<Class<?>, Mapping<?>> MAPPINGS = new ConcurrentHashMap<>();

  private static <C> void add(Class<C> type, Function<String, C> deserializer) {
    DESERIALIZERS.put(type, deserializer);
  }
  static {
    add(String.class, Function.identity());
    add(Character.class, str -> str.isEmpty() ? null : str.charAt(0));
    add(char.class, str -> str.charAt(0));
    add(Integer.class, Integer::parseInt);
    add(int.class, Integer::parseInt);
    add(Long.class, Long::parseLong);
    add(long.class, Long::parseLong);
    add(Float.class, Float::parseFloat);
    add(float.class, Float::parseFloat);
    add(Double.class, Double::parseDouble);
    add(double.class, Double::parseDouble);
    add(Boolean.class, Boolean::parseBoolean);
    add(boolean.class, Boolean::parseBoolean);
    add(UID.class, UID::of);
  }

  private record Column<T>(String name, boolean required, Function<String, T> deserializer) {
  }
  private record Mapping<T extends Record>(Class<T> type, List<Column<?>> columns) {

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
              args[compIdx] = column.deserializer.apply(value);
            }
          }
          try {
            return c.newInstance(args);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }

    private void checkRequired(int[] compIdxByColIdx) {
      List<String> required = columns.stream().filter(Column::required).map(Column::name).toList();
      List<String> present = IntStream.of(compIdxByColIdx).filter(i -> i >= 0).mapToObj(columns::get).map(Column::name).toList();
      if (!present.containsAll(required)) throw new IllegalArgumentException("Required columns are: "+required);
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
    return Stream.of(type.getRecordComponents()).map(RecordComponent::getType).toArray(Class<?>[]::new);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Record> Mapping<T> mapping(Class<T> type) {
    return (Mapping<T>) MAPPINGS.computeIfAbsent(type, t -> new Mapping<>(type, columns(type)));
  }

  private static List<Column<?>> columns(Class<? extends Record> type) {
    return List.copyOf(
        Stream.of(type.getRecordComponents())
            .map(
                c ->
                    new Column<>(
                        c.getName(),
                        c.getType().isPrimitive() || c.isAnnotationPresent(Nonnull.class),
                        DESERIALIZERS.get(c.getType())))
            .toList());
  }

  private record Config(BufferedReader csv) implements CsvConfig {

    @Nonnull
    @Override
    public <T extends Record> CsvReader<T> as(Class<T> type) {
      return new Reader<>(csv, mapping(type));
    }
  }

  private record Reader<T extends Record>(BufferedReader csv, Mapping<T> as) implements CsvReader<T> {

    @Nonnull
    @Override
    public List<T> list() {
      try {
        String header = csv.readLine();
        if (header == null) throw new IllegalArgumentException("No header line provided.");
        List<String> columns = List.of(header.split(","));
        List<T> values = new ArrayList<>();
        Function<List<String>, T> creator = as.from(columns);
        LineBuffer buf = LineBuffer.of(columns);
        while (buf.readLine(csv)) values.add(creator.apply(buf.split()));
        return values;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } finally {
        if (csv != null) {
          try {
            csv.close();
          } catch (IOException e) {
            // ignore
          }
        }
      }
    }
  }

}
