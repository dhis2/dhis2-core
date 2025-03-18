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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.query.QueryException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.planner.PropertyPath;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class GreaterThanOperator<T extends Comparable<T>> extends Operator<T> {
  public GreaterThanOperator(T arg) {
    super("gt", List.of(String.class, Boolean.class, Number.class, Date.class), arg);
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, PropertyPath path) {
    Property property = path.getProperty();

    if (property.isCollection()) {
      Integer value = QueryUtils.parseValue(Integer.class, args.get(0));

      if (value == null) {
        throw new QueryException(
            "Left-side is collection, and right-side is not a valid integer, so can't compare by size.");
      }

      return builder.greaterThan(builder.size(root.get(path.getPath())), value);
    }

    return builder.greaterThan(root.get(path.getPath()), args.get(0));
  }

  @Override
  public boolean test(Object value) {
    if (args.isEmpty() || value == null) {
      return false;
    }

    Type type = new Type(value);

    if (type.isString()) {
      String s1 = getValue(String.class);
      String s2 = (String) value;

      return s1 != null && s2.compareTo(s1) > 0;
    }
    if (type.isInteger()) {
      Integer s1 = getValue(Integer.class);
      Integer s2 = (Integer) value;

      return s1 != null && s2 > s1;
    }
    if (type.isFloat()) {
      Float s1 = getValue(Float.class);
      Float s2 = (Float) value;

      return s1 != null && s2 > s1;
    }
    if (type.isDate()) {
      Date s1 = getValue(Date.class);
      Date s2 = (Date) value;

      return s1 != null && s2.after(s1);
    }
    if (type.isCollection()) {
      Collection<?> collection = (Collection<?>) value;
      Integer size = getValue(Integer.class);

      return size != null && collection.size() > size;
    }
    if (type.isMap()) {
      Map<?, ?> map = (Map<?, ?>) value;
      Integer size = getValue(Integer.class);

      return size != null && map.size() > size;
    }

    return false;
  }
}
