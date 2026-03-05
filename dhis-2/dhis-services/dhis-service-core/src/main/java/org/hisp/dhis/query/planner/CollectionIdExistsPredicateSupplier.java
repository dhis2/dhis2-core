/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.query.planner;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import org.hisp.dhis.query.JpaPredicateSupplier;

/**
 * Builds a correlated EXISTS predicate for collection membership filters on identifier paths (e.g.
 * {@code collectionProperty.id:in:[...]}).
 *
 * @author Jason P. Pickering
 */
class CollectionIdExistsPredicateSupplier implements JpaPredicateSupplier {

  private final Class<?> objectType;
  private final String[] aliases;
  private final String terminalField;
  private final Collection<?> values;

  CollectionIdExistsPredicateSupplier(
      Class<?> objectType, String[] aliases, String terminalPath, Collection<?> values) {
    this.objectType = objectType;
    this.aliases = aliases;
    this.terminalField = terminalPath.substring(terminalPath.lastIndexOf('.') + 1);
    this.values = values;
  }

  /**
   * Generates a correlated subquery of the form: SELECT 1 FROM objectType subRoot JOIN
   * subRoot.aliases[0] a1 JOIN a1.aliases[1] a2 ... WHERE subRoot.id = root.id AND aN.terminalField
   * IN (values) The main query will then filter with an EXISTS on this subquery.
   */
  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public <R> Predicate getPredicate(
      CriteriaBuilder builder, Root<R> root, CriteriaQuery<?> criteriaQuery) {
    var subquery = criteriaQuery.subquery(Integer.class);
    Root<?> subRoot = subquery.from((Class) objectType);
    From<?, ?> joined = subRoot;
    for (String alias : aliases) {
      joined = joined.join(alias);
    }
    subquery.select(builder.literal(1));
    subquery.where(
        builder.equal(subRoot.get("id"), root.get("id")), joined.get(terminalField).in(values));
    return builder.exists(subquery);
  }
}
