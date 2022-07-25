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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

import org.hisp.dhis.organisationunit.OrganisationUnit;

import com.google.common.base.MoreObjects;

/**
 * @author Lars Helge Overland
 */
@Getter
public class UserQueryParams
{
    /**
     * The user query string.
     */
    private String query;

    private String phoneNumber;

    /**
     * The current user in the context of the user query.
     */
    private User user;

    private boolean canManage;

    private boolean authSubset;

    private boolean disjointRoles;

    private Date lastLogin;

    private Date inactiveSince;

    private Date passwordLastUpdated;

    private Integer inactiveMonths;

    private boolean selfRegistered;

    private boolean isNot2FA;

    private UserInvitationStatus invitationStatus;

    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    private Set<OrganisationUnit> dataViewOrganisationUnits = new HashSet<>();

    private Set<OrganisationUnit> teiSearchOrganisationUnits = new HashSet<>();

    private Set<UserGroup> userGroups = new HashSet<>();

    private Integer first;

    private Integer max;

    private boolean userOrgUnits;

    private boolean includeOrgUnitChildren;

    private boolean prefetchUserGroups;

    private Boolean disabled;

    /**
     * Indicates whether users should be able to see users which have the same
     * user roles. This setting is for internal use only, and will override the
     * {@link SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS} system setting.
     * Should not be exposed in the API.
     */
    private boolean canSeeOwnRoles = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public UserQueryParams()
    {
    }

    public UserQueryParams( User user )
    {
        this.user = user;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "query", query )
            .add( "phone number", phoneNumber )
            .add( "user", user != null ? user.getUsername() : null )
            .add( "can manage", canManage )
            .add( "auth subset", authSubset )
            .add( "disjoint roles", disjointRoles )
            .add( "last login", lastLogin )
            .add( "inactive since", inactiveSince )
            .add( "passwordLastUpdated", passwordLastUpdated )
            .add( "inactive months", inactiveMonths )
            .add( "self registered", selfRegistered )
            .add( "isNot2FA", isNot2FA )
            .add( "invitation status", invitationStatus )
            .add( "first", first )
            .add( "max", max )
            .add( "includeOrgUnitChildren", includeOrgUnitChildren )
            .add( "prefetchUserGroups", prefetchUserGroups )
            .add( "disabled", disabled ).toString();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public UserQueryParams addOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
        return this;
    }

    public UserQueryParams addDataViewOrganisationUnit( OrganisationUnit unit )
    {
        this.dataViewOrganisationUnits.add( unit );
        return this;
    }

    public UserQueryParams addTeiSearchOrganisationUnit( OrganisationUnit unit )
    {
        this.teiSearchOrganisationUnits.add( unit );
        return this;
    }

    public boolean hasOrganisationUnits()
    {
        return !organisationUnits.isEmpty();
    }

    public boolean hasDataViewOrganisationUnits()
    {
        return !dataViewOrganisationUnits.isEmpty();
    }

    public boolean hasTeiSearchOrganisationUnits()
    {
        return !teiSearchOrganisationUnits.isEmpty();
    }

    public boolean hasUserGroups()
    {
        return !userGroups.isEmpty();
    }

    public boolean hasUser()
    {
        return user != null;
    }

    // -------------------------------------------------------------------------
    // Set methods
    // -------------------------------------------------------------------------

    public UserQueryParams setQuery( String query )
    {
        this.query = query;
        return this;
    }

    public UserQueryParams setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public UserQueryParams setUser( User user )
    {
        this.user = user;
        return this;
    }

    public UserQueryParams setCanManage( boolean canManage )
    {
        this.canManage = canManage;
        return this;
    }

    public UserQueryParams setAuthSubset( boolean authSubset )
    {
        this.authSubset = authSubset;
        return this;
    }

    public UserQueryParams setDisjointRoles( boolean disjointRoles )
    {
        this.disjointRoles = disjointRoles;
        return this;
    }

    public UserQueryParams setLastLogin( Date lastLogin )
    {
        this.lastLogin = lastLogin;
        return this;
    }

    public UserQueryParams setInactiveSince( Date inactiveSince )
    {
        this.inactiveSince = inactiveSince;
        return this;
    }

    public UserQueryParams setInactiveMonths( Integer inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
        return this;
    }

    public UserQueryParams setSelfRegistered( boolean selfRegistered )
    {
        this.selfRegistered = selfRegistered;
        return this;
    }

    public UserQueryParams setNot2FA( boolean isNot2FA )
    {
        this.isNot2FA = isNot2FA;
        return this;
    }

    public UserQueryParams setInvitationStatus( UserInvitationStatus invitationStatus )
    {
        this.invitationStatus = invitationStatus;
        return this;
    }

    public UserQueryParams setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
        return this;
    }

    public UserQueryParams setDataViewOrganisationUnits( Set<OrganisationUnit> dataViewOrganisationUnits )
    {
        this.dataViewOrganisationUnits = dataViewOrganisationUnits;
        return this;
    }

    public UserQueryParams setTeiSearchOrganisationUnits( Set<OrganisationUnit> teiSearchOrganisationUnits )
    {
        this.teiSearchOrganisationUnits = teiSearchOrganisationUnits;
        return this;
    }

    public UserQueryParams setUserGroups( Set<UserGroup> userGroups )
    {
        this.userGroups = userGroups;
        return this;
    }

    public UserQueryParams setFirst( Integer first )
    {
        this.first = first;
        return this;
    }

    public UserQueryParams setMax( Integer max )
    {
        this.max = max;
        return this;
    }

    public UserQueryParams setUserOrgUnits( boolean userOrgUnits )
    {
        this.userOrgUnits = userOrgUnits;
        return this;
    }

    public UserQueryParams setIncludeOrgUnitChildren( boolean includeOrgUnitChildren )
    {
        this.includeOrgUnitChildren = includeOrgUnitChildren;
        return this;
    }

    public UserQueryParams setPrefetchUserGroups( boolean prefetchUserGroups )
    {
        this.prefetchUserGroups = prefetchUserGroups;
        return this;
    }

    public UserQueryParams setPasswordLastUpdated( Date passwordLastUpdated )
    {
        this.passwordLastUpdated = passwordLastUpdated;
        return this;
    }

    public UserQueryParams setDisabled( Boolean disabled )
    {
        this.disabled = disabled;
        return this;
    }

    public UserQueryParams setCanSeeOwnUserRoles( boolean canSeeOwnUserRoles )
    {
        this.canSeeOwnRoles = canSeeOwnUserRoles;
        return this;
    }
}
