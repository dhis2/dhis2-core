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
import org.hisp.dhis.query.QueryException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.planner.QueryPath;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class NotEqualOperator<T extends Comparable<? super T>> extends EqualOperator<T> {
  public NotEqualOperator(T arg) {
    super("ne", arg);
  }

  @Override
  public Criterion getHibernateCriterion(QueryPath queryPath) {
    return Restrictions.not(super.getHibernateCriterion(queryPath));
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, QueryPath queryPath) {
    Property property = queryPath.getProperty();

    if (property.isCollection()) {
      Integer value = QueryUtils.parseValue(Integer.class, args.get(0));

      if (value == null) {
        throw new QueryException(
            "Left-side is collection, and right-side is not a valid integer, so can't compare by size.");
      }

      return builder.notEqual(builder.size(root.get(queryPath.getPath())), value);
    }
    return builder.notEqual(root.get(queryPath.getPath()), args.get(0));
  }

  @Override
  public boolean test(Object value) {
    return !super.test(value);
  }
}
