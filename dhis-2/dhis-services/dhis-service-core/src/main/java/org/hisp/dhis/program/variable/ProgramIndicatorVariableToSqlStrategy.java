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

package org.hisp.dhis.program.variable;

import com.google.common.base.Preconditions;
import org.hisp.dhis.api.util.DateUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorVariable;

import java.util.Date;

import static org.hisp.dhis.api.util.DateUtils.getSqlDateString;
import static org.hisp.dhis.program.ProgramIndicatorVariable.*;

/**
 * A Strategy pattern implementation to resolve Program Indicator variables
 *
 * @author Luciano Fiandesio
 */
public interface ProgramIndicatorVariableToSqlStrategy
{

    String NULL_IF_CAST = "nullif(cast((";

    String resolve( String originalExpression, AnalyticsType analyticsType, ProgramIndicator indicator, Date startDate,
        Date endDate );

    static ProgramIndicatorVariableToSqlStrategy currentDateStrategy()
    {
        return ( originalExpression, analyticsType, indicator, date, endDate ) -> "'" + DateUtils.getLongDateString()
            + "'";
    }

    static ProgramIndicatorVariableToSqlStrategy eventDateStrategy( StatementBuilder statementBuilder, ProgramIndicatorVariable programIndicatorVariable )
    {
        return ( originalExpression, analyticsType, indicator, startDate, endDate ) -> statementBuilder
            .getProgramIndicatorEventColumnSql( null, programIndicatorVariable.getColumn(), startDate,
                endDate, indicator );
    }

    static ProgramIndicatorVariableToSqlStrategy valueCountStrategy( StatementBuilder statementBuilder )
    {
        return ( originalExpression, analyticsType, indicator, date, endDate ) -> {

            String sql = NULL_IF_CAST;

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( originalExpression,
                analyticsType ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " is not null then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + statementBuilder.getDoubleColumnType() + "),0)";
        };
    }

    static ProgramIndicatorVariableToSqlStrategy zeroPositionValueStrategy( StatementBuilder statementBuilder )
    {
        return ( originalExpression, analyticsType, indicator, date, endDate ) -> {

            String sql = NULL_IF_CAST;

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( originalExpression,
                analyticsType ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " >= 0 then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + statementBuilder.getDoubleColumnType() + "),0)";
        };
    }

    static ProgramIndicatorVariableToSqlStrategy distinctVarStrategy( String var )
    {
        return ( originalExpression, analyticsType, indicator, date, endDate ) -> "distinct "
            + ProgramIndicator.getVariableColumnName( var );

    }

    static ProgramIndicatorVariableToSqlStrategy programStageNameStrategy()
    {
        return ( originalExpression, analyticsType, indicator, date,
            endDate ) -> AnalyticsType.EVENT == analyticsType ? "(select name from programstage where uid = ps)" : "''";
    }

    static ProgramIndicatorVariableToSqlStrategy programStageIdStrategy( String var )
    {
        return ( originalExpression, analyticsType, indicator, date,
            endDate ) -> AnalyticsType.EVENT == analyticsType ? ProgramIndicator.getVariableColumnName( var ) : "''";
    }

    static ProgramIndicatorVariableToSqlStrategy dateStrategy( String var )
    {
        return ( originalExpression, analyticsType, indicator, startDate, endDate ) -> "'"
            + (var.equals( VAR_ANALYTICS_PERIOD_START.getVariableName() ) ? getSqlDateString( startDate )
                : getSqlDateString( endDate ))
            + "'";
    }

    static ProgramIndicatorVariableToSqlStrategy nullStrategy()
    {
        return ( originalExpression, analyticsType, indicator, date, endDate ) -> null;
    }

    static ProgramIndicatorVariableToSqlStrategy defaultStrategy( ProgramIndicatorVariable var )
    {
        return ( originalExpression, analyticsType, indicator, date, endDate ) -> var.getColumn();
    }

    /**
     * Get the Strategy class based on the variable name
     *
     * @param programIndicatorVariable the variable enum used to select the strategy
     *        class
     * @param statementBuilder a {@link StatementBuilder} implementation
     * @return an instance of {@link ProgramIndicatorVariableToSqlStrategy}
     */
    static ProgramIndicatorVariableToSqlStrategy getStrategy( ProgramIndicatorVariable programIndicatorVariable,
        StatementBuilder statementBuilder )
    {
        Preconditions.checkNotNull( statementBuilder );
        switch ( programIndicatorVariable )
        {
        case VAR_CURRENT_DATE:
            return currentDateStrategy();
        case VAR_CREATION_DATE:
            return eventDateStrategy( statementBuilder, VAR_CREATION_DATE );
        case VAR_EVENT_DATE:
            return eventDateStrategy( statementBuilder, VAR_EVENT_DATE );
        case VAR_VALUE_COUNT:
            return valueCountStrategy( statementBuilder );
        case VAR_ZERO_POS_VALUE_COUNT:
            return zeroPositionValueStrategy( statementBuilder );
        case VAR_EVENT_COUNT:
            return distinctVarStrategy( programIndicatorVariable.getVariableName() );
        case VAR_ENROLLMENT_COUNT:
            return distinctVarStrategy( programIndicatorVariable.getVariableName() );
        case VAR_TEI_COUNT:
            return distinctVarStrategy( programIndicatorVariable.getVariableName() );
        case VAR_PROGRAM_STAGE_NAME:
            return programStageNameStrategy();
        case VAR_PROGRAM_STAGE_ID:
            return programStageIdStrategy( programIndicatorVariable.getVariableName() );
        case VAR_ANALYTICS_PERIOD_START:
            return dateStrategy( VAR_ANALYTICS_PERIOD_START.getVariableName() );
        case VAR_ANALYTICS_PERIOD_END:
            return dateStrategy( VAR_ANALYTICS_PERIOD_END.getVariableName() );
        case VAR_UNDEFINED:
            return nullStrategy();
        default:
            return defaultStrategy( programIndicatorVariable );
        }
    }

}
