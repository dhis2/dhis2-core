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
package org.hisp.dhis.expression.dataitem;

import static org.apache.commons.lang3.ObjectUtils.anyNotNull;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.DataType.fromValueType;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.parser.expression.ParserUtils.castSql;
import static org.hisp.dhis.parser.expression.ParserUtils.replaceSqlNull;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.hisp.dhis.subexpression.SubexpressionDimensionItem.getItemColumnName;

import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;

/**
 * Expression items DataElement and DataElementOperand
 *
 * @author Jim Grace
 */
public class DimItemDataElementAndOperand extends DimensionalItem {
  @Override
  public DimensionalItemId getDimensionalItemId(ExprContext ctx, CommonExpressionVisitor visitor) {
    if (isDataElementOperandSyntax(ctx)) {
      return new DimensionalItemId(
          DATA_ELEMENT_OPERAND,
          ctx.uid0.getText(),
          ctx.uid1 == null ? null : ctx.uid1.getText(),
          ctx.uid2 == null ? null : ctx.uid2.getText(),
          ctx.getText(),
          visitor.getState().getQueryMods());
    } else {
      return new DimensionalItemId(
          DATA_ELEMENT,
          ctx.uid0.getText(),
          null,
          null,
          ctx.getText(),
          visitor.getState().getQueryMods());
    }
  }

  @Override
  public Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    String deUid = ctx.uid0.getText();
    String cocUid = (ctx.uid1 == null) ? null : ctx.uid1.getText();
    String aocUid = (ctx.uid2 == null) ? null : ctx.uid2.getText();

    String column = getItemColumnName(deUid, cocUid, aocUid, visitor.getState().getQueryMods());

    if (visitor.getState().isReplaceNulls()) {
      column = replaceDataElementNulls(column, deUid, visitor);
    }

    return column;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Does an item of the form #{...} have the syntax of a data element operand (as opposed to a data
   * element)?
   *
   * @param ctx the item context
   * @return true if data element operand syntax
   */
  private boolean isDataElementOperandSyntax(ExprContext ctx) {
    if (ctx.uid0 == null) {
      throw new ParserExceptionWithoutContext(
          "Data Element or DataElementOperand must have a uid " + ctx.getText());
    }

    return anyNotNull(ctx.uid1, ctx.uid2);
  }

  /**
   * Replaces null data element values with the appropriate replacement value within a subexpression
   * evaluation. This is always done unless the value is within a function that tests for null, such
   * as isNull() or isNotNull().
   *
   * <p>Note that boolean data elements are always aggregated as numeric in a subexpression, so if
   * they are in a context where boolean is desired (such as the first argument of an if function),
   * then cast them as boolean and use a boolean replacement value. Otherwise, boolean values are
   * numeric.
   */
  private String replaceDataElementNulls(
      String column, String deUid, CommonExpressionVisitor visitor) {
    DataElement dataElement = visitor.getIdObjectManager().get(DataElement.class, deUid);
    if (dataElement == null) {
      throw new ParserExceptionWithoutContext(
          "Data element " + deUid + " not found during SQL generation.");
    }

    DataType dataType = fromValueType(dataElement.getValueType());
    if (dataType == BOOLEAN) {
      if (visitor.getParams().getDataType() == BOOLEAN) {
        column = castSql(column, BOOLEAN);
      } else {
        dataType = NUMERIC;
      }
    }

    return replaceSqlNull(column, dataType);
  }
}
