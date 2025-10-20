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
 * A minimal API layer to SELECT queries only that can operate on both hibernate and JDBC.
 *
 * <p>The goal is to decouple native SQL usage from a specific framework as well as provide a more
 * robust and convenient API.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SQL {

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

  @Nonnull
  public static QueryAPI of(Consumer<String> sqlSpy, BiConsumer<String, SQL.Param> paramsSpy) {
    return sql -> {
      sqlSpy.accept(sql);
      return new SpyQuery(paramsSpy);
    };
  }

  public static QueryBuilder selectOf(@Language("sql") String sql, @Nonnull QueryAPI api) {
    return new QueryBuilder(sql, api);
  }

  /** Facade for specific implementations of SELECT query APIs, such as hibernate or JDBC. */
  @FunctionalInterface
  public interface QueryAPI {

    Query createQuery(@Nonnull String sql);
  }

  /** A minimal API that acts as the adapter to underlying implementations. */
  public interface Query {

    void setParameter(@Nonnull Param param);

    void setLimit(int n);

    void setOffset(int n);

    <T> Stream<T> stream(@Nonnull Class<T> of);

    <T> Stream<T> stream(Function<Row, T> map);

    int count();
  }

  /**
   * Reading values for a result row, index is zero based.
   *
   * <p>SQL NULL always translates to a Java {@code null} return value. Or in other words the access
   * API reflects exactly what the DB returned.
   */
  public interface Row {
    Object getObject(int index);

    Object[] getArray(int index);

    default String getString(int index) {
      return (String) getObject(index);
    }

    default Integer getInteger(int index) {
      return (Integer) getObject(index);
    }

    default Long getLong(int index) {
      return (Long) getObject(index);
    }

    default Date getDate(int index) {
      return (Date) getObject(index);
    }

    default Boolean getBoolean(int index) {
      return (Boolean) getObject(index);
    }

    default String[] getStringArray(int index) {
      return (String[]) getArray(index);
    }

    default Long[] getLongArray(int index) {
      return (Long[]) getArray(index);
    }
  }

  /** A {@link Query} to simply record set params to verify them in tests. */
  private record SpyQuery(BiConsumer<String, SQL.Param> paramsSpy) implements Query {

    @Override
    public void setParameter(@Nonnull Param param) {
      paramsSpy.accept(param.name, param);
    }

    @Override
    public void setLimit(int n) {
      // has no effect
    }

    @Override
    public void setOffset(int n) {
      // has no effect
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
