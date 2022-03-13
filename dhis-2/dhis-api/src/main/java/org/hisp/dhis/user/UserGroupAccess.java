/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.user;

import java.io.Serializable;
import java.util.Objects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.schema.annotation.Property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "userGroupAccess", namespace = DxfNamespaces.DXF_2_0 )
public class UserGroupAccess
    implements Serializable
{
    private String access;

    private transient UserGroup userGroup;

    private String uid;

    private String displayName;

    public UserGroupAccess()
    {
    }

    public UserGroupAccess( UserGroup userGroup, String access )
    {
        this.userGroup = userGroup;
        this.access = access;
    }

    public UserGroupAccess( org.hisp.dhis.user.sharing.UserGroupAccess userGroupAccess )
    {
        this.uid = userGroupAccess.getId();
        this.access = userGroupAccess.getAccess();
    }

    public String getId()
    {
        return uid;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( required = Property.Value.TRUE )
    public String getAccess()
    {
        return access;
    }

    public void setAccess( String access )
    {
        this.access = access;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUserGroupUid()
    {
        return userGroup != null ? userGroup.getUid() : null;
    }

    @JsonProperty( "id" )
    @JacksonXmlProperty( localName = "id", namespace = DxfNamespaces.DXF_2_0 )
    @Property( required = Property.Value.TRUE )
    public String getUid()
    {
        return uid != null ? uid : (userGroup != null ? userGroup.getUid() : null);
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @JsonIgnore
    public UserGroup getUserGroup()
    {
        if ( userGroup == null )
        {
            UserGroup userGroup = new UserGroup();
            userGroup.setUid( uid );
            return userGroup;
        }

        return userGroup;
    }

    @JsonProperty
    public void setUserGroup( UserGroup userGroup )
    {
        this.userGroup = userGroup;
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

        return Objects.equals( access, that.access ) && Objects.equals( getUid(), that.getUid() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( access, getUid() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "uid", getUid() )
            .add( "access", access )
            .toString();
    }
}