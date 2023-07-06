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
package org.hisp.dhis.query.operators;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.query.planner.QueryPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class LikeOperator<T extends Comparable<? super T>> extends Operator<T> {
  private final boolean caseSensitive;

  private final JpaQueryUtils.StringSearchMode jpaMatchMode;

  private final org.hibernate.criterion.MatchMode matchMode;

  public LikeOperator(
      T arg, boolean caseSensitive, org.hisp.dhis.query.operators.MatchMode matchMode) {
    super("like", Typed.from(String.class), arg);
    this.caseSensitive = caseSensitive;
    this.jpaMatchMode = getJpaMatchMode(matchMode);
    this.matchMode = getMatchMode(matchMode);
  }

  public LikeOperator(
      String name,
      T arg,
      boolean caseSensitive,
      org.hisp.dhis.query.operators.MatchMode matchMode) {
    super(name, Typed.from(String.class), arg);
    this.caseSensitive = caseSensitive;
    this.jpaMatchMode = getJpaMatchMode(matchMode);
    this.matchMode = getMatchMode(matchMode);
  }

  @Override
  public Criterion getHibernateCriterion(QueryPath queryPath) {
    if (caseSensitive) {
      return Restrictions.like(
          queryPath.getPath(), String.valueOf(args.get(0)).replace("%", "\\%"), matchMode);
    } else {
      return Restrictions.ilike(
          queryPath.getPath(), String.valueOf(args.get(0)).replace("%", "\\%"), matchMode);
    }
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, QueryPath queryPath) {
    if (caseSensitive) {
      return JpaQueryUtils.stringPredicateCaseSensitive(
          builder,
          root.get(queryPath.getPath()),
          String.valueOf(args.get(0)).replace("%", ""),
          jpaMatchMode);
    } else {
      return JpaQueryUtils.stringPredicateIgnoreCase(
          builder,
          root.get(queryPath.getPath()),
          String.valueOf(args.get(0)).replace("%", ""),
          jpaMatchMode);
    }
  }

  @Override
  public boolean test(Object value) {
    if (args.isEmpty() || value == null) {
      return false;
    }

    Type type = new Type(value);

    if (type.isString()) {
      String s1 = caseSensitive ? getValue(String.class) : getValue(String.class).toLowerCase();
      String s2 = caseSensitive ? (String) value : ((String) value).toLowerCase();

      switch (jpaMatchMode) {
        case EQUALS:
          return s2.equals(s1);
        case STARTING_LIKE:
          return s2.startsWith(s1);
        case ENDING_LIKE:
          return s2.endsWith(s1);
        case ANYWHERE:
          return s2.contains(s1);
      }
    }

    return false;
  }
}
