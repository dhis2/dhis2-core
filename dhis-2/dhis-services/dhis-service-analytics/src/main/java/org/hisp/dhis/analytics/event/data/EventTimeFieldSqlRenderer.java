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

import static java.util.Collections.singleton;
import static org.hisp.dhis.analytics.EventOutputType.ENROLLMENT;
import static org.hisp.dhis.analytics.TimeField.EVENT_DATE;
import static org.hisp.dhis.analytics.event.data.JdbcEventAnalyticsManager.OPEN_IN;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.AnalyticsDateFilter;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EventTimeFieldSqlRenderer extends TimeFieldSqlRenderer
{

    private final StatementBuilder statementBuilder;

    @Getter
    private final Collection<TimeField> allowedTimeFields = singleton( TimeField.LAST_UPDATED );

    @Override
    protected String getSqlConditionForPeriods( EventQueryParams params )
    {
        final List<DimensionalItemObject> periods = params.getDimensionOrFilterItems( PERIOD_DIM_ID );

        Optional<TimeField> timeField = getTimeField( params );

        StringBuilder sql = new StringBuilder();
        if ( timeField.isPresent() )
        {
            sql.append( periods.stream()
                .filter( dimensionalItemObject -> dimensionalItemObject instanceof Period )
                .map( dimensionalItemObject -> (Period) dimensionalItemObject )
                .map( period -> toSqlCondition( period, timeField.get() ) )
                .collect( Collectors.joining( " or ", "(", ")" ) ) );
        }
        else
        {
            String alias = getPeriodAlias( params );

            sql.append( quote( alias, params.getPeriodType().toLowerCase() ) )
                .append( OPEN_IN )
                .append( getQuotedCommaDelimitedString( getUids( periods ) ) )
                .append( ") " );
        }
        return sql.toString();
    }

    @Override
    protected String getSqlConditionHasStartEndDate( EventQueryParams params )
    {

        ColumnWithDateRange defaultColumn = params.hasStartEndDate() ? ColumnWithDateRange.builder()
            .column( getTimeCol( params.getOutputType(), getTimeField( params ) ) )
            .dateRange( new DateRange( params.getStartDate(), params.getEndDate() ) )
            .build() : null;

        return Stream.concat(
            Stream.of( defaultColumn ),
            params.getDateRangeByDateFilter().entrySet().stream().map( this::toColumnWithDateRange ) )
            .filter( Objects::nonNull )
            .map( columnWithDateRange -> new StringBuilder()
                .append( columnWithDateRange.getColumn() )
                .append( " >= '" )
                .append( getMediumDateString( columnWithDateRange.getDateRange().getStartDate() ) )
                .append( "' and " )
                .append( columnWithDateRange.getColumn() )
                .append( " <= '" )
                .append( getMediumDateString( columnWithDateRange.getDateRange().getEndDate() ) )
                .append( "' " )
                .toString() )
            .collect( Collectors.joining( " and " ) );

    }

    private ColumnWithDateRange toColumnWithDateRange( Map.Entry<AnalyticsDateFilter, DateRange> entry )
    {
        return ColumnWithDateRange.builder()
            .column( quoteAlias( entry.getKey().getTimeField().getField() ) )
            .dateRange( entry.getValue() )
            .build();
    }

    @Data
    @Builder
    private static class ColumnWithDateRange
    {
        private final String column;

        private final DateRange dateRange;
    }

    @Override
    protected String getSqlConditionForNonDefaultBoundaries( EventQueryParams params )
    {
        return params.getProgramIndicator().getAnalyticsPeriodBoundaries().stream()
            .map( analyticsPeriodBoundary -> statementBuilder.getBoundaryCondition( analyticsPeriodBoundary,
                params.getProgramIndicator(),
                params.getTimeFieldAsField(), params.getEarliestStartDate(), params.getLatestEndDate() ) )
            .collect( Collectors.joining( " and " ) );
    }

    private String getTimeCol( EventOutputType outputType, Optional<TimeField> timeField )
    {
        if ( ENROLLMENT.equals( outputType ) )
        {
            return quoteAlias( TimeField.ENROLLMENT_DATE.getField() );
        }
        // EVENTS
        return quoteAlias(
            timeField
                .map( TimeField::getField )
                .orElse( EVENT_DATE.getField() ) );
    }

    private String toSqlCondition( Period period, TimeField timeField )
    {
        String timeCol = quoteAlias( timeField.getField() );
        return "( " + timeCol + " >= '" + getMediumDateString( period.getStartDate() ) + "' and " + timeCol + " <= '"
            + getMediumDateString( period.getEndDate() ) + "') ";
    }

    private String getPeriodAlias( EventQueryParams params )
    {
        return params.hasTimeField() ? DATE_PERIOD_STRUCT_ALIAS : ANALYTICS_TBL_ALIAS;
    }

}
