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

package org.hisp.dhis.analytics.event.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.event.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * @author Luciano Fiandesio
 */
@Component
public class DefaultProgramIndicatorSubqueryBuilder implements ProgramIndicatorSubqueryBuilder
{
    private final static String ANALYTICS_TABLE_NAME = "analytics";

    private final ProgramIndicatorService programIndicatorService;

    public DefaultProgramIndicatorSubqueryBuilder(ProgramIndicatorService programIndicatorService )
    {
        checkNotNull( programIndicatorService );

        this.programIndicatorService = programIndicatorService;
    }

    /**
     *
     * @param pi
     * @param outerSqlEntity
     * @param earliestStartDate
     * @param latestDate
     * @return
     */
    @Override
    public String getAggregateClauseForProgramIndicator( ProgramIndicator pi, AnalyticsType outerSqlEntity,
        Date earliestStartDate, Date latestDate )
    {
        // Define aggregation function (avg, sum, ...) //
        String function = TextUtils.emptyIfEqual( pi.getAggregationTypeFallback().getValue(),
            AggregationType.CUSTOM.getValue() );
        
        // Get sql construct from Program indicator expression //
        String aggregateSql = getPrgIndSql( pi.getExpression(), pi, earliestStartDate, latestDate );

        aggregateSql += ")";

        // Determine Table name from FROM clause
        aggregateSql += getFrom( pi );

        // Determine JOIN
        aggregateSql += getWhere( outerSqlEntity, pi );

        // Get WHERE condition from Program indicator filter
        if ( !Strings.isNullOrEmpty( pi.getFilter() ) )
        {
            aggregateSql += " AND " + getPrgIndSql( pi.getFilter(), pi, earliestStartDate, latestDate );
        }
        return "(SELECT " + function + " (" + aggregateSql + ")";
    }

    private String getFrom( ProgramIndicator pi )
    {
        return " FROM " + ANALYTICS_TABLE_NAME + "_" + pi.getAnalyticsType().getValue() + "_" + pi.getProgram().getUid();
    }

    /**
     *
     * 1) outer = event | inner = enrollment    -> pi = ax.pi (enrollment is the enrollmennt linked to the inline event)
     * 2) outer = enrollment | inner = event    -> pi = ax.pi
     * 3) outer = event | inner = event         -> psi = ax.psi (inner operate on same event as outer)
     * 4) outer = enrollemnt | inner = enrollment -> pi = ax.pi (enrollment operates on the same enrollment as outer)
     *
     * @param outerSqlEntity
     * @param pi
     * @return
     */
    private String getWhere( AnalyticsType outerSqlEntity, ProgramIndicator pi )
    {
        String condition = "";
        if ( isEnrollment( outerSqlEntity ) )
        {
            condition = "pi = ax.pi";
        }
        else
        {
            if ( isEvent( pi.getAnalyticsType() ) )
            {
                condition = "psi = ax.psi";
            }
        }

        return " WHERE " + condition;
    }

    private boolean isEnrollment( AnalyticsType outerSqlEntity )
    {
        return outerSqlEntity.equals( AnalyticsType.ENROLLMENT );
    }

    private boolean isEvent( AnalyticsType outerSqlEntity )
    {
        return outerSqlEntity.equals( AnalyticsType.EVENT );
    }
    
    private String getPrgIndSql( String expression, ProgramIndicator pi, Date earliestStartDate, Date latestDate )
    {
        return this.programIndicatorService.getAnalyticsSql( expression, pi, earliestStartDate, latestDate );
    }
}
