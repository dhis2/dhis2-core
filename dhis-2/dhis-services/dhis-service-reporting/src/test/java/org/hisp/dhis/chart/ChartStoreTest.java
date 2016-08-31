package org.hisp.dhis.chart;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class ChartStoreTest
    extends DhisSpringTest
{
    @Resource(name="org.hisp.dhis.chart.ChartStore")
    private GenericIdentifiableObjectStore<Chart> chartStore;
    
    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    
    private Indicator indicatorA;
    private Indicator indicatorB;
    private Indicator indicatorC;

    private Period periodA;
    private Period periodB;
    private Period periodC;

    private OrganisationUnit unitA;
    private OrganisationUnit unitB;

    private Chart chartA;
    private Chart chartB;
    private Chart chartC;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        // ---------------------------------------------------------------------
        // Indicator
        // ---------------------------------------------------------------------

        IndicatorType indicatorType = createIndicatorType( 'A' );

        indicatorA = createIndicator( 'A', indicatorType );
        indicatorB = createIndicator( 'B', indicatorType );
        indicatorC = createIndicator( 'C', indicatorType );

        indicatorService.addIndicatorType( indicatorType );
        indicatorService.addIndicator( indicatorA );
        indicatorService.addIndicator( indicatorB );
        indicatorService.addIndicator( indicatorC );

        List<Indicator> indicators = new ArrayList<>();
        indicators.add( indicatorA );
        indicators.add( indicatorB );
        indicators.add( indicatorC );

        // ---------------------------------------------------------------------
        // Period
        // ---------------------------------------------------------------------

        PeriodType periodType = new MonthlyPeriodType();

        periodA = createPeriod( periodType, getDate( 2000, 1, 1 ), getDate( 2000, 1, 2 ) );
        periodB = createPeriod( periodType, getDate( 2000, 1, 3 ), getDate( 2000, 1, 4 ) );
        periodC = createPeriod( periodType, getDate( 2000, 1, 5 ), getDate( 2000, 1, 6 ) );

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );

        List<Period> periods = new ArrayList<>();
        periods.add( periodA );
        periods.add( periodB );
        periods.add( periodC );

        // ---------------------------------------------------------------------
        // OrganisationUnit
        // ---------------------------------------------------------------------

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );

        List<OrganisationUnit> units = new ArrayList<>();
        units.add( unitA );
        units.add( unitB );
        // units.add( unitC );

        chartA = createChart( 'A', indicators, periods, units );
        chartB = createChart( 'B', indicators, periods, units );
        chartC = createChart( 'C', indicators, periods, units );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testSaveGet()
    {
        int idA = chartStore.save( chartA );
        int idB = chartStore.save( chartB );
        int idC = chartStore.save( chartC );

        assertEquals( chartA, chartStore.get( idA ) );
        assertEquals( chartB, chartStore.get( idB ) );
        assertEquals( chartC, chartStore.get( idC ) );

        assertTrue( equals( chartStore.get( idA ).getIndicators(), indicatorA, indicatorB, indicatorC ) );
        assertTrue( equals( chartStore.get( idA ).getOrganisationUnits(), unitA, unitB ) );
        assertTrue( equals( chartStore.get( idA ).getOrganisationUnits(), unitA, unitB ) );
        assertTrue( equals( chartStore.get( idA ).getPeriods(), periodA, periodB, periodC ) );
    }

    @Test
    public void testDelete()
    {
        int idA = chartStore.save( chartA );
        int idB = chartStore.save( chartB );
        int idC = chartStore.save( chartC );

        assertNotNull( chartStore.get( idA ) );
        assertNotNull( chartStore.get( idB ) );
        assertNotNull( chartStore.get( idC ) );

        chartStore.delete( chartA );

        assertNull( chartStore.get( idA ) );
        assertNotNull( chartStore.get( idB ) );
        assertNotNull( chartStore.get( idC ) );

        chartStore.delete( chartB );

        assertNull( chartStore.get( idA ) );
        assertNull( chartStore.get( idB ) );
        assertNotNull( chartStore.get( idC ) );
    }

    @Test
    public void testGetAll()
    {
        chartStore.save( chartA );
        chartStore.save( chartB );
        chartStore.save( chartC );

        assertTrue( equals( chartStore.getAll(), chartA, chartB, chartC ) );
    }

    @Test
    public void testGetByTitle()
    {
        chartStore.save( chartA );
        chartStore.save( chartB );
        chartStore.save( chartC );

        assertEquals( chartB, chartStore.getByName( "ChartB" ) );
    }
}
