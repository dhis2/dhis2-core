/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import java.util.Date;
import org.hisp.dhis.analytics.AnalyticsConstants;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import org.hisp.dhis.parser.expression.statement.StatementBuilder;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.dataitem.ProgramItemStageElement;

/**
 * Program indicator count functions
 *
 * @author Jim Grace
 */
public abstract class ProgramCountFunction extends ProgramExpressionItem {

  private static final String SQL_TEMPLATE =
      """
            (select count(%1$s)
            from %2$s
            where %2$s.enrollment = %3$s.enrollment
            and %1$s is not null
            and %4$s
            %5$s
            %6$s
            and ps = '%7$s')
            """;

  @Override
  public final Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    validateCountFunctionArgs(ctx);

    StatementBuilder sb = visitor.getStatementBuilder();

    ProgramExpressionParams params = visitor.getProgParams();

    ProgramIndicator pi = params.getProgramIndicator();

    Date startDate = params.getReportingStartDate();
    Date endDate = params.getReportingEndDate();

    String programStage = ctx.uid0.getText();
    String dataElement = ctx.uid1.getText();

    String eventTableName = "analytics_event_" + pi.getProgram().getUid();
    String baseColumnName = visitor.getSqlBuilder().quote(dataElement);
    String conditionSql = getConditionSql(ctx, visitor, baseColumnName);

    String endBoundarySql =
        (pi.getEndEventBoundary() != null)
            ? "and " + sb.getBoundaryCondition(pi.getEndEventBoundary(), pi, startDate, endDate)
            : ""; // Empty if no end boundary
    String startBoundarySql =
        (pi.getStartEventBoundary() != null)
            ? "and " + sb.getBoundaryCondition(pi.getStartEventBoundary(), pi, startDate, endDate)
            : ""; // Empty if no start boundary

    return String.format(
        SQL_TEMPLATE,
        baseColumnName, // %1$s: Quoted column name
        eventTableName, // %2$s: Event table name
        AnalyticsConstants.ANALYTICS_TBL_ALIAS, // %3$s: Outer table alias
        conditionSql, // %4$s: Full condition predicate from subclass
        endBoundarySql, // %5$s: End boundary condition (or empty)
        startBoundarySql, // %6$s: Start boundary condition (or empty)
        programStage // %7$s: Program stage UID
        );
  }

  /**
   * Get the description for the first arg #{programStageUid.dataElementUid} and return a value with
   * its data type.
   *
   * @param ctx the expression context
   * @param visitor the tree visitor
   * @return a dummy value for the item (of the right type, for type checking)
   */
  protected Object getProgramStageElementDescription(
      ExprContext ctx, CommonExpressionVisitor visitor) {
    validateCountFunctionArgs(ctx);

    return (new ProgramItemStageElement()).getDescription(ctx, visitor);
  }

  /**
   * Generate the conditional part of the SQL for a d2 count function
   *
   * @param ctx the expression context
   * @param visitor the program indicator expression tree visitor
   * @return the conditional part of the SQL
   */
  public abstract String getConditionSql(
      ExprContext ctx, CommonExpressionVisitor visitor, String baseColumnName);

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void validateCountFunctionArgs(ExprContext ctx) {
    if (!(getProgramArgType(ctx) instanceof ProgramItemStageElement)) {
      throw new ParserExceptionWithoutContext(
          "First argument not supported for d2:count... functions: " + ctx.getText());
    }
  }
}
