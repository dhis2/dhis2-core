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
package org.hisp.dhis.query;

import java.util.Collection;
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
import org.hisp.dhis.query.operators.NotEmptyOperator;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.query.operators.NotInOperator;
import org.hisp.dhis.query.operators.NotLikeOperator;
import org.hisp.dhis.query.operators.NotNullOperator;
import org.hisp.dhis.query.operators.NotTokenOperator;
import org.hisp.dhis.query.operators.NullOperator;
import org.hisp.dhis.query.operators.TokenOperator;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class Filters {
  public static <T extends Comparable<T>> Filter eq(String path, T value) {
    return new Filter(path, new EqualOperator<>(value));
  }

  public static <T extends Comparable<T>> Filter ne(String path, T value) {
    return new Filter(path, new NotEqualOperator<>(value));
  }

  public static <T extends Comparable<T>> Filter gt(String path, T value) {
    return new Filter(path, new GreaterThanOperator<>(value));
  }

  public static <T extends Comparable<T>> Filter lt(String path, T value) {
    return new Filter(path, new LessThanOperator<>(value));
  }

  public static <T extends Comparable<T>> Filter ge(String path, T value) {
    return new Filter(path, new GreaterEqualOperator<>(value));
  }

  public static <T extends Comparable<T>> Filter le(String path, T value) {
    return new Filter(path, new LessEqualOperator<>(value));
  }

  public static <T extends Comparable<T>> Filter between(String path, T lside, T rside) {
    return new Filter(path, new BetweenOperator<>(lside, rside));
  }

  public static <T extends Comparable<T>> Filter like(String path, T value, MatchMode matchMode) {
    return new Filter(path, new LikeOperator<>(value, true, matchMode));
  }

  public static <T extends Comparable<T>> Filter notLike(
      String path, T value, MatchMode matchMode) {
    return new Filter(path, new NotLikeOperator<>(value, true, matchMode));
  }

  public static <T extends Comparable<T>> Filter ilike(String path, T value, MatchMode matchMode) {
    return new Filter(path, new LikeOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<T>> Filter notIlike(
      String path, T value, MatchMode matchMode) {
    return new Filter(path, new NotLikeOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<T>> Filter token(String path, T value, MatchMode matchMode) {
    return new Filter(path, new TokenOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<T>> Filter notToken(
      String path, T value, MatchMode matchMode) {
    return new Filter(path, new NotTokenOperator<>(value, false, matchMode));
  }

  public static <T extends Comparable<T>> Filter in(String path, Collection<T> values) {
    return new Filter(path, new InOperator<>(values));
  }

  public static <T extends Comparable<T>> Filter notIn(String path, Collection<T> values) {
    return new Filter(path, new NotInOperator<>(values));
  }

  public static Filter isNull(String path) {
    return new Filter(path, new NullOperator<>());
  }

  public static Filter isNotNull(String path) {
    return new Filter(path, new NotNullOperator<>());
  }

  public static Filter isEmpty(String path) {
    return new Filter(path, new EmptyOperator<>());
  }

  public static Filter isNotEmpty(String path) {
    return new Filter(path, new NotEmptyOperator<>());
  }

  public static Filter query(String query) {
    return new Filter("$query", new EqualOperator<>(query));
  }

  private Filters() {}
}
