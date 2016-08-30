package org.hisp.dhis.dataset;

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
import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class CompleteDataSetRegistrationServiceTest
    extends DhisSpringTest
{
    @Autowired
    private CompleteDataSetRegistrationService completeDataSetRegistrationService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementCategoryService categoryService;
  
    private CompleteDataSetRegistration registrationA;
    private CompleteDataSetRegistration registrationB;
    private CompleteDataSetRegistration registrationC;
    private CompleteDataSetRegistration registrationD;
    private CompleteDataSetRegistration registrationE;
    private CompleteDataSetRegistration registrationF;
    private CompleteDataSetRegistration registrationG;
    private CompleteDataSetRegistration registrationH;

    private DataSet dataSetA;
    private DataSet dataSetB;
    private DataSet dataSetC;

    private Period periodA;
    private Period periodB;

    private OrganisationUnit sourceA;
    private OrganisationUnit sourceB;
    private OrganisationUnit sourceC;

    private Date onTimeA;

    private DataElementCategoryOptionCombo optionCombo;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B' );
        sourceC = createOrganisationUnit( 'C' );

        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );

        periodA = createPeriod( new MonthlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );
        periodB = createPeriod( new MonthlyPeriodType(), getDate( 2000, 2, 1 ), getDate( 2000, 2, 28 ) );

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );

        dataSetA = createDataSet( 'A', new MonthlyPeriodType() );
        dataSetB = createDataSet( 'B', new MonthlyPeriodType() );
        dataSetC = createDataSet( 'C', new MonthlyPeriodType() );

        dataSetA.getSources().add( sourceA );
        dataSetA.getSources().add( sourceB );
        dataSetB.getSources().add( sourceA );
        dataSetB.getSources().add( sourceB );
        dataSetC.getSources().add( sourceA );
        dataSetC.getSources().add( sourceB );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        dataSetService.addDataSet( dataSetC );

        optionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        onTimeA = getDate( 2000, 1, 10 );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testSaveGet()
    {
        registrationA = new CompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo, new Date(), "" );
        registrationB = new CompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo, new Date(), "" );

        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationA );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationB );

        assertEquals( registrationA,
            completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo ) );
        assertEquals( registrationB,
            completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo ) );
    }

    @Test
    public void testDelete()
    {
        registrationA = new CompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo, new Date(), "" );
        registrationB = new CompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo, new Date(), "" );

        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationA );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationB );

        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo ) );

        completeDataSetRegistrationService.deleteCompleteDataSetRegistration( registrationA );

        assertNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo ) );
    }

    @Test
    public void testGetAll()
    {
        registrationA = new CompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo, new Date(), "" );
        registrationB = new CompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo, new Date(), "" );

        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationA );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationB );

        List<CompleteDataSetRegistration> registrations = completeDataSetRegistrationService
            .getAllCompleteDataSetRegistrations();

        assertEquals( 2, registrations.size() );
        assertTrue( registrations.contains( registrationA ) );
        assertTrue( registrations.contains( registrationB ) );
    }

    @Test
    public void testGetDataSetsSourcesPeriods()
    {
        registrationA = new CompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo, new Date(), "" );
        registrationB = new CompleteDataSetRegistration( dataSetB, periodA, sourceA, optionCombo, new Date(), "" );
        registrationC = new CompleteDataSetRegistration( dataSetA, periodB, sourceA, optionCombo, new Date(), "" );
        registrationD = new CompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo, new Date(), "" );
        registrationE = new CompleteDataSetRegistration( dataSetA, periodA, sourceB, optionCombo, new Date(), "" );
        registrationF = new CompleteDataSetRegistration( dataSetB, periodA, sourceB, optionCombo, new Date(), "" );
        registrationG = new CompleteDataSetRegistration( dataSetA, periodB, sourceB, optionCombo, new Date(), "" );
        registrationH = new CompleteDataSetRegistration( dataSetB, periodB, sourceB, optionCombo, new Date(), "" );

        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationA );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationB );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationC );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationD );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationE );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationF );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationG );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationH );

        List<DataSet> dataSets = new ArrayList<>();

        dataSets.add( dataSetB );

        List<OrganisationUnit> sources = new ArrayList<>();

        sources.add( sourceA );
        sources.add( sourceB );

        List<Period> periods = new ArrayList<>();

        periods.add( periodA );

        List<CompleteDataSetRegistration> registrations = completeDataSetRegistrationService
            .getCompleteDataSetRegistrations( dataSets, sources, periods );

        assertNotNull( registrations );
        assertEquals( 2, registrations.size() );
        assertTrue( registrations.contains( registrationB ) );
        assertTrue( registrations.contains( registrationF ) );
    }

    @Test
    public void testDeleteByDataSet()
    {
        registrationA = new CompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo, onTimeA, "" );
        registrationB = new CompleteDataSetRegistration( dataSetA, periodB, sourceA, optionCombo, onTimeA, "" );
        registrationC = new CompleteDataSetRegistration( dataSetB, periodA, sourceA, optionCombo, onTimeA, "" );
        registrationD = new CompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo, onTimeA, "" );

        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationA );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationB );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationC );
        completeDataSetRegistrationService.saveCompleteDataSetRegistration( registrationD );

        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodB, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodA, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo ) );

        completeDataSetRegistrationService.deleteCompleteDataSetRegistrations( dataSetA );

        assertNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodA, sourceA, optionCombo ) );
        assertNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetA, periodB, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodA, sourceA, optionCombo ) );
        assertNotNull( completeDataSetRegistrationService.getCompleteDataSetRegistration( dataSetB, periodB, sourceA, optionCombo ) );
    }
}
