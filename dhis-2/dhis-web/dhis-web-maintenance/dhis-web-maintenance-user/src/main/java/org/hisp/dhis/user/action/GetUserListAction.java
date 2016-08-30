package org.hisp.dhis.user.action;

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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;

/**
 * @author Torgeir Lorange Ostby
 */
public class GetUserListAction
    extends ActionPagingSupport<User>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private List<User> users = new ArrayList<>();

    public List<User> getUsers()
    {
        return users;
    }

    private String key;

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }

    private Integer months;

    public Integer getMonths()
    {
        return months;
    }

    public void setMonths( Integer months )
    {
        this.months = months;
    }

    private boolean selfRegistered;

    public boolean getSelfRegistered()
    {
        return selfRegistered;
    }

    public void setSelfRegistered( boolean selfRegistered )
    {
        this.selfRegistered = selfRegistered;
    }
    
    private String invitationStatus;

    public String getInvitationStatus()
    {
        return invitationStatus;
    }

    public void setInvitationStatus( String invitationStatus )
    {
        this.invitationStatus = invitationStatus;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        UserQueryParams params = new UserQueryParams();
        
        params.setQuery( key );
        params.setInactiveMonths( months );
        params.setSelfRegistered( selfRegistered );
        params.setInvitationStatus( UserInvitationStatus.fromValue( invitationStatus ) );
        
        int count = userService.getUserCount( params );
        
        this.paging = createPaging( count );
        params.setFirst( paging.getStartPos() );
        params.setMax( paging.getPageSize() );
        
        users = userService.getUsers( params );
        
        return SUCCESS;
    }
}
