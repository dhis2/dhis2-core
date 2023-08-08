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

import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupStore;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.stereotype.Service;

/**
 * @author Dusan Bernat
 */
@Service("org.hisp.dhis.analytics.resolver.CategoryOptionGroupResolver")
@RequiredArgsConstructor
public class CategoryOptionGroupResolver implements ExpressionResolver {
  private static final String LEFT_BRACKET = "(";

  private static final String RIGHT_BRACKET = ")";

  private static final String LOGICAL_AND = "&";

  private static final String CATEGORY_OPTION_GROUP_PREFIX = "coGroup:";

  private static final String EMPTY_STRING = "";

  private final ExpressionService expressionService;

  private final CategoryOptionGroupStore categoryOptionGroupStore;

  private final CategoryOptionComboStore categoryOptionComboStore;

  @Override
  public String resolve(String expression) {
    Set<DimensionalItemId> dimItemIds =
        expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION);

    for (DimensionalItemId id : dimItemIds) {
      if (id.getItem() != null
          && id.getId1() != null
          && id.getId1().startsWith(CATEGORY_OPTION_GROUP_PREFIX)) {
        List<String> cogUidList =
            Arrays.stream(
                    id.getId1()
                        .replace(CATEGORY_OPTION_GROUP_PREFIX, EMPTY_STRING)
                        .split(LOGICAL_AND))
                .collect(Collectors.toList());

        expression = getExpression(expression, id, cogUidList, id.getId0());
      }
    }

    return expression;
  }

  private String getExpression(
      String expression, DimensionalItemId id, List<String> cogUidList, String dataElementId) {
    List<String> cocUidIntersection =
        getCategoryOptionCombosIntersection(cogUidList, dataElementId);

    if (isEmpty(cocUidIntersection)) {
      return expression;
    }

    List<String> resolved =
        cocUidIntersection.stream()
            .map(cocUid -> id.getItem().replace(id.getId1(), cocUid))
            .collect(Collectors.toList());

    expression =
        expression.replace(
            id.getItem(), LEFT_BRACKET + Joiner.on("+").join(resolved) + RIGHT_BRACKET);

    return expression;
  }

  private List<String> getCategoryOptionCombosIntersection(
      List<String> cogUidList, String dataElementId) {
    List<String> cocUidIntersection = new ArrayList<>();

    for (String cogUid : cogUidList) {
      CategoryOptionGroup cog = categoryOptionGroupStore.loadByUid(cogUid);
      List<String> cocUids =
          categoryOptionComboStore
              .getCategoryOptionCombosByGroupUid(cog.getUid(), dataElementId)
              .stream()
              .map(IdentifiableObject::getUid)
              .collect(Collectors.toList());

      if (cocUidIntersection.isEmpty()) {
        cocUidIntersection.addAll(cocUids);
      } else {
        cocUidIntersection.retainAll(cocUids);
      }
    }

    return cocUidIntersection;
  }
}
