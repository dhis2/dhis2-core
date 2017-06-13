package org.hisp.dhis.user.action;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.ouwt.manager.OrganisationUnitSelectionManager;
import org.hisp.dhis.security.RestoreOptions;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.system.util.LocaleUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Torgeir Lorange Ostby
 */
public class AddUserAction
    implements Action
{
    private String ACCOUNT_ACTION_INVITE = "invite";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    private OrganisationUnitSelectionManager selectionManager;

    public void setSelectionManager( OrganisationUnitSelectionManager selectionManager )
    {
        this.selectionManager = selectionManager;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private SecurityService securityService;

    public void setSecurityService( SecurityService securityService )
    {
        this.securityService = securityService;
    }

    private AttributeService attributeService;

    public void setAttributeService( AttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private DataElementCategoryService categoryService;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String accountAction;

    public void setAccountAction( String accountAction )
    {
        this.accountAction = accountAction;
    }

    private String username;

    public void setUsername( String username )
    {
        this.username = username;
    }

    private String inviteUsername;

    public void setInviteUsername( String inviteUsername )
    {
        this.inviteUsername = inviteUsername;
    }

    private boolean externalAuth;

    public void setExternalAuth( boolean externalAuth )
    {
        this.externalAuth = externalAuth;
    }

    private String rawPassword;

    public void setRawPassword( String rawPassword )
    {
        this.rawPassword = rawPassword;
    }

    private String surname;

    public void setSurname( String surname )
    {
        this.surname = surname;
    }

    private String firstName;

    public void setFirstName( String firstName )
    {
        this.firstName = firstName;
    }

    private String email;

    public void setEmail( String email )
    {
        this.email = email;
    }

    private String openId;

    public void setOpenId( String openId )
    {
        this.openId = openId;
    }

    private String ldapId;

    public void setLdapId( String ldapId )
    {
        this.ldapId = ldapId;
    }

    private String inviteEmail;

    public void setInviteEmail( String inviteEmail )
    {
        this.inviteEmail = inviteEmail;
    }

    private String phoneNumber;

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    private String passwordUnMatched;

    public String getPasswordUnMatched()
    {
        return this.passwordUnMatched;
    }

    public void setPasswordUnMatched( String passwordUnMatched )
    {
        this.passwordUnMatched = passwordUnMatched;
    }

    private String localeUi;

    public void setLocaleUi( String localeUi )
    {
        this.localeUi = localeUi;
    }

    private String localeDb;

    public void setLocaleDb( String localeDb )
    {
        this.localeDb = localeDb;
    }

    private List<String> urSelected = new ArrayList<>();

    public void setUrSelected( List<String> urSelected )
    {
        this.urSelected = urSelected;
    }

    private List<String> ugSelected = new ArrayList<>();

    public void setUgSelected( List<String> ugSelected )
    {
        this.ugSelected = ugSelected;
    }

    private List<String> dcSelected = new ArrayList<>();

    public void setDcSelected( List<String> dcSelected )
    {
        this.dcSelected = dcSelected;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    private String ouwtSelected;

    public void setOuwtSelected( String ouwtSelected )
    {
        this.ouwtSelected = ouwtSelected;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( !userService.canAddOrUpdateUser( ugSelected ) )
        {
            throw new AccessDeniedException( "You cannot add this user" );
        }
        
        User currentUser = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // User credentials and user
        // ---------------------------------------------------------------------

        UserCredentials userCredentials = new UserCredentials();
        User user = new User();

        userCredentials.setUserInfo( user );
        user.setUserCredentials( userCredentials );

        userCredentials.setUsername( StringUtils.trimToNull( username ) );
        userCredentials.setExternalAuth( externalAuth );
        userCredentials.setOpenId( StringUtils.trimToNull( openId ) );
        userCredentials.setLdapId( StringUtils.trimToNull( ldapId ) );

        if ( ACCOUNT_ACTION_INVITE.equals( accountAction ) )
        {
            userCredentials.setUsername( StringUtils.trimToNull( inviteUsername ) );
            userCredentials.setInvitation( true );
            user.setEmail( StringUtils.trimToNull( inviteEmail ) );

            securityService.prepareUserForInvite( user );
        }
        else
        {
            user.setSurname( StringUtils.trimToNull( surname ) );
            user.setFirstName( StringUtils.trimToNull( firstName ) );
            user.setEmail( StringUtils.trimToNull( email ) );
            user.setPhoneNumber( StringUtils.trimToNull( phoneNumber ) );

            userService.encodeAndSetPassword( userCredentials, StringUtils.trimToNull( rawPassword ) );
        }

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( user, jsonAttributeValues );
        }

        // ---------------------------------------------------------------------
        // Organisation units
        // ---------------------------------------------------------------------

        Set<OrganisationUnit> dataCaptureOrgUnits = new HashSet<>( selectionManager.getSelectedOrganisationUnits() );
        user.updateOrganisationUnits( dataCaptureOrgUnits );

        Set<OrganisationUnit> dataViewOrgUnits = new HashSet<>( selectionTreeManager.getReloadedSelectedOrganisationUnits() );
        user.setDataViewOrganisationUnits( dataViewOrgUnits );

        if ( dataViewOrgUnits.size() == 0 && currentUser.getDataViewOrganisationUnits().size() != 0 )
        {
            user.setDataViewOrganisationUnits( new HashSet<>( currentUser.getDataViewOrganisationUnits() ) );
        }

        // ---------------------------------------------------------------------
        // User roles
        // ---------------------------------------------------------------------

        Set<UserAuthorityGroup> userAuthorityGroups = new HashSet<>();

        for ( String id : urSelected )
        {
            userAuthorityGroups.add( userService.getUserAuthorityGroup( id ) );
        }

        userService.canIssueFilter( userAuthorityGroups );

        userCredentials.setUserAuthorityGroups( userAuthorityGroups );

        // ---------------------------------------------------------------------
        // Dimension constraints. Note that any new user must inherit dimension 
        // constraints if any from the current user.
        // ---------------------------------------------------------------------

        userCredentials.setCogsDimensionConstraints( new HashSet<>( currentUser.getUserCredentials().getCogsDimensionConstraints() ) );
        userCredentials.setCatDimensionConstraints( new HashSet<>( currentUser.getUserCredentials().getCatDimensionConstraints() ) );

        for ( String id : dcSelected )
        {
            CategoryOptionGroupSet cogs = categoryService.getCategoryOptionGroupSet( id );

            if ( cogs != null )
            {
                userCredentials.getCogsDimensionConstraints().add( cogs );
                continue;
            }

            DataElementCategory cat = categoryService.getDataElementCategory( id );

            if ( cat != null )
            {
                userCredentials.getCatDimensionConstraints().add( cat );
                continue;
            }
        }

        // ---------------------------------------------------------------------
        // Add User
        // ---------------------------------------------------------------------

        userService.addUser( user );
        userService.addUserCredentials( userCredentials );

        // ---------------------------------------------------------------------
        // User settings
        // ---------------------------------------------------------------------

        userSettingService.saveUserSetting( UserSettingKey.UI_LOCALE, LocaleUtils.getLocale( localeUi ), user );
        userSettingService.saveUserSetting( UserSettingKey.DB_LOCALE, LocaleUtils.getLocale( localeDb ), user );
        
        if ( ACCOUNT_ACTION_INVITE.equals( accountAction ) )
        {
            RestoreOptions restoreOptions = inviteUsername == null || inviteUsername.isEmpty() ? RestoreOptions.INVITE_WITH_USERNAME_CHOICE : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

            securityService.sendRestoreMessage( userCredentials, getRootPath(), restoreOptions );
        }

        // ---------------------------------------------------------------------
        // User groups
        // ---------------------------------------------------------------------

        for ( String id : ugSelected )
        {
            UserGroup userGroup = userGroupService.getUserGroup( id );
            userGroup.addUser( user );
            userGroupService.updateUserGroup( userGroup );
        }

        if ( ouwtSelected != null && manager.search( OrganisationUnit.class, ouwtSelected ) != null )
        {
            selectionManager.setSelectedOrganisationUnits( Lists.newArrayList( manager.search( OrganisationUnit.class, ouwtSelected ) ) );
        }
        else
        {
            selectionManager.setSelectedOrganisationUnits( currentUser.getOrganisationUnits() );
        }

        return SUCCESS;
    }

    private String getRootPath()
    {
        return ContextUtils.getContextPath( ServletActionContext.getRequest() );
    }
}
