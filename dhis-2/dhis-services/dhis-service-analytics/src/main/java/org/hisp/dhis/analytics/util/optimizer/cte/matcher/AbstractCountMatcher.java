/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.apache.commons.collections4.CollectionUtils;

public abstract class AbstractCountMatcher implements SubselectMatcher {

  protected Optional<PlainSelect> asPlainSelect(SubSelect subSelect) {
    SelectBody selectBody = subSelect.getSelectBody();
    if (!(selectBody instanceof PlainSelect)) {
      return Optional.empty();
    }
    return Optional.of((PlainSelect) selectBody);
  }

  protected Optional<Expression> getSingleExpression(PlainSelect select) {
    List<SelectItem> selectItems = select.getSelectItems();
    if (CollectionUtils.size(selectItems) != 1) {
      return Optional.empty();
    }
    SelectItem item = selectItems.get(0);
    if (!(item instanceof SelectExpressionItem sei)) {
      return Optional.empty();
    }

    return Optional.of(sei.getExpression());
  }

  /**
   * Extracts conditions from a complex WHERE clause by flattening AND expressions.
   *
   * @param whereExpr the WHERE clause expression.
   * @param dataElementId the data element id to check against.
   * @return a WhereClauseConditions record containing flags and extracted values.
   */
  protected WhereClauseConditions extractWhereConditions(
      Expression whereExpr, String dataElementId) {
    boolean hasEnrollmentCondition = false;
    boolean hasIsNotNullCondition = false;
    boolean hasValueCondition = false;
    boolean hasProgramStageCondition = false;
    String programStageId = null;
    String dataElementValue = null;

    List<Expression> conditions = new ArrayList<>();
    flattenAndConditions(whereExpr, conditions);

    for (Expression condition : conditions) {
      if (condition instanceof EqualsTo equals) {
        if (isEnrollmentCondition(equals)) {
          hasEnrollmentCondition = true;
        } else if (isProgramStageCondition(equals)) {
          hasProgramStageCondition = true;
          programStageId = extractStringValue(equals.getRightExpression());
        } else if (isDataElementValueCondition(equals, dataElementId)) {
          hasValueCondition = true;
          dataElementValue = equals.getRightExpression().toString();
        }
      } else if (condition instanceof IsNullExpression isNull) {
        if (isDataElementNotNullCondition(isNull, dataElementId)) {
          hasIsNotNullCondition = true;
        }
      }
    }

    return new WhereClauseConditions(
        hasEnrollmentCondition,
        hasIsNotNullCondition,
        hasValueCondition,
        hasProgramStageCondition,
        programStageId,
        dataElementValue);
  }

  /**
   * Recursively flattens an AND expression into individual conditions.
   *
   * @param expr the expression to flatten.
   * @param conditions the list to accumulate conditions.
   */
  private void flattenAndConditions(Expression expr, List<Expression> conditions) {
    if (expr instanceof AndExpression and) {
      flattenAndConditions(and.getLeftExpression(), conditions);
      flattenAndConditions(and.getRightExpression(), conditions);
    } else {
      conditions.add(expr);
    }
  }

  /**
   * Checks if the EqualsTo expression is the enrollment condition: <code>
   * enrollment = subax.enrollment</code>
   *
   * @param equals the EqualsTo expression.
   * @return true if it matches the enrollment condition.
   */
  private boolean isEnrollmentCondition(EqualsTo equals) {
    Expression left = equals.getLeftExpression();
    Expression right = equals.getRightExpression();

    return left instanceof Column leftCol
        && right instanceof Column rightCol
        && "enrollment".equals(leftCol.getColumnName())
        && rightCol.getTable() != null
        && "subax".equals(rightCol.getTable().getName())
        && "enrollment".equals(rightCol.getColumnName());
  }

  /**
   * Checks if the EqualsTo expression is a program stage condition (ps = ...).
   *
   * @param equals the EqualsTo expression.
   * @return true if the left-hand side is column "ps".
   */
  private boolean isProgramStageCondition(EqualsTo equals) {
    Expression left = equals.getLeftExpression();
    return left instanceof Column col && "ps".equals(col.getColumnName());
  }

  /**
   * Checks if the EqualsTo expression is a condition comparing the data element value.
   *
   * @param equals the EqualsTo expression.
   * @param dataElementId the expected data element id.
   * @return true if the left-hand side matches the data element id.
   */
  private boolean isDataElementValueCondition(EqualsTo equals, String dataElementId) {
    Expression left = equals.getLeftExpression();
    return left instanceof Column col
        && col.getColumnName().replace("\"", "").equals(dataElementId)
        && equals.getRightExpression() != null;
  }

  /**
   * Checks if the IsNullExpression represents a NOT NULL condition for the given data element.
   *
   * @param isNull the IsNullExpression.
   * @param dataElementId the data element id to check.
   * @return true if the condition is "dataElementId IS NOT NULL".
   */
  private boolean isDataElementNotNullCondition(IsNullExpression isNull, String dataElementId) {
    Expression left = isNull.getLeftExpression();
    return left instanceof Column col
        && col.getColumnName().replace("\"", "").equals(dataElementId)
        && isNull.isNot();
  }

  /**
   * Extracts the string value from an expression if it is a StringValue.
   *
   * @param expr the expression to extract from.
   * @return the string value or null.
   */
  private static String extractStringValue(Expression expr) {
    if (expr instanceof StringValue sv) {
      return sv.getValue();
    }
    return null;
  }

  /** Record holding the extracted conditions from the WHERE clause for a data element count. */
  public record WhereClauseConditions(
      boolean hasEnrollmentCondition,
      boolean hasIsNotNullCondition,
      boolean hasValueCondition,
      boolean hasProgramStageCondition,
      String programStageId,
      String dataElementValue) {
    /**
     * Checks if all required conditions for a valid data element count are met.
     *
     * @return true if valid; false otherwise.
     */
    boolean isValid() {
      return hasEnrollmentCondition
          && hasIsNotNullCondition
          && hasValueCondition
          && dataElementValue != null;
    }
  }
}
