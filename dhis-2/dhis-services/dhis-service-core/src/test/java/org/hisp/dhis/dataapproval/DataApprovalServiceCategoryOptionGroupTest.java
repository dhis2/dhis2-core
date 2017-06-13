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
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalException;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataelement.hibernate.HibernateCategoryOptionGroupStore;
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
    extends DhisTest
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
    private DataElementCategoryService categoryService;

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

    private DataElementCategoryOption brazilA1;
    private DataElementCategoryOption chinaA1_1;
    private DataElementCategoryOption chinaA1_2;
    private DataElementCategoryOption chinaA2;
    private DataElementCategoryOption chinaB2;
    private DataElementCategoryOption indiaA1;

    private DataElementCategory mechanismCategory;

    private DataElementCategoryCombo mechanismCategoryCombo;

    private DataElementCategoryOptionCombo brazilA1Combo;
    private DataElementCategoryOptionCombo chinaA1_1Combo;
    private DataElementCategoryOptionCombo chinaA1_2Combo;
    private DataElementCategoryOptionCombo chinaA2Combo;
    private DataElementCategoryOptionCombo chinaB2Combo;
    private DataElementCategoryOptionCombo indiaA1Combo;

    private CategoryOptionGroup agencyA;
    private CategoryOptionGroup agencyB;
    private CategoryOptionGroup partner1;
    private CategoryOptionGroup partner2;

    private CategoryOptionGroupSet agencies;
    private CategoryOptionGroupSet partners;

    private DataApprovalLevel globalLevel1;
    private DataApprovalLevel countryLevel2;
    private DataApprovalLevel agencyLevel3;
    private DataApprovalLevel partnerLevel4;

    private DataApprovalWorkflow workflowAll;
    private DataApprovalWorkflow workflowAgency;

    private PeriodType periodType;
    
    private Period periodA;

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

        brazilA1 = new DataElementCategoryOption( "BrazilA1" );
        chinaA1_1 = new DataElementCategoryOption( "ChinaA1_1" );
        chinaA1_2 = new DataElementCategoryOption( "ChinaA1_2" );
        chinaA2 = new DataElementCategoryOption( "ChinaA2" );
        chinaB2 = new DataElementCategoryOption( "ChinaB2" );
        indiaA1 = new DataElementCategoryOption( "IndiaA1" );

        brazilA1.setOrganisationUnits( Sets.newHashSet( brazil ) );
        chinaA1_1.setOrganisationUnits( Sets.newHashSet( china ) );
        chinaA1_2.setOrganisationUnits( Sets.newHashSet( china ) );
        chinaA2.setOrganisationUnits( Sets.newHashSet( china ) );
        chinaB2.setOrganisationUnits( Sets.newHashSet( china ) );
        indiaA1.setOrganisationUnits( Sets.newHashSet( india ) );

        categoryService.addDataElementCategoryOption( brazilA1 );
        categoryService.addDataElementCategoryOption( chinaA1_1 );
        categoryService.addDataElementCategoryOption( chinaA1_2 );
        categoryService.addDataElementCategoryOption( chinaA2 );
        categoryService.addDataElementCategoryOption( chinaB2 );
        categoryService.addDataElementCategoryOption( indiaA1 );

        setPrivateAccess( brazilA1, globalUsers, brazilInteragencyUsers, brazilAgencyAUsers, brazilPartner1Users );
        setPrivateAccess( chinaA1_1, globalUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner1Users );
        setPrivateAccess( chinaA1_2, globalUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner1Users );
        setPrivateAccess( chinaA2, globalUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner2Users );
        setPrivateAccess( chinaB2, globalUsers, chinaInteragencyUsers, chinaAgencyBUsers, chinaPartner2Users );
        setPrivateAccess( indiaA1, globalUsers, indiaInteragencyUsers, indiaAgencyAUsers, indiaPartner1Users );

        mechanismCategory = createDataElementCategory( 'A', brazilA1, chinaA1_1, chinaA1_2, chinaA2, chinaB2, indiaA1 );
        categoryService.addDataElementCategory( mechanismCategory );

        mechanismCategoryCombo = createCategoryCombo( 'A', mechanismCategory );
        categoryService.addDataElementCategoryCombo( mechanismCategoryCombo );

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

        brazilA1Combo = createCategoryOptionCombo( 'A', mechanismCategoryCombo, brazilA1 );
        chinaA1_1Combo = createCategoryOptionCombo( 'B', mechanismCategoryCombo, chinaA1_1 );
        chinaA1_2Combo = createCategoryOptionCombo( 'C', mechanismCategoryCombo, chinaA1_2 );
        chinaA2Combo = createCategoryOptionCombo( 'D', mechanismCategoryCombo, chinaA2 );
        chinaB2Combo = createCategoryOptionCombo( 'E', mechanismCategoryCombo, chinaB2 );
        indiaA1Combo = createCategoryOptionCombo( 'F', mechanismCategoryCombo, indiaA1 );

        categoryService.addDataElementCategoryOptionCombo( brazilA1Combo );
        categoryService.addDataElementCategoryOptionCombo( chinaA1_1Combo );
        categoryService.addDataElementCategoryOptionCombo( chinaA1_2Combo );
        categoryService.addDataElementCategoryOptionCombo( chinaA2Combo );
        categoryService.addDataElementCategoryOptionCombo( chinaB2Combo );
        categoryService.addDataElementCategoryOptionCombo( indiaA1Combo );

        agencyA = createCategoryOptionGroup( 'A', brazilA1, chinaA1_1, chinaA1_2, chinaA2, indiaA1 );
        agencyB = createCategoryOptionGroup( 'B', chinaB2 );
        partner1 = createCategoryOptionGroup( '1', brazilA1, chinaA1_1, chinaA1_2, indiaA1 );
        partner2 = createCategoryOptionGroup( '2', chinaA2, chinaB2 );

        categoryService.saveCategoryOptionGroup( agencyA );
        categoryService.saveCategoryOptionGroup( agencyB );
        categoryService.saveCategoryOptionGroup( partner1 );
        categoryService.saveCategoryOptionGroup( partner2 );

        setPrivateAccess( agencyA, globalUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
            brazilAgencyAUsers, chinaAgencyAUsers, indiaAgencyAUsers );
        setPrivateAccess( agencyB, globalUsers, chinaInteragencyUsers, chinaAgencyBUsers );
        setPrivateAccess( partner1, globalUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
            brazilAgencyAUsers, chinaAgencyAUsers, indiaAgencyAUsers,
            brazilPartner1Users, chinaPartner1Users, indiaPartner1Users );
        setPrivateAccess( partner2, globalUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner2Users );

        agencies = new CategoryOptionGroupSet( "Agencies" );
        partners = new CategoryOptionGroupSet( "Partners" );

        categoryService.saveCategoryOptionGroupSet( partners );
        categoryService.saveCategoryOptionGroupSet( agencies );

        setPrivateAccess( agencies, globalUsers, brazilInteragencyUsers, chinaInteragencyUsers, indiaInteragencyUsers,
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
        countryLevel2 = new DataApprovalLevel( "CountryLevel2", 3, null );
        agencyLevel3 = new DataApprovalLevel( "AgencyLevel3", 3, agencies );
        partnerLevel4 = new DataApprovalLevel( "PartnerLevel4", 3, partners );

        dataApprovalLevelService.addDataApprovalLevel( globalLevel1, 1 );
        dataApprovalLevelService.addDataApprovalLevel( countryLevel2, 2 );
        dataApprovalLevelService.addDataApprovalLevel( agencyLevel3, 3 );
        dataApprovalLevelService.addDataApprovalLevel( partnerLevel4, 4 );

        periodType = PeriodType.getPeriodTypeByName( "Monthly" );
        
        periodA = createPeriod( "201801" );
        periodService.addPeriod( periodA );

        workflowAll = new DataApprovalWorkflow( "workflowAll", periodType, newHashSet( globalLevel1, countryLevel2, agencyLevel3, partnerLevel4 ) );
        workflowAgency = new DataApprovalWorkflow( "workflowAgency", periodType, newHashSet( globalLevel1, countryLevel2, agencyLevel3 ) );

        dataApprovalService.addWorkflow( workflowAll );
        dataApprovalService.addWorkflow( workflowAgency );

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
                        + " mechanism=" + ( status.getAttributeOptionComboUid() == null ? "(null)" : categoryService.getDataElementCategoryOptionCombo( status.getAttributeOptionComboUid() ).getName() )
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

    private String getUserLevels( CurrentUserService mockUserService )
    {
        setUser( mockUserService );

        List<DataApprovalLevel> levels = dataApprovalLevelService.getUserDataApprovalLevels();

        String names = "";

        for ( DataApprovalLevel level : levels )
        {
            names += (names.isEmpty() ? "" : ", ") + level.getName();
        }

        return names;
    }

    private boolean approve( CurrentUserService mockUserService, DataApprovalLevel dataApprovalLevel,
        DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        DataElementCategoryOptionCombo mechanismCombo )
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
        DataElementCategoryOptionCombo mechanismCombo )
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
        DataElementCategoryOptionCombo mechanismCombo )
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
        DataElementCategoryOptionCombo mechanismCombo )
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
    @Category( IntegrationTest.class )
    public void testGetUserDataApprovalLevels()
    {
        assertEquals( "GlobalLevel1, CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( superUser ) );
        assertEquals( "GlobalLevel1, CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( globalConsultant ) );
        assertEquals( "GlobalLevel1, CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( globalUser ) );
        assertEquals( "GlobalLevel1, CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( globalApproveOnly ) );
        assertEquals( "GlobalLevel1, CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( globalAcceptOnly ) );
        assertEquals( "GlobalLevel1, CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( globalReadEverything ) );
        assertEquals( "CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( brazilInteragencyUser ) );
        assertEquals( "CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( chinaInteragencyUser ) );
        assertEquals( "CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( chinaInteragencyApproveOnly ) );
        assertEquals( "CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( chinalInteragencyAcceptOnly ) );
        assertEquals( "CountryLevel2, AgencyLevel3, PartnerLevel4", getUserLevels( indiaInteragencyUser ) );
        assertEquals( "AgencyLevel3, PartnerLevel4", getUserLevels( brazilAgencyAUser ) );
        assertEquals( "AgencyLevel3, PartnerLevel4", getUserLevels( chinaAgencyAUser ) );
        assertEquals( "AgencyLevel3, PartnerLevel4", getUserLevels( chinaAgencyAApproveOnly ) );
        assertEquals( "AgencyLevel3, PartnerLevel4", getUserLevels( chinaAgencyAAcceptOnly ) );
        assertEquals( "AgencyLevel3, PartnerLevel4", getUserLevels( chinaAgencyBUser ) );
        assertEquals( "AgencyLevel3, PartnerLevel4", getUserLevels( indiaAgencyAUser ) );
        assertEquals( "PartnerLevel4", getUserLevels( brazilPartner1User ) );
        assertEquals( "PartnerLevel4", getUserLevels( chinaPartner1User ) );
        assertEquals( "PartnerLevel4", getUserLevels( chinaPartner2User ) );
        assertEquals( "PartnerLevel4", getUserLevels( indiaPartner1User ) );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testApprovals()
    {
        // ---------------------------------------------------------------------
        // Nothing approved yet
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 4
        // ---------------------------------------------------------------------

        assertTrue( approve( superUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( globalUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalReadEverything, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( chinaPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_2 at level 4
        // ---------------------------------------------------------------------

        //TODO: test approving at wrong levels

        assertTrue( approve( superUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertTrue( unapprove( superUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        assertTrue( approve( globalConsultant, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertTrue( unapprove( globalConsultant, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( globalUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( globalApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( globalAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( globalReadEverything, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( brazilInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaInteragencyApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( indiaInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( brazilAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyBUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyAApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( indiaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        assertFalse( approve( brazilPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( chinaPartner2User, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );
        assertFalse( approve( indiaPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        assertTrue( approve( chinaPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_2Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 4
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Accept ChinaA1_1 at level 4
        // ---------------------------------------------------------------------

        assertTrue( accept( superUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( superUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalConsultant, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalConsultant, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( globalUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalReadEverything, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinalInteragencyAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaInteragencyUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyBUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner2User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaPartner1User, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( chinaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( chinaAgencyAUser, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAApproveOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( chinaAgencyAAcceptOnly, partnerLevel4, workflowAll, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is accepted at level 4
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 3
        // ---------------------------------------------------------------------
        assertTrue( approve( superUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( globalUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalApproveOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalReadEverything, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyApproveOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner1User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( chinaAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( chinaAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( approve( chinaAgencyAApproveOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 3
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        // (Note: Level 4 user can't see the level 3 approval, etc.)
        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Accept ChinaA1_1 at level 3
        // ---------------------------------------------------------------------

        assertTrue( accept( superUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( superUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalConsultant, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalConsultant, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( globalUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalApproveOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalAcceptOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalReadEverything, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAApproveOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAAcceptOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyBUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaAgencyAUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilPartner1User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner1User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner2User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaPartner1User, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( chinaInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( chinaInteragencyUser, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyApproveOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( chinalInteragencyAcceptOnly, agencyLevel3, workflowAll, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is accepted at level 3
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 2
        // ---------------------------------------------------------------------

        assertTrue( approve( superUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( globalUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalApproveOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( globalReadEverything, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAApproveOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAAcceptOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner1User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( approve( chinaInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unapprove( chinaInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( approve( chinalInteragencyAcceptOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( approve( chinaInteragencyApproveOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 2
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Accept ChinaA1_1 at level 2
        // ---------------------------------------------------------------------

        assertTrue( accept( superUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( superUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalConsultant, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalConsultant, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( globalReadEverything, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaInteragencyApproveOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinalInteragencyAcceptOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaInteragencyUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilAgencyAUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAApproveOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyAAcceptOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaAgencyBUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaAgencyAUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertFalse( accept( brazilPartner1User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner1User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( chinaPartner2User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( indiaPartner1User, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( accept( globalUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( unaccept( globalUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertFalse( accept( globalApproveOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( globalAcceptOnly, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is accepted at level 2
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Approve ChinaA1_1 at level 1
        // ---------------------------------------------------------------------

        assertFalse( approve( superUser, globalLevel1, workflowAll, periodA, china, chinaA1_1Combo ) ); // Wrong org unit.

        assertTrue( approve( superUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( superUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertTrue( approve( globalConsultant, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( globalConsultant, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( globalReadEverything, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( brazilInteragencyUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaInteragencyUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( indiaInteragencyUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( brazilAgencyAUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyAUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaAgencyBUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( indiaAgencyAUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( approve( brazilPartner1User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner1User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( chinaPartner2User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( indiaPartner1User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertTrue( approve( globalUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( globalUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( approve( globalAcceptOnly, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( globalApproveOnly, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        // ---------------------------------------------------------------------
        // ChinaA1_1 is approved at level 1
        // ChinaA1_2 is approved at level 4
        // ---------------------------------------------------------------------

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalConsultant, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( globalAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( globalReadEverything, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaInteragencyApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinalInteragencyAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( brazilAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAApproveOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyAAcceptOnly, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( chinaAgencyBUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F" },
            getUserApprovalsAndPermissions( indiaAgencyAUser, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( brazilPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner1User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( chinaPartner2User, workflowAll, periodA, null ) );

        assertArrayEquals( new String[] {
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( indiaPartner1User, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Unapprove ChinaA1_1 at level 1
        // ---------------------------------------------------------------------

        assertTrue( unapprove( superUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( superUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertTrue( unapprove( globalConsultant, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( globalConsultant, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( globalReadEverything, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( brazilInteragencyUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaInteragencyUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( indiaInteragencyUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( brazilAgencyAUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaAgencyAUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaAgencyBUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( indiaAgencyAUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertFalse( unapprove( brazilPartner1User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaPartner1User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( chinaPartner2User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( indiaPartner1User, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertTrue( unapprove( globalUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( approve( globalUser, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertTrue( unapprove( globalApproveOnly, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );
        assertFalse( unapprove( globalAcceptOnly, globalLevel1, workflowAll, periodA, global, chinaA1_1Combo ) );

        assertArrayEquals( new String[] {
                "ou=Brazil mechanism=BrazilA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaA1_1 level=2 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
                "ou=China mechanism=ChinaA1_2 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
                "ou=China mechanism=ChinaA2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=China mechanism=ChinaB2 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
                "ou=India mechanism=IndiaA1 level=4 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T" },
            getUserApprovalsAndPermissions( superUser, workflowAll, periodA, null ) );

        // ---------------------------------------------------------------------
        // Unaccept ChinaA1_1 at level 2
        // ---------------------------------------------------------------------

        assertTrue( unaccept( superUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( superUser, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );

        assertTrue( unaccept( globalConsultant, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
        assertTrue( accept( globalConsultant, countryLevel2, workflowAll, periodA, china, chinaA1_1Combo ) );
    }
}
