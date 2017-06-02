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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
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
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.dataapproval.DataApprovalAction.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jim Grace
 */
public class DataApprovalAuditServiceTest
    extends DhisTest
{
    private static final String ACCESS_NONE = "--------";
    private static final String ACCESS_READ = "r-------";

    @Autowired
    private DataApprovalAuditService dataApprovalAuditService;

    @Autowired
    private DataApprovalAuditStore dataApprovalAuditStore;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    protected UserGroupAccessService userGroupAccessService;

    @Autowired
    protected UserGroupService userGroupService;

    @Autowired
    protected IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataApprovalLevel level1;
    private DataApprovalLevel level2;
    private DataApprovalLevel level3;

    private DataApprovalWorkflow workflowA;
    private DataApprovalWorkflow workflowB;

    private Period periodA;
    private Period periodB;

    private OrganisationUnit sourceA;
    private OrganisationUnit sourceB;

    private CurrentUserService superUserService;
    private CurrentUserService userAService;
    private CurrentUserService userBService;
    private CurrentUserService userCService;
    private CurrentUserService userDService;

    private User userZ;

    private DataElementCategoryOption optionA;
    private DataElementCategoryOption optionB;

    private DataElementCategory categoryA;

    private DataElementCategoryCombo categoryComboA;

    private DataElementCategoryOptionCombo optionComboA;
    private DataElementCategoryOptionCombo optionComboB;
    private DataElementCategoryOptionCombo optionComboC;

    private CategoryOptionGroup optionGroupA;
    private CategoryOptionGroup optionGroupB;

    private CategoryOptionGroupSet optionGroupSetB;

    private Date dateA;
    private Date dateB;

    private DataApprovalAudit auditAA1;
    private DataApprovalAudit auditAB1;
    private DataApprovalAudit auditAC1;
    private DataApprovalAudit auditBA2;
    private DataApprovalAudit auditBB2;
    private DataApprovalAudit auditBC2;
    private DataApprovalAudit auditBA3;
    private DataApprovalAudit auditBB3;
    private DataApprovalAudit auditBC3;

    // -------------------------------------------------------------------------
    // Set up/tear down helper methods
    // -------------------------------------------------------------------------

    private CurrentUserService getMockCurrentUserService( String userName, boolean superUserFlag, OrganisationUnit orgUnit, String... auths )
    {
        CurrentUserService mockCurrentUserService = new MockCurrentUserService( superUserFlag, Sets.newHashSet( orgUnit ), Sets.newHashSet( orgUnit ), auths );

        User user = mockCurrentUserService.getCurrentUser();

        user.setFirstName( "Test" );
        user.setSurname( userName );

        UserCredentials credentials = user.getUserCredentials();

        credentials.setUsername( userName );

        for ( UserAuthorityGroup role : credentials.getUserAuthorityGroups() )
        {
            role.setName( CodeGenerator.generateUid() ); // Give the role an arbitrary name

            userService.addUserAuthorityGroup( role );
        }

        userService.addUserCredentials( credentials );
        userService.addUser( user );

        return mockCurrentUserService;
    }

    private UserGroup getUserGroup( String userGroupName, Set<User> users )
    {
        UserGroup userGroup = new UserGroup();
        userGroup.setAutoFields();

        userGroup.setName( userGroupName );
        userGroup.setMembers( users );

        userGroupService.addUserGroup( userGroup );

        return userGroup;
    }

    private void setPrivateAccess( BaseIdentifiableObject object, UserGroup... userGroups )
    {
        object.setPublicAccess( ACCESS_NONE );
        object.setUser( userZ ); // Needed for sharing to work

        for ( UserGroup group : userGroups )
        {
            UserGroupAccess userGroupAccess = new UserGroupAccess();

            userGroupAccess.setAccess( ACCESS_READ );

            userGroupAccess.setUserGroup( group );

            userGroupAccessService.addUserGroupAccess( userGroupAccess );

            object.getUserGroupAccesses().add( userGroupAccess );
        }

        identifiableObjectManager.updateNoAcl( object );
    }

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest() throws Exception
    {
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------
        PeriodType periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        periodA = createPeriod( new MonthlyPeriodType(), getDate( 2017, 1, 1 ), getDate( 2017, 1, 31 ) );
        periodB = createPeriod( new MonthlyPeriodType(), getDate( 2018, 1, 1 ), getDate( 2018, 1, 31 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );

        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B', sourceA );
        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );

        superUserService = getMockCurrentUserService( "SuperUser", true, sourceA, UserAuthorityGroup.AUTHORITY_ALL );
        userAService = getMockCurrentUserService( "UserA", false, sourceA );
        userBService = getMockCurrentUserService( "UserB", false, sourceB );
        userCService = getMockCurrentUserService( "UserC", false, sourceB );
        userDService = getMockCurrentUserService( "UserD", false, sourceB );

        userZ = createUser( 'Z' );
        userService.addUser( userZ );

        UserGroup userGroupC = getUserGroup( "UserGroupA", Sets.newHashSet( userCService.getCurrentUser() ) );
        UserGroup userGroupD = getUserGroup( "UserGroupB", Sets.newHashSet( userDService.getCurrentUser() ) );

        optionA = new DataElementCategoryOption( "CategoryOptionA" );
        optionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryService.addDataElementCategoryOption( optionA );
        categoryService.addDataElementCategoryOption( optionB );

        categoryA = createDataElementCategory( 'A', optionA, optionB );
        categoryService.addDataElementCategory( categoryA );

        categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addDataElementCategoryCombo( categoryComboA );

        optionComboA = createCategoryOptionCombo( 'A', categoryComboA, optionA );
        optionComboB = createCategoryOptionCombo( 'B', categoryComboA, optionB );
        optionComboC = createCategoryOptionCombo( 'C', categoryComboA, optionA, optionB );
        categoryService.addDataElementCategoryOptionCombo( optionComboA );
        categoryService.addDataElementCategoryOptionCombo( optionComboB );
        categoryService.addDataElementCategoryOptionCombo( optionComboC );

        optionGroupA = createCategoryOptionGroup( 'A', optionA );
        optionGroupB = createCategoryOptionGroup( 'B', optionB );
        categoryService.saveCategoryOptionGroup( optionGroupA );
        categoryService.saveCategoryOptionGroup( optionGroupB );

        optionGroupSetB = new CategoryOptionGroupSet( "OptionGroupSetB" );
        categoryService.saveCategoryOptionGroupSet( optionGroupSetB );

        optionGroupSetB.addCategoryOptionGroup( optionGroupA );
        optionGroupSetB.addCategoryOptionGroup( optionGroupB );

        optionGroupA.getGroupSets().add( optionGroupSetB );
        optionGroupB.getGroupSets().add( optionGroupSetB );

        setPrivateAccess( optionA, userGroupC );
        setPrivateAccess( optionB );
        setPrivateAccess( optionGroupA );
        setPrivateAccess( optionGroupB, userGroupD );

        categoryService.updateCategoryOptionGroupSet( optionGroupSetB );

        categoryService.updateCategoryOptionGroup( optionGroupA );
        categoryService.updateCategoryOptionGroup( optionGroupB );

        userCService.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( categoryA );
        userDService.getCurrentUser().getUserCredentials().getCogsDimensionConstraints().add( optionGroupSetB );

        dateA = getDate( 2017, 1, 1 );
        dateB = getDate( 2018, 1, 1 );

        level1 = new DataApprovalLevel( "01", 1, null );
        level2 = new DataApprovalLevel( "02", 2, null );
        level3 = new DataApprovalLevel( "03", 2, optionGroupSetB );
        dataApprovalLevelService.addDataApprovalLevel( level1 );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level3 );

        workflowA = new DataApprovalWorkflow( "workflowA", periodType, newHashSet( level1 ) );
        workflowB = new DataApprovalWorkflow( "workflowB", periodType, newHashSet( level1, level2, level3 ) );
        dataApprovalService.addWorkflow( workflowA );
        dataApprovalService.addWorkflow( workflowB );

        DataApproval approvalAA1 = new DataApproval( level1, workflowA, periodA, sourceA, optionComboA, false, dateA, userZ );
        DataApproval approvalAB1 = new DataApproval( level1, workflowA, periodA, sourceA, optionComboB, false, dateA, userZ );
        DataApproval approvalAC1 = new DataApproval( level1, workflowA, periodA, sourceA, optionComboC, false, dateA, userZ );
        DataApproval approvalBA2 = new DataApproval( level2, workflowB, periodB, sourceB, optionComboA, false, dateB, userZ );
        DataApproval approvalBB2 = new DataApproval( level2, workflowB, periodB, sourceB, optionComboB, false, dateB, userZ );
        DataApproval approvalBC2 = new DataApproval( level2, workflowB, periodB, sourceB, optionComboC, false, dateB, userZ );
        DataApproval approvalBA3 = new DataApproval( level3, workflowB, periodB, sourceB, optionComboA, false, dateB, userZ );
        DataApproval approvalBB3 = new DataApproval( level3, workflowB, periodB, sourceB, optionComboB, false, dateB, userZ );
        DataApproval approvalBC3 = new DataApproval( level3, workflowB, periodB, sourceB, optionComboC, false, dateB, userZ );
        auditAA1 = new DataApprovalAudit( approvalAA1, APPROVE );
        auditAB1 = new DataApprovalAudit( approvalAB1, UNAPPROVE );
        auditAC1 = new DataApprovalAudit( approvalAC1, ACCEPT );
        auditBA2 = new DataApprovalAudit( approvalBA2, UNACCEPT );
        auditBB2 = new DataApprovalAudit( approvalBB2, APPROVE );
        auditBC2 = new DataApprovalAudit( approvalBC2, UNAPPROVE );
        auditBA3 = new DataApprovalAudit( approvalBA3, ACCEPT );
        auditBB3 = new DataApprovalAudit( approvalBB3, UNACCEPT );
        auditBC3 = new DataApprovalAudit( approvalBC3, APPROVE );
        dataApprovalAuditStore.save( auditAA1 );
        dataApprovalAuditStore.save( auditAB1 );
        dataApprovalAuditStore.save( auditAC1 );
        dataApprovalAuditStore.save( auditBA2 );
        dataApprovalAuditStore.save( auditBB2 );
        dataApprovalAuditStore.save( auditBC2 );
        dataApprovalAuditStore.save( auditBA3 );
        dataApprovalAuditStore.save( auditBB3 );
        dataApprovalAuditStore.save( auditBC3 );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( dataApprovalAuditService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( dataApprovalAuditStore, "currentUserService", currentUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // Test helper methods
    // -------------------------------------------------------------------------

    private void setMockUserService( CurrentUserService mockUserService )
    {
        setDependency( dataApprovalLevelService, "currentUserService", mockUserService, CurrentUserService.class );
        setDependency( dataApprovalAuditService, "currentUserService", mockUserService, CurrentUserService.class );
        setDependency( dataApprovalAuditStore, "currentUserService", mockUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // DataApprovalAudit
    // -------------------------------------------------------------------------

    @Test
    public void testDeleteDataApprovalAudits() throws Exception
    {
        DataApprovalAuditQueryParams params = new DataApprovalAuditQueryParams();
        List<DataApprovalAudit> audits;

        setMockUserService( userAService );

        dataApprovalAuditService.deleteDataApprovalAudits( sourceB );

        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 3, audits.size() );
        assertTrue( audits.contains( auditAA1 ) );
        assertTrue( audits.contains( auditAB1 ) );
        assertTrue( audits.contains( auditAC1 ) );
    }

    @Test
    public void TestGetDataApprovalAudits() throws Exception
    {
        DataApprovalAuditQueryParams params = new DataApprovalAuditQueryParams();
        List<DataApprovalAudit> audits;

        // Superuser can see all audits.
        setMockUserService( superUserService );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 9, audits.size() );
        assertTrue( audits.contains( auditAA1 ) );
        assertTrue( audits.contains( auditAB1 ) );
        assertTrue( audits.contains( auditAC1 ) );
        assertTrue( audits.contains( auditBA2 ) );
        assertTrue( audits.contains( auditBB2 ) );
        assertTrue( audits.contains( auditBC2 ) );
        assertTrue( audits.contains( auditBA3 ) );
        assertTrue( audits.contains( auditBB3 ) );
        assertTrue( audits.contains( auditBC3 ) );

        // User A can see all options from sourceA or its children.
        setMockUserService( userAService );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 9, audits.size() );
        assertTrue( audits.contains( auditAA1 ) );
        assertTrue( audits.contains( auditAB1 ) );
        assertTrue( audits.contains( auditAC1 ) );
        assertTrue( audits.contains( auditBA2 ) );
        assertTrue( audits.contains( auditBB2 ) );
        assertTrue( audits.contains( auditBC2 ) );
        assertTrue( audits.contains( auditBA3 ) );
        assertTrue( audits.contains( auditBB3 ) );
        assertTrue( audits.contains( auditBC3 ) );

        // User B can see all options from sourceB.
        setMockUserService( userBService );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 6, audits.size() );
        assertTrue( audits.contains( auditBA2 ) );
        assertTrue( audits.contains( auditBB2 ) );
        assertTrue( audits.contains( auditBC2 ) );
        assertTrue( audits.contains( auditBA3 ) );
        assertTrue( audits.contains( auditBB3 ) );
        assertTrue( audits.contains( auditBC3 ) );

        // User C can see only level 3, optionA from sourceB.
        setMockUserService( userCService );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertTrue( audits.contains( auditBA3 ) );

        // User D can see only level 3, optionB from sourceB.
        setMockUserService( userDService );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertTrue( audits.contains( auditBB3 ) );
    }
}
