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
package org.hisp.dhis.dataset;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalStore;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO Test delete with data set elements
 *
 * @author Lars Helge Overland
 */
class DataSetServiceTest extends DhisTest
{

    private PeriodType periodType;

    private Period period;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private OrganisationUnit unitC;

    private OrganisationUnit unitD;

    private OrganisationUnit unitE;

    private OrganisationUnit unitF;

    private CategoryOptionCombo attributeOptionCombo;

    private User superUser;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    protected UserService _userService;

    @Autowired
    private DataApprovalService approvalService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DataApprovalStore approvalStore;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private DataApprovalLevelService levelService;

    @Autowired
    private AclService aclService;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
        throws Exception
    {
        userService = _userService;
        preCreateInjectAdminUser();

        periodType = new MonthlyPeriodType();
        period = createPeriod( periodType, getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) );
        periodService.addPeriod( period );
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );
        unitD = createOrganisationUnit( 'D' );
        unitE = createOrganisationUnit( 'E' );
        unitF = createOrganisationUnit( 'F' );
        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );
        organisationUnitService.addOrganisationUnit( unitF );
        attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        superUser = createAndAddUser( true, "username", newHashSet( unitA ), newHashSet( unitA ),
            UserRole.AUTHORITY_ALL );
        injectSecurityContext( superUser );

    }

    @Override
    public void tearDownTest()
    {
        setDependency( CurrentUserServiceTarget.class, CurrentUserServiceTarget::setCurrentUserService,
            currentUserService, approvalService, approvalStore, levelService );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
    private void assertEq( char uniqueCharacter, DataSet dataSet )
    {
        assertEquals( "DataSet" + uniqueCharacter, dataSet.getName() );
        assertEquals( "DataSetShort" + uniqueCharacter, dataSet.getShortName() );
        assertEquals( periodType, dataSet.getPeriodType() );
    }

    private void approveData( DataSet dataSet, Period period, OrganisationUnit unit )
    {
        DataApprovalLevel level = new DataApprovalLevel( "Level A", unit.getLevel(), null );
        levelService.addDataApprovalLevel( level );
        DataApprovalWorkflow workflow = new DataApprovalWorkflow( "Workflow A", period.getPeriodType(),
            newHashSet( level ) );
        dataApprovalService.addWorkflow( workflow );
        dataSet.assignWorkflow( workflow );
        dataSet.addOrganisationUnit( unit );
        dataSetService.updateDataSet( dataSet );
        DataApproval approval = new DataApproval( level, workflow, period, unit, attributeOptionCombo, false,
            new Date(), superUser );
        approvalService.approveData( newArrayList( approval ) );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------
    @Test
    void testAddDataSet()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        dataSetA.addDataSetElement( dataElementA );
        dataSetA.addDataSetElement( dataElementB );
        long idA = dataSetService.addDataSet( dataSetA );
        long idB = dataSetService.addDataSet( dataSetB );
        dataSetA = dataSetService.getDataSet( idA );
        dataSetB = dataSetService.getDataSet( idB );
        assertEquals( idA, dataSetA.getId() );
        assertEq( 'A', dataSetA );
        assertEquals( idB, dataSetB.getId() );
        assertEq( 'B', dataSetB );
    }

    @Test
    void testUpdateDataSet()
    {
        DataSet dataSet = createDataSet( 'A', periodType );
        dataSet.addDataSetElement( dataElementA );
        dataSet.addDataSetElement( dataElementB );
        long id = dataSetService.addDataSet( dataSet );
        dataSet = dataSetService.getDataSet( id );
        assertEq( 'A', dataSet );
        dataSet.setName( "DataSetB" );
        dataSetService.updateDataSet( dataSet );
        dataSet = dataSetService.getDataSet( id );
        assertEquals( dataSet.getName(), "DataSetB" );
    }

    @Test
    void testDeleteAndGetDataSet()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        dataSetA.addDataSetElement( dataElementA );
        dataSetA.addDataSetElement( dataElementB );
        long idA = dataSetService.addDataSet( dataSetA );
        long idB = dataSetService.addDataSet( dataSetB );
        assertNotNull( dataSetService.getDataSet( idA ) );
        assertNotNull( dataSetService.getDataSet( idB ) );
        dataSetService.deleteDataSet( dataSetService.getDataSet( idA ) );
        assertNull( dataSetService.getDataSet( idA ) );
        assertNotNull( dataSetService.getDataSet( idB ) );
        dataSetService.deleteDataSet( dataSetService.getDataSet( idB ) );
        assertNull( dataSetService.getDataSet( idA ) );
        assertNull( dataSetService.getDataSet( idB ) );
    }

    @Test
    void testUpdateRemoveDataSetElements()
    {
        DataSet dataSet = createDataSet( 'A', periodType );
        dataSet.addDataSetElement( dataElementA );
        dataSet.addDataSetElement( dataElementB );
        dataSetService.addDataSet( dataSet );
        dataSet = dataSetService.getDataSet( dataSet.getId() );
        assertNotNull( dataSet );
        List<DataSetElement> dataSetElements = new ArrayList<>( dataSet.getDataSetElements() );
        assertEquals( 2, dataSet.getDataSetElements().size() );
        assertEquals( 2, dataSetElements.size() );
        // Remove data element A
        dataSet.removeDataSetElement( dataElementA );
        dataSetService.updateDataSet( dataSet );
        dataSet = dataSetService.getDataSet( dataSet.getId() );
        assertNotNull( dataSet );
        dataSetElements = new ArrayList<>( dataSet.getDataSetElements() );
        assertEquals( 1, dataSet.getDataSetElements().size() );
        assertEquals( 1, dataSetElements.size() );
        // Remove data element B
        dataSet.removeDataSetElement( dataElementB );
        dataSetService.updateDataSet( dataSet );
        dataSet = dataSetService.getDataSet( dataSet.getId() );
        assertNotNull( dataSet );
        dataSetElements = new ArrayList<>( dataSet.getDataSetElements() );
        assertEquals( 0, dataSet.getDataSetElements().size() );
        assertEquals( 0, dataSetElements.size() );
    }

    @Test
    void testDeleteRemoveDataSetElements()
    {
        DataSet dataSet = createDataSet( 'A', periodType );
        dataSet.addDataSetElement( dataElementA );
        dataSet.addDataSetElement( dataElementB );
        long ds = dataSetService.addDataSet( dataSet );
        dataSet = dataSetService.getDataSet( dataSet.getId() );
        assertNotNull( dataSet );
        List<DataSetElement> dataSetElements = new ArrayList<>( dataSet.getDataSetElements() );
        assertEquals( dataSet, dataSetService.getDataSet( ds ) );
        assertEquals( 2, dataSet.getDataSetElements().size() );
        assertEquals( 2, dataSetElements.size() );
        dataSetService.deleteDataSet( dataSet );
        assertNull( dataSetService.getDataSet( ds ) );
    }

    @Test
    void testGetAllDataSets()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        List<DataSet> dataSets = dataSetService.getAllDataSets();
        assertEquals( dataSets.size(), 2 );
        assertTrue( dataSets.contains( dataSetA ) );
        assertTrue( dataSets.contains( dataSetB ) );
    }

    @Test
    void testAddDataSetElement()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        dataSetA.addDataSetElement( dataElementA );
        dataSetA.addDataSetElement( dataElementB );
        dataSetService.addDataSet( dataSetA );
        assertEquals( 2, dataSetA.getDataSetElements().size() );
        assertEquals( 1, dataElementA.getDataSetElements().size() );
        assertEquals( dataSetA, dataElementA.getDataSetElements().iterator().next().getDataSet() );
        assertEquals( 1, dataElementB.getDataSetElements().size() );
        assertEquals( dataSetA, dataElementB.getDataSetElements().iterator().next().getDataSet() );
    }

    // -------------------------------------------------------------------------
    // LockException
    // -------------------------------------------------------------------------
    @Test
    void testSaveGet()
    {
        Period period = periodType.createPeriod();
        DataSet dataSet = createDataSet( 'A', periodType );
        dataSetService.addDataSet( dataSet );
        LockException lockException = new LockException( period, unitA, dataSet );
        long id = dataSetService.addLockException( lockException );
        lockException = dataSetService.getLockException( id );
        assertNotNull( lockException );
        assertEquals( unitA, lockException.getOrganisationUnit() );
        assertEquals( period, lockException.getPeriod() );
        assertEquals( dataSet, lockException.getDataSet() );
    }

    @Test
    @Disabled( "TODO: fix this test 12098" )
    void testIsLockedDataElement()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        dataSetA.setExpiryDays( 20 );
        dataSetA.setTimelyDays( 15 );
        dataSetB.setExpiryDays( 10 );
        dataSetB.setTimelyDays( 15 );
        dataSetA.addDataSetElement( dataElementA );
        dataSetB.addDataSetElement( dataElementA );
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        User user = currentUserService.getCurrentUser();
        // ---------------------------------------------------------------------
        // Expiry days
        // ---------------------------------------------------------------------
        assertEquals(
            dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 1 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 5 ) ),
            LockStatus.OPEN );
        assertEquals( dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 15 ) ), LockStatus.OPEN );
        assertEquals( dataSetService.getLockStatus( user, dataElementB, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 25 ) ), LockStatus.OPEN );
        assertEquals( dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 25 ) ), LockStatus.LOCKED );
        // ---------------------------------------------------------------------
        // Lock exception
        // ---------------------------------------------------------------------
        LockException lockException = new LockException( period, unitA, dataSetA );
        dataSetService.addLockException( lockException );
        assertEquals(
            dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 1 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 5 ) ),
            LockStatus.OPEN );
        assertEquals( dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 15 ) ), LockStatus.OPEN );
        assertEquals( dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 25 ) ), LockStatus.OPEN );
        assertEquals( dataSetService.getLockStatus( user, dataElementB, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 25 ) ), LockStatus.OPEN );
        // ---------------------------------------------------------------------
        // Approved
        // ---------------------------------------------------------------------
        approveData( dataSetA, period, unitA );
        assertEquals(
            dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 1 ) ),
            LockStatus.APPROVED );
        assertEquals(
            dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 5 ) ),
            LockStatus.APPROVED );
        assertEquals( dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 15 ) ), LockStatus.APPROVED );
        assertEquals( dataSetService.getLockStatus( user, dataElementA, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 25 ) ), LockStatus.APPROVED );
        assertEquals( dataSetService.getLockStatus( user, dataElementB, period, unitA, attributeOptionCombo,
            getDate( 2000, 4, 25 ) ), LockStatus.OPEN );
    }

    @Test
    @Disabled( "TODO: fix this test 12098" )
    void testIsLockedDataSet()
    {

        injectSecurityContext( superUser );

        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        dataSetA.setExpiryDays( 10 );
        dataSetA.setTimelyDays( 15 );
        dataSetB.setExpiryDays( 15 );
        dataSetB.setTimelyDays( 15 );
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        // ---------------------------------------------------------------------
        // Expiry days
        // ---------------------------------------------------------------------
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 1 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 5 ) ),
            LockStatus.OPEN );
        // 12098 org.opentest4j.AssertionFailedError:
        // Expected :OPEN
        // Actual :LOCKED
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 15 ) ),
            LockStatus.LOCKED );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 25 ) ),
            LockStatus.LOCKED );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetB, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 10 ) ),
            LockStatus.OPEN );
        // Test Expiry days with user has authority "ALL"
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetB, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 25 ) ),
            LockStatus.OPEN );
        // ---------------------------------------------------------------------
        // Lock exception
        // ---------------------------------------------------------------------
        LockException lockException = new LockException( period, unitA, dataSetA );
        dataSetService.addLockException( lockException );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 1 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 5 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 15 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 25 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetB, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 10 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetB, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 25 ) ),
            LockStatus.LOCKED );
        // ---------------------------------------------------------------------
        // Approved
        // ---------------------------------------------------------------------
        approveData( dataSetA, period, unitA );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 1 ) ),
            LockStatus.APPROVED );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 5 ) ),
            LockStatus.APPROVED );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 15 ) ),
            LockStatus.APPROVED );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetA, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 25 ) ),
            LockStatus.APPROVED );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetB, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 10 ) ),
            LockStatus.OPEN );
        assertEquals(
            dataSetService.getLockStatus( superUser, dataSetB, period, unitA, attributeOptionCombo,
                getDate( 2000, 4, 25 ) ),
            LockStatus.LOCKED );
    }

    @Test
    void testDataSharingDataSet()
    {
        User user = createAndAddUser( false, "usernameA", null );
        injectSecurityContext( user );
        DataSet dataSet = createDataSet( 'A', new MonthlyPeriodType() );
        UserAccess userAccess = new UserAccess();
        userAccess.setUser( user );
        userAccess.setAccess( AccessStringHelper.DATA_READ_WRITE );
        dataSet.getSharing().addUserAccess( userAccess );
        Access access = aclService.getAccess( dataSet, user );
        assertTrue( access.getData().isRead() );
        assertTrue( access.getData().isWrite() );
    }
}
