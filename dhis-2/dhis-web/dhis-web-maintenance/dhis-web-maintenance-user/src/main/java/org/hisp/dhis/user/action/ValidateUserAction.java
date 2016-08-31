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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: ValidateUserAction.java 3816 2007-11-02 23:00:19Z larshelg $
 */
public class ValidateUserAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String username;

    public void setUsername( String username )
    {
        this.username = username;
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

    private String inviteUsername;

    public void setInviteUsername( String inviteUsername )
    {
        this.inviteUsername = inviteUsername;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private String message;

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( username != null )
        {
            UserCredentials match = userService.getUserCredentialsByUsername( username );

            if ( match != null && (id == null || match.getId() != id) )
            {
                message = i18n.getString( "username_in_use" );

                return ERROR;
            }
        }

        if ( openId != null )
        {
            UserCredentials match = userService.getUserCredentialsByOpenId( openId );

            if ( match != null && (id == null || match.getId() != id) )
            {
                message = i18n.getString( "openid_in_use" );

                return ERROR;
            }
        }

        if ( ldapId != null )
        {
            UserCredentials match = userService.getUserCredentialsByLdapId( ldapId );

            if ( match != null && (id == null || match.getId() != id) )
            {
                message = i18n.getString( "ldap_in_use" );

                return ERROR;
            }
        }
        
        if ( inviteUsername != null )
        {
            UserCredentials match = userService.getUserCredentialsByUsername( inviteUsername );

            if ( match != null && (id == null || match.getId() != id) )
            {
                message = i18n.getString( "username_in_use" );

                return ERROR;
            }
        }

        message = i18n.getString( "everything_is_ok" );

        return SUCCESS;
    }
}
