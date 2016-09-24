package org.hisp.dhis.dataapproval;

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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeApprovedException;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
@Category( IntegrationTest.class )
public class DataApprovalServiceTest
    extends DhisTest
{
    private static final String AUTH_APPR_LEVEL = "F_SYSTEM_SETTING";

    private final static boolean ACCEPTED = true;
    private final static boolean NOT_ACCEPTED = false;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private DataApprovalStore dataApprovalStore;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;
   
    @Autowired 
    protected IdentifiableObjectManager identifiableObjectManager;
    
    @Autowired
    protected UserService _userService;

    @Autowired
    protected CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataElementCategoryOptionCombo defaultOptionCombo;

    private PeriodType periodType;

    private Period periodA; // Monthly: Jan
    private Period periodB; // Monthly: Feb
    private Period periodC; // Monthly: Mar

    private Period periodM12; // Monthly: December

    private Period periodD; // Daily
    private Period periodQ; // Quarterly
    private Period periodW; // Weekly
    private Period periodW4; // Weekly
    private Period periodW5; // Weekly
    private Period periodY; // Yearly

    private OrganisationUnit organisationUnitA;
    private OrganisationUnit organisationUnitB;
    private OrganisationUnit organisationUnitC;
    private OrganisationUnit organisationUnitD;
    private OrganisationUnit organisationUnitE;
    private OrganisationUnit organisationUnitF;

    private DataApprovalLevel level1;
    private DataApprovalLevel level2;
    private DataApprovalLevel level3;
    private DataApprovalLevel level4;

    private DataApprovalLevel level2ABCD;
    private DataApprovalLevel level2EFGH;

    private DataApprovalWorkflow workflow0;
    private DataApprovalWorkflow workflow1;
    private DataApprovalWorkflow workflow12;
    private DataApprovalWorkflow workflow12A;
    private DataApprovalWorkflow workflow12B;
    private DataApprovalWorkflow workflow12A_H;
    private DataApprovalWorkflow workflow3;
    private DataApprovalWorkflow workflow1234;

    private User userA;
    private User userB;

    private DataElementCategoryOption optionA;
    private DataElementCategoryOption optionB;
    private DataElementCategoryOption optionC;
    private DataElementCategoryOption optionD;
    private DataElementCategoryOption optionE;
    private DataElementCategoryOption optionF;
    private DataElementCategoryOption optionG;
    private DataElementCategoryOption optionH;

    private DataElementCategoryOptionCombo optionComboAE;
    private DataElementCategoryOptionCombo optionComboAF;
    private DataElementCategoryOptionCombo optionComboAG;
    private DataElementCategoryOptionCombo optionComboAH;
    private DataElementCategoryOptionCombo optionComboBE;
    private DataElementCategoryOptionCombo optionComboBF;
    private DataElementCategoryOptionCombo optionComboBG;
    private DataElementCategoryOptionCombo optionComboBH;
    private DataElementCategoryOptionCombo optionComboCE;
    private DataElementCategoryOptionCombo optionComboCF;
    private DataElementCategoryOptionCombo optionComboCG;
    private DataElementCategoryOptionCombo optionComboCH;
    private DataElementCategoryOptionCombo optionComboDE;
    private DataElementCategoryOptionCombo optionComboDF;
    private DataElementCategoryOptionCombo optionComboDG;
    private DataElementCategoryOptionCombo optionComboDH;

    private DataElementCategory categoryA;
    private DataElementCategory categoryB;

    private DataElementCategoryCombo categoryComboA;

    private CategoryOptionGroup groupAB;
    private CategoryOptionGroup groupCD;
    private CategoryOptionGroup groupEF;
    private CategoryOptionGroup groupGH;

    private CategoryOptionGroupSet groupSetABCD;
    private CategoryOptionGroupSet groupSetEFGH;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------
    
    @Override
    public void setUpTest()
    {
        userService = _userService;
        
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        periodA = createPeriod( "201401" ); // Monthly: Jan
        periodB = createPeriod( "201402" ); // Monthly: Feb
        periodC = createPeriod( "201403" ); // Monthly: Mar

        periodM12 = createPeriod( "201412" ); // Monthly: December

        periodD = createPeriod( "20140105" ); // Daily

        periodQ = createPeriod( "2014Q1" ); // Quarterly

        periodW = createPeriod( "2014W1" ); // Weekly, Monday 2013-12-30 to Sunday 2014-01-05
        periodW4 = createPeriod( "2014W4" ); // Weekly, Monday 2014-01-20 to Sunday 2014-01-26
        periodW5 = createPeriod( "2014W5" ); // Weekly, Monday 2014-01-27 to Sunday 2014-02-02

        periodY = createPeriod( "2014" ); // Yearly

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodY );
        periodService.addPeriod( periodW );

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodQ );

        //
        // Organisation unit hierarchy:
        //
        // Level 1       A
        //               |
        // Level 2       B
        //              / \
        // Level 3     C   E
        //             |   |
        // Level 4     D   F
        //

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B', organisationUnitA );
        organisationUnitC = createOrganisationUnit( 'C', organisationUnitB );
        organisationUnitD = createOrganisationUnit( 'D', organisationUnitC );
        organisationUnitE = createOrganisationUnit( 'E', organisationUnitB );
        organisationUnitF = createOrganisationUnit( 'F', organisationUnitE );

        organisationUnitService.addOrganisationUnit( organisationUnitA );
        organisationUnitService.addOrganisationUnit( organisationUnitB );
        organisationUnitService.addOrganisationUnit( organisationUnitC );
        organisationUnitService.addOrganisationUnit( organisationUnitD );
        organisationUnitService.addOrganisationUnit( organisationUnitE );
        organisationUnitService.addOrganisationUnit( organisationUnitF );

        level1 = new DataApprovalLevel( "level1", 1, null );
        level2 = new DataApprovalLevel( "level2", 2, null );
        level3 = new DataApprovalLevel( "level3", 3, null );
        level4 = new DataApprovalLevel( "level4", 4, null );

        dataApprovalLevelService.addDataApprovalLevel( level1 );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level3 );
        dataApprovalLevelService.addDataApprovalLevel( level4 );

        workflow0 = new DataApprovalWorkflow( "workflow0", periodType, newHashSet() );
        workflow1 = new DataApprovalWorkflow( "workflow1", periodType, newHashSet( level1 ) );
        workflow12 = new DataApprovalWorkflow( "workflow12", periodType, newHashSet( level1, level2 ) );
        workflow12A = new DataApprovalWorkflow( "workflow12A", periodType, newHashSet( level1, level2 ) );
        workflow12B = new DataApprovalWorkflow( "workflow12B", periodType, newHashSet( level1, level2 ) );
        workflow3 = new DataApprovalWorkflow( "workflow3", periodType, newHashSet( level3 ) );
        workflow1234 = new DataApprovalWorkflow( "workflow1234", periodType, newHashSet( level1, level2, level3, level4 ) );

        dataApprovalService.addWorkflow( workflow0 );
        dataApprovalService.addWorkflow( workflow1 );
        dataApprovalService.addWorkflow( workflow12 );
        dataApprovalService.addWorkflow( workflow12A );
        dataApprovalService.addWorkflow( workflow12B );
        dataApprovalService.addWorkflow( workflow3 );
        dataApprovalService.addWorkflow( workflow1234 );

        userA = createUser( 'A' );
        userB = createUser( 'B' );

        userService.addUser( userA );
        userService.addUser( userB );

        defaultOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        setDependency( dataApprovalService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( organisationUnitService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( dataApprovalStore, "currentUserService", currentUserService, CurrentUserService.class );

        DataApprovalPermissionsEvaluator.invalidateCache();
    }

    private void setCurrentUserServiceDependencies( CurrentUserService mockCurrentUserService )
    {
        setDependency( dataApprovalService, "currentUserService", mockCurrentUserService, CurrentUserService.class );
        setDependency( dataApprovalLevelService, "currentUserService", mockCurrentUserService, CurrentUserService.class );
        setDependency( organisationUnitService, "currentUserService", mockCurrentUserService, CurrentUserService.class );
        setDependency( dataApprovalStore, "currentUserService", mockCurrentUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // Categories supportive methods
    // -------------------------------------------------------------------------

    private void setUpCategories()
    {
        optionA = new DataElementCategoryOption( "CategoryOptionA" );
        optionB = new DataElementCategoryOption( "CategoryOptionB" );
        optionC = new DataElementCategoryOption( "CategoryOptionC" );
        optionD = new DataElementCategoryOption( "CategoryOptionD" );
        optionE = new DataElementCategoryOption( "CategoryOptionE" );
        optionF = new DataElementCategoryOption( "CategoryOptionF" );
        optionG = new DataElementCategoryOption( "CategoryOptionG" );
        optionH = new DataElementCategoryOption( "CategoryOptionH" );

        categoryService.addDataElementCategoryOption( optionA );
        categoryService.addDataElementCategoryOption( optionB );
        categoryService.addDataElementCategoryOption( optionC );
        categoryService.addDataElementCategoryOption( optionD );
        categoryService.addDataElementCategoryOption( optionE );
        categoryService.addDataElementCategoryOption( optionF );
        categoryService.addDataElementCategoryOption( optionG );
        categoryService.addDataElementCategoryOption( optionH );

        categoryA = createDataElementCategory( 'A', optionA, optionB, optionC, optionD );
        categoryB = createDataElementCategory( 'B', optionE, optionF, optionG, optionH );

        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );

        categoryComboA = createCategoryCombo( 'A', categoryA, categoryB );

        categoryService.addDataElementCategoryCombo( categoryComboA );

        optionComboAE = createCategoryOptionCombo( 'A', categoryComboA, optionA, optionE );
        optionComboAF = createCategoryOptionCombo( 'B', categoryComboA, optionA, optionF );
        optionComboAG = createCategoryOptionCombo( 'C', categoryComboA, optionA, optionG );
        optionComboAH = createCategoryOptionCombo( 'D', categoryComboA, optionA, optionH );
        optionComboBE = createCategoryOptionCombo( 'E', categoryComboA, optionB, optionE );
        optionComboBF = createCategoryOptionCombo( 'F', categoryComboA, optionB, optionF );
        optionComboBG = createCategoryOptionCombo( 'G', categoryComboA, optionB, optionG );
        optionComboBH = createCategoryOptionCombo( 'H', categoryComboA, optionB, optionH );
        optionComboCE = createCategoryOptionCombo( 'I', categoryComboA, optionC, optionE );
        optionComboCF = createCategoryOptionCombo( 'J', categoryComboA, optionC, optionF );
        optionComboCG = createCategoryOptionCombo( 'K', categoryComboA, optionC, optionG );
        optionComboCH = createCategoryOptionCombo( 'L', categoryComboA, optionC, optionH );
        optionComboDE = createCategoryOptionCombo( 'M', categoryComboA, optionD, optionE );
        optionComboDF = createCategoryOptionCombo( 'N', categoryComboA, optionD, optionF );
        optionComboDG = createCategoryOptionCombo( 'O', categoryComboA, optionD, optionG );
        optionComboDH = createCategoryOptionCombo( 'P', categoryComboA, optionD, optionH );

        categoryService.addDataElementCategoryOptionCombo( optionComboAE );
        categoryService.addDataElementCategoryOptionCombo( optionComboAF );
        categoryService.addDataElementCategoryOptionCombo( optionComboAG );
        categoryService.addDataElementCategoryOptionCombo( optionComboAH );
        categoryService.addDataElementCategoryOptionCombo( optionComboBE );
        categoryService.addDataElementCategoryOptionCombo( optionComboBF );
        categoryService.addDataElementCategoryOptionCombo( optionComboBG );
        categoryService.addDataElementCategoryOptionCombo( optionComboBH );
        categoryService.addDataElementCategoryOptionCombo( optionComboCE );
        categoryService.addDataElementCategoryOptionCombo( optionComboCF );
        categoryService.addDataElementCategoryOptionCombo( optionComboCG );
        categoryService.addDataElementCategoryOptionCombo( optionComboCH );
        categoryService.addDataElementCategoryOptionCombo( optionComboDE );
        categoryService.addDataElementCategoryOptionCombo( optionComboDF );
        categoryService.addDataElementCategoryOptionCombo( optionComboDG );
        categoryService.addDataElementCategoryOptionCombo( optionComboDH );

        groupAB = createCategoryOptionGroup( 'A', optionA, optionB );
        groupCD = createCategoryOptionGroup( 'C', optionC, optionD );
        groupEF = createCategoryOptionGroup( 'E', optionE, optionF );
        groupGH = createCategoryOptionGroup( 'G', optionG, optionH );

        categoryService.saveCategoryOptionGroup( groupAB );
        categoryService.saveCategoryOptionGroup( groupCD );
        categoryService.saveCategoryOptionGroup( groupEF );
        categoryService.saveCategoryOptionGroup( groupGH );

        groupSetABCD = new CategoryOptionGroupSet( "GroupSetABCD" );
        groupSetEFGH = new CategoryOptionGroupSet( "GroupSetEFGH" );

        categoryService.saveCategoryOptionGroupSet( groupSetABCD );
        categoryService.saveCategoryOptionGroupSet( groupSetEFGH );

        groupSetABCD.addCategoryOptionGroup( groupAB );
        groupSetABCD.addCategoryOptionGroup( groupCD );

        groupSetEFGH.addCategoryOptionGroup( groupAB );
        groupSetEFGH.addCategoryOptionGroup( groupEF );

        groupAB.setGroupSet( groupSetABCD );
        groupCD.setGroupSet( groupSetABCD );
        groupEF.setGroupSet( groupSetEFGH );
        groupGH.setGroupSet( groupSetEFGH );

        level2ABCD = new DataApprovalLevel( "level2ABCD", 2, groupSetABCD );
        level2EFGH = new DataApprovalLevel( "level2EFGH", 2, groupSetEFGH );

        dataApprovalLevelService.addDataApprovalLevel( level2EFGH );
        dataApprovalLevelService.addDataApprovalLevel( level2ABCD );

        workflow12A_H = new DataApprovalWorkflow( "workflow12A_H", periodType, newHashSet( level1, level2, level2ABCD, level2EFGH ) );
        dataApprovalService.addWorkflow( workflow12A_H );
    }

    // -------------------------------------------------------------------------
    // Basic DataApproval
    // -------------------------------------------------------------------------

    @Test
    public void testAddDuplicateDataApproval()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();
        DataApproval dataApprovalA = new DataApproval( level2, workflow12, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow12, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ); // Same

        dataApprovalService.approveData( newArrayList( dataApprovalA ) );
        dataApprovalService.approveData( newArrayList( dataApprovalB ) ); // Redundant, so call is ignored.
    }

    @Test
    @Category( IntegrationTest.class )
    public void testAddAllAndGetDataApprovalStatus()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();
        DataApproval dataApprovalA = new DataApproval( level1, workflow12A, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow12A, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level2, workflow12A, periodB, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level2, workflow12B, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );

        dataApprovalService.approveData( newArrayList( dataApprovalB, dataApprovalC, dataApprovalD ) ); // Must be approved before A.
        dataApprovalService.approveData( newArrayList( dataApprovalA ) );

        DataApprovalStatus status = dataApprovalService.getDataApprovalStatus( workflow12A, periodA, organisationUnitA, defaultOptionCombo );
        assertEquals( DataApprovalState.APPROVED_HERE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitA.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        DataApprovalLevel level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level1.getName(), level.getName() );

        status = dataApprovalService.getDataApprovalStatus( workflow12A, periodA, organisationUnitB, defaultOptionCombo );
        assertEquals( DataApprovalState.APPROVED_ABOVE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitB.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level2.getName(), level.getName() );

        status = dataApprovalService.getDataApprovalStatus( workflow12A, periodB, organisationUnitB, defaultOptionCombo );
        assertEquals( DataApprovalState.APPROVED_HERE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitB.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level2.getName(), level.getName() );

        status = dataApprovalService.getDataApprovalStatus( workflow12B, periodA, organisationUnitB, defaultOptionCombo );
        assertEquals( DataApprovalState.APPROVED_HERE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitB.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level2.getName(), level.getName() );

        status = dataApprovalService.getDataApprovalStatus( workflow12B, periodB, organisationUnitB, defaultOptionCombo );
        assertEquals( DataApprovalState.UNAPPROVED_READY, status.getState() );
        level = status.getApprovedLevel();
        assertNull( level );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testDeleteDataApproval()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();
        DataApproval dataApprovalA = new DataApproval( level1, workflow12, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow12, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userB );

        dataApprovalService.approveData( newArrayList( dataApprovalB ) );
        dataApprovalService.approveData( newArrayList( dataApprovalA ) );

        assertTrue( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitA, defaultOptionCombo ).getState().isApproved() );
        assertTrue( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitB, defaultOptionCombo ).getState().isApproved() );

        dataApprovalService.unapproveData( newArrayList( dataApprovalA ) ); // Only A should be deleted.

        assertFalse( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitA, defaultOptionCombo ).getState().isApproved() );
        assertTrue( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitB, defaultOptionCombo ).getState().isApproved() );

        dataApprovalService.unapproveData( newArrayList( dataApprovalB ) );

        assertFalse( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitA, defaultOptionCombo ).getState().isApproved() );
        assertFalse( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitB, defaultOptionCombo ).getState().isApproved() );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testGetDataApprovalState()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        // No levels defined.
        assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        // Levels defined.

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        Date date = new Date();

        // Approved for organisation unit F
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalF ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        // Also approved also for organisation unit E
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalE ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        // Also approved for organisation unit D
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalD ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        // Also approved for organisation unit C
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalC ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        // Also approved for organisation unit B
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalB ) );

        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        // Also approved for organisation unit A
        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalA ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testGetDataApprovalStateAbove()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Approved for organisation unit C
        DataApproval dataApprovalC = new DataApproval( level3, workflow3, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalC ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow3, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow3, periodA, organisationUnitD, defaultOptionCombo ).getState() );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testGetDataApprovalStateWithMultipleChildren()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getState() );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testGetDataApprovalStateOtherPeriodTypes()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodB, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodY, organisationUnitA, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodY, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodD, organisationUnitD, defaultOptionCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodW, organisationUnitD, defaultOptionCombo ).getState() );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayApproveSameLevel()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Level 4 (organisationUnitD and organisationUnitF ready)
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit C." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 2 (organisationUnitB) ready
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit F." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit E." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit C." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo) );
        assertEquals( "APPROVED_HERE level=level3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo) );
        assertEquals( "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo) );
        assertEquals( "APPROVED_HERE level=level3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo) );
        assertEquals( "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo) );

        // Level 1 (organisationUnitA) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        assertEquals( "APPROVED_HERE level=level2 approve=F unapprove=T accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo) );
        assertEquals( "APPROVED_ABOVE level=level3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo) );
        assertEquals( "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo) );
        assertEquals( "APPROVED_ABOVE level=level3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo) );
        assertEquals( "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ) );

        // Level 1 (organisationUnitA) try to approve
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit A." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected
        }
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayApproveLowerLevels()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Level 4 (organisationUnitD and organisationUnitF ready)
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 2 (organisationUnitB) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove() );

        // Level 1 (organisationUnitA) ready
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit B." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove() );

        // Level 1 (organisationUnitA) try to approve
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit A." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected
        }
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayApproveSameAndLowerLevels()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Level 4 (organisationUnitD and organisationUnitF ready)
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 2 (organisationUnitB) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove());

        // Level 1 (organisationUnitA) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayApprove() );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayApprove() );

        // Level 1 (organisationUnitA) try to approve
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit A." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected
        }
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayApproveNoAuthority()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayApprove());

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayApprove());

        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayApprove());

        dataApprovalStore.addDataApproval( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayApprove());
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayUnapproveSameLevel()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayUnapproveLowerLevels()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayUnapproveWithAcceptAuthority()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_ACCEPT_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());
    }

    @Test
    @Category( IntegrationTest.class )
    public void testMayUnapproveNoAuthority()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultOptionCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultOptionCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultOptionCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultOptionCombo ).getPermissions().isMayUnapprove());
    }

    // -------------------------------------------------------------------------
    // Test with Categories
    // -------------------------------------------------------------------------

    @Test
    @Category( IntegrationTest.class )
    public void testApprovalsWithCategories()
    {
        setUpCategories();

        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS, DataApproval.AUTH_ACCEPT_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Category A -> Options A,B,C,D
        // Category B -> Options E,F,G,H
        //
        // Option combo A -> Options A,E -> Groups A,C
        // Option combo B -> Options A,F -> Groups A,C
        // Option combo C -> Options A,G -> Groups A,D
        // Option combo D -> Options A,H -> Groups A,D
        // Option combo E -> Options B,E -> Groups B,C
        // Option combo F -> Options B,F -> Groups B,C
        // Option combo G -> Options B,G -> Groups B,D
        // Option combo H -> Options B,H -> Groups B,D
        // Option combo I -> Options C,E -> Groups B,D
        // Option combo J -> Options C,F -> Groups B,D
        // Option combo K -> Options C,G -> Groups B,D

        List<DataApprovalLevel> levels = dataApprovalLevelService.getAllDataApprovalLevels();
        assertEquals( 6, levels.size() );
        assertEquals("level1", levels.get(0).getName() );
        assertEquals("level2", levels.get(1).getName() );
        assertEquals("level2ABCD", levels.get(2).getName() );
        assertEquals("level2EFGH", levels.get(3).getName() );
        assertEquals("level3", levels.get(4).getName() );
        assertEquals("level4", levels.get(5).getName() );

        levels = workflow12A_H.getSortedLevels();
        assertEquals( 4, levels.size() );
        assertEquals("level1", levels.get(0).getName() );
        assertEquals("level2", levels.get(1).getName() );
        assertEquals("level2ABCD", levels.get(2).getName() );
        assertEquals("level2EFGH", levels.get(3).getName() );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, null ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, null ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAE ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAE ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAF ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAG ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAG ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCE ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCE ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCF ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCF ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCG ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCG ) );

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow12A_H, periodA, organisationUnitA, optionComboAE, NOT_ACCEPTED, date, userA ) ) );
            fail( "organisationUnitB should not be ready for approval at level 1." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Can't add this approval before approving at a lower level.
        }

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow12A_H, periodA, organisationUnitA, optionComboAE, NOT_ACCEPTED, date, userA ) ) );
            fail( "organisationUnitB should not be ready for approval at level 2." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Can't add this approval before approving at a lower level.
        }

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level2ABCD, workflow12A_H, periodA, organisationUnitA, optionComboAE, NOT_ACCEPTED, date, userA ) ) );
            fail( "organisationUnitB should not be ready for approval at level 2ABCD." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Can't add this approval before approving at a lower level.
        }

        dataApprovalService.approveData( newArrayList( new DataApproval( level2EFGH, workflow12A_H, periodA, organisationUnitB, optionComboAE, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, null ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, null ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAE ) );
        assertEquals( "APPROVED_HERE level=level2EFGH approve=T unapprove=T accept=T unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAE ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAF ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAG ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAG ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCE ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCE ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCF ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCF ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCG ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCG ) );

        dataApprovalService.approveData( newArrayList( new DataApproval( level2EFGH, workflow12A_H, periodA, organisationUnitB, optionComboAF, ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level2EFGH, workflow12A_H, periodA, organisationUnitB, optionComboCG, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, null ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, null ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAE ) );
        assertEquals( "APPROVED_HERE level=level2EFGH approve=T unapprove=T accept=T unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAE ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "ACCEPTED_HERE level=level2EFGH approve=T unapprove=T accept=F unaccept=T read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAF ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAG ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAG ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCE ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCE ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCF ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCF ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboCG ) );
        assertEquals( "APPROVED_HERE level=level2EFGH approve=T unapprove=T accept=T unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboCG ) );

        dataApprovalService.approveData( newArrayList( new DataApproval( level2ABCD, workflow12A_H, periodA, organisationUnitB, optionComboAF, ACCEPTED, date, userA ) ) );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "ACCEPTED_HERE level=level2ABCD approve=T unapprove=T accept=F unaccept=T read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAF ) );

        dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow12A_H, periodA, organisationUnitB, optionComboAF, ACCEPTED, date, userA ) ) );

        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "ACCEPTED_HERE level=level2 approve=T unapprove=T accept=F unaccept=T read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAF ) );

        dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow12A_H, periodA, organisationUnitA, optionComboAF, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( "APPROVED_HERE level=level1 approve=F unapprove=T accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "APPROVED_ABOVE level=level2 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H, periodA, organisationUnitB, optionComboAF ) );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testWorkflows()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow3, periodA, organisationUnitC, defaultOptionCombo ) );

        Date date = new Date();

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level2, workflow12, periodA, organisationUnitB, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow3, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );

        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=T accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level3 approve=T unapprove=T accept=F unaccept=F read=T", statusAndPermissions( workflow3, periodA, organisationUnitC, defaultOptionCombo ) );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testPeriodsEndingDuringWorkflowApproval()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitC );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL, DataApproval.AUTH_APPROVE );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW4, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodB, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW5, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodY, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodC, organisationUnitC, defaultOptionCombo ) );

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodB, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW4, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodB, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW5, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodC, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodY, organisationUnitC, defaultOptionCombo ) );

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodM12, organisationUnitC, defaultOptionCombo, NOT_ACCEPTED, date, userA ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW4, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodB, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW5, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodY, organisationUnitC, defaultOptionCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodC, organisationUnitC, defaultOptionCombo ) );
    }

    /**
     * Returns approval status and permissions information as a string.
     * This allows a test to compare the result against a string and test
     * several things at once. More importantly, it shows in the log
     * all of the ways in which the test status and permissions differs from
     * expected, instead of showing only one different value. This can
     * save time in understanding the difference between the expected value
     * and the test result.
     *
     * @param workflow Approval workflow
     * @param period Approval period
     * @param organisationUnit Approval orgaisation unit
     * @param attributeOptionCombo Approval attribute option combination
     * @return A String representing the state, level, and allowed user actions
     */
    private String statusAndPermissions( DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        DataElementCategoryOptionCombo attributeOptionCombo )
    {
        DataApprovalStatus status = dataApprovalService.getDataApprovalStatusAndPermissions( workflow, period, organisationUnit, attributeOptionCombo );

        DataApprovalPermissions permissions = status.getPermissions();

        return status.getState().toString()
                + " level=" + ( status.getApprovedLevel() == null ? "null" : status.getApprovedLevel().getName() )
                + " approve=" + ( permissions.isMayApprove() ? "T" : "F" )
                + " unapprove=" + ( permissions.isMayUnapprove() ? "T" : "F" )
                + " accept=" + ( permissions.isMayAccept() ? "T" : "F" )
                + " unaccept=" + ( permissions.isMayUnaccept() ? "T" : "F" )
                + " read=" + ( permissions.isMayReadData() ? "T" : "F" );
    }
}
