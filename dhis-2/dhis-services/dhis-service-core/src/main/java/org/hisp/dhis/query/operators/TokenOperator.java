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
import java.util.List;
import java.util.Locale;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.query.planner.PropertyPath;
import org.hisp.dhis.setting.UserSettings;

/**
 * @author Henning Håkonsen
 */
public class TokenOperator<T extends Comparable<T>> extends Operator<T> {
  private final boolean caseSensitive;

  private final org.hibernate.criterion.MatchMode matchMode;

  public TokenOperator(T arg, boolean caseSensitive, MatchMode matchMode) {
    super("token", List.of(String.class), arg);
    this.caseSensitive = caseSensitive;
    this.matchMode = getMatchMode(matchMode);
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, PropertyPath path) {
    String value = caseSensitive ? getValue(String.class) : getValue(String.class).toLowerCase();
    if (skipUidToken(value, path)) {
      return null;
    }
    Predicate defaultSearch =
        builder.equal(
            builder.function(
                JsonbFunctions.REGEXP_SEARCH,
                Boolean.class,
                root.get(path.getPath()),
                builder.literal(TokenUtils.createRegex(value).toString())),
            true);

    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();
    if (locale == null
        || !path.getProperty().isTranslatable()
        || path.getProperty().getTranslationKey() == null) {
      return defaultSearch;
    }

    return builder.or(
        builder.equal(
            builder.function(
                JsonbFunctions.SEARCH_TRANSLATION_TOKEN,
                Boolean.class,
                root.get("translations"),
                builder.literal("{" + path.getProperty().getTranslationKey() + "}"),
                builder.literal(locale.getLanguage()),
                builder.literal(TokenUtils.createRegex(value).toString())),
            true),
        defaultSearch);
  }

  @Override
  public boolean test(Object value) {
    return TokenUtils.test(value, getValue(String.class), caseSensitive, matchMode);
  }

  private boolean skipUidToken(String value, PropertyPath query) {
    return "uid".equals(query.getProperty().getFieldName()) && value.length() < 4;
  }
}
