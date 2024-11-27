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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.query.planner.QueryPath;

/**
 * @author Henning Håkonsen
 */
public class TokenOperator<T extends Comparable<? super T>> extends Operator<T> {
  private final boolean caseSensitive;

  private final org.hibernate.criterion.MatchMode matchMode;

  public TokenOperator(T arg, boolean caseSensitive, MatchMode matchMode) {
    super("token", Typed.from(String.class), arg);
    this.caseSensitive = caseSensitive;
    this.matchMode = getMatchMode(matchMode);
  }

  @Override
  public Criterion getHibernateCriterion(QueryPath queryPath) {
    String value = caseSensitive ? getValue(String.class) : getValue(String.class).toLowerCase();

    return Restrictions.sqlRestriction(
        "c_." + queryPath.getPath() + " ~* '" + TokenUtils.createRegex(value) + "'");
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, QueryPath queryPath) {
    String value = caseSensitive ? getValue(String.class) : getValue(String.class).toLowerCase();
    if (skipUidToken(value, queryPath)) {
      return null;
    }
    Predicate defaultSearch =
        builder.equal(
            builder.function(
                JsonbFunctions.REGEXP_SEARCH,
                Boolean.class,
                root.get(queryPath.getPath()),
                builder.literal(TokenUtils.createRegex(value).toString())),
            true);

    if (queryPath.getLocale() == null
        || !queryPath.getProperty().isTranslatable()
        || queryPath.getProperty().getTranslationKey() == null) {
      return defaultSearch;
    }

    return builder.or(
        builder.equal(
            builder.function(
                JsonbFunctions.SEARCH_TRANSLATION_TOKEN,
                Boolean.class,
                root.get("translations"),
                builder.literal("{" + queryPath.getProperty().getTranslationKey() + "}"),
                builder.literal(queryPath.getLocale().getLanguage()),
                builder.literal(TokenUtils.createRegex(value).toString())),
            true),
        defaultSearch);
  }

  @Override
  public boolean test(Object value) {
    return TokenUtils.test(value, getValue(String.class), caseSensitive, matchMode);
  }

  /**
   * Return
   *
   * @param value
   * @param query
   * @return
   */
  private boolean skipUidToken(String value, QueryPath query) {
    return "uid".equals(query.getProperty().getFieldName()) && value.length() < 4;
  }
}
