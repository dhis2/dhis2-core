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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import org.hisp.dhis.query.JpaPredicateSupplier;
import org.hisp.dhis.query.planner.PropertyPath;

/**
 * Matches a root organisation unit or descendants of any supplied full stored path. This is an
 * internal hierarchy restriction, not a public filter operator.
 *
 * @author Morten Svanæs
 */
public final class DescendantOfOperator extends Operator<String> implements JpaPredicateSupplier {
  public DescendantOfOperator(Collection<String> rootPaths) {
    super("descendantOf", List.of(String.class), rootPaths);
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, PropertyPath path) {
    return getPredicate(builder, getPropertyPath(root, path));
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, CriteriaQuery<?> query) {
    return getPredicate(builder, root.get("path"));
  }

  private Predicate getPredicate(CriteriaBuilder builder, Expression<String> path) {
    Predicate[] roots = new Predicate[args.size()];
    for (int i = 0; i < args.size(); i++) {
      String rootPath = args.get(i);
      String prefix = rootPath.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "/%";
      roots[i] = builder.or(builder.equal(path, rootPath), builder.like(path, prefix, '!'));
    }
    return builder.or(roots);
  }

  @Override
  public boolean test(Object value) {
    if (!(value instanceof String path)) return false;
    for (String rootPath : args) {
      if (path.equals(rootPath)
          || (path.startsWith(rootPath)
              && path.length() > rootPath.length()
              && path.charAt(rootPath.length()) == '/')) return true;
    }
    return false;
  }
}
