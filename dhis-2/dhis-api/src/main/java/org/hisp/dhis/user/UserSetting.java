package org.hisp.dhis.user;

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

import java.io.Serializable;

/**
 * TODO make IdentifiableObject
 *
 * @author Nguyen Hong Duc
 */
public class UserSetting
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -5436090314407097851L;

    /**
     * Required. Unique together with name.
     */
    private User user;

    /**
     * Required. Unique together with user.
     */
    private String name;

    private Serializable value;

    public UserSetting()
    {
    }

    public UserSetting( User user, String name, Serializable value )
    {
        this.user = user;
        this.name = name;
        this.value = value;
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null )
        {
            return false;
        }

        if ( !(o instanceof UserSetting) )
        {
            return false;
        }

        final UserSetting other = (UserSetting) o;

        return user.equals( other.getUser() ) && name.equals( other.getName() );
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;

        result = result * prime + user.hashCode();
        result = result * prime + name.hashCode();

        return result;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"user\":\"" + user.getUsername() + "\", " +
            "\"name:\":\"" + name + "\", " +
            "\"value\":\"" + value + "\" " +
            "}";
    }

    public boolean hasValue()
    {
        return value != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public Serializable getValue()
    {
        return value;
    }

    public void setValue( Serializable value )
    {
        this.value = value;
    }
}
