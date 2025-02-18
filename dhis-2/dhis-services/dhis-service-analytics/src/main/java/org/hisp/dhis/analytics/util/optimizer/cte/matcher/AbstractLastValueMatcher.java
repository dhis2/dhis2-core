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

import java.util.List;
import java.util.Optional;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

/** An abstract matcher that implements the common logic for matching last-value subselects. */
public abstract class AbstractLastValueMatcher implements SubselectMatcher {

  /**
   * Attempts to match the provided SubSelect.
   *
   * @param subSelect the subselect to check.
   * @return an Optional FoundSubSelect if the pattern matches; empty otherwise.
   */
  public Optional<FoundSubSelect> match(SubSelect subSelect) {
    Optional<PlainSelect> maybePlain = asPlainSelect(subSelect);
    if (maybePlain.isEmpty()) {
      return Optional.empty();
    }
    PlainSelect plain = maybePlain.get();

    // Validate that the SELECT clause is a single expression item.
    Optional<SelectExpressionItem> seiOpt = extractSingleSelectExpressionItem(plain);
    if (seiOpt.isEmpty()) {
      return Optional.empty();
    }
    SelectExpressionItem sei = seiOpt.get();
    Expression selectExpr = sei.getExpression();
    if (!(selectExpr instanceof Column col)) {
      return Optional.empty();
    }

    // Delegate column-specific validation to the subclass.
    if (!validateColumn(col, plain)) {
      return Optional.empty();
    }

    // Validate FROM clause is a table.
    if (!isTable(plain.getFromItem())) {
      return Optional.empty();
    }

    // Validate that the WHERE clause exists and contains "subax.enrollment".
    Expression where = plain.getWhere();
    if (where == null || !where.toString().toLowerCase().contains("subax.enrollment")) {
      return Optional.empty();
    }

    // Allow subclasses to perform additional validations.
    if (!additionalValidation(plain, col)) {
      return Optional.empty();
    }

    // Validate that ORDER BY and LIMIT exist.
    if (!hasOrderByAndLimit(plain)) {
      return Optional.empty();
    }

    // Create the CTE name based on the matched column.
    String cteName = getCteName(col);
    return Optional.of(new FoundSubSelect(cteName, subSelect, col.getColumnName()));
  }

  /**
   * Validates the column from the SELECT clause.
   *
   * @param col the column from the SELECT clause.
   * @param plain the PlainSelect statement.
   * @return true if the column meets the expected criteria.
   */
  protected abstract boolean validateColumn(Column col, PlainSelect plain);

  /**
   * Provides the CTE name for the matched subselect.
   *
   * @param col the column from the SELECT clause.
   * @return the CTE name.
   */
  protected abstract String getCteName(Column col);

  /**
   * Allows subclasses to perform any additional validation after the common checks. Defaults to
   * returning true.
   *
   * @param plain the PlainSelect statement.
   * @param col the matched column.
   * @return true if additional validation passes.
   */
  protected boolean additionalValidation(PlainSelect plain, Column col) {
    return true;
  }

  // -------------------- Helper Methods --------------------

  protected Optional<PlainSelect> asPlainSelect(SubSelect subSelect) {
    if (subSelect.getSelectBody() instanceof PlainSelect plain) {
      return Optional.of(plain);
    }
    return Optional.empty();
  }

  protected Optional<SelectExpressionItem> extractSingleSelectExpressionItem(PlainSelect plain) {
    List<SelectItem> items = plain.getSelectItems();
    if (items == null || items.size() != 1) {
      return Optional.empty();
    }
    SelectItem item = items.get(0);
    if (item instanceof SelectExpressionItem sei) {
      return Optional.of(sei);
    }
    return Optional.empty();
  }

  protected boolean isTable(FromItem fromItem) {
    return fromItem instanceof Table;
  }

  protected boolean hasOrderByAndLimit(PlainSelect plain) {
    return plain.getOrderByElements() != null
        && !plain.getOrderByElements().isEmpty()
        && plain.getLimit() != null;
  }
}
