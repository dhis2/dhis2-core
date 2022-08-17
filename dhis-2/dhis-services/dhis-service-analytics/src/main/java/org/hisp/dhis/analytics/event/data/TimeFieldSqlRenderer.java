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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.AnalyticsDateFilter;
import org.hisp.dhis.common.DateRange;

public abstract class TimeFieldSqlRenderer
{

    public String renderTimeFieldSql( EventQueryParams params )
    {
        StringBuilder sql = new StringBuilder();

        if ( params.hasNonDefaultBoundaries() )
        {
            sql.append( getSqlConditionForNonDefaultBoundaries( params ) );
        }
        // When multiple periods are set
        else if ( params.hasStartEndDate() || !params.getDateRangeByDateFilter().isEmpty() )
        {
            sql.append( getSqlConditionHasStartEndDate( params ) );
        }
        // Periods should not go here when line list, only pivot table
        else
        {
            sql.append( getSqlConditionForPeriods( params ) );
        }

        return sql.toString();
    }

    @Data
    @Builder
    private static class ColumnWithDateRange
    {
        private final String column;

        private final DateRange dateRange;

        static ColumnWithDateRange of( Map.Entry<AnalyticsDateFilter, DateRange> entry )
        {
            return ColumnWithDateRange.builder()
                .column( quoteAlias( entry.getKey().getTimeField().getField() ) )
                .dateRange( entry.getValue() )
                .build();
        }
    }

    protected Optional<TimeField> getTimeField( EventQueryParams params )
    {
        return TimeField.of( params.getTimeField() )
            .filter( this::isAllowed );
    }

    private boolean isAllowed( TimeField timeField )
    {
        return getAllowedTimeFields().contains( timeField );
    }

    protected abstract String getSqlConditionForPeriods( EventQueryParams params );

    /**
     * renders all periods, which are now organized by dateFilter, into SQL
     */
    protected String getSqlConditionHasStartEndDate( EventQueryParams params )
    {

        ColumnWithDateRange defaultColumn = params.hasStartEndDate() ? ColumnWithDateRange.builder()
            .column( getColumnName( params ) )
            .dateRange( new DateRange( params.getStartDate(), params.getEndDate() ) )
            .build() : null;

        return Stream.concat(
            Stream.of( defaultColumn ),
            params.getDateRangeByDateFilter().entrySet().stream().map( ColumnWithDateRange::of ) )
            .filter( Objects::nonNull )
            .map( columnWithDateRange -> new StringBuilder()
                .append( columnWithDateRange.getColumn() )
                .append( " >= '" )
                .append( getMediumDateString( columnWithDateRange.getDateRange().getStartDate() ) )
                .append( "' and " )
                .append( columnWithDateRange.getColumn() )
                .append( " < '" )
                .append( getMediumDateString( columnWithDateRange.getDateRange().getEndDatePlusOneDay() ) )
                .append( "' " )
                .toString() )
            .collect( Collectors.joining( " and " ) );
    }

    protected abstract String getColumnName( EventQueryParams params );

    protected abstract String getSqlConditionForNonDefaultBoundaries( EventQueryParams params );

    protected abstract Collection<TimeField> getAllowedTimeFields();

}
