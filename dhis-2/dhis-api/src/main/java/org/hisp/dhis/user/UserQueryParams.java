package org.hisp.dhis.user;

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

import java.util.Date;

import org.hisp.dhis.organisationunit.OrganisationUnit;

import com.google.common.base.MoreObjects;

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
    
    private Integer inactiveMonths;
    
    private boolean selfRegistered;
    
    private UserInvitationStatus invitationStatus;
    
    private OrganisationUnit organisationUnit;
    
    private Integer first;
    
    private Integer max;

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
        return MoreObjects.toStringHelper( this ).
            add( "query", query ).
            add( "phone number", phoneNumber ).
            add( "user", user != null ? user.getUsername() : null ).
            add( "can manage", canManage ).
            add( "auth subset", authSubset ).
            add( "disjoint roles", disjointRoles ).
            add( "last login", lastLogin ).
            add( "inactive since", inactiveSince ).
            add( "inactive months", inactiveMonths ).
            add( "self registered", selfRegistered ).
            add( "invitation status", invitationStatus ).
            add( "organisation unit", organisationUnit != null ? organisationUnit.getUid() : null ).
            add( "first", first ).
            add( "max", max ).toString();
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getQuery()
    {
        return query;
    }

    public void setQuery( String query )
    {
        this.query = query;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public boolean isCanManage()
    {
        return canManage;
    }

    public void setCanManage( boolean canManage )
    {
        this.canManage = canManage;
    }

    public boolean isAuthSubset()
    {
        return authSubset;
    }

    public void setAuthSubset( boolean authSubset )
    {
        this.authSubset = authSubset;
    }

    public boolean isDisjointRoles()
    {
        return disjointRoles;
    }

    public void setDisjointRoles( boolean disjointRoles )
    {
        this.disjointRoles = disjointRoles;
    }

    public Date getLastLogin()
    {
        return lastLogin;
    }

    public void setLastLogin( Date lastLogin )
    {
        this.lastLogin = lastLogin;
    }

    public Date getInactiveSince()
    {
        return inactiveSince;
    }

    public void setInactiveSince( Date inactiveSince )
    {
        this.inactiveSince = inactiveSince;
    }

    public Integer getInactiveMonths()
    {
        return inactiveMonths;
    }

    public void setInactiveMonths( Integer inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
    }

    public boolean isSelfRegistered()
    {
        return selfRegistered;
    }

    public void setSelfRegistered( boolean selfRegistered )
    {
        this.selfRegistered = selfRegistered;
    }

    public UserInvitationStatus getInvitationStatus()
    {
        return invitationStatus;
    }

    public void setInvitationStatus( UserInvitationStatus invitationStatus )
    {
        this.invitationStatus = invitationStatus;
    }

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    public Integer getFirst()
    {
        return first;
    }

    public void setFirst( Integer first )
    {
        this.first = first;
    }

    public Integer getMax()
    {
        return max;
    }

    public void setMax( Integer max )
    {
        this.max = max;
    }    
}
