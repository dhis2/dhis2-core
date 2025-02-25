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

import static org.hisp.dhis.analytics.util.optimizer.cte.StringUtils.preserveLettersAndNumbers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

public class DataElementCountMatcher extends AbstractCountMatcher {

  @Override
  public Optional<FoundSubSelect> match(SubSelect subSelect) {
    Optional<PlainSelect> maybePlain = asPlainSelect(subSelect);
    if (maybePlain.isEmpty()) {
      return Optional.empty();
    }
    PlainSelect plain = maybePlain.get();

    Optional<Expression> selectExpr = getSingleExpression(plain);
    if (selectExpr.isEmpty()) {
      return Optional.empty();
    }

    if (!(selectExpr.get() instanceof Function func)
        || !"count".equalsIgnoreCase(func.getName())
        || func.getParameters() == null
        || func.getParameters().getExpressions().size() != 1) {
      return Optional.empty();
    }
    Expression countParam = func.getParameters().getExpressions().get(0);
    if (!(countParam instanceof Column col)) {
      return Optional.empty();
    }
    String dataElementId = col.getColumnName().replace("\"", "");

    // FROM clause must be a table containing "analytics_event"
    FromItem fromItem = plain.getFromItem();
    if (!(fromItem instanceof Table table)
        || !table.getName().toLowerCase().contains("analytics_event")) {
      return Optional.empty();
    }

    // WHERE clause must be an AndExpression with required conditions.
    Expression where = plain.getWhere();
    if (!(where instanceof AndExpression)) {
      return Optional.empty();
    }
    WhereClauseConditions conditions = extractWhereConditions(where, dataElementId);
    if (!conditions.isValid()) {
      return Optional.empty();
    }

    Map<String, String> metadata = new HashMap<>();
    metadata.put("dataElementId", dataElementId);
    metadata.put("programStageId", conditions.programStageId());
    metadata.put("value", conditions.dataElementValue());

    String cteName = "de_count_" + preserveLettersAndNumbers(dataElementId);
    return Optional.of(new FoundSubSelect(cteName, subSelect, "de_count", metadata));
  }
}
