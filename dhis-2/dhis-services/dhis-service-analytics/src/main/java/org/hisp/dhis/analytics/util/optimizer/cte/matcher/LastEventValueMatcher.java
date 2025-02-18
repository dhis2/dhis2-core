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

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.StringUtils;

/**
 * Matcher for the "last event value" pattern:
 *
 * <pre>
 *   SELECT "columnName"
 *   FROM &lt;table&gt;
 *   WHERE &lt;table&gt;.enrollment = subax.enrollment
 *     AND "columnName" IS NOT NULL
 *     AND ps = 'programStageId'
 *   ORDER BY occurreddate DESC
 *   LIMIT 1
 * </pre>
 *
 * This matcher applies only if the column is not "scheduleddate" or "created".
 */
public class LastEventValueMatcher extends AbstractLastValueMatcher {
  @Override
  protected boolean validateColumn(Column col, PlainSelect plain) {
    String columnName = col.getColumnName();
    // Exclude columns handled by other matchers.
    return !("scheduleddate".equalsIgnoreCase(columnName)
        || "created".equalsIgnoreCase(columnName));
  }

  @Override
  protected boolean additionalValidation(PlainSelect plain, Column col) {
    String columnName = col.getColumnName();
    String whereStr = plain.getWhere().toString().toLowerCase();
    // Ensure that the WHERE clause contains the column IS NOT NULL condition
    // and a program stage condition.
    return whereStr.contains(columnName.toLowerCase() + " is not null")
        && (whereStr.contains("ps =") || whereStr.contains("ps is"));
  }

  @Override
  protected String getCteName(Column col) {

    String cleaned = StringUtils.preserveLettersAndNumbers(col.getColumnName().toLowerCase());
    return "last_value_" + cleaned;
  }
}
