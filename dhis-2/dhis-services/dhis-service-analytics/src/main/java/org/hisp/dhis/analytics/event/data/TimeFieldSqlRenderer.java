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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
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
        else if ( params.useStartEndDates() )
        {
            sql.append( getSqlDateRangeCondition( params ) );
        }
        // Periods condition only for pivot table.
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

        static ColumnWithDateRange of( String column, DateRange dateRange )
        {
            return ColumnWithDateRange.builder()
                .column( column )
                .dateRange( dateRange )
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
     * Renders all periods, which are now organized by dateFilter, into SQL
     */
    protected String getSqlDateRangeCondition( EventQueryParams params )
    {
        List<String> orConditions = new ArrayList<>();

        if ( params.hasStartEndDate() )
        {
            addDefaultDateToConditions( params, orConditions );
        }

        params.getTimeDateRanges().forEach( ( timeField, dateRanges ) -> {
            if ( params.hasContinuousDateRangeList( dateRanges ) )
            {
                // Picks the start date of the first range and end date of the last range.
                DateRange dateRange = new DateRange( dateRanges.get( 0 ).getStartDate(),
                    dateRanges.get( dateRanges.size() - 1 ).getEndDate() );

                ColumnWithDateRange columnWithDateRange = ColumnWithDateRange.of(
                    getColumnName( Optional.of( timeField ), params.getOutputType() ), dateRange );
                orConditions.add( getDateRangeCondition( columnWithDateRange ) );
            }
            else
            {
                dateRanges.forEach( ( dateRange ) -> {
                    ColumnWithDateRange columnWithDateRange = ColumnWithDateRange.of(
                        getColumnName( Optional.of( timeField ), params.getOutputType() ), dateRange );
                    orConditions.add( getDateRangeCondition( columnWithDateRange ) );
                } );
            }
        } );

        return isNotEmpty( orConditions ) ? "(" + String.join( " or ", orConditions ) + ")" : EMPTY;
    }

    private void addDefaultDateToConditions( EventQueryParams params, List<String> conditions )
    {
        ColumnWithDateRange defaultDateRangeColumn = ColumnWithDateRange.builder()
            .column( getColumnName( TimeField.of( params.getTimeField() ), params.getOutputType() ) )
            .dateRange( new DateRange( params.getStartDate(), params.getEndDate() ) )
            .build();

        conditions.add( getDateRangeCondition( defaultDateRangeColumn ) );
    }

    private String getDateRangeCondition( ColumnWithDateRange defaultDateRangeColumn )
    {
        return "(" +
            defaultDateRangeColumn.getColumn() +
            " >= '" +
            getMediumDateString( defaultDateRangeColumn.getDateRange().getStartDate() ) +
            "' and " +
            defaultDateRangeColumn.getColumn() +
            " < '" +
            getMediumDateString( defaultDateRangeColumn.getDateRange().getEndDatePlusOneDay() ) +
            "')";
    }

    protected abstract String getColumnName( Optional<TimeField> timeField, EventOutputType outputType );

    protected abstract String getSqlConditionForNonDefaultBoundaries( EventQueryParams params );

    protected abstract Collection<TimeField> getAllowedTimeFields();
}
