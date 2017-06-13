package org.hisp.dhis.light.settings.action;

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

import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

import com.opensymphony.xwork2.Action;

public class SaveSettingsFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private LocaleManager localeManagerInterface;

    public void setLocaleManagerInterface( LocaleManager localeManagerInterface )
    {
        this.localeManagerInterface = localeManagerInterface;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String currentLocale;

    public void setCurrentLocale( String locale )
    {
        this.currentLocale = locale;
    }

    private String firstName;

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName( String firstName )
    {
        this.firstName = firstName;
    }

    private String surname;

    public String getSurname()
    {
        return surname;
    }

    public void setSurname( String surname )
    {
        this.surname = surname;
    }

    private String phoneNumber;

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    private String email;

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        Validate.notNull( currentLocale );
        Validate.notNull( firstName );
        Validate.notNull( surname );

        // ---------------------------------------------------------------------
        // Update user account settings
        // ---------------------------------------------------------------------

        User user = currentUserService.getCurrentUser();

        user.setFirstName( firstName );
        user.setSurname( surname );
        user.setPhoneNumber( phoneNumber );

        if ( ValidationUtils.emailIsValid( email ) )
        {
            user.setEmail( email );
        }

        userService.updateUser( user );

        // ---------------------------------------------------------------------
        // Update UI locale settings
        // ---------------------------------------------------------------------

        localeManagerInterface.setCurrentLocale( getRespectiveLocale( currentLocale ) );

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Locale getRespectiveLocale( String locale )
    {
        String[] tokens = locale.split( "_" );
        Locale newLocale = null;

        switch ( tokens.length )
        {
            case 1:
                newLocale = new Locale( tokens[0] );
                break;

            case 2:
                newLocale = new Locale( tokens[0], tokens[1] );
                break;

            case 3:
                newLocale = new Locale( tokens[0], tokens[1], tokens[2] );
                break;

            default:
        }

        return newLocale;
    }
}
