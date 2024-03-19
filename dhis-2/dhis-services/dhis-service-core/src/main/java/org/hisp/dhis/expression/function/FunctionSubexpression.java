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
package org.hisp.dhis.expression.function;

import static org.hisp.dhis.common.DimensionItemType.SUBEXPRESSION_DIMENSION_ITEM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.util.Set;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.expression.ExpressionInfo;
import org.hisp.dhis.expression.dataitem.DimensionalItem;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;

/**
 * Function subexpression
 *
 * @author Jim Grace
 */
public class FunctionSubexpression extends DimensionalItem {
  @Override
  public Object getDescription(ExprContext ctx, CommonExpressionVisitor visitor) {
    return visitor.visit(ctx.expr(0));
  }

  @Override
  public DimensionalItemId getDimensionalItemId(ExprContext ctx, CommonExpressionVisitor visitor) {
    CommonExpressionVisitor infoVisitor =
        visitor.toBuilder()
            .itemMethod(ITEM_GET_EXPRESSION_INFO)
            .info(new ExpressionInfo())
            .state(getSubexpressionState(visitor))
            .build();

    Object returnObject = infoVisitor.visit(ctx.expr(0));

    Set<DimensionalItemId> itemIds = infoVisitor.getInfo().getItemIds();

    validateSubexpressionItemIds(itemIds);

    String subexSql = getSubexpressionSql(ctx, visitor);

    QueryModifiers mods =
        visitor.getState().getQueryModsBuilder().valueType(getValueType(returnObject)).build();

    return new DimensionalItemId(SUBEXPRESSION_DIMENSION_ITEM, subexSql, itemIds, mods);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Checks that the subexpression item ids are valid. */
  private void validateSubexpressionItemIds(Set<DimensionalItemId> itemIds) {
    if (itemIds.isEmpty()) {
      throw new ParserExceptionWithoutContext(
          "subExpression must include at least one data element or data element operand");
    }

    if (itemIds.stream().anyMatch(i -> !i.isDataElementOrOperand())) {
      throw new ParserExceptionWithoutContext(
          "subExpression may not contain data items other than a data element or data element operand");
    }
  }

  /** Gets the SQL that will be executed for this subexpresison. */
  private String getSubexpressionSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    CommonExpressionVisitor sqlVisitor =
        visitor.toBuilder().itemMethod(ITEM_GET_SQL).state(getSubexpressionState(visitor)).build();

    sqlVisitor.setExpressionLiteral(new SqlLiteral());

    return sqlVisitor.castStringVisit(ctx.expr(0));
  }

  /**
   * Gets the current state adding that we are in a subexpression.
   *
   * <p>Also, if there is an aggregation type override for the subexpression as a whole, it does not
   * pass through to items inside the subexpression.
   */
  private ExpressionState getSubexpressionState(CommonExpressionVisitor visitor) {
    QueryModifiers internalQueryMods =
        visitor.getState().getQueryModsBuilder().aggregationType(null).build();

    return visitor.getState().toBuilder()
        .inSubexpression(true)
        .queryMods(internalQueryMods)
        .build();
  }

  /**
   * Deduces the value type of the subexpression from the object returned while getting information
   * about the expression.
   */
  private ValueType getValueType(Object returnObject) {
    if (returnObject instanceof String) {
      return ValueType.TEXT;
    }

    if (returnObject instanceof Boolean) {
      return ValueType.BOOLEAN;
    }

    return ValueType.NUMBER;
  }
}
