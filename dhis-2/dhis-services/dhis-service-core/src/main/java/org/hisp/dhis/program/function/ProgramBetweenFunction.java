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

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.function.ScalarFunctionToEvaluate;
import org.hisp.dhis.program.ProgramStage;

import static org.hisp.dhis.antlr.AntlrParserUtils.castDate;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.parser.expression.CommonExpressionVisitor.DEFAULT_DOUBLE_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.CompareDateContext;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

/**
 * Program indicator date/time between functions
 *
 * @author Jim Grace
 */
public abstract class ProgramBetweenFunction
    implements ScalarFunctionToEvaluate
{
    @Override
    public final Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        validateDateArg( ctx.compareDate( 0 ), visitor );
        validateDateArg( ctx.compareDate( 1 ), visitor );

        return DEFAULT_DOUBLE_VALUE;
    }

    @Override
    public final Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        String startDate = getCompareDate( ctx.compareDate( 0 ), visitor );
        String endDate = getCompareDate( ctx.compareDate( 1 ), visitor );

        return getSqlBetweenDates( startDate, endDate );
    }

    /**
     * Generate SQL to compare dates, based on the time/date unit to compare.
     *
     * @param startDate starting date
     * @param endDate ending date
     * @return the SQL to compare the dates based on the time/date unit
     */
    public abstract Object getSqlBetweenDates( String startDate, String endDate );

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Validates a compare date
     *
     * @param ctx     program function compare date context
     * @param visitor the program indicator expression tree visitor
     */
    private void validateDateArg( CompareDateContext ctx, CommonExpressionVisitor visitor )
    {
        if ( ctx.uid0 != null )
        {
            String programStageUid = ctx.uid0.getText();

            ProgramStage programStage = visitor.getProgramStageService().getProgramStage( programStageUid );

            if ( programStage == null )
            {
                throw new ParserExceptionWithoutContext( "Program stage " + ctx.uid0.getText() + " not found" );
            }

            visitor.getItemDescriptions().put( programStageUid, programStage.getDisplayName() );

            return;
        }

        castDate( visitor.visit( ctx.expr() ) );
    }

    /**
     * Resolves a start or end date program function argument.
     * Don't replace nulls while evaluating the sub-expression,
     * because we don't have a good thing to replace null dates with;
     * it's better to let null dates remain null so the date comparison
     * for that event will not be evaluated and aggregated with the
     * non-null date values.
     *
     * @param ctx program function compare date context
     * @param visitor the program indicator expression tree visitor
     * @return the resolved date
     */
    private String getCompareDate( CompareDateContext ctx, CommonExpressionVisitor visitor )
    {
        if ( ctx.uid0 != null )
        {
            return visitor.getStatementBuilder().getProgramIndicatorEventColumnSql(
                ctx.uid0.getText(), "executiondate",
                visitor.getReportingStartDate(), visitor.getReportingEndDate(), visitor.getProgramIndicator() );
        }

        return castString( visitor.visitAllowingNulls( ctx.expr() ) );
    }
}
