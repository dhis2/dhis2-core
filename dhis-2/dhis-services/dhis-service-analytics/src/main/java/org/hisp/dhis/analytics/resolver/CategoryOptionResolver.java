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
package org.hisp.dhis.analytics.resolver;

import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.stereotype.Service;

/**
 * @author Dusan Bernat
 */
@Service("org.hisp.dhis.analytics.resolver.CategoryOptionResolver")
@AllArgsConstructor
public class CategoryOptionResolver implements ExpressionResolver {
  private final ExpressionService expressionService;

  private final CategoryOptionStore categoryOptionStore;

  private static final String LEFT_BRACKET = "(";

  private static final String RIGHT_BRACKET = ")";

  private static final String CATEGORY_OPTION_PREFIX = "co:";

  private static final String EMPTY_STRING = "";

  @Override
  public String resolve(String expression) {
    Set<DimensionalItemId> dimItemIds =
        expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION);

    for (DimensionalItemId id : dimItemIds) {
      if (id.getItem() != null
          && id.getId1() != null
          && id.getId1().startsWith(CATEGORY_OPTION_PREFIX)) {
        CategoryOption co =
            categoryOptionStore.getByUid(id.getId1().replace(CATEGORY_OPTION_PREFIX, EMPTY_STRING));

        if (co != null) {
          List<String> resolved =
              co.getCategoryOptionCombos().stream()
                  .map(coc -> id.getItem().replace(id.getId1(), coc.getUid()))
                  .collect(Collectors.toList());

          expression =
              expression.replace(
                  id.getItem(), LEFT_BRACKET + Joiner.on("+").join(resolved) + RIGHT_BRACKET);
        }
      }
    }

    return expression;
  }
}
