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

import static org.hisp.dhis.analytics.TimeField.INCIDENT_DATE;
import static org.hisp.dhis.analytics.TimeField.LAST_UPDATED;
import static org.hisp.dhis.analytics.TimeField.SCHEDULED_DATE;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;
import static org.hisp.dhis.program.AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dusan Bernat
 */
class TimeFieldSqlRendererTest extends DhisConvenienceTest
{
    private Period peA;

    private Period peB;

    private Period peC;

    @BeforeEach
    void before()
    {
        peA = new MonthlyPeriodType().createPeriod( new DateTime( 2022, 4, 1, 0, 0 ).toDate() );
        peB = new MonthlyPeriodType().createPeriod( new DateTime( 2022, 5, 1, 0, 0 ).toDate() );
        peC = new MonthlyPeriodType().createPeriod( new DateTime( 2022, 6, 1, 0, 0 ).toDate() );
    }

    @Test
    void testRenderEventTimeFieldSqlWhenNonContinuousDateRange()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( peA, peC ) ) )
            .build();

        TimeFieldSqlRenderer timeFieldSqlRenderer = new EventTimeFieldSqlRenderer( new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((ax.\"executiondate\" >= '2022-04-01' and ax.\"executiondate\" < '2022-07-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testRenderEventTimeFieldSqlWhenContinuousDateRange()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( peA, peB, peC ) ) )
            .build();

        TimeFieldSqlRenderer timeFieldSqlRenderer = new EventTimeFieldSqlRenderer( new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((ax.\"executiondate\" >= '2022-04-01' and ax.\"executiondate\" < '2022-07-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenNonContinuousDateRange()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( peA, peC ) ) )
            .build();
        TimeFieldSqlRenderer timeFieldSqlRenderer = new EnrollmentTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((enrollmentdate >= '2022-04-01' and enrollmentdate < '2022-07-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenContinuousDateRange()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( peA, peB, peC ) ) )
            .build();

        TimeFieldSqlRenderer timeFieldSqlRenderer = new EnrollmentTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((enrollmentdate >= '2022-04-01' and enrollmentdate < '2022-07-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenContinuousDateRangeWithTimeFieldAllowed()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( peA, peB, peC ) ) )
            .withTimeField( LAST_UPDATED.name() )
            .build();

        TimeFieldSqlRenderer timeFieldSqlRenderer = new EnrollmentTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((lastupdated >= '2022-04-01' and lastupdated < '2022-07-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenContinuousDateRangeWithTimeFieldNotAllowed()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( peA, peB, peC ) ) )
            .withTimeField( INCIDENT_DATE.getField() )
            .build();

        TimeFieldSqlRenderer timeFieldSqlRenderer = new EventTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((ax.\"executiondate\" >= '2022-04-01' and ax.\"executiondate\" < '2022-07-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenNoContinuousMonthlyPeriodWithTimeFieldAllowed()
    {
        Period march = new MonthlyPeriodType().createPeriod( new DateTime( 2022, 3, 1, 0, 0 ).toDate() );
        march.setDateField( "SCHEDULED_DATE" );
        march.setPeriodType( new MonthlyPeriodType() );

        Period september = new MonthlyPeriodType().createPeriod( new DateTime( 2022, 9, 1, 0, 0 ).toDate() );
        september.setDateField( "SCHEDULED_DATE" );
        march.setPeriodType( new MonthlyPeriodType() );

        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD, List.of( march, september ) ) )
            .withTimeField( SCHEDULED_DATE.getField() )
            .withStartEndDatesForPeriods()
            .build();

        TimeFieldSqlRenderer timeFieldSqlRenderer = new EventTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        assertEquals( "((ax.\"duedate\" >= '2022-03-01' and ax.\"duedate\" < '2022-04-01') " +
            "or (ax.\"duedate\" >= '2022-09-01' and ax.\"duedate\" < '2022-10-01'))",
            timeFieldSqlRenderer.renderPeriodTimeFieldSql( params ) );
    }

    @Test
    void testEnrollmentTimeFieldWithEventDate()
    {
        TimeFieldSqlRenderer timeFieldSqlRenderer = new EnrollmentTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        Set<AnalyticsPeriodBoundary> boundaries = Set.of(
            new AnalyticsPeriodBoundary( "EVENT_DATE", BEFORE_END_OF_REPORTING_PERIOD ) );

        ProgramIndicator programIndicator = mock( ProgramIndicator.class );
        when( programIndicator.getAnalyticsPeriodBoundaries() ).thenReturn( boundaries );

        EventQueryParams eventQueryParams = mock( EventQueryParams.class );
        when( eventQueryParams.getProgramIndicator() ).thenReturn( programIndicator );
        when( eventQueryParams.hasNonDefaultBoundaries() ).thenReturn( true );

        assertEquals( EMPTY, timeFieldSqlRenderer.renderPeriodTimeFieldSql( eventQueryParams ) );
    }
}
