package org.hisp.dhis.dataapproval;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalException;
import org.hisp.dhis.category.hibernate.HibernateCategoryOptionGroupStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;

/**
 * @author Jim Grace
 */
@Category( IntegrationTest.class )
public class DataApprovalServiceCategoryOptionGroupTest
    extends IntegrationTestBase
{
    private static final String ACCESS_NONE = "--------";
    private static final String ACCESS_READ = "r-------";

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private DataApprovalStore dataApprovalStore;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private HibernateCategoryOptionGroupStore hibernateCategoryOptionGroupStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    protected IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    protected UserGroupAccessService userGroupAccessService;

    @Autowired
    protected UserGroupService userGroupService;

    @Autowired
    protected UserService _userService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected DataSetService dataSetService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private OrganisationUnit global;
    private OrganisationUnit americas;
    private OrganisationUnit asia;
    private OrganisationUnit brazil;
    private OrganisationUnit china;
    private OrganisationUnit india;

    private User userA;

    private Date dateA;

    private CurrentUserService superUser;
    private CurrentUserService globalConsultant;
    private CurrentUserService globalUser;
    private CurrentUserService globalApproveOnly;
    private CurrentUserService globalAcceptOnly;
    private CurrentUserService globalReadEverything;
    private CurrentUserService globalAgencyAUser;
    private CurrentUserService globalAgencyBUser;
    private CurrentUserService brazilInteragencyUser;
    private CurrentUserService chinaInteragencyUser;
    private CurrentUserService chinaInteragencyApproveOnly;
    private CurrentUserService chinalInteragencyAcceptOnly;
    private CurrentUserService indiaInteragencyUser;
    private CurrentUserService brazilAgencyAUser;
    private CurrentUserService chinaAgencyAUser;
    private CurrentUserService chinaAgencyAApproveOnly;
    private CurrentUserService chinaAgencyAAcceptOnly;
    private CurrentUserService chinaAgencyBUser;
    private CurrentUserService indiaAgencyAUser;
    private CurrentUserService brazilPartner1User;
    private CurrentUserService chinaPartner1User;
    private CurrentUserService chinaPartner2User;
    private CurrentUserService indiaPartner1User;
    private CurrentUserService currentMockUserService;

    private CategoryOption brazilA1;
    private CategoryOption chinaA1_1;
    private CategoryOption chinaA1_2;
    private CategoryOption chinaA2;
    private CategoryOption chinaB2;
    private CategoryOption indiaA1;

    private org.hisp.dhis.category.Category mechanismCategory;

    private CategoryCombo mechanismCategoryCombo;

    private CategoryOptionCombo brazilA1Combo;
    private CategoryOptionCombo chinaA1_1Combo;
    private CategoryOptionCombo chinaA1_2Combo;
    private CategoryOptionCombo chinaA2Combo;
    private CategoryOptionCombo chinaB2Combo;
    private CategoryOptionCombo indiaA1Combo;

    private CategoryOptionGroup agencyA;
    private CategoryOptionGroup agencyB;
    private CategoryOptionGroup partner1;
    private CategoryOptionGroup partner2;

    private CategoryOptionGroupSet agencies;
    private CategoryOptionGroupSet partners;

    private DataApprovalLevel globalLevel1;
    private DataApprovalLevel globalAgencyLevel2;
    private DataApprovalLevel countryLevel3;
    private DataApprovalLevel agencyLevel4;
    private DataApprovalLevel partnerLevel5;

    private DataApprovalWorkflow workflow1;
    private DataApprovalWorkflow workflow2;

    private PeriodType periodType;

    private Period periodA;

    private DataSet dataSetA;
    private DataSet dataSetB;

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

    private Set<User> userSet( CurrentUserService... mockServices )
    {
        Set<User> users = new HashSet<>();

        for ( CurrentUserService mock : mockServices )
        {
            users.add( mock.getCurrentUser() );
        }

        return users;
    }

    private void setPrivateAccess( BaseIdentifiableObject object, UserGroup... userGroups )
    {
        object.setPublicAccess( ACCESS_NONE );
        object.setUser( userA ); // Needed for sharing to work

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
        userService = _userService;

        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        global = createOrganisationUnit( "Global" );
        americas = createOrganisationUnit( "Americas", global );
        asia = createOrganisationUnit( "Asia", global );
        brazil = createOrganisationUnit( "Brazil", americas );
        china = createOrganisationUnit( "China", asia );
        india = createOrganisationUnit( "India", asia );

        organisationUnitService.addOrganisationUnit( global );
        organisationUnitService.addOrganisationUnit( americas );
        organisationUnitService.addOrganisationUnit( asia );
        organisationUnitService.addOrganisationUnit( brazil );
        organisationUnitService.addOrganisationUnit( china );
        organisationUnitService.addOrganisationUnit( india );

        userA = createUser( 'A' );
        userService.addUser( userA );

        dateA = new Date();

        superUser = getMockCurrentUserService( "SuperUser", true, global, UserAuthorityGroup.AUTHORITY_ALL );
        globalConsultant = getMockCurrentUserService( "GlobalConsultant", false, global, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS, DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        globalUser = getMockCurrentUserService( "GlobalUser", false, global, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        globalApproveOnly = getMockCurrentUserService( "GlobalApproveOnly", false, global, DataApproval.AUTH_APPROVE );
        globalAcceptOnly = getMockCurrentUserService( "GlobalAcceptOnly", false, global, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        globalReadEverything = getMockCurrentUserService( "GlobalReadEverything", false, global, DataApproval.AUTH_VIEW_UNAPPROVED_DATA );
        globalAgencyAUser = getMockCurrentUserService( "GlobalAgencyAUser", false, global, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        globalAgencyBUser = getMockCurrentUserService( "GlobalAgencyBUser", false, global, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        brazilInteragencyUser = getMockCurrentUserService( "BrazilInteragencyUser", false, brazil, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        chinaInteragencyUser = getMockCurrentUserService( "ChinaInteragencyUser", false, china, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        chinaInteragencyApproveOnly = getMockCurrentUserService( "ChinaInteragencyApproveOnly", false, china, DataApproval.AUTH_APPROVE );
        chinalInteragencyAcceptOnly = getMockCurrentUserService( "ChinalInteragencyAcceptOnly", false, china, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        indiaInteragencyUser = getMockCurrentUserService( "IndiaInteragencyUser", false, india, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        brazilAgencyAUser = getMockCurrentUserService( "BrazilAgencyAUser", false, brazil, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        chinaAgencyAUser = getMockCurrentUserService( "ChinaAgencyAUser", false, china, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        chinaAgencyAApproveOnly = getMockCurrentUserService( "ChinaAgencyAApproveOnly", false, china, DataApproval.AUTH_APPROVE );
        chinaAgencyAAcceptOnly = getMockCurrentUserService( "ChinaAgencyAAcceptOnly", false, china, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        chinaAgencyBUser = getMockCurrentUserService( "ChinaAgencyBUser", false, china, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        indiaAgencyAUser = getMockCurrentUserService( "IndiaAgencyAUser", false, india, DataApproval.AUTH_APPROVE, DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        brazilPartner1User = getMockCurrentUserService( "BrazilPartner1User", false, brazil, DataApproval.AUTH_APPROVE );
        chinaPartner1User = getMockCurrentUserService( "ChinaPartner1User", false, china, DataApproval.AUTH_APPROVE );
        chinaPartner2User = getMockCurrentUserService( "ChinaPartner2User", false, china, DataApproval.AUTH_APPROVE );
        indiaPartner1User = getMockCurrentUserService( "IndiaPartner1User", false, india, DataApproval.AUTH_APPROVE );
        currentMockUserService = null;

        UserGroup globalUsers = getUserGroup( "GlobalUsers", userSet( globalUser, globalApproveOnly, globalAcceptOnly, globalConsultant, globalReadEverything ) );
        UserGroup globalAgencyAUsers = getUserGroup( "GlobalAgencyAUsers", userSet( globalAgencyAUser ) );
        UserGroup globalAgencyBUsers = getUserGroup( "GlobalAgencyBUsers", userSet( globalAgencyBUser ) );
        UserGroup brazilInteragencyUsers = getUserGroup( "BrazilInteragencyUsers", userSet( brazilInteragencyUser ) );
        UserGroup chinaInteragencyUsers = getUserGroup( "ChinaInteragencyUsers", userSet( chinaInteragencyUser, chinaInteragencyApproveOnly, chinalInteragencyAcceptOnly ) );
        UserGroup indiaInteragencyUsers = getUserGroup( "IndiaInteragencyUsers", userSet( indiaInteragencyUser ) );
        UserGroup brazilAgencyAUsers = getUserGroup( "BrazilAgencyAUsers", userSet( brazilAgencyAUser ) );
        UserGroup chinaAgencyAUsers = getUserGroup( "ChinaAgencyAUsers", userSet( chinaAgencyAUser, chinaAgencyAApproveOnly, chinaAgencyAAcceptOnly ) );
        UserGroup chinaAgencyBUsers = getUserGroup( "ChinaAgencyBUsers", userSet( chinaAgencyBUser ) );
        UserGroup indiaAgencyAUsers = getUserGroup( "IndiaAgencyAUsers", userSet( indiaAgencyAUser ) );
        UserGroup brazilPartner1Users = getUserGroup( "BrazilPartner1Users", userSet( brazilPartner1User ) );
        UserGroup chinaPartner1Users = getUserGroup( "ChinaPartner1Users", userSet( chinaPartner1User ) );
        UserGroup chinaPartner2Users = getUserGroup( "ChinaPartner2Users", userSet( chinaPartner2User ) );
        UserGroup indiaPartner1Users = getUserGroup( "IndiaPartner1Users", userSet( indiaPartner1User ) );

        brazilA1 = new CategoryOption( "BrazilA1" );
        chinaA1_1 = new CategoryOption( "ChinaA1_1" );
        chinaA1_2 = new CategoryOption( "ChinaA1_2" );
        chinaA2 = new CategoryOption( "ChinaA2" );
        chinaB2 = new CategoryOption( "ChinaB2" );
        indiaA1 = new CategoryOption( "IndiaA1" );

        brazilA1.setOrganisationUnits( Sets.newHashSet( brazil ) );
        chinaA1_1.setOrganisationUnits( Sets.newHashSet( china ) );
        chinaA1_2.setOrganisationUnits( Sets.newHashSet( china ) );
        chinaA2.setOrganisationUnits( Sets.newHashSet( china ) );
        chinaB2.setOrganisationUnits( Sets.newHashSet( china ) );
        indiaA1.setOrganisationUnits( Sets.newHashSet( india ) );

        categoryService.addCategoryOption( brazilA1 );
        categoryService.addCategoryOption( chinaA1_1 );
        categoryService.addCategoryOption( chinaA1_2 );
        categoryService.addCategoryOption( chinaA2 );
        categoryService.addCategoryOption( chinaB2 );
        categoryService.addCategoryOption( indiaA1 );

        setPrivateAccess( brazilA1, globalUsers, globalAgencyAUsers, brazilInteragencyUsers, brazilAgencyAUsers, brazilPartner1Users );
        setPrivateAccess( chinaA1_1, globalUsers, globalAgencyAUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner1Users );
        setPrivateAccess( chinaA1_2, globalUsers, globalAgencyAUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner1Users );
        setPrivateAccess( chinaA2, globalUsers, globalAgencyAUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner2Users );
        setPrivateAccess( chinaB2, globalUsers, globalAgencyBUsers, chinaInteragencyUsers, chinaAgencyBUsers, chinaPartner2Users );
        setPrivateAccess( indiaA1, globalUsers, globalAgencyAUsers, indiaInteragencyUsers, indiaAgencyAUsers, indiaPartner1Users );

        mechanismCategory = createCategory( 'A', brazilA1, chinaA1_1, chinaA1_2, chinaA2, chinaB2, indiaA1 );
        categoryService.addCategory( mechanismCategory );

        mechanismCategoryCombo = createCategoryCombo( 'A', mechanismCategory );
        categoryService.addCategoryCombo( mechanismCategoryCombo );

        globalAgencyAUser.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        globalAgencyBUser.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        brazilAgencyAUser.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        chinaAgencyAUser.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        chinaAgencyAApproveOnly.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        chinaAgencyAAcceptOnly.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        chinaAgencyBUser.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        indiaAgencyAUser.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        brazilPartner1User.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        chinaPartner1User.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        chinaPartner2User.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );
        indiaPartner1User.getCurrentUser().getUserCredentials().getCatDimensionConstraints().add( mechanismCategory );

        userService.updateUser( globalAgencyAUser.getCurrentUser() );
        userService.updateUser( globalAgencyBUser.getCurrentUser() );
        userService.updateUser( brazilAgencyAUser.getCurrentUser() );
        userService.updateUser( chinaAgencyAUser.getCurrentUser() );
        userService.updateUser( chinaAgencyAApproveOnly.getCurrentUser() );
        userService.updateUser( chinaAgencyAAcceptOnly.getCurrentUser() );
        userService.updateUser( chinaAgencyBUser.getCurrentUser() );
        userService.updateUser( indiaAgencyAUser.getCurrentUser() );
        userService.updateUser( brazilPartner1User.getCurrentUser() );
        userService.updateUser( chinaPartner1User.getCurrentUser() );
        userService.updateUser( chinaPartner2User.getCurrentUser() );
        userService.updateUser( indiaPartner1User.getCurrentUser() );

        brazilA1Combo = createCategoryOptionCombo( mechanismCategoryCombo, brazilA1 );
        chinaA1_1Combo = createCategoryOptionCombo( mechanismCategoryCombo, chinaA1_1 );
        chinaA1_2Combo = createCategoryOptionCombo( mechanismCategoryCombo, chinaA1_2 );
        chinaA2Combo = createCategoryOptionCombo( mechanismCategoryCombo, chinaA2 );
        chinaB2Combo = createCategoryOptionCombo( mechanismCategoryCombo, chinaB2 );
        indiaA1Combo = createCategoryOptionCombo( mechanismCategoryCombo, indiaA1 );

        categoryService.addCategoryOptionCombo( brazilA1Combo );
        categoryService.addCategoryOptionCombo( chinaA1_1Combo );
        categoryService.addCategoryOptionCombo( chinaA1_2Combo );
        categoryService.addCategoryOptionCombo( chinaA2Combo );
        categoryService.addCategoryOptionCombo( chinaB2Combo );
        categoryService.addCategoryOptionCombo( indiaA1Combo );

        mechanismCategoryCombo.getOptionCombos().add( brazilA1Combo );
        mechanismCategoryCombo.getOptionCombos().add( chinaA1_1Combo );
        mechanismCategoryCombo.getOptionCombos().add( chinaA1_2Combo );
        mechanismCategoryCombo.getOptionCombos().add( chinaA2Combo );
        mechanismCategoryCombo.getOptionCombos().add( chinaB2Combo );
        mechanismCategoryCombo.getOptionCombos().add( indiaA1Combo );

        categoryService.updateCategoryCombo( mechanismCategoryCombo );

        agencyA = createCategoryOptionGroup( 'A', brazilA1, chinaA1_1, chinaA1_2, chinaA2, indiaA1 );
        agencyB = createCategoryOptionGroup( 'B', chinaB2 );
        partner1 = createCategoryOptionGroup( '1', brazilA1, chinaA1_1, chinaA1_2, indiaA1 );
        partner2 = createCategoryOptionGroup( '2', chinaA2, chinaB2 );

        categoryService.saveCategoryOptionGroup( agencyA );
        categoryService.saveCategoryOptionGroup( agencyB );
        categoryService.saveCategoryOptionGroup( partner1 );
        categoryService.saveCategoryOptionGroup( partner2 );

        setPrivateAccess( agencyA, globalUsers, globalAgencyAUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
            brazilAgencyAUsers, chinaAgencyAUsers, indiaAgencyAUsers );
        setPrivateAccess( agencyB, globalUsers, globalAgencyBUsers, chinaInteragencyUsers, chinaAgencyBUsers );
        setPrivateAccess( partner1, globalUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
            brazilAgencyAUsers, chinaAgencyAUsers, indiaAgencyAUsers,
            brazilPartner1Users, chinaPartner1Users, indiaPartner1Users );
        setPrivateAccess( partner2, globalUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner2Users );

        agencies = new CategoryOptionGroupSet( "Agencies" );
        partners = new CategoryOptionGroupSet( "Partners" );

        categoryService.saveCategoryOptionGroupSet( partners );
        categoryService.saveCategoryOptionGroupSet( agencies );

        setPrivateAccess( agencies, globalUsers, globalAgencyAUsers, globalAgencyBUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
            brazilAgencyAUsers, chinaAgencyAUsers, chinaAgencyBUsers, chinaAgencyBUsers, indiaAgencyAUsers );

        setPrivateAccess( partners, globalUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
            brazilAgencyAUsers, chinaAgencyAUsers, chinaAgencyBUsers, chinaAgencyBUsers, indiaAgencyAUsers,
            brazilPartner1Users, chinaPartner1Users, chinaPartner2Users, indiaPartner1Users );

        agencies.addCategoryOptionGroup( agencyA );
        agencies.addCategoryOptionGroup( agencyB );
        partners.addCategoryOptionGroup( partner1 );
        partners.addCategoryOptionGroup( partner2 );

        agencyA.getGroupSets().add( agencies );
        agencyB.getGroupSets().add( agencies );
        partner1.getGroupSets().add( partners );
        partner2.getGroupSets().add( partners );

        categoryService.updateCategoryOptionGroupSet( partners );
        categoryService.updateCategoryOptionGroupSet( agencies );

        categoryService.updateCategoryOptionGroup( agencyA );
        categoryService.updateCategoryOptionGroup( agencyB );
        categoryService.updateCategoryOptionGroup( partner1 );
        categoryService.updateCategoryOptionGroup( partner2 );

        globalLevel1 = new DataApprovalLevel( "GlobalLevel1", 1, null );
        globalAgencyLevel2 = new DataApprovalLevel( "GlobalAgencyLevel2", 1, agencies );
        countryLevel3 = new DataApprovalLevel( "CountryLevel3", 3, null );
        agencyLevel4 = new DataApprovalLevel( "AgencyLevel4", 3, agencies );
        partnerLevel5 = new DataApprovalLevel( "PartnerLevel5", 3, partners );

        dataApprovalLevelService.addDataApprovalLevel( globalLevel1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( globalAgencyLevel2, 2 );
        dataApprovalLevelService.addDataApprovalLevel( countryLevel3, 3 );
        dataApprovalLevelService.addDataApprovalLevel( agencyLevel4, 4 );
        dataApprovalLevelService.addDataApprovalLevel( partnerLevel5, 5 );

        periodType = periodService.reloadPeriodType( PeriodType.getPeriodTypeByName( "Monthly" ) );
        periodA = createPeriod( "201801" );
        periodService.addPeriod( periodA );

        workflow1 = new DataApprovalWorkflow( "workflow1", periodType, newHashSet( globalLevel1, countryLevel3, agencyLevel4, partnerLevel5 ) );
        workflow2 = new DataApprovalWorkflow( "workflow2", periodType, newHashSet( globalLevel1, globalAgencyLevel2, agencyLevel4, partnerLevel5 ) );

        dataApprovalService.addWorkflow( workflow1 );
        dataApprovalService.addWorkflow( workflow2 );

        dataSetA = createDataSet( 'A', periodType, mechanismCategoryCombo );
        dataSetB = createDataSet( 'B', periodType, mechanismCategoryCombo );

        dataSetA.assignWorkflow( workflow1 );
        dataSetB.assignWorkflow( workflow2 );

        dataSetA.addOrganisationUnit( global );
        dataSetA.addOrganisationUnit( americas );
        dataSetA.addOrganisationUnit( asia );
        dataSetA.addOrganisationUnit( brazil );
        dataSetA.addOrganisationUnit( china );
        dataSetA.addOrganisationUnit( india );

        dataSetB.addOrganisationUnit( global );
        dataSetB.addOrganisationUnit( americas );
        dataSetB.addOrganisationUnit( asia );
        dataSetB.addOrganisationUnit( brazil );
        dataSetB.addOrganisationUnit( china );
        dataSetB.addOrganisationUnit( india );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        systemSettingManager.saveSystemSetting( SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD, 0 );
        systemSettingManager.saveSystemSetting( SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL, true );
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
        setDependency( dataApprovalStore, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( dataApprovalLevelService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( organisationUnitService, "currentUserService", currentUserService, CurrentUserService.class );
        setDependency( hibernateCategoryOptionGroupStore, "currentUserService", currentUserService, CurrentUserService.class );

        systemSettingManager.saveSystemSetting( SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD, -1 );
        systemSettingManager.saveSystemSetting( SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL, false );

        DataApprovalPermissionsEvaluator.invalidateCache();
    }

    // -------------------------------------------------------------------------
    // Test helper methods
    // -------------------------------------------------------------------------

    private void setUser( CurrentUserService mockUserService )
    {
        if ( mockUserService != currentMockUserService )
        {
            setDependency( dataApprovalService, "currentUserService", mockUserService, CurrentUserService.class );
            setDependency( dataApprovalStore, "currentUserService", mockUserService, CurrentUserService.class );
            setDependency( dataApprovalLevelService, "currentUserService", mockUserService, CurrentUserService.class );
            setDependency( organisationUnitService, "currentUserService", mockUserService, CurrentUserService.class );
            setDependency( hibernateCategoryOptionGroupStore, "currentUserService", mockUserService, CurrentUserService.class );

            currentMockUserService = mockUserService;
        }
    }

    private String getStatusString( DataApprovalStatus status )
    {
        DataApprovalLevel dal = status.getActionLevel();
        String approval = dal == null ? "approval=null" :
                "ou=" + ( status.getOrganisationUnitUid() == null ? "(null)" : organisationUnitService.getOrganisationUnit( status.getOrganisationUnitUid() ).getName() )
                        + " mechanism=" + ( status.getAttributeOptionComboUid() == null ? "(null)" : categoryService.getCategoryOptionCombo( status.getAttributeOptionComboUid() ).getName() )
                        + " level=" + ( dal == null ? "(null)" : dal.getLevel() );

        DataApprovalPermissions p = status.getPermissions();

        return approval + " "
            + ( status.getState() == null ? "state=null" : status.getState().toString() )
            + ( p == null ? " permissions=null" :
                " approve=" + ( p.isMayApprove() ? "T" : "F" )
                + " unapprove=" + ( p.isMayUnapprove() ? "T" : "F" )
                + " accept=" + ( p.isMayAccept() ? "T" : "F" )
                + " unaccept=" + ( p.isMayUnaccept() ? "T" : "F" )
                + " read=" + ( p.isMayReadData() ? "T" : "F" ) );
    }

    private String[] getUserApprovalsAndPermissions( CurrentUserService mockUserService, DataApprovalWorkflow workflow, Period period, OrganisationUnit orgUnit )
    {
        setUser( mockUserService );

        List<DataApprovalStatus> approvals = dataApprovalService.getUserDataApprovalsAndPermissions( workflow, period, orgUnit, mechanismCategoryCombo );

        List<String> approvalStrings = new ArrayList<>();

        for ( DataApprovalStatus status : approvals )
        {
            approvalStrings.add( getStatusString ( status ) );
        }

        Collections.sort( approvalStrings );

        return Arrays.copyOf( approvalStrings.toArray(), approvalStrings.size(), String[].class );
    }

    private String getUserLevels( CurrentUserService mockUserService, DataApprovalWorkflow workflow )
    {
        setUser( mockUserService );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels( mockUserService.getCurrentUser(), workflow );

        String names = "";

        for ( DataApprovalLevel level : levels )
        {
            names += (names.isEmpty() ? "" : ", ") + level.getName();
        }

        return names;
    }

    private boolean approve( CurrentUserService mockUserService, DataApprovalLevel dataApprovalLevel,
        DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo mechanismCombo )
    {
        DataApproval da = new DataApproval( dataApprovalLevel, workflow, period,
                organisationUnit, mechanismCombo, false, dateA, userA );

        setUser( mockUserService );

        try
        {
            dataApprovalService.approveData( Arrays.asList( da ) );

            return true;
        }
        catch ( DataApprovalException ex )
        {
            return false;
        }
        catch ( Throwable ex )
        {
            throw ex;
        }
    }

    private boolean unapprove( CurrentUserService mockUserService, DataApprovalLevel dataApprovalLevel,
        DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo mechanismCombo )
    {
        DataApproval da = new DataApproval( dataApprovalLevel, workflow, period,
                organisationUnit, mechanismCombo, false, dateA, userA );

        setUser( mockUserService );

        try
        {
            dataApprovalService.unapproveData( Arrays.asList( da ) );

            return true;
        }
        catch ( DataApprovalException ex )
        {
            return false;
        }
        catch ( Throwable ex )
        {
            throw ex;
        }
    }

    private boolean accept( CurrentUserService mockUserService, DataApprovalLevel dataApprovalLevel,
        DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo mechanismCombo )
    {
        DataApproval da = new DataApproval( dataApprovalLevel, workflow, period,
                organisationUnit, mechanismCombo, false, dateA, userA );

        setUser( mockUserService );

        try
        {
            dataApprovalService.acceptData( Arrays.asList( da ) );

            return true;
        }
        catch ( DataApprovalException ex )
        {
            return false;
        }
        catch ( Throwable ex )
        {
            throw ex;
        }
    }

    private boolean unaccept( CurrentUserService mockUserService, DataApprovalLevel dataApprovalLevel,
        DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo mechanismCombo )
    {
        DataApproval da = new DataApproval( dataApprovalLevel, workflow, period,
                organisationUnit, mechanismCombo, false, dateA, userA );

        setUser( mockUserService );

        try
        {
            dataApprovalService.unacceptData( Arrays.asList( da ) );

            return true;
        }
        catch ( DataApprovalException ex )
        {
            return false;
        }
        catch ( Throwable ex )
        {
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetUserDataApprovalLevels()
    {
        assertEquals( "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( superUser, workflow1 ) );
        assertEquals( "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( globalConsultant, workflow1 ) );
        assertEquals( "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( globalUser, workflow1 ) );
        assertEquals( "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( globalApproveOnly, workflow1 ) );
        assertEquals( "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( globalAcceptOnly, workflow1 ) );
        assertEquals( "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( globalReadEverything, workflow1 ) );
        assertEquals( "CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( brazilInteragencyUser, workflow1 ) );
        assertEquals( "CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( chinaInteragencyUser, workflow1 ) );
        assertEquals( "CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( chinaInteragencyApproveOnly, workflow1 ) );
        assertEquals( "CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( chinalInteragencyAcceptOnly, workflow1 ) );
        assertEquals( "CountryLevel3, AgencyLevel4, PartnerLevel5", getUserLevels( indiaInteragencyUser, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( globalAgencyAUser, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( globalAgencyBUser, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( brazilAgencyAUser, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyAUser, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyAApproveOnly, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyAAcceptOnly, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyBUser, workflow1 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( indiaAgencyAUser, workflow1 ) );
        assertEquals( "PartnerLevel5", getUserLevels( brazilPartner1User, workflow1 ) );
        assertEquals( "PartnerLevel5", getUserLevels( chinaPartner1User, workflow1 ) );
        assertEquals( "PartnerLevel5", getUserLevels( chinaPartner2User, workflow1 ) );
        assertEquals( "PartnerLevel5", getUserLevels( indiaPartner1User, workflow1 ) );

        assertEquals( "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( superUser, workflow2 ) );
        assertEquals( "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalConsultant, workflow2 ) );
        assertEquals( "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalUser, workflow2 ) );
        assertEquals( "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalApproveOnly, workflow2 ) );
        assertEquals( "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalAcceptOnly, workflow2 ) );
        assertEquals( "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalReadEverything, workflow2 ) );
        assertEquals( "GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalAgencyAUser, workflow2 ) );
        assertEquals( "GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", getUserLevels( globalAgencyBUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( brazilInteragencyUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaInteragencyUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaInteragencyApproveOnly, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinalInteragencyAcceptOnly, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( indiaInteragencyUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( brazilAgencyAUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyAUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyAApproveOnly, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyAAcceptOnly, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( chinaAgencyBUser, workflow2 ) );
        assertEquals( "AgencyLevel4, PartnerLevel5", getUserLevels( indiaAgencyAUser, workflow2 ) );
        assertEquals( "PartnerLevel5", getUserLevels( brazilPartner1User, workflow2 ) );
        assertEquals( "PartnerLevel5", getUserLevels( chinaPartner1User, workflow2 ) );
        assertEquals( "PartnerLevel5", getUserLevels( chinaPartner2User, workflow2 ) );
        assertEquals( "PartnerLevel5", getUserLevels( indiaPartner1User, workflow2 ) );
    }

    @Test
    public void testApprovals()
    {
        // ---------------------------------------------------------------------
        // Nothing approved yet
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 5
        // ---------------------------------------------------------------------

        assertTrue( approve( superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( globalUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalReadEverything, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( chinaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_2 at level 5
        // ---------------------------------------------------------------------

        //TODO: test approving at wrong levels

        assertTrue( approve( superUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertTrue( unapprove( superUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        assertTrue( approve( globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertTrue( unapprove( globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( globalUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( globalApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( globalAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( globalReadEverything, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( brazilInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaInteragencyApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( indiaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( brazilAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyBUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyAApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( indiaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( brazilPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaPartner2User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( indiaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        assertTrue( approve( chinaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 5
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Accept ChinaA1_1 at level 5
        // ---------------------------------------------------------------------

        assertTrue( accept( superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( globalUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalReadEverything, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinalInteragencyAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyBUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner2User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( chinaAgencyAAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is accepted at level 5
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 4
        // ---------------------------------------------------------------------
        assertTrue( approve( superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( globalUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalReadEverything, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( chinaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( chinaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( approve( chinaAgencyAApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 4
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        // (Note: Level 4 user can't see the level 3 approval, etc.)
        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Accept ChinaA1_1 at level 4
        // ---------------------------------------------------------------------

        assertTrue( accept( superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( globalUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalReadEverything, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyBUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner2User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( chinaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( chinaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( chinalInteragencyAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is accepted at level 4
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 3
        // ---------------------------------------------------------------------

        assertTrue( approve( superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( globalUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalReadEverything, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( chinaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( chinaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( approve( chinaInteragencyApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 3
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Accept ChinaA1_1 at level 3
        // ---------------------------------------------------------------------

        assertTrue( accept( superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( globalReadEverything, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinalInteragencyAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyBUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner2User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( globalAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is accepted at level 3
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 1
        // ---------------------------------------------------------------------

        assertFalse( approve( superUser, globalLevel1, workflow1, periodA, china, chinaA1_1Combo ) ); // Wrong org unit.

        assertTrue( approve( superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( globalReadEverything, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertTrue( approve( globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( globalApproveOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 1
        // ChinaA1_2 is approved at level 5
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflow1, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Unapprove ChinaA1_1 at level 1
        // ---------------------------------------------------------------------

        assertTrue( unapprove( superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertTrue( unapprove( globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( globalReadEverything, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( brazilInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( indiaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( brazilAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaAgencyBUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( indiaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( brazilPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaPartner2User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( indiaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertTrue( unapprove( globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( globalApproveOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( globalAcceptOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflow1, periodA, null ) );

        // ---------------------------------------------------------------------
        // Unaccept ChinaA1_1 at level 3
        // ---------------------------------------------------------------------

        assertTrue( unaccept( superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );

        assertTrue( unaccept( globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo ) );
    }
}
