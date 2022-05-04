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
package org.hisp.dhis.program.variable;

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.program.AnalyticsType;

/**
 * Program indicator variable: event date (also used for execution date)
 *
 * @author Jim Grace
 */
public class vEventDate
    extends ProgramDateVariable
{
    @Override
    public Object getSql( CommonExpressionVisitor visitor )
    {
        ProgramExpressionParams params = visitor.getProgParams();

        if ( AnalyticsType.ENROLLMENT == params.getProgramIndicator().getAnalyticsType() )
        {
            String sqlStatement = visitor.getStatementBuilder().getProgramIndicatorEventColumnSql(
                null, "executiondate", params.getReportingStartDate(), params.getReportingEndDate(),
                params.getProgramIndicator() );

            return maybeAppendEventStatusFilterIntoWhere( sqlStatement );
        }

        return "executiondate";
    }

    private String maybeAppendEventStatusFilterIntoWhere( String sqlStatement )
    {
        int index = sqlStatement.indexOf( "order by executiondate" );

        if ( index == -1 )
        {
            return sqlStatement;
        }

        return sqlStatement.substring( 0, index )
            + " and psistatus IN ('COMPLETED', 'ACTIVE') "
            + sqlStatement.substring( index );
    }
}
