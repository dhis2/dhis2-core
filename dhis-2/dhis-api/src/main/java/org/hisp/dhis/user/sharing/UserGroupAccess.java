/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.user.sharing;

import java.io.Serializable;

import lombok.NoArgsConstructor;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.user.UserGroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@NoArgsConstructor
@JacksonXmlRootElement( localName = "userGroupAccess", namespace = DxfNamespaces.DXF_2_0 )
public class UserGroupAccess
    implements Serializable
{
    private String access;

    private String id;

    private transient String displayName;

    public UserGroupAccess( String access, String id )
    {
        this.access = access;
        this.id = id;
    }

    public UserGroupAccess( UserGroup userGroup, String access )
    {
        this.access = access;
        this.id = userGroup.getUid();
    }

    public UserGroupAccess( org.hisp.dhis.user.UserGroupAccess userGroupAccess )
    {
        this.id = userGroupAccess.getUid();
        this.access = userGroupAccess.getAccess();
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    public void setUserGroup( UserGroup userGroup )
    {
        this.id = userGroup.getUid();
    }

    public org.hisp.dhis.user.UserGroupAccess toDtoObject()
    {
        org.hisp.dhis.user.UserGroupAccess userGroupAccess = new org.hisp.dhis.user.UserGroupAccess();
        userGroupAccess.setUid( this.id );
        userGroupAccess.setAccess( this.access );
        UserGroup userGroup = new UserGroup();
        userGroup.setUid( this.id );
        userGroupAccess.setUserGroup( userGroup );
        userGroupAccess.setUid( this.id );

        return userGroupAccess;
    }
}
