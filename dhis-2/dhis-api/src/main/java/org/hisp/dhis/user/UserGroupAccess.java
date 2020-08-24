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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import lombok.Builder;
import org.hisp.dhis.common.DxfNamespaces;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "userGroupAccess", namespace = DxfNamespaces.DXF_2_0 )
@Builder
public class UserGroupAccess
    implements Serializable
{
    private String id;

    private String access;

    private transient UserGroup userGroup;

    public UserGroupAccess()
    {
    }

    public UserGroupAccess( String id, String access )
    {
        this.id = id;
        this.access = access;
    }

    public UserGroupAccess( UserGroup userGroup, String access )
    {
        this.access = access;
        this.userGroup = userGroup;
        this.id = userGroup.getUid();
    }

    public UserGroupAccess( String id, String access, UserGroup userGroup )
    {
        this.id = id;
        this.access = access;
        this.userGroup = userGroup;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAccess()
    {
        return access;
    }

    public void setAccess( String access )
    {
        this.access = access;
    }

    public void setUserGroup( UserGroup userGroup )
    {
        this.id = userGroup.getUid();
    }

    @JsonIgnore
    public UserGroup getUserGroup()
    {
        return userGroup;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        UserGroupAccess that = (UserGroupAccess) o;

        return Objects.equals( access, that.access ) && Objects.equals( getId(), that.getId() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( access, getId() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "id", getId() )
            .add( "access", access )
            .toString();
    }
}
