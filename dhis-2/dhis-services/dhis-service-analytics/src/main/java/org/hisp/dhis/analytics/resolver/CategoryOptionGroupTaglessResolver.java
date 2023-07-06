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

import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.expression.Expression.*;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import com.google.common.base.Joiner;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupStore;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luciano Fiandesio
 */
@Service("org.hisp.dhis.analytics.resolver.CategoryOptionGroupTaglessResolver")
@AllArgsConstructor
public class CategoryOptionGroupTaglessResolver implements ExpressionResolver {
  private final ExpressionService expressionService;

  private final CategoryOptionGroupStore categoryOptionGroupStore;

  private final CategoryOptionComboStore categoryOptionComboStore;

  private Set<String> resolveCoCFromCog(String categoryOptionGroupUid, String dataElementUid) {
    return categoryOptionComboStore
        .getCategoryOptionCombosByGroupUid(categoryOptionGroupUid, dataElementUid)
        .stream()
        .map(BaseIdentifiableObject::getUid)
        .collect(Collectors.toSet());
  }

  /**
   * Resolves a Data Element Operand expression containing one or two Category Option Group UID to
   * an equivalent expression where the associated Category Option Combos for the given Category
   * Option Group are "exploded" into the expression.
   *
   * <p>Resolves one of the expressions below:
   *
   * <p>1) #{DEUID.COGUID.AOCUID} 2) #{DEUID.COCUID.COGUID} 3) #{DEUID.COG1UID.COG2UID}
   *
   * <p>to:
   *
   * <p>1) #{DEUID.COCUID1.AOCUID} + #{DEUID.COCUID2.AOCUID} + #{DEUID.COCUID3.AOCUID} where
   * COCUID1,2,3... are resolved by fetching all COCUID by COGUID
   *
   * <p>2) #{DEUID.COCUID} + #{DEUID.COCUID1} + #{DEUID.COCUID2} + #{DEUID.COCUID3} +
   * #{DEUID.COCUID4} where COCUID1,2,3... are resolved by fetching all COCUID by COGUID
   *
   * <p>3) #{DEUID.COCUID1} + #{DEUID.COCUID2} + #{DEUID.COCUID3} + #{DEUID.COCUID4} where COCUID1
   * and COCUID2 are resolved from COG1UID and COCUID3 and COCUID4 are resolved from COGUID2
   *
   * <p>DEUID = Data Element UID COCUID = Category Option Combo UID COGUID = Category Option Group
   * UID
   *
   * @param expression a Data Element Expression
   * @return an expression containing additional CategoryOptionCombos based on the given Category
   *     Option Group
   */
  @Override
  @Transactional(readOnly = true)
  public String resolve(String expression) {
    // Get a DimensionalItemId from the expression. The expression is parsed
    // and
    // each element placed in the DimensionalItemId
    Set<DimensionalItemId> dimItemIds =
        expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION);
    List<String> resolvedOperands = new ArrayList<>();
    if (isDataElementOperand(dimItemIds)) {
      Optional<DimensionalItemId> dimItemId = dimItemIds.stream().findFirst();

      if (dimItemId.isPresent()) {
        DimensionalItemId dimensionalItemId = dimItemId.get();
        // First element is always the Data Element Id
        String dataElementUid = dimensionalItemId.getId0();

        resolvedOperands.addAll(
            evaluate(dataElementUid, dimensionalItemId.getId1(), dimensionalItemId.getId2()));

        resolvedOperands.addAll(evaluate(dataElementUid, dimensionalItemId.getId2(), null));
      }
    }
    if (resolvedOperands.isEmpty()) {
      // nothing to resolve, add the expression as it is
      resolvedOperands.add(expression);
    }
    return Joiner.on("+").join(resolvedOperands);
  }

  private List<String> evaluate(String dataElementUid, String uid, String uid2) {
    List<String> resolvedExpression = new ArrayList<>();
    Optional<String> cogUid = getCategoryOptionGroupUid(uid);
    if (cogUid.isPresent()) {
      Set<String> cocs = resolveCoCFromCog(cogUid.get(), dataElementUid);
      resolvedExpression = Arrays.asList(resolve(cocs, dataElementUid, uid2).split("\\+"));
    }
    return resolvedExpression;
  }

  private String resolve(Set<String> cocs, String dataElementUid, String third) {
    boolean isAoc = isAoc(third);

    return cocs.stream()
        .map(
            coc ->
                EXP_OPEN
                    + dataElementUid
                    + SEPARATOR
                    + coc
                    + (isAoc ? SEPARATOR + third : "")
                    + EXP_CLOSE)
        .collect(Collectors.joining("+"));
  }

  private boolean isAoc(String uid) {
    return (uid != null && categoryOptionComboStore.getByUid(uid) != null);
  }

  private Optional<String> getCategoryOptionGroupUid(String uid) {
    CategoryOptionGroup cog = categoryOptionGroupStore.getByUid(uid);

    return cog == null ? Optional.empty() : Optional.of(cog.getUid());
  }

  private boolean isDataElementOperand(Set<DimensionalItemId> dimensionalItemIds) {
    return dimensionalItemIds.size() == 1
        && dimensionalItemIds.iterator().next().getDimensionItemType().equals(DATA_ELEMENT_OPERAND);
  }
}
