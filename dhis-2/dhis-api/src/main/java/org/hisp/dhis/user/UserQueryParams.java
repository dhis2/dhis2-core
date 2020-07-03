package org.hisp.dhis.user;

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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class UserQueryParams
{
    private String query;
    
    private String phoneNumber;
    
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
    
    private List<OrganisationUnit> organisationUnits = new ArrayList<>();
    
    private Integer first;
    
    private Integer max;
    
    private boolean userOrgUnits;

    private boolean includeOrgUnitChildren;
    
    private boolean prefetchUserGroups;
    
    private Boolean disabled;

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
    // Builder
    // -------------------------------------------------------------------------

    public UserQueryParams addOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
        return this;
    }
    
    public boolean hasUser()
    {
        return user != null;
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getQuery()
    {
        return query;
    }

    public UserQueryParams setQuery( String query )
    {
        this.query = query;
        return this;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public UserQueryParams setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public User getUser()
    {
        return user;
    }

    public UserQueryParams setUser( User user )
    {
        this.user = user;
        return this;
    }

    public boolean isCanManage()
    {
        return canManage;
    }

    public UserQueryParams setCanManage( boolean canManage )
    {
        this.canManage = canManage;
        return this;
    }

    public boolean isAuthSubset()
    {
        return authSubset;
    }

    public UserQueryParams setAuthSubset( boolean authSubset )
    {
        this.authSubset = authSubset;
        return this;
    }

    public boolean isDisjointRoles()
    {
        return disjointRoles;
    }

    public UserQueryParams setDisjointRoles( boolean disjointRoles )
    {
        this.disjointRoles = disjointRoles;
        return this;
    }

    public Date getLastLogin()
    {
        return lastLogin;
    }

    public UserQueryParams setLastLogin( Date lastLogin )
    {
        this.lastLogin = lastLogin;
        return this;
    }

    public Date getInactiveSince()
    {
        return inactiveSince;
    }

    public UserQueryParams setInactiveSince( Date inactiveSince )
    {
        this.inactiveSince = inactiveSince;
        return this;
    }

    public Integer getInactiveMonths()
    {
        return inactiveMonths;
    }

    public UserQueryParams setInactiveMonths( Integer inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
        return this;
    }

    public boolean isSelfRegistered()
    {
        return selfRegistered;
    }

    public UserQueryParams setSelfRegistered( boolean selfRegistered )
    {
        this.selfRegistered = selfRegistered;
        return this;
    }

    public boolean isNot2FA()
    {
        return isNot2FA;
    }

    public UserQueryParams setNot2FA( boolean isNot2FA )
    {
        this.isNot2FA = isNot2FA;
        return this;
    }
    
    public UserInvitationStatus getInvitationStatus()
    {
        return invitationStatus;
    }

    public UserQueryParams setInvitationStatus( UserInvitationStatus invitationStatus )
    {
        this.invitationStatus = invitationStatus;
        return this;
    }

    public List<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public UserQueryParams setOrganisationUnits( List<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
        return this;
    }

    public Integer getFirst()
    {
        return first;
    }

    public UserQueryParams setFirst( Integer first )
    {
        this.first = first;
        return this;
    }

    public Integer getMax()
    {
        return max;
    }

    public UserQueryParams setMax( Integer max )
    {
        this.max = max;
        return this;
    }

    public boolean isUserOrgUnits()
    {
        return userOrgUnits;
    }

    public UserQueryParams setUserOrgUnits( boolean userOrgUnits )
    {
        this.userOrgUnits = userOrgUnits;
        return this;
    }

    public boolean isIncludeOrgUnitChildren()
    {
        return includeOrgUnitChildren;
    }

    public UserQueryParams setIncludeOrgUnitChildren( boolean includeOrgUnitChildren )
    {
        this.includeOrgUnitChildren = includeOrgUnitChildren;
        return this;
    }

    public boolean isPrefetchUserGroups()
    {
        return prefetchUserGroups;
    }

    public UserQueryParams setPrefetchUserGroups( boolean prefetchUserGroups )
    {
        this.prefetchUserGroups = prefetchUserGroups;
        return this;
    }

    public Date getPasswordLastUpdated()
    {
        return passwordLastUpdated;
    }

    public UserQueryParams setPasswordLastUpdated( Date passwordLastUpdated )
    {
        this.passwordLastUpdated = passwordLastUpdated;
        return this;
    }

    public Boolean getDisabled()
    {
        return disabled;
    }

    public UserQueryParams setDisabled( Boolean disabled )
    {
        this.disabled = disabled;
        return this;
    }
}
