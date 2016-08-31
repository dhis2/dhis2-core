package org.hisp.dhis.webapi.webdomain.user;

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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class UserAccount
{
    private String id;
    
    // user account
    private String username;
    
    private String firstName;

    private String surname;

    private String email;

    private String phoneNumber;

    // profile
    private String introduction;

    private String jobTitle;

    private String gender;

    private String birthday;

    private String nationality;

    private String employer;

    private String education;

    private String interests;

    private String languages;

    private Map<String, String> settings = new HashMap<>();
    
    public UserAccount()
    {
    }

    @JsonProperty
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    public String getUsername()
    {
        return username;
    }
    
    public void setUsername( String username )
    {
        this.username = username;
    }
    
    @JsonProperty( required = true )
    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName( String firstName )
    {
        this.firstName = firstName;
    }

    @JsonProperty( required = true )
    public String getSurname()
    {
        return surname;
    }

    public void setSurname( String surname )
    {
        this.surname = surname;
    }

    @JsonProperty
    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    @JsonProperty
    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty
    public String getIntroduction()
    {
        return introduction;
    }

    public void setIntroduction( String introduction )
    {
        this.introduction = introduction;
    }

    @JsonProperty
    public String getJobTitle()
    {
        return jobTitle;
    }

    public void setJobTitle( String jobTitle )
    {
        this.jobTitle = jobTitle;
    }

    @JsonProperty
    public String getGender()
    {
        return gender;
    }

    public void setGender( String gender )
    {
        this.gender = gender;
    }

    @JsonProperty
    public String getBirthday()
    {
        return birthday;
    }

    public void setBirthday( String birthday )
    {
        this.birthday = birthday;
    }

    @JsonProperty
    public String getNationality()
    {
        return nationality;
    }

    public void setNationality( String nationality )
    {
        this.nationality = nationality;
    }

    @JsonProperty
    public String getEmployer()
    {
        return employer;
    }

    public void setEmployer( String employer )
    {
        this.employer = employer;
    }

    @JsonProperty
    public String getEducation()
    {
        return education;
    }

    public void setEducation( String education )
    {
        this.education = education;
    }

    @JsonProperty
    public String getInterests()
    {
        return interests;
    }

    public void setInterests( String interests )
    {
        this.interests = interests;
    }

    @JsonProperty
    public String getLanguages()
    {
        return languages;
    }

    public void setLanguages( String languages )
    {
        this.languages = languages;
    }

    @JsonProperty
    public Map<String, String> getSettings()
    {
        return settings;
    }

    public void setSettings( Map<String, String> settings )
    {
        this.settings = settings;
    }
}
