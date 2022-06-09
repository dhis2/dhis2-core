/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.antlr.AntlrParserUtils.trimQuotes;
import static org.hisp.dhis.parser.expression.CommonExpressionVisitor.DEFAULT_DOUBLE_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;

/**
 * Program indicator function: d2 count if condition
 *
 * @author Jim Grace
 */
public class D2CountIfCondition
    extends ProgramCountFunction
{
    @Override
    public Object getDescription( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        castDouble( getProgramStageElementDescription( ctx, visitor ) );

        String conditionExpression = getConditionalExpression( ctx );

        visitor.getProgramIndicatorService().validate( conditionExpression,
            Boolean.class, visitor.getItemDescriptions() );

        return DEFAULT_DOUBLE_VALUE;
    }

    @Override
    public String getConditionSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        String conditionExpression = getConditionalExpression( ctx );

        String conditionSql = visitor.getProgramIndicatorService().getAnalyticsSql(
            conditionExpression, BOOLEAN, visitor.getProgramIndicator(),
            visitor.getReportingStartDate(), visitor.getReportingEndDate() );

        return conditionSql.substring( 1 );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets a complete expression that is used to test a condition (e.g. "<5")
     * by putting a "0" in front to get a complete expression (e.g., "0<5").
     *
     * @param ctx the expression context
     * @return the complete expression
     */
    private String getConditionalExpression( ExprContext ctx )
    {
        return "0" + trimQuotes( ctx.stringLiteral().getText() );
    }
}
