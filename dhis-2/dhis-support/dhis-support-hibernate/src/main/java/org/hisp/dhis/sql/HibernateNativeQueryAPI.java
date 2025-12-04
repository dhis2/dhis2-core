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

import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryProducer;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DateType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

/**
 * Implementation of the {@link org.hisp.dhis.sql.SQL.QueryAPI} as a {@link NativeQuery}.
 *
 * @author Jan Bernitt
 */
class HibernateNativeQueryAPI {

  record HibernateQuery(QueryProducer impl, String sql, List<Consumer<NativeQuery<?>>> setters)
      implements SQL.Query {

    @Override
    public SQL.Query setParameter(@Nonnull SQL.Param param) {
      Type type =
          switch (param.type()) {
            case DATE -> DateType.INSTANCE;
            case STRING -> StringType.INSTANCE;
            case LONG -> LongType.INSTANCE;
            case INTEGER -> IntegerType.INSTANCE;
            case BOOLEAN -> BooleanType.INSTANCE;
            case LONG_ARRAY -> LongArrayType.INSTANCE;
            case STRING_ARRAY -> StringArrayType.INSTANCE;
          };
      setters.add(q -> q.setParameter(param.name(), param.value(), type));
      return this;
    }

    @Override
    public SQL.Query setLimit(int n) {
      setters.add(q -> q.setMaxResults(n));
      return this;
    }

    @Override
    public SQL.Query setOffset(int n) {
      setters.add(q -> q.setFirstResult(n));
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Stream<T> stream(@Nonnull Class<T> of) {
      NativeQuery<T> query =
          isKnownNotMappedType(of) ? impl.createNativeQuery(sql) : impl.createNativeQuery(sql, of);
      setters.forEach(s -> s.accept(query));
      return query.stream();
    }

    private static <T> boolean isKnownNotMappedType(@Nonnull Class<T> of) {
      return of == Object[].class
          || of == String.class
          || of == Boolean.class
          || Number.class.isAssignableFrom(of);
    }

    @Override
    public <T> Stream<T> stream(Function<SQL.Row, T> map) {
      return stream(Object[].class)
          .map(
              row ->
                  map.apply(
                      new SQL.Row() {

                        @Override
                        public Object getObject(int index) {
                          return row[index];
                        }

                        @Override
                        public Object[] getArray(int index) {
                          return (Object[]) row[index];
                        }
                      }));
    }

    @Override
    public int count() {
      NativeQuery<?> query = impl.createNativeQuery(sql);
      setters.forEach(s -> s.accept(query));
      return query.getSingleResult() instanceof Number n ? n.intValue() : 0;
    }
  }
}
