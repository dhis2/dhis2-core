/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.query;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.query.operators.BetweenOperator;
import org.hisp.dhis.query.operators.EmptyOperator;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.query.operators.GreaterEqualOperator;
import org.hisp.dhis.query.operators.GreaterThanOperator;
import org.hisp.dhis.query.operators.InOperator;
import org.hisp.dhis.query.operators.LessEqualOperator;
import org.hisp.dhis.query.operators.LessThanOperator;
import org.hisp.dhis.query.operators.LikeOperator;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.query.operators.NotInOperator;
import org.hisp.dhis.query.operators.NotLikeOperator;
import org.hisp.dhis.query.operators.NotNullOperator;
import org.hisp.dhis.query.operators.NotTokenOperator;
import org.hisp.dhis.query.operators.NullOperator;
import org.hisp.dhis.query.operators.TokenOperator;
import org.hisp.dhis.schema.Schema;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class Restrictions {
  public static <T extends Comparable<? super T>> Restriction eq(String path, T value) {
    return new Restriction(path, new EqualOperator<>(value));
  }

  public static <T extends Comparable<? super T>> Restriction ne(String path, T value) {
    return new Restriction(path, new NotEqualOperator<>(value));
  }

  public static <T extends Comparable<? super T>> Restriction gt(String path, T value) {
    return new Restriction(path, new GreaterThanOperator<>(value));
  }

  public static <T extends Comparable<? super T>> Restriction lt(String path, T value) {
    return new Restriction(path, new LessThanOperator<>(value));
  }

  public static <T extends Comparable<? super T>> Restriction ge(String path, T value) {
    return new Restriction(path, new GreaterEqualOperator<>(value));
  }

  public static <T extends Comparable<? super T>> Restriction le(String path, T value) {
    return new Restriction(path, new LessEqualOperator<>(value));
  }

  public static <T extends Comparable<? super T>> Restriction between(
      String path, T lside, T rside) {
    return new Restriction(path, new BetweenOperator<>(lside, rside));
  }

  public static <T extends Comparable<? super T>> Restriction like(
      String path, T value, MatchMode matchMode) {
    return new Restriction(path, new LikeOperator<>(value, true, matchMode));
  }

  public static <T extends Comparable<? super T>> Restriction notLike(
      String path, T value, MatchMode matchMode) {
    return new Restriction(path, new NotLikeOperator<>(value, true, matchMode));
  }

  public static <T extends Comparable<? super T>> Restriction ilike(
      String path, T value, MatchMode matchMode) {
    return new Restriction(path, new LikeOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<? super T>> Restriction notIlike(
      String path, T value, MatchMode matchMode) {
    return new Restriction(path, new NotLikeOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<? super T>> Restriction token(
      String path, T value, MatchMode matchMode) {
    return new Restriction(path, new TokenOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<? super T>> Restriction notToken(
      String path, T value, MatchMode matchMode) {
    return new Restriction(path, new NotTokenOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<? super T>> Restriction in(
      String path, Collection<T> values) {
    return new Restriction(path, new InOperator<>(values));
  }

  public static <T extends Comparable<? super T>> Restriction notIn(
      String path, Collection<T> values) {
    return new Restriction(path, new NotInOperator<>(values));
  }

  public static Restriction isNull(String path) {
    return new Restriction(path, new NullOperator<>());
  }

  public static Restriction isNotNull(String path) {
    return new Restriction(path, new NotNullOperator<>());
  }

  public static Restriction isEmpty(String path) {
    return new Restriction(path, new EmptyOperator<>());
  }

  /**
   * Builds a PR group to match the query string against id, code and name.
   *
   * @param schema of the root entity
   * @param query the query string value used the URL {@code query} parameter
   * @return OR group with the filters for the query string
   */
  public static Disjunction query(Schema schema, String query) {
    Restriction name = ilike("name", query, MatchMode.ANYWHERE);
    Restriction code = eq("code", query);
    if (query.length() != 11) return or(schema, code, name);
    // only a query with length 11 has a chance of matching a UID
    Restriction id = eq("id", query);
    return or(schema, id, code, name);
  }

  public static Disjunction or(Schema schema, Criterion... filters) {
    return or(schema, List.of(filters));
  }

  public static Disjunction or(Schema schema, List<? extends Criterion> filters) {
    Disjunction or = new Disjunction(schema);
    or.add(filters);
    return or;
  }

  public static Conjunction and(Schema schema, Criterion... filters) {
    return and(schema, List.of(filters));
  }

  public static Conjunction and(Schema schema, List<? extends Criterion> filters) {
    Conjunction and = new Conjunction(schema);
    and.add(filters);
    return and;
  }

  private Restrictions() {}
}
