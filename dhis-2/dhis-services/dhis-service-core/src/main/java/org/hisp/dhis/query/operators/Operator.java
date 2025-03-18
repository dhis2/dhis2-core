/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.query.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.planner.PropertyPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Operator<T extends Comparable<T>> {

  protected final String name;
  protected final List<Class<?>> typed;
  protected final Type argumentType;
  @Getter protected final List<T> args;

  Operator(String name, List<Class<?>> typed) {
    this(name, typed, null, List.of());
  }

  Operator(String name, List<Class<?>> typed, Collection<T> collectionArg) {
    this(name, typed, new Type(collectionArg), List.copyOf(collectionArg));
  }

  Operator(String name, List<Class<?>> typed, T arg) {
    this(name, typed, new Type(arg), List.of(arg));
    validateArgs();
  }

  @SafeVarargs
  Operator(String name, List<Class<?>> typed, T... args) {
    this(name, typed, new Type(args[0]), List.of(args));
    validateArgs();
  }

  private void validateArgs() {
    for (Object arg : args) {
      if (!isValid(arg.getClass())) {
        throw new QueryParserException(
            "Value `"
                + arg
                + "` of type `"
                + arg.getClass().getSimpleName()
                + "` is not supported by this operator.");
      }
    }
  }

  protected <S> S getValue(Class<S> klass, Class<?> secondaryClass, int idx) {
    if (Collection.class.isAssignableFrom(klass)) {
      if (idx != 0) throw new IndexOutOfBoundsException();
      return QueryUtils.parseValue(klass, secondaryClass, getArgs());
    }

    return QueryUtils.parseValue(klass, secondaryClass, args.get(idx));
  }

  protected <S> S getValue(Class<S> klass, int idx) {
    if (Collection.class.isAssignableFrom(klass)) {
      if (idx != 0) throw new IndexOutOfBoundsException();
      return QueryUtils.parseValue(klass, null, getArgs());
    }

    return QueryUtils.parseValue(klass, null, args.get(idx));
  }

  protected <S> S getValue(Class<S> klass) {
    if (Collection.class.isAssignableFrom(klass)) {
      return QueryUtils.parseValue(klass, null, getArgs());
    }

    return getValue(klass, 0);
  }

  protected <S> T getValue(Class<S> klass, Class<?> secondaryClass, Object value) {
    return QueryUtils.parseValue(klass, secondaryClass, value);
  }

  protected <S> S getValue(Class<S> klass, Object value) {
    return QueryUtils.parseValue(klass, value);
  }

  public boolean isValid(Class<?> klass) {
    if (typed.isEmpty() || klass == null) return true;
    return typed.stream().anyMatch(k -> k != null && k.isAssignableFrom(klass));
  }

  public abstract <Y> Predicate getPredicate(
      CriteriaBuilder builder, Root<Y> root, PropertyPath path);

  public abstract boolean test(Object value);

  org.hibernate.criterion.MatchMode getMatchMode(MatchMode matchMode) {
    return switch (matchMode) {
      case EXACT -> org.hibernate.criterion.MatchMode.EXACT;
      case START -> org.hibernate.criterion.MatchMode.START;
      case END -> org.hibernate.criterion.MatchMode.END;
      case ANYWHERE -> org.hibernate.criterion.MatchMode.ANYWHERE;
    };
  }

  protected JpaQueryUtils.StringSearchMode getJpaMatchMode(MatchMode matchMode) {
    return switch (matchMode) {
      case EXACT -> JpaQueryUtils.StringSearchMode.EQUALS;
      case START -> JpaQueryUtils.StringSearchMode.STARTING_LIKE;
      case END -> JpaQueryUtils.StringSearchMode.ENDING_LIKE;
      case ANYWHERE -> JpaQueryUtils.StringSearchMode.ANYWHERE;
    };
  }

  /**
   * Get JPA String search mode for NOT LIKE match mode.
   *
   * @param matchMode {@link MatchMode}
   * @return {@link JpaQueryUtils.StringSearchMode} used for generating JPA Api Query
   */
  protected JpaQueryUtils.StringSearchMode getNotLikeJpaMatchMode(MatchMode matchMode) {
    return switch (matchMode) {
      case EXACT -> JpaQueryUtils.StringSearchMode.NOT_EQUALS;
      case START -> JpaQueryUtils.StringSearchMode.NOT_STARTING_LIKE;
      case END -> JpaQueryUtils.StringSearchMode.NOT_ENDING_LIKE;
      case ANYWHERE -> JpaQueryUtils.StringSearchMode.NOT_ANYWHERE;
    };
  }

  @Override
  public String toString() {
    return "[" + name + ", args: " + args + "]";
  }
}
