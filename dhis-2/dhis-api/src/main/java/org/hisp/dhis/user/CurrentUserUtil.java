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
package org.hisp.dhis.user;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.initializeAndUnproxy;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.DimensionalObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public class CurrentUserUtil
{
    private CurrentUserUtil()
    {
        throw new UnsupportedOperationException( "Utility class" );
    }

    public static String getCurrentUsername()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null )
        {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // Principal being a string implies anonymous authentication
        // This is the state before the user is authenticated.
        if ( principal instanceof String )
        {
            if ( !"anonymousUser".equals( principal ) )
            {
                return null;
            }

            return (String) principal;
        }

        if ( principal instanceof UserDetails )
        {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        else if ( principal instanceof Dhis2User )
        {
            Dhis2User dhisOidcUser = (Dhis2User) authentication.getPrincipal();
            return dhisOidcUser.getUsername();
        }
        else
        {
            throw new RuntimeException( "Authentication principal is not supported; principal:" + principal );
        }
    }

    public static User getCurrentUser()
    {
        String username = getCurrentUsername();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null )
        {
            return null;
        }

        if ( username == null )
        {
            throw new IllegalStateException( "No current user" );
        }

        if ( username.equals( "anonymousUser" ) )
        {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if ( principal instanceof UserDetails )
        {
            User user = (User) authentication.getPrincipal();
            // initializeUser( user );

            return user;
        }
        else if ( principal instanceof Dhis2User )
        {
            Dhis2User dhisOidcUser = (Dhis2User) authentication.getPrincipal();
            User user = dhisOidcUser.getDhis2User();
            // initializeUser( user );

            return user;
        }
        else
        {
            throw new RuntimeException( "Authentication principal is not supported; principal:" + principal );
        }
    }

    public static void setUserSetting( UserSettingKey key, Serializable value )
    {
        setUserSettingInternal( key.getName(), value );
    }

    public static void setUserSettingInternal( String key, Serializable value )
    {
        User currentUser = getCurrentUser();
        if ( currentUser != null )
        {
            Map<String, Serializable> userSettings = currentUser.getUserSettings();
            if ( userSettings != null )
            {
                if ( value != null )
                {
                    userSettings.put( key, value );
                }
                else
                {
                    userSettings.remove( key );
                }
            }
        }
    }

    public static <T> T getUserSetting( UserSettingKey key )
    {
        User currentUser = getCurrentUser();
        if ( currentUser == null )
        {
            return null;
        }

        Map<String, Serializable> userSettings = currentUser.getUserSettings();
        if ( userSettings == null )
        {
            return null;
        }

        return (T) userSettings.get( key.getName() );
    }

    public static void initializeUser( User user )
    {
        initializeAndUnproxy( user );
        initializeAndUnproxy( user.getPreviousPasswords() );
        initializeAndUnproxy( user.getApps() );
        initializeAndUnproxy( user.getOrganisationUnits() );
        initializeAndUnproxy( user.getTeiSearchOrganisationUnits() );
        initializeAndUnproxy( user.getDataViewOrganisationUnits() );
        user.getOrganisationUnits().stream().filter( Objects::nonNull )
            .forEach( organisationUnit -> initializeAndUnproxy( organisationUnit.getChildren() ) );
        initializeAndUnproxy( user.getTeiSearchOrganisationUnits() );
        user.getTeiSearchOrganisationUnits().stream().filter( Objects::nonNull )
            .forEach( organisationUnit -> initializeAndUnproxy( organisationUnit.getChildren() ) );
        initializeAndUnproxy( user.getDataViewOrganisationUnits() );
        user.getDataViewOrganisationUnits().stream().filter( Objects::nonNull )
            .forEach( organisationUnit -> initializeAndUnproxy( organisationUnit.getChildren() ) );

        initializeAndUnproxy( user.getUserRoles() );
        for ( UserRole userRole : user.getUserRoles() )
        {
            if ( userRole == null )
            {
                continue;
            }
            initializeAndUnproxy( userRole );
            initializeAndUnproxy( userRole.getAuthorities() );
            initializeAndUnproxy( userRole.getMembers() );
            initializeAndUnproxy( userRole.getAttributeValues() );
        }

        initializeAndUnproxy( user.getGroups() );
        for ( UserGroup group : user.getGroups() )
        {
            if ( group == null )
            {
                continue;
            }
            initializeAndUnproxy( group.getManagedByGroups() );
            initializeAndUnproxy( group.getManagedGroups() );
            initializeAndUnproxy( group.getMembers() );
            initializeAndUnproxy( group.getAttributeValues() );
            initializeAndUnproxy( group.getTranslations() );
        }

        initializeAndUnproxy( user.getCogsDimensionConstraints() );
        for ( CategoryOptionGroupSet groupSet : user.getCogsDimensionConstraints() )
        {
            if ( groupSet == null )
            {
                continue;
            }
            initializeAndUnproxy( groupSet.getMembers() );
            initializeAndUnproxy( groupSet.getItems() );
            initializeAndUnproxy( groupSet.getFilterItemsAsList() );
            initializeAndUnproxy( groupSet.getAttributeValues() );
            initializeAndUnproxy( groupSet.getFavorites() );
            initializeAndUnproxy( groupSet.getTranslations() );

        }

        initializeAndUnproxy( user.getDimensionConstraints() );
        for ( DimensionalObject dimension : user.getDimensionConstraints() )
        {
            if ( dimension == null )
            {
                continue;
            }
            initializeAndUnproxy( dimension.getItems() );
            initializeAndUnproxy( dimension.getAttributeValues() );
            initializeAndUnproxy( dimension.getFavorites() );
            initializeAndUnproxy( dimension.getTranslations() );
        }

        initializeAndUnproxy( user.getCatDimensionConstraints() );
        for ( DimensionalObject dimension : user.getCatDimensionConstraints() )
        {
            if ( dimension == null )
            {
                continue;
            }
            initializeAndUnproxy( dimension.getItems() );
            initializeAndUnproxy( dimension.getAttributeValues() );
            initializeAndUnproxy( dimension.getFavorites() );
            initializeAndUnproxy( dimension.getTranslations() );
        }
    }
}
