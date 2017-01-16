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
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 * @version $Id$
 */
public class UpdateUserProfileAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private UserService userService;

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    private I18nFormat format;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    private String email;

    private String phoneNumber;

    private String introduction;

    private String gender;

    private String birthday;

    private String nationality;

    private String employer;

    private String education;

    private String interests;

    private String languages;

    private String message;

    private String jobTitle;

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public void setIntroduction( String introduction )
    {
        this.introduction = introduction;
    }

    public void setJobTitle( String jobTitle )
    {
        this.jobTitle = jobTitle;
    }

    public void setGender( String gender )
    {
        this.gender = gender;
    }

    public void setBirthday( String birthday )
    {
        this.birthday = birthday;
    }

    public void setNationality( String nationality )
    {
        this.nationality = nationality;
    }

    public void setEmployer( String employer )
    {
        this.employer = employer;
    }

    public void setEducation( String education )
    {
        this.education = education;
    }

    public void setInterests( String interests )
    {
        this.interests = interests;
    }

    public void setLanguages( String languages )
    {
        this.languages = languages;
    }

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
        // ---------------------------------------------------------------------
        // Prepare values
        // ---------------------------------------------------------------------

        email = StringUtils.trimToNull( email );
        phoneNumber = StringUtils.trimToNull( phoneNumber );
        introduction = StringUtils.trimToNull( introduction );
        jobTitle = StringUtils.trimToNull( jobTitle );
        nationality = StringUtils.trimToNull( nationality );
        employer = StringUtils.trimToNull( employer );
        education = StringUtils.trimToNull( education );
        interests = StringUtils.trimToNull( interests );
        languages = StringUtils.trimToNull( languages );

        User user = userService.getUser( id );

        if ( user == null )
        {
            message = i18n.getString( "user_is_not_available" );

            return ERROR;
        }

        // ---------------------------------------------------------------------
        // Update User
        // ---------------------------------------------------------------------

        user.setEmail( email );
        user.setPhoneNumber( phoneNumber );
        user.setIntroduction( introduction );
        user.setJobTitle( jobTitle );
        user.setGender( gender );
        user.setBirthday( format.parseDate( birthday ) );
        user.setNationality( nationality );
        user.setEmployer( employer );
        user.setEducation( education );
        user.setInterests( interests );
        user.setLanguages( languages );

        userService.updateUser( user );

        message = i18n.getString( "update_user_profile_success" );

        return SUCCESS;
    }
}
