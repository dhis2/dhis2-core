package org.hisp.dhis.completeness;

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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Ignore //TODO rewrite this test, takes too long
public class DataSetCompletenessServiceTest
    extends DhisSpringTest
{
    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private CompleteDataSetRegistrationService registrationService;
    
    @Resource(name="locationManager")
    LocationManager  locationManager;
    
    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataSetCompletenessService registrationCompletenessService;

    private PeriodType periodType;
    
    private Period periodA;
    private Period periodB;
    private Period periodC;
    
    private int periodIdA;
    private int periodIdC;
    
    private OrganisationUnit unitA;
    private OrganisationUnit unitB;
    private OrganisationUnit unitC;
    private OrganisationUnit unitD;
    private OrganisationUnit unitE;
    private OrganisationUnit unitF;
    private OrganisationUnit unitG;
    private OrganisationUnit unitH;
    
    private OrganisationUnitGroup groupA;
    private OrganisationUnitGroup groupB;
    private OrganisationUnitGroup groupC;
        
    private int unitIdA;
    private int unitIdB;
    private int unitIdC;
    private Collection<Integer> unitIdsA;
    
    private DataSet dataSetA;
    private DataSet dataSetB;
    private DataSet dataSetC;
    
    private int dataSetIdA;
    
    private DataElement dataElementA;
    private DataElement dataElementB;
    
    private DataElementCategoryOptionCombo categoryOptionCombo;
    
    private Date onTimeA;
    private Date tooLateA;
    private Date onTimeB;
    private Date tooLateB;
    
    private Set<Integer> groupIds = new HashSet<>();

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        setExternalTestDir( locationManager );
    
        categoryOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        
        periodType = new MonthlyPeriodType();
        
        periodA = createPeriod( periodType, getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );
        periodB = createPeriod( periodType, getDate( 2000, 2, 1 ), getDate( 2000, 2, 28 ) );
        periodC = createPeriod( new QuarterlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 3, 31 ) );        
        
        periodIdA = periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodIdC = periodService.addPeriod( periodC );

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );
        unitD = createOrganisationUnit( 'D' );
        unitE = createOrganisationUnit( 'E' );
        unitF = createOrganisationUnit( 'F' );
        unitG = createOrganisationUnit( 'G' );
        unitH = createOrganisationUnit( 'H' );
        
        unitB.setParent( unitA );
        unitC.setParent( unitA );
        unitE.setParent( unitB );
        unitF.setParent( unitB );
        unitG.setParent( unitC );
        unitH.setParent( unitC );
        
        unitA.getChildren().add( unitB );
        unitA.getChildren().add( unitC );
        unitB.getChildren().add( unitE );
        unitB.getChildren().add( unitF );
        unitC.getChildren().add( unitG );
        unitC.getChildren().add( unitH );
        
        unitIdA = organisationUnitService.addOrganisationUnit( unitA );
        unitIdB = organisationUnitService.addOrganisationUnit( unitB );
        unitIdC = organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );
        organisationUnitService.addOrganisationUnit( unitF );
        organisationUnitService.addOrganisationUnit( unitG );
        organisationUnitService.addOrganisationUnit( unitH );
        
        unitIdsA = new HashSet<>();
        unitIdsA.add( unitIdA );
        unitIdsA.add( unitIdB );
        unitIdsA.add( unitIdC );
        
        groupA = createOrganisationUnitGroup( 'A' );
        groupB = createOrganisationUnitGroup( 'B' );
        groupC = createOrganisationUnitGroup( 'C' );
        
        groupA.addOrganisationUnit( unitA );
        groupB.addOrganisationUnit( unitA );
        groupB.addOrganisationUnit( unitB );
        groupC.addOrganisationUnit( unitE );
        groupC.addOrganisationUnit( unitF );
        
        organisationUnitGroupService.addOrganisationUnitGroup( groupA );
        organisationUnitGroupService.addOrganisationUnitGroup( groupB );
        organisationUnitGroupService.addOrganisationUnitGroup( groupC );
        
        dataSetA = createDataSet( 'A', periodType );
        dataSetB = createDataSet( 'B', periodType );
        dataSetC = createDataSet( 'C', periodType );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        
        dataSetA.getCompulsoryDataElementOperands().add( new DataElementOperand( dataElementA, categoryOptionCombo ) );
        dataSetA.getCompulsoryDataElementOperands().add( new DataElementOperand( dataElementB, categoryOptionCombo ) );
        
        dataSetB.getCompulsoryDataElementOperands().add( new DataElementOperand( dataElementA, categoryOptionCombo ) );
        
        onTimeA = getDate( 2000, 2, 10 );
        tooLateA = getDate( 2000, 2, 25 );
        onTimeB = getDate( 2000, 3, 10 );
        tooLateB = getDate( 2000, 3, 25 );
    }

    // -------------------------------------------------------------------------
    //       A
    //   B       C
    // E   F   G   H
    // -------------------------------------------------------------------------
    
    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetPercentage()
    {
        DataSetCompletenessResult resultA = new DataSetCompletenessResult( dataSetA.getName(), 20, 15, 10 );
        DataSetCompletenessResult resultB = new DataSetCompletenessResult( dataSetA.getName(), 0, 15, 10 );
        
        assertEquals( 75.0, resultA.getPercentage(), DELTA );
        assertEquals( 0.0, resultB.getPercentage(), DELTA );
        
        assertEquals( 50.0, resultA.getPercentageOnTime(), DELTA );
        assertEquals( 0.0, resultB.getPercentageOnTime(), DELTA );
    }
    
    // -------------------------------------------------------------------------
    // Complete registration based completeness
    // -------------------------------------------------------------------------

    @Test
    public void testGetDataSetCompletenessByDataSetA()
    {
        dataSetA.getSources().add( unitA );
        dataSetA.getSources().add( unitB );
        
        dataSetB.getSources().add( unitA );
        dataSetB.getSources().add( unitB );

        dataSetC.getSources().add( unitA );
        dataSetC.getSources().add( unitB );
        
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        dataSetService.addDataSet( dataSetC );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitB, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitA, null, tooLateB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitD, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitC, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodB, unitB, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetC, periodA, unitC, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetC, periodB, unitA, null, tooLateB, "") );

        List<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );        
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 2, 2, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetB.getName(), 2, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetC.getName(), 2, 0, 0 ) ) );  

        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdA, null );
   
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 6, 3, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetB.getName(), 6, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetC.getName(), 6, 1, 0 ) ) );            
    }

    @Test
    public void testGetDataSetCompletenessByDataSetB()
    {
        dataSetA.getSources().add( unitA );
        dataSetA.getSources().add( unitB );
        dataSetA.getSources().add( unitC );

        dataSetB.getSources().add( unitB );
        dataSetB.getSources().add( unitC );

        dataSetC.getSources().add( unitB );
        dataSetC.getSources().add( unitC );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        dataSetService.addDataSet( dataSetC );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitA, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitD, null, tooLateA, "") );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitB, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitC, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodB, unitC, null, tooLateB, "") );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetC, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetC, periodA, unitB, null, onTimeA, "") );

        List<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );        
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 3, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetB.getName(), 2, 2, 2 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetC.getName(), 2, 1, 1 ) ) );
        
        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );        
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 9, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetB.getName(), 6, 3, 2 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetC.getName(), 6, 1, 1 ) ) );
    }

    @Test
    public void testGetDataSetCompletenessByDataSetC()
    {
        dataSetA.getSources().add( unitA );
        dataSetA.getSources().add( unitB );
        dataSetA.getSources().add( unitC );
        dataSetA.getSources().add( unitE );
        dataSetA.getSources().add( unitF );
        dataSetA.getSources().add( unitG );
        dataSetA.getSources().add( unitH );
        
        dataSetService.addDataSet( dataSetA );

        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitB, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitC, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitE, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitF, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitG, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitE, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitF, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitG, null, tooLateB, "") );

        List<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdA, null );
        
        assertNotNull( results );
        assertEquals( 1, results.size() );        
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 7, 5, 3 ) ) );

        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdA, null );
        
        assertNotNull( results );
        assertEquals( 1, results.size() );        
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 21, 8, 5 ) ) );
    }

    @Test
    public void testGetDataSetCompletenessByDataSetD()
    {
        dataSetA.getSources().add( unitA );
        dataSetA.getSources().add( unitB );
        
        dataSetB.getSources().add( unitA );
        dataSetB.getSources().add( unitB );

        dataSetC.getSources().add( unitA );
        dataSetC.getSources().add( unitB );
        
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        dataSetService.addDataSet( dataSetC );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitB, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitA, null, tooLateB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitD, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitA, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodA, unitC, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetB, periodB, unitB, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetC, periodA, unitC, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetC, periodB, unitA, null, tooLateB, "") );

        groupIds.clear();
        groupIds.add( groupA.getId() );
        
        List<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdA, groupIds );
   
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 3, 2, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetB.getName(), 3, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetC.getName(), 3, 1, 0 ) ) );

        groupIds.clear();
        groupIds.add( groupA.getId() );
        groupIds.add( groupB.getId() );
        
        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdA, groupIds );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetA.getName(), 3, 2, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetB.getName(), 3, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( dataSetC.getName(), 3, 1, 0 ) ) );
    }

    @Test
    public void testGetDataSetCompletenessByOrganisationUnitA()
    {
        dataSetA.getSources().add( unitE );
        dataSetA.getSources().add( unitF );
        dataSetA.getSources().add( unitG );
        dataSetA.getSources().add( unitH );
        
        dataSetIdA = dataSetService.addDataSet( dataSetA );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitE, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitF, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitG, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitE, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitF, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitG, null, onTimeA, "") );

        Collection<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdsA, dataSetIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 2, 2, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitC.getName(), 2, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 4, 3, 0 ) ) );
        
        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdsA, dataSetIdA, null );

        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 6, 4, 2 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitC.getName(), 6, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 12, 6, 3 ) ) );
    }

    @Test
    public void testGetDataSetCompletenessByOrganisationUnitB()
    {
        dataSetA.getSources().add( unitE );
        dataSetA.getSources().add( unitF );
        dataSetA.getSources().add( unitG );

        dataSetIdA = dataSetService.addDataSet( dataSetA );

        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitE, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitG, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitH, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitE, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitG, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitH, null, onTimeB, "") );

        Collection<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdsA, dataSetIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 2, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitC.getName(), 1, 1, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 3, 2, 0 ) ) );

        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdsA, dataSetIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 6, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitC.getName(), 3, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 9, 4, 2 ) ) );
    }

    @Test
    public void testGetDataSetCompletenessByOrganisationUnitC()
    {
        dataSetA.getSources().add( unitE );
        dataSetA.getSources().add( unitF );
        dataSetA.getSources().add( unitG );
        dataSetA.getSources().add( unitH );

        dataSetIdA = dataSetService.addDataSet( dataSetA );

        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitE, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitF, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitG, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitH, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitE, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitF, null, tooLateB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitG, null, onTimeB, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitH, null, tooLateB, "") );

        Collection<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdsA, dataSetIdA, null );

        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 2, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitC.getName(), 2, 2, 1 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 4, 4, 2 ) ) );
        
        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdsA, dataSetIdA, null );
        
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 6, 4, 2 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitC.getName(), 6, 4, 2 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 12, 8, 4 ) ) );        
    }

    @Test
    public void testGetDataSetCompletenessByOrganisationUnitD()
    {
        dataSetA.getSources().add( unitE );
        dataSetA.getSources().add( unitF );
        dataSetA.getSources().add( unitG );
        dataSetA.getSources().add( unitH );
        
        dataSetIdA = dataSetService.addDataSet( dataSetA );
        
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitE, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitF, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodA, unitG, null, tooLateA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitE, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitF, null, onTimeA, "") );
        registrationService.saveCompleteDataSetRegistration( new CompleteDataSetRegistration( dataSetA, periodB, unitG, null, onTimeA, "") );

        groupIds.clear();
        groupIds.add( groupC.getId() );
        
        Collection<DataSetCompletenessResult> results = registrationCompletenessService.getDataSetCompleteness( periodIdA, unitIdsA, dataSetIdA, groupIds );
        
        assertNotNull( results );
        assertEquals( 2, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 2, 2, 0 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 2, 2, 0 ) ) );
        
        results = registrationCompletenessService.getDataSetCompleteness( periodIdC, unitIdsA, dataSetIdA, groupIds );

        assertNotNull( results );
        assertEquals( 2, results.size() );
        assertTrue( results.contains( new DataSetCompletenessResult( unitB.getName(), 6, 4, 2 ) ) );
        assertTrue( results.contains( new DataSetCompletenessResult( unitA.getName(), 6, 4, 2 ) ) );
    }
}
