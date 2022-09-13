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
package org.hisp.dhis.analytics.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.system.grid.ListGrid;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

class PeriodOffsetUtilsTest
{
    @Test
    void verifyAddShiftedPeriods()
    {
        // Given
        Period month1 = createMonthlyPeriod( 2020, 1 );
        Period month2 = createMonthlyPeriod( 2020, 2 );
        Period month3 = createMonthlyPeriod( 2020, 3 );
        Period q1 = createQuarterPeriod( 2020, 1 );
        DataElement dataElement = createDataElement( -1 );
        DataQueryParams queryParams = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( dataElement ) )
            .withPeriods( Lists.newArrayList( month1, month2, month3, q1 ) ).build();
        // When
        DataQueryParams params = PeriodOffsetUtils.addShiftedPeriods( queryParams );
        // Then
        assertIsoPeriodsInOrder( params.getPeriods(), "202001", "202002", "202003", "2020Q1", "201912", "2019Q4" );
    }

    @Test
    void verifyAddShiftedPeriodsWithNothingShifted()
    {
        // Given
        Period month1 = createMonthlyPeriod( 2020, 1 );
        Period q1 = createQuarterPeriod( 2020, 1 );
        DataElement dataElement = createDataElement( 0 );
        DataQueryParams queryParams = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( dataElement ) )
            .withPeriods( Lists.newArrayList( month1, q1 ) ).build();
        // When
        DataQueryParams params = PeriodOffsetUtils.addShiftedPeriods( queryParams );
        // Then
        assertThat( params.getPeriods(), is( queryParams.getPeriods() ) );
    }

    @Test
    void verifyShiftPeriod()
    {
        Period p1 = PeriodOffsetUtils.shiftPeriod( createMonthlyPeriod( 2020, 1 ), 12 );
        assertThat( p1.getIsoDate(), is( "202101" ) );
        Period p2 = PeriodOffsetUtils.shiftPeriod( createQuarterPeriod( 2020, 1 ), 12 );
        assertThat( p2.getIsoDate(), is( "2023Q1" ) );
        Period p3 = PeriodOffsetUtils.shiftPeriod( createWeeklyType( 2020, 5, 1 ), 2 );
        assertThat( p3.getIsoDate(), is( "2020W20" ) );
        Period p4 = PeriodOffsetUtils.shiftPeriod( createMonthlyPeriod( 2020, 1 ), -12 );
        assertThat( p4.getIsoDate(), is( "201901" ) );
        Period p5 = PeriodOffsetUtils.shiftPeriod( createQuarterPeriod( 2020, 1 ), -12 );
        assertThat( p5.getIsoDate(), is( "2017Q1" ) );
        Period p6 = PeriodOffsetUtils.shiftPeriod( createWeeklyType( 2020, 5, 1 ), -2 );
        assertThat( p6.getIsoDate(), is( "2020W16" ) );
    }

    @Test
    void verifyGetPeriodOffsetRow()
    {
        // Given
        Grid grid = new ListGrid();
        grid.addHeader( new GridHeader( DimensionalObject.DATA_X_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.ORGUNIT_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.PERIOD_DIM_ID ) );
        int periodIndex = 2;
        grid.addRow();
        grid.addValue( "de1" );
        grid.addValue( "ou2" );
        grid.addValue( "202001" );
        grid.addValue( 3 );
        grid.addRow();
        grid.addValue( "de1" );
        grid.addValue( "ou3" );
        grid.addValue( "202002" );
        grid.addValue( 5 );
        DataElement dataElement = createDataElement( 0 );
        dataElement.setUid( "de1" );

        // When
        List<Object> row1 = PeriodOffsetUtils.getPeriodOffsetRow( grid.getRow( 0 ), periodIndex, 1 );
        // Then
        assertThat( row1, is( notNullValue() ) );
        assertThat( row1, hasSize( 4 ) );
        assertThat( row1.get( periodIndex ), is( "201912" ) );

        // When
        List<Object> row2 = PeriodOffsetUtils.getPeriodOffsetRow( grid.getRow( 1 ), periodIndex, -1 );
        // Then
        assertThat( row2, is( notNullValue() ) );
        assertThat( row2, hasSize( 4 ) );
        assertThat( row2.get( periodIndex ), is( "202003" ) );
    }

    private void assertIsoPeriodsInOrder( List<DimensionalItemObject> periods, String... isoPeriod )
    {
        List<String> isoPeriods = periods.stream().map( dim -> (Period) dim ).map( Period::getIsoDate )
            .collect( Collectors.toList() );
        assertThat( isoPeriods, is( Arrays.asList( isoPeriod ) ) );
    }

    private Period createMonthlyPeriod( int year, int month )
    {
        CalendarPeriodType periodType = new MonthlyPeriodType();
        return periodType.createPeriod( new DateTime( year, month, 1, 0, 0 ).toDate() );
    }

    private Period createQuarterPeriod( int year, int month )
    {
        CalendarPeriodType periodType = new QuarterlyPeriodType();
        return periodType.createPeriod( new DateTime( year, month, 1, 0, 0 ).toDate() );
    }

    private Period createWeeklyType( int year, int month, int day )
    {
        CalendarPeriodType periodType = new WeeklyPeriodType();
        return periodType.createPeriod( new DateTime( year, month, day, 0, 0 ).toDate() );
    }

    private DataElement createDataElement( int offset )
    {
        DataElement de = DhisConvenienceTest.createDataElement( 'A' );
        de.setQueryMods( QueryModifiers.builder().periodOffset( offset ).build() );
        return de;
    }
}
