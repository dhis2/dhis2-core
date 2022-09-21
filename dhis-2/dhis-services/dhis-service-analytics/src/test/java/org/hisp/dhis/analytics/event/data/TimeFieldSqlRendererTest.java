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

import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

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
    void testRenderEventTimeFieldSqlWhenNonContinuousDateRangeList()
    {
        // Given
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peC ) ) )
            .build();
        TimeFieldSqlRenderer timeFieldSqlRenderer = new EventTimeFieldSqlRenderer( new PostgreSQLStatementBuilder() );

        // When
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        // Then
        assertEquals(
            "(ax.\"executiondate\">='2022-04-01'andax.\"executiondate\"<'2022-05-01'orax.\"executiondate\">='2022-06-01'andax.\"executiondate\"<'2022-07-01')",
            timeFieldSqlRenderer.renderTimeFieldSql( params ).replace( " ", "" ) );
    }

    @Test
    void testRenderEventTimeFieldSqlWhenContinuousDateRangeList()
    {
        // Given
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB, peC ) ) )
            .build();
        TimeFieldSqlRenderer timeFieldSqlRenderer = new EventTimeFieldSqlRenderer( new PostgreSQLStatementBuilder() );

        // When
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        // Then
        assertEquals( "ax.\"executiondate\">='2022-04-01'andax.\"executiondate\"<'2022-07-01'",
            timeFieldSqlRenderer.renderTimeFieldSql( params ).replace( " ", "" ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenNonContinuousDateRangeList()
    {
        // Given
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peC ) ) )
            .build();
        TimeFieldSqlRenderer timeFieldSqlRenderer = new EnrollmentTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        // When
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        // Then
        assertEquals(
            "(enrollmentdate>='2022-04-01'andenrollmentdate<'2022-05-01'orenrollmentdate>='2022-06-01'andenrollmentdate<'2022-07-01')",
            timeFieldSqlRenderer.renderTimeFieldSql( params ).replace( " ", "" ) );
    }

    @Test
    void testRenderEnrollmentTimeFieldSqlWhenContinuousDateRangeList()
    {
        // Given
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB, peC ) ) )
            .build();
        TimeFieldSqlRenderer timeFieldSqlRenderer = new EnrollmentTimeFieldSqlRenderer(
            new PostgreSQLStatementBuilder() );

        // When
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        // Then
        assertEquals( "enrollmentdate>='2022-04-01'andenrollmentdate<'2022-07-01'",
            timeFieldSqlRenderer.renderTimeFieldSql( params ).replace( " ", "" ) );
    }
}