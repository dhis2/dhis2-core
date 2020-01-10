package org.hisp.dhis.program.function;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.parser.expression.function.ScalarFunctionToEvaluate;
import org.hisp.dhis.parser.expression.function.SimpleScalarFunction;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;

import java.util.Date;

import static org.hisp.dhis.antlr.AntlrParserUtils.castDate;
import static org.hisp.dhis.parser.expression.CommonExpressionVisitor.DEFAULT_DOUBLE_VALUE;

/**
 * @Author Zubair Asghar.
 */
public abstract class ProgramMinMaxFunction
    implements ScalarFunctionToEvaluate
{
    @Override
    public Object evaluate( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor )
    {
        if ( ctx.compareDate( 0 ) != null )
        {
            validateProgramStage( ctx.compareDate( 0 ), visitor );

            return DEFAULT_DOUBLE_VALUE;
        }

        visitor.validateStageDataElement( ctx.getText(),
            ctx.item( 0 ).uid0.getText(),
            ctx.item( 0 ).uid1.getText() );

        return DEFAULT_DOUBLE_VALUE;
    }

    @Override
    public Object getSql( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor )
    {
        ProgramIndicator pi = visitor.getProgramIndicator();
        StatementBuilder sb = visitor.getStatementBuilder();

        if ( AnalyticsType.EVENT == pi.getAnalyticsType() )
        {
            if ( ctx.compareDate(0 ) != null )
            {
                return "executiondate";
            }
            else
            {
                return ctx.item( 0 ).uid1.getText();
            }
        }

        Date startDate = visitor.getReportingStartDate();
        Date endDate = visitor.getReportingEndDate();

        String eventTableName = "analytics_event_" + pi.getProgram().getUid();
        String programStage = "";
        String columnName = "";

        if ( ctx.compareDate( 0 ) != null ) // When latest or oldest event is needed i.e d2:maxValue(PS_EVENTDATE:<psUid0>)
        {
            columnName = "\"executiondate\"";
            programStage = ctx.compareDate( 0 ).uid0.getText();
        }
        else  // When min/max value of data element is needed i.e d2:maxValue(#{uid0.uid1})
        {
            programStage = ctx.item( 0 ).uid0.getText();
            String dataElement = ctx.item( 0 ).uid1.getText();
            columnName = "\"" + dataElement + "\"";
        }

        return  "(select " + getAggregationOperator() + "(" + columnName + ") from " + eventTableName +
            " where " + eventTableName + ".pi = " + StatementBuilder.ANALYTICS_TBL_ALIAS + ".pi " +
            ( pi.getEndEventBoundary() != null ? ( "and " + sb.getBoundaryCondition( pi.getEndEventBoundary(), pi, startDate, endDate ) + " " ) : "" ) +
            ( pi.getStartEventBoundary() != null ? ( "and " + sb.getBoundaryCondition( pi.getStartEventBoundary(), pi, startDate, endDate ) + " " ) : "" ) + "and ps = '" + programStage + "')";
    }

    /***
     * Generate the function part of the SQL
     * @return string sql min/max functions
     */
    public abstract String getAggregationOperator();

    private void validateProgramStage( ExpressionParser.CompareDateContext ctx, CommonExpressionVisitor visitor )
    {
        if ( ctx.uid0 != null )
        {
            String psUid = ctx.uid0.getText();

            ProgramStage ps = visitor.getProgramStageService().getProgramStage( psUid );

            if ( ps == null )
            {
                throw new ParserExceptionWithoutContext( "Program stage " + ctx.uid0.getText() + " not found" );
            }

            visitor.getItemDescriptions().put( psUid, ps.getDisplayName() );

            return;
        }

        castDate( visitor.visit( ctx.expr() ) );
    }
}
