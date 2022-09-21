/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.validation;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.expression.Expression;
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
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 */
public class ValidationResultStoreTest
    extends DhisTest
{
    private static final String ACCESS_NONE = "--------";

<<<<<<< HEAD
=======
    private static final String ACCESS_READ = "r-------";

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    @Autowired
    private ValidationRuleStore validationRuleStore;

    @Autowired
    private ValidationResultStore validationResultStore;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    protected UserGroupAccessService userGroupAccessService;

    @Autowired
    protected UserGroupService userGroupService;

    @Autowired
    protected IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private Expression expressionA;

    private Expression expressionB;

    private ValidationRule validationRuleA;

    private ValidationRule validationRuleB;

    private ValidationResult validationResultAA;

    private ValidationResult validationResultAB;

    private ValidationResult validationResultAC;

    private ValidationResult validationResultBA;

    private ValidationResult validationResultBB;

    private ValidationResult validationResultBC;

    private ValidationResult validationResultCA;

    private Period periodA;

    private Period periodB;

    private OrganisationUnit sourceA;

    private OrganisationUnit sourceB;

    private OrganisationUnit sourceC;

    private CurrentUserService superUserService;

    private CurrentUserService userAService;

    private CurrentUserService userBService;

    private CurrentUserService userCService;

    private CurrentUserService userDService;

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

    // -------------------------------------------------------------------------
    // Set up/tear down helper methods
    // -------------------------------------------------------------------------

    private CurrentUserService getMockCurrentUserService( String userName, boolean superUserFlag,
        OrganisationUnit orgUnit, String... auths )
    {
        CurrentUserService mockCurrentUserService = new MockCurrentUserService( superUserFlag,
            Sets.newHashSet( orgUnit ), Sets.newHashSet( orgUnit ), auths );

        User user = mockCurrentUserService.getCurrentUser();

        user.setFirstName( "Test" );
        user.setSurname( userName );

        UserCredentials credentials = user.getUserCredentials();

        credentials.setUsername( userName );

        for ( UserAuthorityGroup role : credentials.getUserAuthorityGroups() )
        {
            role.setName( CodeGenerator.generateUid() ); // Give the role an
            // arbitrary name

            userService.addUserAuthorityGroup( role );
        }

        userService.addUserCredentials( credentials );
        userService.addUser( user );

        return mockCurrentUserService;
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
    public void setUpTest()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------
        PeriodType periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        periodA = createPeriod( new MonthlyPeriodType(), getDate( 2017, 1, 1 ), getDate( 2017, 1, 31 ) );
        periodB = createPeriod( new MonthlyPeriodType(), getDate( 2017, 2, 1 ), getDate( 2017, 2, 28 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );

        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B', sourceA );
        sourceC = createOrganisationUnit( 'C' );
        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );

        superUserService = getMockCurrentUserService( "SuperUser", true, sourceA, UserAuthorityGroup.AUTHORITY_ALL );
        userAService = getMockCurrentUserService( "UserA", false, sourceA );
        userBService = getMockCurrentUserService( "UserB", false, sourceB );
        userCService = getMockCurrentUserService( "UserC", false, sourceB );
        userDService = getMockCurrentUserService( "UserD", false, sourceB );

        userZ = createUser( 'Z' );
        userService.addUser( userZ );

        UserGroup userGroupC = createUserGroup( 'A', Sets.newHashSet( userCService.getCurrentUser() ) );
        UserGroup userGroupD = createUserGroup( 'B', Sets.newHashSet( userDService.getCurrentUser() ) );
        userGroupService.addUserGroup( userGroupC );
        userGroupService.addUserGroup( userGroupD );

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

        userCService.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( categoryA );
        userDService.getCurrentUser().getUserCredentials().getCogsDimensionConstraints().add( optionGroupSetB );

        expressionA = new Expression( "expressionA", "descriptionA" );
        expressionB = new Expression( "expressionB", "descriptionB" );

        validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        validationRuleB = createValidationRule( 'B', equal_to, expressionB, expressionA, periodType );
        validationRuleStore.save( validationRuleA );
        validationRuleStore.save( validationRuleB );

        validationResultAA = new ValidationResult( validationRuleA, periodA, sourceA, optionComboA, 1.0, 2.0, 3 );
        validationResultAB = new ValidationResult( validationRuleA, periodA, sourceA, optionComboB, 1.0, 2.0, 3 );
        validationResultAC = new ValidationResult( validationRuleA, periodA, sourceA, optionComboC, 1.0, 2.0, 3 );
        validationResultBA = new ValidationResult( validationRuleB, periodB, sourceB, optionComboA, 1.0, 2.0, 3 );
        validationResultBB = new ValidationResult( validationRuleB, periodB, sourceB, optionComboB, 1.0, 2.0, 3 );
        validationResultBC = new ValidationResult( validationRuleB, periodB, sourceB, optionComboC, 1.0, 2.0, 3 );
        validationResultCA = new ValidationResult( validationRuleB, periodB, sourceC, optionComboA, 1.0, 2.0, 3 );

        validationResultAB.setNotificationSent( true );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        setDependency( validationResultStore, "currentUserService", currentUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // Test helper methods
    // -------------------------------------------------------------------------

    private void setMockUserService( CurrentUserService mockUserService )
    {
        setDependency( validationResultStore, "currentUserService", mockUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // Test ValidationResultStore
    // -------------------------------------------------------------------------

    @Test
    public void testSaveValidationResult()
        throws Exception
    {
        Date beforeSave = new Date();
        validationResultStore.save( validationResultAA );
        Date afterSave = new Date();

        long id = validationResultAA.getId();
        ValidationResult validationResult = validationResultStore.get( id );

        assertNotNull( validationResult );
        assertEquals( validationResult.getValidationRule(), validationRuleA );
        assertEquals( validationResult.getPeriod(), periodA );
        assertEquals( validationResult.getOrganisationUnit(), sourceA );
        assertEquals( validationResult.getAttributeOptionCombo(), optionComboA );
        assertEquals( validationResult.getLeftsideValue(), (Double) 1.0 );
        assertEquals( validationResult.getRightsideValue(), (Double) 2.0 );
        assertEquals( validationResult.getDayInPeriod(), 3L );
        assertTrue( validationResult.getCreated().getTime() >= beforeSave.getTime() );
        assertTrue( validationResult.getCreated().getTime() <= afterSave.getTime() );
    }

    @Test
    public void testDeleteValidationResult()
        throws Exception
    {
        validationResultStore.save( validationResultAA );
        long id = validationResultAA.getId();

        validationResultStore.delete( validationResultAA );

        ValidationResult validationResult = validationResultStore.get( id );

        assertNull( validationResult );
    }

    @Test
    public void testGetAllUnreportedValidationResults()
        throws Exception
    {
        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultAB );
        validationResultStore.save( validationResultAC );
        validationResultStore.save( validationResultBA );
        validationResultStore.save( validationResultBB );
        validationResultStore.save( validationResultBC );

        List<ValidationResult> results;

        // Superuser can see all unreported results.
        setMockUserService( superUserService );
        results = validationResultStore.getAllUnreportedValidationResults();
        assertEquals( 5, results.size() );
        assertTrue( results.contains( validationResultAA ) );
        assertTrue( results.contains( validationResultAC ) );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultBB ) );
        assertTrue( results.contains( validationResultBC ) );

        // User A can see all unreported results from sourceA or its children.
        setMockUserService( userAService );
        results = validationResultStore.getAllUnreportedValidationResults();
        assertEquals( 5, results.size() );
        assertTrue( results.contains( validationResultAA ) );
        assertTrue( results.contains( validationResultAC ) );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultBB ) );
        assertTrue( results.contains( validationResultBC ) );

        // User B can see all unreported results from sourceB.
        setMockUserService( userBService );
        results = validationResultStore.getAllUnreportedValidationResults();
        assertEquals( 3, results.size() );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultBB ) );
        assertTrue( results.contains( validationResultBC ) );

        // User C can see only optionA from sourceB.
        setMockUserService( userCService );
        results = validationResultStore.getAllUnreportedValidationResults();
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultBA ) );

        // User D can see only optionB from sourceB.
        setMockUserService( userDService );
        results = validationResultStore.getAllUnreportedValidationResults();
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultBB ) );
    }

    @Test
    public void testGetById()
        throws Exception
    {
        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultAB );
        validationResultStore.save( validationResultAC );
        validationResultStore.save( validationResultBA );
        validationResultStore.save( validationResultBB );
        validationResultStore.save( validationResultBC );

        setMockUserService( superUserService );
        assertEquals( validationResultAA, validationResultStore.getById( validationResultAA.getId() ) );
        assertEquals( validationResultAB, validationResultStore.getById( validationResultAB.getId() ) );
        assertEquals( validationResultAC, validationResultStore.getById( validationResultAC.getId() ) );
        assertEquals( validationResultBA, validationResultStore.getById( validationResultBA.getId() ) );
        assertEquals( validationResultBB, validationResultStore.getById( validationResultBB.getId() ) );
        assertEquals( validationResultBC, validationResultStore.getById( validationResultBC.getId() ) );

        setMockUserService( userAService );
        assertEquals( validationResultAA, validationResultStore.getById( validationResultAA.getId() ) );
        assertEquals( validationResultAB, validationResultStore.getById( validationResultAB.getId() ) );
        assertEquals( validationResultAC, validationResultStore.getById( validationResultAC.getId() ) );
        assertEquals( validationResultBA, validationResultStore.getById( validationResultBA.getId() ) );
        assertEquals( validationResultBB, validationResultStore.getById( validationResultBB.getId() ) );
        assertEquals( validationResultBC, validationResultStore.getById( validationResultBC.getId() ) );

        setMockUserService( userBService );
        assertNull( validationResultStore.getById( validationResultAA.getId() ) );
        assertNull( validationResultStore.getById( validationResultAB.getId() ) );
        assertNull( validationResultStore.getById( validationResultAC.getId() ) );
        assertEquals( validationResultBA, validationResultStore.getById( validationResultBA.getId() ) );
        assertEquals( validationResultBB, validationResultStore.getById( validationResultBB.getId() ) );
        assertEquals( validationResultBC, validationResultStore.getById( validationResultBC.getId() ) );

        setMockUserService( userCService );
        assertNull( validationResultStore.getById( validationResultAA.getId() ) );
        assertNull( validationResultStore.getById( validationResultAB.getId() ) );
        assertNull( validationResultStore.getById( validationResultAC.getId() ) );
        assertEquals( validationResultBA, validationResultStore.getById( validationResultBA.getId() ) );
        assertNull( validationResultStore.getById( validationResultBB.getId() ) );
        assertNull( validationResultStore.getById( validationResultBC.getId() ) );

        setMockUserService( userDService );
        assertNull( validationResultStore.getById( validationResultAA.getId() ) );
        assertNull( validationResultStore.getById( validationResultAB.getId() ) );
        assertNull( validationResultStore.getById( validationResultAC.getId() ) );
        assertNull( validationResultStore.getById( validationResultBA.getId() ) );
        assertEquals( validationResultBB, validationResultStore.getById( validationResultBB.getId() ) );
        assertNull( validationResultStore.getById( validationResultBC.getId() ) );
    }

    @Test
    public void testQuery()
        throws Exception
    {
        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultAB );
        validationResultStore.save( validationResultAC );
        validationResultStore.save( validationResultBA );
        validationResultStore.save( validationResultBB );
        validationResultStore.save( validationResultBC );

        ValidationResultQuery validationResultQuery = new ValidationResultQuery();

        List<ValidationResult> results;

        setMockUserService( superUserService );
        results = validationResultStore.query( validationResultQuery );
        assertEquals( 6, results.size() );
        assertTrue( results.contains( validationResultAA ) );
        assertTrue( results.contains( validationResultAB ) );
        assertTrue( results.contains( validationResultAC ) );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultBB ) );
        assertTrue( results.contains( validationResultBC ) );

        setMockUserService( userAService );
        results = validationResultStore.query( validationResultQuery );
        assertEquals( 6, results.size() );
        assertTrue( results.contains( validationResultAA ) );
        assertTrue( results.contains( validationResultAB ) );
        assertTrue( results.contains( validationResultAC ) );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultBB ) );
        assertTrue( results.contains( validationResultBC ) );

        setMockUserService( userBService );
        results = validationResultStore.query( validationResultQuery );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultBB ) );
        assertTrue( results.contains( validationResultBC ) );

        setMockUserService( userCService );
        results = validationResultStore.query( validationResultQuery );
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultBA ) );

        setMockUserService( userDService );
        results = validationResultStore.query( validationResultQuery );
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultBB ) );
    }

    @Test
    public void testCount()
        throws Exception
    {
        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultAB );
        validationResultStore.save( validationResultAC );
        validationResultStore.save( validationResultBA );
        validationResultStore.save( validationResultBB );
        validationResultStore.save( validationResultBC );

        ValidationResultQuery validationResultQuery = new ValidationResultQuery();

        int count;

        setMockUserService( superUserService );
        count = validationResultStore.count( validationResultQuery );
        assertEquals( 6, count );

        setMockUserService( userAService );
        count = validationResultStore.count( validationResultQuery );
        assertEquals( 6, count );

        setMockUserService( userBService );
        count = validationResultStore.count( validationResultQuery );
        assertEquals( 3, count );

        setMockUserService( userCService );
        count = validationResultStore.count( validationResultQuery );
        assertEquals( 1, count );

        setMockUserService( userDService );
        count = validationResultStore.count( validationResultQuery );
        assertEquals( 1, count );
    }

    @Test
    public void testGetValidationResults()
        throws Exception
    {
        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultBA );
        validationResultStore.save( validationResultCA );

        List<ValidationRule> rulesA = Lists.newArrayList( validationRuleA );
        List<ValidationRule> rulesAB = Lists.newArrayList( validationRuleA, validationRuleB );

        List<Period> periodsB = Lists.newArrayList( periodB );
        List<Period> periodsAB = Lists.newArrayList( periodA, periodB );

        List<ValidationResult> results;

        results = validationResultStore.getValidationResults( null, false, rulesA, periodsAB );
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultAA ) );

        results = validationResultStore.getValidationResults( null, true, rulesAB, periodsB );
        assertEquals( 2, results.size() );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultCA ) );

        results = validationResultStore.getValidationResults( null, true, rulesAB, periodsAB );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( validationResultAA ) );
        assertTrue( results.contains( validationResultBA ) );
        assertTrue( results.contains( validationResultCA ) );

        results = validationResultStore.getValidationResults( sourceA, true, rulesAB, periodsAB );
        assertEquals( 2, results.size() );
        assertTrue( results.contains( validationResultAA ) );
        assertTrue( results.contains( validationResultBA ) );

        results = validationResultStore.getValidationResults( sourceA, false, rulesAB, periodsAB );
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultAA ) );

        results = validationResultStore.getValidationResults( sourceB, false, rulesAB, periodsAB );
        assertEquals( 1, results.size() );
        assertTrue( results.contains( validationResultBA ) );
    }
}
