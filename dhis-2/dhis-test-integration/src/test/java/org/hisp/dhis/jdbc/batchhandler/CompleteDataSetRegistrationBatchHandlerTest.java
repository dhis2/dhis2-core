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
package org.hisp.dhis.jdbc.batchhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class CompleteDataSetRegistrationBatchHandlerTest extends IntegrationTestBase
{

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    private BatchHandler<CompleteDataSetRegistration> batchHandler;

    private PeriodType periodTypeA;

    private DataSet dataSetA;

    private Period periodA;

    private Period periodB;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private CategoryOptionCombo attributeOptionCombo;

    private CompleteDataSetRegistration regA;

    private CompleteDataSetRegistration regB;

    private CompleteDataSetRegistration regC;

    private CompleteDataSetRegistration regD;

    private Date now = new Date();

    private String storedBy = "johndoe";

    private String lastUpdatedBy = "johndoe";

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        batchHandler = batchHandlerFactory.createBatchHandler( CompleteDataSetRegistrationBatchHandler.class );
        periodTypeA = PeriodType.getPeriodType( PeriodTypeEnum.MONTHLY );
        dataSetA = createDataSet( 'A', periodTypeA );
        idObjectManager.save( dataSetA );
        periodA = createPeriod( periodTypeA, getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );
        periodB = createPeriod( periodTypeA, getDate( 2000, 2, 1 ), getDate( 2000, 2, 28 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        idObjectManager.save( unitA );
        idObjectManager.save( unitB );
        attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        regA = new CompleteDataSetRegistration( dataSetA, periodA, unitA, attributeOptionCombo, now, storedBy, now,
            lastUpdatedBy, true );
        regB = new CompleteDataSetRegistration( dataSetA, periodA, unitB, attributeOptionCombo, now, storedBy, now,
            lastUpdatedBy, true );
        regC = new CompleteDataSetRegistration( dataSetA, periodB, unitA, attributeOptionCombo, now, storedBy, now,
            lastUpdatedBy, true );
        regD = new CompleteDataSetRegistration( dataSetA, periodB, unitB, attributeOptionCombo, now, storedBy, now,
            lastUpdatedBy, true );
        batchHandler.init();
    }

    @Override
    public void tearDownTest()
    {
        batchHandler.flush();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testAddObject()
    {
        batchHandler.addObject( regA );
        batchHandler.addObject( regB );
        batchHandler.addObject( regC );
        batchHandler.addObject( regD );
        batchHandler.flush();
        List<CompleteDataSetRegistration> registrations = registrationService.getAllCompleteDataSetRegistrations();
        assertNotNull( registrations );
        assertEquals( 4, registrations.size() );
        assertTrue( registrations.contains( regA ) );
        assertTrue( registrations.contains( regB ) );
        assertTrue( registrations.contains( regC ) );
        assertTrue( registrations.contains( regD ) );
    }

    @Test
    void testFindObject()
    {
        registrationService.saveCompleteDataSetRegistration( regA );
        registrationService.saveCompleteDataSetRegistration( regD );
        CompleteDataSetRegistration retrievedRegA = batchHandler.findObject( regA );
        CompleteDataSetRegistration retrievedRegB = batchHandler.findObject( regB );
        assertNotNull( retrievedRegA.getStoredBy() );
        assertEquals( retrievedRegA.getStoredBy(), regA.getStoredBy() );
        assertNull( retrievedRegB );
    }

    @Test
    void testObjectExists()
    {
        registrationService.saveCompleteDataSetRegistration( regA );
        registrationService.saveCompleteDataSetRegistration( regD );
        assertTrue( batchHandler.objectExists( regA ) );
        assertTrue( batchHandler.objectExists( regD ) );
        assertFalse( batchHandler.objectExists( regB ) );
        assertFalse( batchHandler.objectExists( regC ) );
    }

    @Test
    @Disabled( "ERROR: cannot execute UPDATE in a read-only transaction" )
    void testUpdateObject()
    {
        registrationService.saveCompleteDataSetRegistration( regA );
        regA.setStoredBy( "tom" );
        batchHandler.updateObject( regA );
        assertEquals( "tom", registrationService
            .getCompleteDataSetRegistration( dataSetA, periodA, unitA, attributeOptionCombo ).getStoredBy() );
    }

    @Test
    void testDeleteObject()
    {
        registrationService.saveCompleteDataSetRegistration( regA );
        registrationService.saveCompleteDataSetRegistration( regD );
        assertTrue( batchHandler.objectExists( regA ) );
        assertTrue( batchHandler.objectExists( regD ) );
        batchHandler.deleteObject( regD );
        assertTrue( batchHandler.objectExists( regA ) );
        assertFalse( batchHandler.objectExists( regD ) );
    }
}
