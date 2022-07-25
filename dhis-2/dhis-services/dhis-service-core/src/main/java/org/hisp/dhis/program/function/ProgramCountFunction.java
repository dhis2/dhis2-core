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

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.dataitem.ProgramItemStageElement;

/**
 * Program indicator count functions
 *
 * @author Jim Grace
 */
public abstract class ProgramCountFunction
    extends ProgramExpressionItem
{
    @Override
    public final Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        validateCountFunctionArgs( ctx );

        StatementBuilder sb = visitor.getStatementBuilder();

        ProgramExpressionParams params = visitor.getProgParams();

        ProgramIndicator pi = params.getProgramIndicator();

        Date startDate = params.getReportingStartDate();
        Date endDate = params.getReportingEndDate();

        String programStage = ctx.uid0.getText();
        String dataElement = ctx.uid1.getText();

        String eventTableName = "analytics_event_" + pi.getProgram().getUid();
        String columnName = "\"" + dataElement + "\"";

        String conditionSql = getConditionSql( ctx, visitor );

        return "(select count(" + columnName + ") from " + eventTableName +
            " where " + eventTableName + ".pi = " + StatementBuilder.ANALYTICS_TBL_ALIAS + ".pi and " +
            columnName + " is not null and " + columnName + conditionSql + " " +
            (pi.getEndEventBoundary() != null ? ("and " +
                sb.getBoundaryCondition( pi.getEndEventBoundary(), pi,
                    startDate, endDate )
                +
                " ") : "")
            + (pi.getStartEventBoundary() != null ? ("and " +
                sb.getBoundaryCondition( pi.getStartEventBoundary(), pi,
                    startDate, endDate )
                +
                " ") : "")
            + "and ps = '" + programStage + "')";
    }

    /**
     * Get the description for the first arg #{programStageUid.dataElementUid}
     * and return a value with its data type.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item (of the right type, for type checking)
     */
    protected Object getProgramStageElementDescription( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        validateCountFunctionArgs( ctx );

        return (new ProgramItemStageElement()).getDescription( ctx, visitor );
    }

    /**
     * Generate the conditional part of the SQL for a d2 count function
     *
     * @param ctx the expression context
     * @param visitor the program indicator expression tree visitor
     * @return the conditional part of the SQL
     */
    public abstract String getConditionSql( ExprContext ctx, CommonExpressionVisitor visitor );

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void validateCountFunctionArgs( ExprContext ctx )
    {
        if ( !(getProgramArgType( ctx ) instanceof ProgramItemStageElement) )
        {
            throw new ParserExceptionWithoutContext(
                "First argument not supported for d2:count... functions: " + ctx.getText() );
        }
    }
}
