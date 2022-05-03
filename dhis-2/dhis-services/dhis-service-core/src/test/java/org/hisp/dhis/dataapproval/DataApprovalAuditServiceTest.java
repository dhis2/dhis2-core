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
package org.hisp.dhis.dataapproval;

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.dataapproval.DataApprovalAction.ACCEPT;
import static org.hisp.dhis.dataapproval.DataApprovalAction.APPROVE;
import static org.hisp.dhis.dataapproval.DataApprovalAction.UNACCEPT;
import static org.hisp.dhis.dataapproval.DataApprovalAction.UNAPPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 */
// FIXME refactor this test to use mocks
class DataApprovalAuditServiceTest extends TransactionalIntegrationTest
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
    private CategoryService categoryService;

    @Autowired
    private UserService _userService;

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

    private User superUser;

    private User userA;

    private User userB;

    private User userC;

    private User userD;

    private User userZ;

    private CategoryOption optionA;

    private CategoryOption optionB;

    private Category categoryA;

    private CategoryCombo categoryComboA;

    private CategoryOptionCombo optionComboA;

    private CategoryOptionCombo optionComboB;

    private CategoryOptionCombo optionComboC;

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
        object.getSharing().setPublicAccess( ACCESS_NONE );
        // Needed for sharing to work
        object.setOwner( userZ.getUid() );
        object.getSharing().setOwner( userZ );
        for ( UserGroup group : userGroups )
        {
            object.getSharing().addUserGroupAccess( new UserGroupAccess( group, ACCESS_READ ) );
        }
        identifiableObjectManager.updateNoAcl( object );
    }

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------
    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void setUpTest()
        throws Exception
    {
        userService = _userService;
        preCreateInjectAdminUser();

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
        superUser = createAndAddUser( true, "SuperUser", newHashSet( sourceA ), newHashSet( sourceA ),
            UserRole.AUTHORITY_ALL );
        userA = createAndAddUser( false, "UserA", sourceA, sourceA );
        userB = createAndAddUser( false, "UserB", sourceB, sourceB );
        userC = createAndAddUser( false, "UserC", sourceB, sourceB );
        userD = createAndAddUser( false, "UserD", sourceB, sourceB );
        userZ = createAndAddUser( "Z" );
        UserGroup userGroupC = getUserGroup( "UserGroupA", Sets.newHashSet( userC ) );
        UserGroup userGroupD = getUserGroup( "UserGroupB", Sets.newHashSet( userD ) );
        userC.getGroups().add( userGroupC );
        userD.getGroups().add( userGroupD );
        optionA = new CategoryOption( "CategoryOptionA" );
        optionB = new CategoryOption( "CategoryOptionB" );
        categoryService.addCategoryOption( optionA );
        categoryService.addCategoryOption( optionB );
        categoryA = createCategory( 'A', optionA, optionB );
        categoryService.addCategory( categoryA );
        categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addCategoryCombo( categoryComboA );
        optionComboA = createCategoryOptionCombo( categoryComboA, optionA );
        optionComboB = createCategoryOptionCombo( categoryComboA, optionB );
        optionComboC = createCategoryOptionCombo( categoryComboA, optionA, optionB );
        categoryService.addCategoryOptionCombo( optionComboA );
        categoryService.addCategoryOptionCombo( optionComboB );
        categoryService.addCategoryOptionCombo( optionComboC );
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
        userC.getCatDimensionConstraints().add( categoryA );
        userD.getCogsDimensionConstraints().add( optionGroupSetB );
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
        DataApproval approvalAA1 = new DataApproval( level1, workflowA, periodA, sourceA, optionComboA, false, dateA,
            userZ );
        DataApproval approvalAB1 = new DataApproval( level1, workflowA, periodA, sourceA, optionComboB, false, dateA,
            userZ );
        DataApproval approvalAC1 = new DataApproval( level1, workflowA, periodA, sourceA, optionComboC, false, dateA,
            userZ );
        DataApproval approvalBA2 = new DataApproval( level2, workflowB, periodB, sourceB, optionComboA, false, dateB,
            userZ );
        DataApproval approvalBB2 = new DataApproval( level2, workflowB, periodB, sourceB, optionComboB, false, dateB,
            userZ );
        DataApproval approvalBC2 = new DataApproval( level2, workflowB, periodB, sourceB, optionComboC, false, dateB,
            userZ );
        DataApproval approvalBA3 = new DataApproval( level3, workflowB, periodB, sourceB, optionComboA, false, dateB,
            userZ );
        DataApproval approvalBB3 = new DataApproval( level3, workflowB, periodB, sourceB, optionComboB, false, dateB,
            userZ );
        DataApproval approvalBC3 = new DataApproval( level3, workflowB, periodB, sourceB, optionComboC, false, dateB,
            userZ );
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

        userService.updateUser( userA );
        userService.updateUser( userB );
        userService.updateUser( userC );
        userService.updateUser( userD );
        userService.updateUser( userZ );
    }

    @Override
    public void tearDownTest()
    {
        setDependency( CurrentUserServiceTarget.class, CurrentUserServiceTarget::setCurrentUserService,
            currentUserService, dataApprovalLevelService, dataApprovalAuditService, dataApprovalAuditStore );
    }

    @Test
    void testDeleteDataApprovalAudits()
    {
        DataApprovalAuditQueryParams params = new DataApprovalAuditQueryParams();
        List<DataApprovalAudit> audits;
        injectSecurityContext( userA );
        dataApprovalAuditService.deleteDataApprovalAudits( sourceB );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 3, audits.size() );
        assertTrue( audits.contains( auditAA1 ) );
        assertTrue( audits.contains( auditAB1 ) );
        assertTrue( audits.contains( auditAC1 ) );
    }

    @Test
    @Disabled( "TODO: 12098 fix this test" )
    void TestGetDataApprovalAudits()
    {
        DataApprovalAuditQueryParams params = new DataApprovalAuditQueryParams();
        List<DataApprovalAudit> audits;
        // Superuser can see all audits.
        injectSecurityContext( superUser );
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
        injectSecurityContext( userA );
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
        injectSecurityContext( userB );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 6, audits.size() );
        assertTrue( audits.contains( auditBA2 ) );
        assertTrue( audits.contains( auditBB2 ) );
        assertTrue( audits.contains( auditBC2 ) );
        assertTrue( audits.contains( auditBA3 ) );
        assertTrue( audits.contains( auditBB3 ) );
        assertTrue( audits.contains( auditBC3 ) );
        // User C can see only level 3, optionA from sourceB.
        injectSecurityContext( userC );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        // TODO: 12098 AssertionFailedError:
        // Expected :1
        // Actual :2
        assertEquals( 1, audits.size() );
        assertTrue( audits.contains( auditBA3 ) );
        // User D can see only level 3, optionB from sourceB.
        injectSecurityContext( userD );
        audits = dataApprovalAuditService.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertTrue( audits.contains( auditBB3 ) );
    }
}
