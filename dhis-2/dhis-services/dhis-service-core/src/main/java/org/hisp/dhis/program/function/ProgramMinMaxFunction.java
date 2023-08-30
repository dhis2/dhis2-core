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
package org.hisp.dhis.program.function;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.util.Date;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.program.ProgramIndicator;

/**
 * @Author Zubair Asghar.
 */
public abstract class ProgramMinMaxFunction extends ProgramExpressionItem {
  @Override
  public Object getDescription(ExprContext ctx, CommonExpressionVisitor visitor) {
    return getProgramArgType(ctx).getDescription(ctx, visitor);
  }

  @Override
  public Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    StatementBuilder sb = visitor.getStatementBuilder();

    ProgramExpressionParams params = visitor.getProgParams();

    ProgramIndicator pi = params.getProgramIndicator();

    String columnName = "";

    if (ctx.uid1 == null) // arg: PS_EVENTDATE:programStageUid
    {
      columnName = "\"executiondate\"";
    } else // arg: #{programStageUid.dataElementUid}
    {
      String dataElement = ctx.uid1.getText();
      columnName = "\"" + dataElement + "\"";
    }

    if (AnalyticsType.EVENT == pi.getAnalyticsType()) {
      return columnName;
    }

    Date startDate = params.getReportingStartDate();
    Date endDate = params.getReportingEndDate();

    String eventTableName = "analytics_event_" + pi.getProgram().getUid();
    String programStage = ctx.uid0.getText();

    return "(select "
        + getAggregationOperator()
        + "("
        + columnName
        + ") from "
        + eventTableName
        + " where "
        + eventTableName
        + ".pi = "
        + StatementBuilder.ANALYTICS_TBL_ALIAS
        + ".pi "
        + (pi.getEndEventBoundary() != null
            ? ("and "
                + sb.getBoundaryCondition(pi.getEndEventBoundary(), pi, startDate, endDate)
                + " ")
            : "")
        + (pi.getStartEventBoundary() != null
            ? ("and "
                + sb.getBoundaryCondition(pi.getStartEventBoundary(), pi, startDate, endDate)
                + " ")
            : "")
        + "and ps = '"
        + programStage
        + "')";
  }

  /***
   * Generate the function part of the SQL
   *
   * @return string sql min/max functions
   */
  public abstract String getAggregationOperator();
}
