package org.hisp.dhis.light.messaging.action;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;

import com.opensymphony.xwork2.Action;

public class FindUserAction
    implements Action
{
    private static final String REDIRECT = "redirect";

    private UserService userService;

    public FindUserAction()
    {
    }

    public UserService getUserService()
    {
        return userService;
    }

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private Collection<User> users;

    public Collection<User> getUsers()
    {
        return users;
    }

    public void setUsers( Collection<User> users )
    {
        this.users = users;
    }

    private String keyword;

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword( String keyword )
    {
        this.keyword = keyword;
    }

    private Integer organisationUnitId;

    public Integer getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    public void setOrganisationUnitId( Integer organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    private Integer userId;

    public Integer getUserId()
    {
        return userId;
    }

    public void setUserId( Integer userId )
    {
        this.userId = userId;
    }

    private String recipientCheckBox;
    public String getRecipientCheckBox()
    {
        return recipientCheckBox;
    }

    public void setRecipientCheckBox( String recipientCheckBox )
    {
        this.recipientCheckBox = recipientCheckBox;
    }

    private Set<User> recipient;
    public Set<User> getRecipient()
    {
        return recipient;
    }

    public void setRecipients( Set<User> recipient )
    {
        this.recipient = recipient;
    }

    private int foundUsers;
    public int getFoundUsers()
    {
        return foundUsers;
    }

    public void setFoundUsers( int foundUsers )
    {
        this.foundUsers = foundUsers;
    }
    
    
    @Override
    public String execute()
        throws Exception
    {
        updateRecipients(recipientCheckBox);

        if ( keyword != null )
        {
            int index = keyword.indexOf( ' ' );

            if ( index != -1 && index == keyword.lastIndexOf( ' ' ) )
            {
                String[] keys = keyword.split( " " );
                keyword = keys[0] + "  " + keys[1];
            }
        }

        UserQueryParams params = new UserQueryParams();
        params.setQuery( keyword );        
        users = userService.getUsers( params );

        if ( users.size() == 1 )
        {
            User user = users.iterator().next();
            userId = user.getId();
            return REDIRECT;
        }

        foundUsers = users.size();
        
        return SUCCESS;
    }
    
    /**
     * 
     * @param recipientCheckBox
     */
    private void updateRecipients(String recipientCheckBox)
    {
        recipient = new HashSet<>();
        
        if ( recipientCheckBox != null )
        {
            String rcbArray[] = recipientCheckBox.split( "," );

            for ( int i = 0; i < rcbArray.length; i++ )
            {
                rcbArray[i] = rcbArray[i].trim();
                User u = userService.getUser( Integer.parseInt( rcbArray[i] ) );
                recipient.add( u );
            }
        }
    } 
}
