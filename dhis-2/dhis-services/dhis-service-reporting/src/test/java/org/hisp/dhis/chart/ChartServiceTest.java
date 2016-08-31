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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mock.MockI18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.jfree.chart.JFreeChart;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class ChartServiceTest
    extends DhisSpringTest
{
    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    @Autowired
    private ChartService chartService;

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
        throws Exception
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

        chartA = createChart( 'A', indicators, periods, units );
        chartA.setType( ChartType.BAR );

        chartB = createChart( 'B', indicators, periods, units );
        chartB.setType( ChartType.BAR );

        chartC = createChart( 'C', indicators, periods, units );
        chartC.setType( ChartType.BAR );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetBarChart()
    {
        int id = chartService.addChart( chartA );

        JFreeChart jFreeChart = chartService.getJFreeChart( id, new MockI18nFormat() );

        assertNotNull( jFreeChart );
    }

    @Test
    public void testSaveGet()
    {
        int idA = chartService.addChart( chartA );
        int idB = chartService.addChart( chartB );
        int idC = chartService.addChart( chartC );

        assertEquals( chartA, chartService.getChart( idA ) );
        assertEquals( chartB, chartService.getChart( idB ) );
        assertEquals( chartC, chartService.getChart( idC ) );

        assertTrue( equals( chartService.getChart( idA ).getIndicators(), indicatorA, indicatorB, indicatorC ) );
        assertTrue( equals( chartService.getChart( idA ).getOrganisationUnits(), unitA, unitB ) );
        assertTrue( equals( chartService.getChart( idA ).getPeriods(), periodA, periodB, periodC ) );
    }

    @Test
    public void testDelete()
    {
        int idA = chartService.addChart( chartA );
        int idB = chartService.addChart( chartB );
        int idC = chartService.addChart( chartC );

        assertNotNull( chartService.getChart( idA ) );
        assertNotNull( chartService.getChart( idB ) );
        assertNotNull( chartService.getChart( idC ) );

        chartService.deleteChart( chartA );

        assertNull( chartService.getChart( idA ) );
        assertNotNull( chartService.getChart( idB ) );
        assertNotNull( chartService.getChart( idC ) );

        chartService.deleteChart( chartB );

        assertNull( chartService.getChart( idA ) );
        assertNull( chartService.getChart( idB ) );
        assertNotNull( chartService.getChart( idC ) );
    }

    @Test
    public void testGetAll()
    {
        chartService.addChart( chartA );
        chartService.addChart( chartB );
        chartService.addChart( chartC );

        assertTrue( equals( chartService.getAllCharts(), chartA, chartB, chartC ) );
    }

    @Test
    public void testGetByTitle()
    {
        chartService.addChart( chartA );
        chartService.addChart( chartB );
        chartService.addChart( chartC );

        assertEquals( chartB, chartService.getChartByName( "ChartB" ) );
    }
}
