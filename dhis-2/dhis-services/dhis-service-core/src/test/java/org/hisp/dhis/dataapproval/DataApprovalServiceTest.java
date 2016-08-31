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
import java.util.Set;

import org.hisp.dhis.DhisTest;
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
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Jim Grace
 */
public class DataApprovalServiceTest
    extends DhisTest
{
    private static final String AUTH_APPR_LEVEL = "F_SYSTEM_SETTING";
    
    private final static boolean NOT_ACCEPTED = false;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private DataApprovalStore dataApprovalStore;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private DataApprovalWorkflowService dataApprovalWorkflowService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;
   
    @Autowired 
    protected IdentifiableObjectManager identifiableObjectManager;
    
    @Autowired
    protected UserService _userService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataElementCategoryOptionCombo defaultCombo;

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

    private DataApprovalLevel level1ABCD;
    private DataApprovalLevel level1EFGH;
    private DataApprovalLevel level2ABCD;
    private DataApprovalLevel level3ABCD;

    private DataApprovalWorkflow workflow0;
    private DataApprovalWorkflow workflow1;
    private DataApprovalWorkflow workflow12;
    private DataApprovalWorkflow workflow12A;
    private DataApprovalWorkflow workflow12B;
    private DataApprovalWorkflow workflow12C;
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
        workflow12C = new DataApprovalWorkflow( "workflow12C", periodType, newHashSet( level1, level2 ) );
        workflow3 = new DataApprovalWorkflow( "workflow3", periodType, newHashSet( level3 ) );
        workflow1234 = new DataApprovalWorkflow( "workflow1234", periodType, newHashSet( level1, level2, level3, level4 ) );

        dataApprovalWorkflowService.addWorkflow( workflow0 );
        dataApprovalWorkflowService.addWorkflow( workflow1 );
        dataApprovalWorkflowService.addWorkflow( workflow12 );
        dataApprovalWorkflowService.addWorkflow( workflow12A );
        dataApprovalWorkflowService.addWorkflow( workflow12B );
        dataApprovalWorkflowService.addWorkflow( workflow12C );
        dataApprovalWorkflowService.addWorkflow( workflow3 );
        dataApprovalWorkflowService.addWorkflow( workflow1234 );

        userA = createUser( 'A' );
        userB = createUser( 'B' );

        userService.addUser( userA );
        userService.addUser( userB );

        defaultCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        int idA = organisationUnitA.getId();
        int idB = organisationUnitB.getId();
        int idC = organisationUnitC.getId();
        int idD = organisationUnitD.getId();
        int idE = organisationUnitE.getId();
        int idF = organisationUnitF.getId();

        String uidA = organisationUnitA.getUid();
        String uidB = organisationUnitB.getUid();
        String uidC = organisationUnitC.getUid();
        String uidD = organisationUnitD.getUid();
        String uidE = organisationUnitE.getUid();
        String uidF = organisationUnitF.getUid();

        jdbcTemplate.execute(
                "CREATE TABLE _orgunitstructure "+
                "(" +
                "  organisationunitid integer NOT NULL, " +
                "  organisationunituid character(11) NOT NULL, " +
                "  level integer, " +
                "  idlevel1 integer, " +
                "  idlevel2 integer, " +
                "  idlevel3 integer, " +
                "  idlevel4 integer, " +
                "  CONSTRAINT _orgunitstructure_pkey PRIMARY KEY (organisationunitid)" +
                ");" );

        jdbcTemplate.execute( "INSERT INTO _orgunitstructure VALUES (" + idA + ", '" + uidA + "', 1, " + idA + ", null, null, null);" );
        jdbcTemplate.execute( "INSERT INTO _orgunitstructure VALUES (" + idB + ", '" + uidB + "', 2, " + idA + ", " + idB + ", null, null);" );
        jdbcTemplate.execute( "INSERT INTO _orgunitstructure VALUES (" + idC + ", '" + uidC + "', 3, " + idA + ", " + idB + ", " + idC + ", null);" );
        jdbcTemplate.execute( "INSERT INTO _orgunitstructure VALUES (" + idD + ", '" + uidD + "', 4, " + idA + ", " + idB + ", " + idC + ", " + idD + ");" );
        jdbcTemplate.execute( "INSERT INTO _orgunitstructure VALUES (" + idE + ", '" + uidE + "', 3, " + idA + ", " + idB + ", " + idE + ", null);" );
        jdbcTemplate.execute( "INSERT INTO _orgunitstructure VALUES (" + idF + ", '" + uidF + "', 4, " + idA + ", " + idB + ", " + idE + ", " + idF + ");" );
        
        systemSettingManager.invalidateCache();
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

    // ---------------------------------------------------------------------
    // Set Up Categories
    // ---------------------------------------------------------------------

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

        level1ABCD = new DataApprovalLevel( "level1ABCD", 1, groupSetABCD );
        level1EFGH = new DataApprovalLevel( "level1EFGH", 1, groupSetEFGH );
        level2ABCD = new DataApprovalLevel( "level2ABCD", 2, groupSetABCD );
        level3ABCD = new DataApprovalLevel( "level3ABCD", 3, groupSetABCD );

        dataApprovalLevelService.addDataApprovalLevel( level1ABCD );
        dataApprovalLevelService.addDataApprovalLevel( level1EFGH );
        dataApprovalLevelService.addDataApprovalLevel( level2ABCD );
        dataApprovalLevelService.addDataApprovalLevel( level3ABCD );
    }

    // -------------------------------------------------------------------------
    // Basic DataApproval
    // -------------------------------------------------------------------------

    @Test
    public void test()
    {
    }

    @Test
    public void testAddAllAndGetDataApproval()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();
        DataApproval dataApprovalA = new DataApproval( level1, workflow12A, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow12A, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level2, workflow12A, periodB, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level2, workflow12B, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );

        dataApprovalService.approveData( newArrayList( dataApprovalB, dataApprovalC, dataApprovalD ) ); // Must be approved before A.
        dataApprovalService.approveData( newArrayList( dataApprovalA ) );

        DataApprovalStatus status = dataApprovalService.getDataApprovalStatus( workflow12A, periodA, organisationUnitA, defaultCombo );
        assertEquals( DataApprovalState.APPROVED_HERE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitA.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        DataApprovalLevel level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level1, level );

        status = dataApprovalService.getDataApprovalStatus( workflow12A, periodA, organisationUnitB, defaultCombo );
        assertEquals( DataApprovalState.APPROVED_ABOVE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitB.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level2, level );

        status = dataApprovalService.getDataApprovalStatus( workflow12A, periodB, organisationUnitB, defaultCombo );
        assertEquals( DataApprovalState.APPROVED_HERE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitB.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level2, level );

        status = dataApprovalService.getDataApprovalStatus( workflow12B, periodA, organisationUnitB, defaultCombo );
        assertEquals( DataApprovalState.APPROVED_HERE, status.getState() );
        assertNotNull( status );
        assertEquals( organisationUnitB.getUid(), status.getOrganisationUnitUid() );
        assertEquals( date, status.getCreated() );
        assertEquals( userA.getId(), status.getCreator().getId() );
        level = status.getApprovedLevel();
        assertNotNull( level );
        assertEquals( level2, level );

        status = dataApprovalService.getDataApprovalStatus( workflow12B, periodB, organisationUnitB, defaultCombo );
        assertEquals( DataApprovalState.UNAPPROVED_READY, status.getState() );
        level = status.getApprovedLevel();
        assertNull( level );
    }

        @Ignore //TODO Fails with DataMayNotBeApproved
        @Test
        public void testAddDuplicateDataApproval()
        {
            Set<OrganisationUnit> units = newHashSet( organisationUnitA );

            CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
            setCurrentUserServiceDependencies( currentUserService );

            Date date = new Date();
            DataApproval dataApprovalA = new DataApproval( level1, workflow12, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
            DataApproval dataApprovalB = new DataApproval( level1, workflow12, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA ); // Same

            dataApprovalService.approveData( newArrayList( dataApprovalA ) );
            dataApprovalService.approveData( newArrayList( dataApprovalB ) ); // Redundant, so call is ignored.
        }

        @Test
        @Ignore
        public void testDeleteDataApproval()
        {
            Set<OrganisationUnit> units = newHashSet( organisationUnitA );

            CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
            setCurrentUserServiceDependencies( currentUserService );

            Date date = new Date();
            DataApproval dataApprovalA = new DataApproval( level1, workflow12, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
            DataApproval dataApprovalB = new DataApproval( level2, workflow12, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userB );

            dataApprovalService.approveData( newArrayList( dataApprovalB ) );
            dataApprovalService.approveData( newArrayList( dataApprovalA ) );

            assertTrue( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitA, defaultCombo ).getState().isApproved() );
            assertTrue( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitB, defaultCombo ).getState().isApproved() );

            dataApprovalService.unapproveData( newArrayList( dataApprovalA ) ); // Only A should be deleted.

            assertFalse( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitA, defaultCombo ).getState().isApproved() );
            assertTrue( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitB, defaultCombo ).getState().isApproved() );

            dataApprovalService.unapproveData( newArrayList( dataApprovalB ) );

            assertFalse( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitA, defaultCombo ).getState().isApproved() );
            assertFalse( dataApprovalService.getDataApprovalStatus( workflow12, periodA, organisationUnitB, defaultCombo ).getState().isApproved() );
        }

        @Test
        public void testGetDataApprovalState()
        {
            Set<OrganisationUnit> units = newHashSet( organisationUnitA );

            CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
            setCurrentUserServiceDependencies( currentUserService );

            // No levels defined.
            assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVABLE, dataApprovalService.getDataApprovalStatus( workflow0, periodA, organisationUnitF, defaultCombo ).getState() );

            // Levels defined.

            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

            Date date = new Date();

            // Approved for organisation unit F
            DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA );
            dataApprovalService.approveData( newArrayList( dataApprovalF ) );

            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

            // Also approved also for organisation unit E
            DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA );
            dataApprovalService.approveData( newArrayList( dataApprovalE ) );

            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

            // Also approved for organisation unit D
            DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA );
            dataApprovalService.approveData( newArrayList( dataApprovalD ) );

            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

            // Also approved for organisation unit C
            DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA );
            dataApprovalService.approveData( newArrayList( dataApprovalC ) );

            assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

            // Also approved for organisation unit B
            DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
            dataApprovalService.approveData( newArrayList( dataApprovalB ) );

            assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

            // Also approved for organisation unit A
            DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
            dataApprovalService.approveData( newArrayList( dataApprovalA ) );

            assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
            assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );
        }

    @Test
    public void testGetDataApprovalStateAbove()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Approved for organisation unit C
        DataApproval dataApprovalC = new DataApproval( level3, workflow3, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA );
        dataApprovalService.approveData( newArrayList( dataApprovalC ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow3, periodA, organisationUnitC, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow3, periodA, organisationUnitD, defaultCombo ).getState() );
    }

    @Test
    public void testGetDataApprovalStateWithMultipleChildren()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitC, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitE, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitF, defaultCombo ).getState() );
    }

    @Test
    public void testGetDataApprovalStateOtherPeriodTypes()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodA, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodB, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflow1234, periodY, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodY, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodD, organisationUnitD, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflow1234, periodW, organisationUnitD, defaultCombo ).getState() );
    }

    @Test
    public void testMayApproveSameLevel()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Level 4 (organisationUnitD and organisationUnitF ready)
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit C." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA ) );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 2 (organisationUnitB) ready
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit F." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA ) );

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit E." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA ) );

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit C." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );

        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo) );
        assertEquals( "APPROVED_HERE level=3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo) );
        assertEquals( "APPROVED_ABOVE level=4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo) );
        assertEquals( "APPROVED_HERE level=3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo) );
        assertEquals( "APPROVED_ABOVE level=4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo) );

        // Level 1 (organisationUnitA) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        assertEquals( "APPROVED_HERE level=2 approve=F unapprove=T accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo) );
        assertEquals( "APPROVED_ABOVE level=3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo) );
        assertEquals( "APPROVED_ABOVE level=4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo) );
        assertEquals( "APPROVED_ABOVE level=3 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo) );
        assertEquals( "APPROVED_ABOVE level=4 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ) );

        // Level 1 (organisationUnitA) try to approve
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit A." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected
        }
    }

    @Test
    public void testMayApproveLowerLevels()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Level 4 (organisationUnitD and organisationUnitF ready)
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 2 (organisationUnitB) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 1 (organisationUnitA) ready
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit B." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected error, so add the data through dataApprovalStore:
        }
        dataApprovalStore.addDataApproval( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA ) );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 1 (organisationUnitA) try to approve
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit A." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected
        }
    }

    @Test
    public void testMayApproveSameAndLowerLevels()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        // Level 4 (organisationUnitD and organisationUnitF ready)
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 2 (organisationUnitB) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        dataApprovalService.approveData( newArrayList( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 1 (organisationUnitA) ready
        dataApprovalService.approveData( newArrayList( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayApprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayApprove());

        // Level 1 (organisationUnitA) try to approve
        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA ) ) );
            fail( "User should not have permission to approve org unit A." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Expected
        }
    }

    @Test
    public void testMayApproveNoAuthority()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayApprove());

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayApprove());

        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA ) );
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayApprove());

        dataApprovalStore.addDataApproval( new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA ) );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayApprove());
    }

    @Test
    public void testMayUnapproveSameLevel()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());
    }

    @Test
    public void testMayUnapproveLowerLevels()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());
    }

    @Ignore //TODO Fails
    @Test
    public void testMayUnapproveWithAcceptAuthority()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_ACCEPT_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertTrue( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());
    }

    @Test
    public void testMayUnapproveNoAuthority()
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitB );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        DataApproval dataApprovalA = new DataApproval( level1, workflow1234, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalB = new DataApproval( level2, workflow1234, periodA, organisationUnitB, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalC = new DataApproval( level3, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalD = new DataApproval( level4, workflow1234, periodA, organisationUnitD, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalE = new DataApproval( level3, workflow1234, periodA, organisationUnitE, defaultCombo, NOT_ACCEPTED, date, userA );
        DataApproval dataApprovalF = new DataApproval( level4, workflow1234, periodA, organisationUnitF, defaultCombo, NOT_ACCEPTED, date, userA );

        dataApprovalStore.addDataApproval( dataApprovalD );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalF );
        dataApprovalStore.addDataApproval( dataApprovalE );
        dataApprovalStore.addDataApproval( dataApprovalC );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalB );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());

        dataApprovalStore.addDataApproval( dataApprovalA );

        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitA, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitB, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitD, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitE, defaultCombo ).getPermissions().isMayUnapprove());
        assertFalse( dataApprovalService.getDataApprovalStatusAndPermissions( workflow1234, periodA, organisationUnitF, defaultCombo ).getPermissions().isMayUnapprove());
    }

    // -------------------------------------------------------------------------
    // Test with Categories
    // -------------------------------------------------------------------------

    @Ignore //TODO Get this test working
    @Test
    public void testApprovalStateWithCategories()
    {
        setUpCategories();

        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
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

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, null ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, null ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, null ) );

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAE ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAE ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAE ) );

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAF ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAF ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAF ) );

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAG ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAG ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAG ) );

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCE ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCE ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCE ) );

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCF ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCF ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCF ) );

        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCG ) );
        assertEquals("UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCG ) );
        assertEquals("UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCG ) );

        try
        {
            dataApprovalService.approveData( newArrayList( new DataApproval( level2ABCD, workflow1234, periodA, organisationUnitB, optionComboAE, NOT_ACCEPTED, date, userA ) ) );
            fail( "organisationUnitB should not be ready for data approval." );
        }
        catch ( DataMayNotBeApprovedException ex )
        {
            // Can't add this approval before approving at a lower level.
        }

        dataApprovalService.approveData( newArrayList( new DataApproval( level3ABCD, workflow1234, periodA, organisationUnitC, optionComboAE, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, null ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, null ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, null ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAE ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAF ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAG ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCE ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCF ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCG ) );

        dataApprovalService.approveData( newArrayList( new DataApproval( level2ABCD, workflow1234, periodA, organisationUnitB, optionComboAE, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, null ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, null ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, null ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAE ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAF ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboAG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboAG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboAG ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCE ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCE ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCF ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCF ) );

        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitA, optionComboCG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitB, optionComboCG ) );
        assertEquals( "UNAPPROVABLE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, optionComboCG ) );
    }

    @Ignore //TODO get this test working
    @Test
    public void testApprovalLevelWithCategories()
    {
        setUpCategories();

        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        dataApprovalLevelService.addDataApprovalLevel( level1EFGH );

        DataApprovalWorkflow workflowA = new DataApprovalWorkflow( "workflowA", periodType, newHashSet( level1EFGH ) );
        dataApprovalWorkflowService.addWorkflow( workflowA );

        DataApproval dab = new DataApproval( level1EFGH, workflowA, periodA, organisationUnitA, null, NOT_ACCEPTED, date, userA );

        dataApprovalService.approveData( newArrayList( dab ) );

        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getState() );

        assertEquals( level1EFGH, dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getState() );
        assertNull( dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, defaultCombo ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatusAndPermissions( workflowA, periodA, organisationUnitA, null ).getApprovedLevel() );

        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, optionComboAE ).getState() );

        assertNull( dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertNull( dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, optionComboAE ).getApprovedLevel() );

        dataApprovalLevelService.addDataApprovalLevel( level1ABCD );

        DataApprovalWorkflow workflowB = new DataApprovalWorkflow( "workflowB", periodType, newHashSet( level1ABCD, level1EFGH ) );
        dataApprovalWorkflowService.addWorkflow( workflowB );

        dataApprovalService.approveData( newArrayList( new DataApproval( level1ABCD, workflowB, periodA, organisationUnitA, null, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, optionComboAE ).getState() );

        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1EFGH, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, optionComboAE ).getApprovedLevel() );

        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, optionComboAE ).getState() );

        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, optionComboAE ).getApprovedLevel() );

        dataApprovalService.unapproveData( newArrayList( dab ) );

        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, optionComboAE ).getState() );

        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1EFGH, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitA, optionComboAE ).getApprovedLevel() );

        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, optionComboAE ).getState() );

        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( null, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1ABCD, dataApprovalService.getDataApprovalStatus( workflowB, periodA, organisationUnitB, optionComboAE ).getApprovedLevel() );

        dataApprovalLevelService.addDataApprovalLevel( level1 );

        DataApprovalWorkflow workflowC = new DataApprovalWorkflow( "workflowB", periodType, newHashSet( level1, level1ABCD, level1EFGH ) );
        dataApprovalWorkflowService.addWorkflow( workflowC );

        dataApprovalService.approveData( newArrayList( new DataApproval( level1, workflowC, periodA, organisationUnitA, defaultCombo, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, optionComboAE ).getState() );

        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitA, optionComboAE ).getApprovedLevel() );

        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, defaultCombo ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_ABOVE, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, optionComboAE ).getState() );

        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, null ).getApprovedLevel() );
        assertEquals( level1, dataApprovalService.getDataApprovalStatus( workflowC, periodA, organisationUnitB, optionComboAE ).getApprovedLevel() );
    }

    @Ignore //TODO get this test working
    @Test
    public void testCategoriesWithOrgUnitLevels()
    {
        setUpCategories();

        DataApprovalWorkflow workflowA = new DataApprovalWorkflow( "workflowA", periodType, newHashSet( level2, level3ABCD ) );
        dataApprovalWorkflowService.addWorkflow( workflowA );

        Date date = new Date();

        Set<OrganisationUnit> units = newHashSet( organisationUnitA );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        setCurrentUserServiceDependencies( currentUserService );

        optionA.setOrganisationUnits( newHashSet( organisationUnitC ) );
        optionB.setOrganisationUnits( newHashSet( organisationUnitE ) );
        optionC.setOrganisationUnits( newHashSet( organisationUnitE ) );
        optionD.setOrganisationUnits( newHashSet( organisationUnitE ) );

        categoryService.updateDataElementCategoryOption( optionA );
        categoryService.updateDataElementCategoryOption( optionB );
        categoryService.updateDataElementCategoryOption( optionC );
        categoryService.updateDataElementCategoryOption( optionD );

        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3ABCD, workflowA, periodA, organisationUnitC, null, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3ABCD, workflowA, periodA, organisationUnitE, null, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_WAITING, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, defaultCombo ).getState() );

        dataApprovalService.approveData( newArrayList( new DataApproval( level3ABCD, workflowA, periodA, organisationUnitE, null, NOT_ACCEPTED, date, userA ) ) );

        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitC, null ).getState() );
        assertEquals( DataApprovalState.APPROVED_HERE, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitE, null ).getState() );
        assertEquals( DataApprovalState.UNAPPROVED_READY, dataApprovalService.getDataApprovalStatus( workflowA, periodA, organisationUnitB, defaultCombo ).getState() );
    }

    @Test
    public void testWorkflows() throws Exception
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitC );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL, DataApproval.AUTH_APPROVE );
        setCurrentUserServiceDependencies( currentUserService );

        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow3, periodA, organisationUnitC, defaultCombo ) );

        Date date = new Date();

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level2, workflow12, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );
        dataApprovalStore.addDataApproval( new DataApproval( level3, workflow3, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );

        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=2 approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow12, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=3 approve=F unapprove=T accept=F unaccept=F read=T", statusAndPermissions( workflow3, periodA, organisationUnitC, defaultCombo ) );
    }

    @Test
    public void testPeriodsEndingDuringWorkflowApproval() throws Exception
    {
        Set<OrganisationUnit> units = newHashSet( organisationUnitC );

        CurrentUserService currentUserService = new MockCurrentUserService( units, null, AUTH_APPR_LEVEL, DataApproval.AUTH_APPROVE );
        setCurrentUserServiceDependencies( currentUserService );

        Date date = new Date();

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodA, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW4, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodB, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW5, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodY, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodC, organisationUnitC, defaultCombo ) );

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodB, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW4, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodB, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW5, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodC, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodY, organisationUnitC, defaultCombo ) );

        dataApprovalStore.addDataApproval( new DataApproval( level4, workflow1234, periodM12, organisationUnitC, defaultCombo, NOT_ACCEPTED, date, userA ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodA, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW4, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodB, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodW5, organisationUnitC, defaultCombo ) );
        assertEquals( "APPROVED_HERE level=4 approve=T unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodY, organisationUnitC, defaultCombo ) );
        assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T", statusAndPermissions( workflow1234, periodC, organisationUnitC, defaultCombo ) );
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
                + " level=" + ( status.getApprovedLevel() == null ? "null" : status.getApprovedLevel().getLevel() )
                + " approve=" + ( permissions.isMayApprove() ? "T" : "F" )
                + " unapprove=" + ( permissions.isMayUnapprove() ? "T" : "F" )
                + " accept=" + ( permissions.isMayAccept() ? "T" : "F" )
                + " unaccept=" + ( permissions.isMayUnaccept() ? "T" : "F" )
                + " read=" + ( permissions.isMayReadData() ? "T" : "F" );
    }
}
