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
package org.hisp.dhis.sql;

import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;

/**
 * A minimal API layer to build and execute queries which can operate on different implementation
 * APIs such as hibernate native or JDBC.
 *
 * <p>The goal is to decouple native SQL usage from a specific framework as well as provide a more
 * robust and convenient API.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SQL {

  /**
   * A value binding for a named parameter in a query as indicated by a {@code :name} placeholder in
   * the SQL.
   *
   * @param type SQL equivalent type of the named parameter
   * @param name name use in the SQL (without the colon)
   * @param value value to set for this named parameter (can be null, e.g. when a filter should
   *     match columns that are null)
   */
  public record Param(@Nonnull Type type, @Nonnull String name, @CheckForNull Object value) {

    @RequiredArgsConstructor
    public enum Type {
      BOOLEAN(Boolean.class),
      INTEGER(Integer.class),
      LONG(Long.class),
      STRING(String.class),
      DATE(Date.class),
      LONG_ARRAY(Long[].class),
      STRING_ARRAY(String[].class);

      /**
       * The type that a {@link Param#value()} is expected to have to be consistent with the {@link
       * Param#type()}
       */
      final Class<?> valueType;

      public boolean isArray() {
        return this == LONG_ARRAY || this == STRING_ARRAY;
      }

      public Type elementType() {
        return switch (this) {
          case STRING_ARRAY -> STRING;
          case LONG_ARRAY -> LONG;
          default -> this;
        };
      }
    }

    public Param {
      requireNonNull(type);
      requireNonNull(name);
      if (value != null && type.valueType != value.getClass())
        throw new IllegalArgumentException(
            "Value must be of type %s but was: %s".formatted(type.valueType, value.getClass()));
    }
  }

  /**
   * Creates a {@link QueryAPI} that allows to "spy" on the SQL and parameters forwarded to an
   * implementation during testing. This allows to unit test the SQL generation without actually
   * running the query or loading the framework that could run it.
   *
   * @param sqlSpy the target to accept the SQL
   * @param paramsSpy the target to accept parameters
   * @return a spy {@link QueryAPI} implementation
   */
  @Nonnull
  public static QueryAPI spy(Consumer<String> sqlSpy, BiConsumer<String, SQL.Param> paramsSpy) {
    return sql -> {
      sqlSpy.accept(sql);
      return new SpyQuery(paramsSpy);
    };
  }

  /**
   * Creates a new {@link QueryBuilder} utility around a given SQL query for a given implementation
   * API.
   *
   * @param sql the SQL used by the builder as a basis. It might get reduced and extended before it
   *     is passed on to given {@link QueryAPI#createQuery(String)} once a terminal method like
   *     {@link QueryBuilder#stream()} is called.
   * @param api the implementation or backend to use to execute the query
   * @return a new builder using the given SQL as starting point
   */
  public static QueryBuilder of(@Language("sql") String sql, @Nonnull QueryAPI api) {
    return new QueryBuilder(sql, api);
  }

  /** Facade for specific implementations of query APIs, such as hibernate or JDBC. */
  @FunctionalInterface
  public interface QueryAPI {

    /**
     * Factory method to create a new query based on the given SQL.
     *
     * @param sql The SQL as it should run against the DB using the implementation this API reflects
     * @return the {@link Query} facade created for the given SQL query
     */
    Query createQuery(@Nonnull String sql);
  }

  /** A minimal API that acts as the adapter to underlying implementations. */
  public interface Query {

    /**
     * Will only be called for named parameters that remain in the SQL as passed to {@link
     * QueryAPI#createQuery(String)}.
     *
     * @param param a named parameter to set
     * @return self for chaining
     */
    Query setParameter(@Nonnull Param param);

    /**
     * Maximum number of matches that should be returned. Is only called when a limit is set. Zero
     * or negative numbers cause undefined behaviour.
     *
     * @param n maximum number of matches to fetch
     * @return self for chaining
     */
    Query setLimit(int n);

    /**
     * Offset to the first result that should be returned. Is only called when an offset is set.
     * Negative numbers cause undefined behaviour.
     *
     * @param n offset to the first result, for example 0 to not use any offset at all
     * @return self for chaining
     */
    Query setOffset(int n);

    /**
     * Execute the query and return results as a stream of objects of the given type. This assumes
     * the query was created from a {@code SELECT} SQL query.
     *
     * <p>The type mapping depends on the underlying implementation and will only work if that type
     * works "out of the box" with that implementation.
     *
     * <p>A special case is {@code Object[]} as type which should be supported by all
     * implementations to simply put each row's values in an array. The returned array may be reused
     * as a row container, therefor it must be mapped immediately to some other type in a way that
     * no longer referenced the array itself beyond the function that maps a single row.
     *
     * @param of type of objects to return
     * @return the result stream
     */
    <T> Stream<T> stream(@Nonnull Class<T> of);

    /**
     * Execute the query and return results as a stream mapping each row to an object using the
     * given function. This assumes the query was created from a {@code SELECT} SQL query.
     *
     * <p>Each row of the result is mapped from a {@link Row} to the target object type. The {@link
     * Row} should be understood as if it was an access API to an {@code Object[]} that holds the
     * values for the different columns of the row. In some implementations this might actually be
     * what happens, in others the column values might be extracted more directly from the DB
     * response without materializing them in an intermediate data structure. This means the {@link
     * Row} based mapping is potentially more efficient than using {@link #stream(Class)} with
     * {@code Object[]} while also providing convenience methods to avoid the need to cast in the
     * mapper function.
     *
     * @param of type of objects to return
     * @return the result stream
     */
    <T> Stream<T> stream(Function<Row, T> map);

    /**
     * Execute the query and return a row count result. This assumes the query was created from a
     * {@code SELECT count(...)} SQL query.
     *
     * @return number of matching rows
     */
    int count();
  }

  /**
   * Reading values for a result row, index is zero based.
   *
   * <p>SQL NULL always translates to a Java {@code null} return value. Or in other words the access
   * API reflects exactly what the DB returned.
   */
  public interface Row {
    /**
     * Note that {@link #getArray(int)} (or methods based on it) must be used on columns returning
     * an array.
     *
     * @param index zero based column index
     * @return the value as returned by DB (null only if the DB returned null)
     */
    Object getObject(int index);

    /**
     * Note that {@link #getObject(int)} (or methods based on it) must be used on columns returning
     * a scalar value.
     *
     * @param index zero based column index
     * @return the value as returned by DB (null only if the DB returned null)
     */
    Object[] getArray(int index);

    /*
    Convenience methods to keep casts in one place...
     */

    /**
     * DB must have returned a string or a {@link ClassCastException} will occur.
     *
     * @param index zero based column index
     * @return string value or null, if the DB returned null
     */
    default String getString(int index) {
      return (String) getObject(index);
    }

    /**
     * DB must have returned an integer equivalent value or a {@link ClassCastException} will occur.
     *
     * @param index zero based column index
     * @return integer value or null, if the DB returned null
     */
    default Integer getInteger(int index) {
      return (Integer) getObject(index);
    }

    /**
     * DB must have returned a long equivalent value or a {@link ClassCastException} will occur.
     *
     * @param index zero based column index
     * @return long value or null, if the DB returned null
     */
    default Long getLong(int index) {
      return (Long) getObject(index);
    }

    /**
     * DB must have returned a {@link Date} equivalent value (timestamp) or a {@link
     * ClassCastException} will occur.
     *
     * @param index zero based column index
     * @return {@link Date} value or null, if the DB returned null
     */
    default Date getDate(int index) {
      return (Date) getObject(index);
    }

    /**
     * DB must have returned a boolean equivalent value or a {@link ClassCastException} will occur.
     *
     * @param index zero based column index
     * @return boolean value or null, if the DB returned null
     */
    default Boolean getBoolean(int index) {
      return (Boolean) getObject(index);
    }

    /**
     * DB must have returned a string array value or a {@link ClassCastException} will occur.
     *
     * @param index zero based column index
     * @return string array value or null, if the DB returned null
     */
    default String[] getStringArray(int index) {
      return (String[]) getArray(index);
    }

    /**
     * DB must have returned a long array equivalent value or a {@link ClassCastException} will
     * occur.
     *
     * @param index zero based column index
     * @return long array value or null, if the DB returned null
     */
    default Long[] getLongArray(int index) {
      return (Long[]) getArray(index);
    }
  }

  /** A {@link Query} to simply record set params to verify them in tests. */
  private record SpyQuery(BiConsumer<String, SQL.Param> paramsSpy) implements Query {

    @Override
    public SQL.Query setParameter(@Nonnull Param param) {
      paramsSpy.accept(param.name, param);
      return this;
    }

    @Override
    public SQL.Query setLimit(int n) {
      // has no effect
      return this;
    }

    @Override
    public SQL.Query setOffset(int n) {
      // has no effect
      return this;
    }

    @Override
    public <T> Stream<T> stream(Function<Row, T> map) {
      return Stream.empty();
    }

    @Override
    public <T> Stream<T> stream(@Nonnull Class<T> of) {
      return Stream.empty();
    }

    @Override
    public int count() {
      return 0;
    }
  }
}
