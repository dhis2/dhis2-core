/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dataelement;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DataElementTest
{
    private PeriodType periodType = new MonthlyPeriodType();

    @Test
    public void testGetCategoryCombos()
    {
        CategoryCombo ccA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION );
        CategoryCombo ccB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION );

        DataSet dsA = new DataSet( "DataSetA" );
        DataSet dsB = new DataSet( "DataSetB" );

        DataElement deA = new DataElement( "DataElementA" );
        DataElement deB = new DataElement( "DataElementB" );

        deA.setCategoryCombo( ccA );
        dsA.addDataSetElement( deA );
        dsB.addDataSetElement( deA, ccB );

        assertEquals( 2, deA.getCategoryCombos().size() );
        assertEquals( Sets.newHashSet( ccA, ccB ), deA.getCategoryCombos() );

        deB.setCategoryCombo( ccA );
        dsB.addDataSetElement( deB );

        assertEquals( 1, deB.getCategoryCombos().size() );
        assertEquals( Sets.newHashSet( ccA ), deB.getCategoryCombos() );
    }

    @Test
    public void testGetCategoryComboDataSet()
    {
        CategoryCombo ccA = new CategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION );
        CategoryCombo ccB = new CategoryCombo( "CategoryComboB", DataDimensionType.DISAGGREGATION );

        DataSet dsA = new DataSet( "DataSetA" );
        DataSet dsB = new DataSet( "DataSetB" );

        DataElement deA = new DataElement( "DataElementA" );
        DataElement deB = new DataElement( "DataElementB" );

        deA.setCategoryCombo( ccA );
        dsA.addDataSetElement( deA );
        dsB.addDataSetElement( deA, ccB );

        assertEquals( ccA, deA.getDataElementCategoryCombo( dsA ) );
        assertEquals( ccB, deA.getDataElementCategoryCombo( dsB ) );

        deB.setCategoryCombo( ccA );
        dsA.addDataSetElement( deB );

        assertEquals( ccA, deB.getDataElementCategoryCombo( dsA ) );
        assertEquals( ccA, deB.getDataElementCategoryCombo( dsB ) );
    }

    @Test
    public void testGetPeriodType()
    {
        DataElement de = new DataElement();

        DataSet dsA = new DataSet( "A", periodType );
        DataSet dsB = new DataSet( "B", periodType );
        DataSet dsC = new DataSet( "C", periodType );

        dsA.addDataSetElement( de );
        dsB.addDataSetElement( de );
        dsC.addDataSetElement( de );

        assertEquals( periodType, de.getPeriodType() );
    }

    @Test
    public void testPeriodTypeIsValid()
    {
        DataElement de = new DataElement();

        DataSet dsA = new DataSet( "A", new MonthlyPeriodType() );
        DataSet dsB = new DataSet( "B", new MonthlyPeriodType() );
        DataSet dsC = new DataSet( "C", new QuarterlyPeriodType() );

        dsA.addDataSetElement( de );
        dsB.addDataSetElement( de );

        assertTrue( de.periodTypeIsValid() );

        dsC.addDataSetElement( de );

        assertFalse( de.periodTypeIsValid() );
    }

    @Test
    public void testGetOpenFuturePeriods()
    {
        DataElement deA = new DataElement( "A" );

        assertEquals( 0, deA.getOpenFuturePeriods() );

        DataSet dsA = new DataSet( "A" );
        DataSet dsB = new DataSet( "B" );

        dsA.setOpenFuturePeriods( 0 );
        dsB.setOpenFuturePeriods( 3 );

        dsA.addDataSetElement( deA );

        assertEquals( 0, deA.getOpenFuturePeriods() );

        dsB.addDataSetElement( deA );

        assertEquals( 3, deA.getOpenFuturePeriods() );
    }

    @Test
    public void testGetLatestOpenFuturePeriod()
    {
        DataElement deA = new DataElement( "A" );

        assertEquals( 0, deA.getOpenFuturePeriods() );

        DataSet dsA = new DataSet( "A", periodType );
        DataSet dsB = new DataSet( "B", periodType );

        dsA.setOpenFuturePeriods( 3 );
        dsB.setOpenFuturePeriods( 3 );

        dsA.addDataSetElement( deA );
        dsB.addDataSetElement( deA );

        Period lastOpen = deA.getLatestOpenFuturePeriod();

        assertTrue( lastOpen.isAfter( new MonthlyPeriodType().createPeriod() ) );
    }

    @Test
    public void testIsExpired_BeforeFirstDayOfPeriod()
    {
        assertIsExpired( false, period -> new Date( period.getStartDate().getTime() - 1L ) );
    }

    @Test
    public void testIsExpired_FirstDayOfPeriod()
    {
        assertIsExpired( false, Period::getStartDate );
    }

    @Test
    public void testIsExpired_LastDayOfPeriod()
    {
        assertIsExpired( false, Period::getEndDate );
    }

    @Test
    public void testIsExpired_AfterLastDayOfPeriod()
    {
        // expiryDays is 1 so 1 extra day after the end is still ok
        assertIsExpired( false, period -> new Date( period.getEndDate().getTime() + TimeUnit.DAYS.toMillis( 1 ) ) );
        // but 2 is too much
        assertIsExpired( true, period -> new Date( period.getEndDate().getTime() + TimeUnit.DAYS.toMillis( 2 ) ) );
    }

    private void assertIsExpired( boolean expected, Function<Period, Date> actual )
    {
        Date now = new Date();
        Period thisMonth = periodType.createPeriod( now );
        DataElement de = new DataElement();
        DataSet ds = new DataSet();
        ds.setExpiryDays( 1 );
        de.setDataSetElements( Set.of( new DataSetElement( ds, de ) ) );
        assertEquals( expected, de.isExpired( thisMonth, actual.apply( thisMonth ) ) );
    }
}
