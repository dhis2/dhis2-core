/*
 * Copyright (c) 2004-2025, University of Oslo
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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.query.planner.PropertyPath;

/**
 * @author Jan Bernitt
 * @since 2.42
 */
public class NotEmptyOperator<T extends Comparable<T>> extends Operator<T> {

  public NotEmptyOperator() {
    super("!empty", List.of(Collection.class));
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, PropertyPath path) {
    if (path.getProperty().isRelation()) return builder.isNotEmpty(root.get(path.getPath()));
    // JSONB column backed collections
    Path<Object> p = root.get(path.getPath());
    Expression<String> pathAsText = p.as(String.class);
    return builder.and(
        builder.isNotNull(p),
        builder.notEqual(pathAsText, builder.literal("null")),
        builder.not(
            builder.or(
                builder.equal(pathAsText, builder.literal("[]")),
                builder.equal(pathAsText, builder.literal("{}")))));
  }

  @Override
  public boolean test(Object value) {
    if (value == null) return true;
    if (value instanceof Collection<?> c) return !c.isEmpty();
    if (value instanceof Map<?, ?> m) return !m.isEmpty();
    if (value instanceof String s) return !s.isEmpty();
    return false;
  }
}
