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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.comparator.AttributeSortOrderComparator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.i18n.I18nLocaleService;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.ouwt.manager.OrganisationUnitSelectionManager;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.AttributeUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Nguyen Hong Duc
 * @version $Id: SetupTreeAction.java 5556 2008-08-20 11:36:20Z abyot $
 */
public class SetupTreeAction
    implements Action
{
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

    private AttributeService attributeService;

    public void setAttributeService( AttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    private I18nLocaleService i18nLocaleService;

    public void setI18nLocaleService( I18nLocaleService i18nLocaleService )
    {
        this.i18nLocaleService = i18nLocaleService;
    }

    private LocaleManager localeManager;

    public void setLocaleManager( LocaleManager localeManager )
    {
        this.localeManager = localeManager;
    }

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private UserCredentials userCredentials;

    public UserCredentials getUserCredentials()
    {
        return userCredentials;
    }

    private User user;

    public User getUser()
    {
        return user;
    }

    private List<UserAuthorityGroup> userAuthorityGroups;

    public List<UserAuthorityGroup> getUserAuthorityGroups()
    {
        return userAuthorityGroups;
    }

    private List<UserGroup> userGroups;

    public List<UserGroup> getUserGroups()
    {
        return userGroups;
    }

    private List<DimensionalObject> dimensionConstraints;

    public List<DimensionalObject> getDimensionConstraints()
    {
        return dimensionConstraints;
    }

    private List<Locale> availableLocales;

    public List<Locale> getAvailableLocales()
    {
        return availableLocales;
    }

    private Locale currentLocale;

    public Locale getCurrentLocale()
    {
        return currentLocale;
    }

    private List<Locale> availableLocalesDb;

    public List<Locale> getAvailableLocalesDb()
    {
        return availableLocalesDb;
    }

    private Locale currentLocaleDb;

    public Locale getCurrentLocaleDb()
    {
        return currentLocaleDb;
    }

    private List<Attribute> attributes;

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public Map<Integer, String> attributeValues = new HashMap<>();

    public Map<Integer, String> getAttributeValues()
    {
        return attributeValues;
    }

    private boolean allowInvite;

    public boolean isAllowInvite()
    {
        return allowInvite;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( id != null )
        {
            User currentUser = currentUserService.getCurrentUser();

            user = userService.getUser( id );

            if ( user == null || !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( user.getGroups() ) )
                || !currentUser.getUserCredentials().canModifyUser( user.getUserCredentials() ) )
            {
                throw new AccessDeniedException( "You cannot edit this user" );
            }
            
            if ( user.hasOrganisationUnit() )
            {
                selectionManager.setSelectedOrganisationUnits( user.getOrganisationUnits() );
            }
            else
            {
                selectionManager.clearSelectedOrganisationUnits();
            }

            if ( user.hasDataViewOrganisationUnit() )
            {
                selectionTreeManager.setSelectedOrganisationUnits( user.getDataViewOrganisationUnits() );
            }
            else
            {
                selectionTreeManager.clearSelectedOrganisationUnits();
            }

            userCredentials = user.getUserCredentials();

            userAuthorityGroups = new ArrayList<>( userCredentials.getUserAuthorityGroups() );
            userService.canIssueFilter( userAuthorityGroups );
            Collections.sort( userAuthorityGroups );

            userGroups = new ArrayList<>( user.getGroups() );
            Collections.sort( userGroups );

            dimensionConstraints = new ArrayList<>( userCredentials.getDimensionConstraints() );
            Collections.sort( dimensionConstraints );

            attributeValues = AttributeUtils.getAttributeValueMap( user.getAttributeValues() );

            currentLocale = (Locale) userSettingService.getUserSetting( UserSettingKey.UI_LOCALE, user );

            currentLocaleDb = (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE, user );
        }
        
        currentLocale = ObjectUtils.firstNonNull( currentLocale, LocaleManager.DEFAULT_LOCALE );

        availableLocales = localeManager.getAvailableLocales();

        availableLocalesDb = i18nLocaleService.getAllLocales();

        attributes = new ArrayList<>( attributeService.getAttributes( User.class ) );
        Collections.sort( attributes, AttributeSortOrderComparator.INSTANCE );

        allowInvite = systemSettingManager.emailEnabled();

        return SUCCESS;
    }
}
