package org.hisp.dhis.program.function;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.function.ScalarFunctionToEvaluate;
import org.hisp.dhis.parser.expression.function.SimpleScalarFunction;
import org.hisp.dhis.program.ProgramIndicator;

import java.util.Date;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

/**
 * Program indicator count functions
 *
 * @author Jim Grace
 */
public abstract class ProgramCountFunction
    implements ScalarFunctionToEvaluate
{

    @Override
    public final Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        ProgramIndicator pi = visitor.getProgramIndicator();
        StatementBuilder sb = visitor.getStatementBuilder();

        Date startDate = visitor.getReportingStartDate();
        Date endDate = visitor.getReportingEndDate();

        String programStage = ctx.stageDataElement().uid0.getText();
        String dataElement = ctx.stageDataElement().uid1.getText();

        String eventTableName = "analytics_event_" + pi.getProgram().getUid();
        String columnName = "\"" + dataElement + "\"";

        String conditionSql = getConditionSql( ctx, visitor );

        return "(select count(" + columnName + ") from " + eventTableName +
            " where " + eventTableName + ".pi = " + StatementBuilder.ANALYTICS_TBL_ALIAS + ".pi and " +
            columnName + " is not null and " + columnName + conditionSql + " " +
            (pi.getEndEventBoundary() != null ? ("and " +
                sb.getBoundaryCondition( pi.getEndEventBoundary(), pi,
                    startDate, endDate ) +
                " ") : "") + (pi.getStartEventBoundary() != null ? ("and " +
            sb.getBoundaryCondition( pi.getStartEventBoundary(), pi,
                startDate, endDate ) +
            " ") : "") + "and ps = '" + programStage + "')";
    }

    /**
     * Generate the conditional part of the SQL for a d2 count function
     *
     * @param ctx the expression context
     * @param visitor the program indicator expression tree visitor
     * @return the conditional part of the SQL
     */
    public abstract String getConditionSql( ExprContext ctx, CommonExpressionVisitor visitor );
}
