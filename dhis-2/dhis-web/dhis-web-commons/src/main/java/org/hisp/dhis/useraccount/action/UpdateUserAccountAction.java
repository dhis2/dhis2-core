package org.hisp.dhis.useraccount.action;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;

import com.opensymphony.xwork2.Action;

/**
 * @author Torgeir Lorange Ostby
 */
public class UpdateUserAccountAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private UserService userService;

    private PasswordManager passwordManager;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------
    
    private I18n i18n;

    private Integer id;

    private String oldPassword;

    private String rawPassword;

    private String surname;

    private String firstName;

    private String email;

    private String phoneNumber;

    private String message;

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    public void setPasswordManager( PasswordManager passwordManager )
    {
        this.passwordManager = passwordManager;
    }

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    public void setOldPassword( String oldPassword )
    {
        this.oldPassword = oldPassword;
    }

    public void setRawPassword( String rawPassword )
    {
        this.rawPassword = rawPassword;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public String getMessage()
    {
        return message;
    }

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    public void setSurname( String surname )
    {
        this.surname = surname;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public void setFirstName( String firstName )
    {
        this.firstName = firstName;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Prepare values
        // ---------------------------------------------------------------------

        email = StringUtils.trimToNull( email );
        rawPassword = StringUtils.trimToNull( rawPassword );

        User user = userService.getUser( id );
        UserCredentials credentials = user.getUserCredentials();
        
        String currentPassword = credentials.getPassword();

        // ---------------------------------------------------------------------
        // Deny update if user has local authentication and password is wrong
        // ---------------------------------------------------------------------

        if ( !credentials.isExternalAuth() && !passwordManager.matches( oldPassword, currentPassword ) )
        {
            message = i18n.getString( "wrong_password" );
            return INPUT;
        }

        // ---------------------------------------------------------------------
        // Update userCredentials and user
        // ---------------------------------------------------------------------

        user.setSurname( surname );
        user.setFirstName( firstName );
        user.setEmail( email );
        user.setPhoneNumber( phoneNumber );
        
        userService.encodeAndSetPassword( user, rawPassword );
        
        userService.updateUserCredentials( credentials );
        userService.updateUser( user );

        message = i18n.getString( "update_user_success" );

        return SUCCESS;
    }
}
