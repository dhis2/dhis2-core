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
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.planner.PropertyPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class BetweenOperator<T extends Comparable<T>> extends Operator<T> {
  public BetweenOperator(T arg0, T arg1) {
    super("between", List.of(String.class, Number.class, Date.class), arg0, arg1);
  }

  @Override
  public <Y> Predicate getPredicate(CriteriaBuilder builder, Root<Y> root, PropertyPath path) {
    return builder.between(root.get(path.getPath()), getArgs().get(0), getArgs().get(1));
  }

  @Override
  public boolean test(Object value) {
    if (args.isEmpty() || value == null) {
      return false;
    }

    Type type = new Type(value);

    if (type.isInteger()) {
      Integer s1 = getValue(Integer.class, value);
      Integer min = getValue(Integer.class, 0);
      Integer max = getValue(Integer.class, 1);

      return s1 >= min && s1 <= max;
    }
    if (type.isFloat()) {
      Float s1 = getValue(Float.class, value);
      Integer min = getValue(Integer.class, 0);
      Integer max = getValue(Integer.class, 1);

      return s1 >= min && s1 <= max;
    }
    if (type.isDate()) {
      Date min = getValue(Date.class, 0);
      Date max = getValue(Date.class, 1);
      Date s2 = (Date) value;

      return (s2.equals(min) || s2.after(min)) && (s2.before(max) || s2.equals(max));
    }
    if (type.isCollection()) {
      Collection<?> collection = (Collection<?>) value;
      Integer min = getValue(Integer.class, 0);
      Integer max = getValue(Integer.class, 1);

      return collection.size() >= min && collection.size() <= max;
    }

    return false;
  }
}
