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
package org.hisp.dhis.analytics.common.query;

import static java.util.Collections.singleton;
import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.QueryKey.NV;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;

/**
 * This class represents the constant values renderer. It will render the constant values and bind
 * them to the query.
 */
@Getter
@RequiredArgsConstructor(staticName = "of")
public class ConstantValuesRenderer extends BaseRenderable {
  private final Object values;

  private final ValueTypeMapping valueTypeMapping;

  private final QueryContext queryContext;

  private final Function<String, String> argumentTransformer;

  public static ConstantValuesRenderer of(
      Object values, ValueTypeMapping valueTypeMapping, QueryContext queryContext) {
    return of(values, valueTypeMapping, queryContext, Function.identity());
  }

  @Override
  public String render() {
    if (values instanceof Collection) {
      return renderCollection((Collection<?>) values);
    } else {
      return renderSingleValue(values);
    }
  }

  private String renderSingleValue(Object value) {
    return renderCollection(singleton(value));
  }

  private String renderCollection(Collection<?> values) {
    List<String> valuesAsStringList =
        values.stream()
            .map(Object::toString)
            .filter(not(isEqual(NV)))
            .map(argumentTransformer)
            .collect(toList());

    if (valuesAsStringList.isEmpty()) {
      return EMPTY;
    }

    if (valuesAsStringList.size() > 1) {
      return queryContext.bindParamAndGetIndex(valueTypeMapping.convertMany(valuesAsStringList));
    }
    return queryContext.bindParamAndGetIndex(
        valueTypeMapping.convertSingle(valuesAsStringList.get(0)));
  }

  public static boolean hasNullValue(Renderable renderableValues) {
    if (renderableValues instanceof ConstantValuesRenderer) {
      Object values = ((ConstantValuesRenderer) renderableValues).getValues();
      if (values instanceof Collection) {
        return ((Collection<?>) values).stream().anyMatch(isEqual(NV));
      }
      return values.equals(NV);
    }
    return false;
  }

  public static boolean hasMultipleValues(Renderable renderableValues) {
    return renderableValues instanceof ConstantValuesRenderer
        && ((ConstantValuesRenderer) renderableValues).getValues() instanceof Collection
        && ((Collection<?>) ((ConstantValuesRenderer) renderableValues).getValues()).size() > 1;
  }

  public ConstantValuesRenderer withArgumentTransformer(UnaryOperator<String> valueTransformer) {
    return of(values, valueTypeMapping, queryContext, valueTransformer);
  }
}
