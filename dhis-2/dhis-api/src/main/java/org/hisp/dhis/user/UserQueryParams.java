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
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@Accessors( chain = true )
@ToString( onlyExplicitlyIncluded = true )
@NoArgsConstructor
public class UserQueryParams
{
    /**
     * The user query string.
     */
    @ToString.Include
    private String query;

    @ToString.Include
    private String phoneNumber;

    /**
     * The current user in the context of the user query.
     */
    private User user;

    @ToString.Include
    private boolean canManage;

    @ToString.Include
    private boolean authSubset;

    @ToString.Include
    private boolean disjointRoles;

    @ToString.Include
    private Date lastLogin;

    @ToString.Include
    private Date inactiveSince;

    @ToString.Include
    private Date passwordLastUpdated;

    @ToString.Include
    private Integer inactiveMonths;

    @ToString.Include
    private boolean selfRegistered;

    @ToString.Include
    private boolean isNot2FA;

    @ToString.Include
    private UserInvitationStatus invitationStatus;

    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    private Set<UserGroup> userGroups = new HashSet<>();

    @ToString.Include
    private Integer first;

    @ToString.Include
    private Integer max;

    @ToString.Include
    private boolean userOrgUnits;

    @ToString.Include
    private UserOrgUnitType orgUnitBoundary;

    @ToString.Include
    private boolean includeOrgUnitChildren;

    @ToString.Include
    private boolean prefetchUserGroups;

    @ToString.Include
    private Boolean disabled;

    /**
     * Indicates whether users should be able to see users which have the same
     * user roles. This setting is for internal use only, and will override the
     * {@link SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS} system setting.
     * Should not be exposed in the API.
     */
    @ToString.Include
    private boolean canSeeOwnRoles = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public UserQueryParams( User user )
    {
        this.user = user;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public UserQueryParams addOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
        setOrgUnitBoundary( UserOrgUnitType.DATA_CAPTURE );
        return this;
    }

    public UserQueryParams addDataViewOrganisationUnit( OrganisationUnit unit )
    {

        this.organisationUnits.add( unit );
        setOrgUnitBoundary( UserOrgUnitType.DATA_OUTPUT );
        return this;
    }

    public UserQueryParams addTeiSearchOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
        setOrgUnitBoundary( UserOrgUnitType.TEI_SEARCH );
        return this;
    }

    public UserQueryParams setOrganisationUnits( Set<OrganisationUnit> units )
    {
        this.organisationUnits = units;
        if ( orgUnitBoundary == null )
        {
            this.orgUnitBoundary = UserOrgUnitType.DATA_CAPTURE;
        }
        return this;
    }

    public UserQueryParams setDataViewOrganisationUnits( Set<OrganisationUnit> units )
    {
        this.organisationUnits = units;
        this.orgUnitBoundary = UserOrgUnitType.DATA_OUTPUT;
        return this;
    }

    public UserQueryParams setTeiSearchOrganisationUnits( Set<OrganisationUnit> units )
    {
        this.organisationUnits = units;
        this.orgUnitBoundary = UserOrgUnitType.TEI_SEARCH;
        return this;
    }

    public boolean hasUserGroups()
    {
        return !userGroups.isEmpty();
    }

    public boolean hasUser()
    {
        return user != null;
    }

}
