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
package org.hisp.dhis.query.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.hisp.dhis.query.JpaPredicateSupplier;
import org.hisp.dhis.query.planner.PropertyPath;

/**
 * Applies another operator on a resolved property as a {@link JpaPredicateSupplier}, so a caller
 * can AND the condition with the query instead of adding it to the root junction. This is an
 * internal restriction, not a public filter operator.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class MandatoryOperator<T extends Comparable<T>> extends Operator<T>
    implements JpaPredicateSupplier {
  private final Operator<T> operator;

  private final PropertyPath path;

  private MandatoryOperator(Operator<T> operator, PropertyPath path) {
    super("mandatory", List.of(), operator.getArgs());
    this.operator = operator;
    this.path = path;
  }

  public static <T extends Comparable<T>> MandatoryOperator<T> of(
      Operator<T> operator, PropertyPath path) {
    return new MandatoryOperator<>(operator, path);
  }

  /**
   * @return true, when the wrapped condition cannot match any rows, e.g. an in-operator with an
   *     empty collection to test against
   */
  public boolean isAlwaysFalse() {
    return operator instanceof InOperator && args.isEmpty();
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, PropertyPath path) {
    return isAlwaysFalse() ? builder.disjunction() : operator.getPredicate(builder, root, path);
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, CriteriaQuery<?> query) {
    return getPredicate(builder, root, path);
  }

  @Override
  public boolean test(Object value) {
    return !isAlwaysFalse() && operator.test(value);
  }
}
