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

import java.io.*;

import lombok.*;

import org.hisp.dhis.common.*;
import org.hisp.dhis.user.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.xml.annotation.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@NoArgsConstructor
@JacksonXmlRootElement( localName = "userAccess", namespace = DxfNamespaces.DXF_2_0 )
public class UserAccess
    implements Serializable
{
    private String access;

    private String id;

    private transient String displayName;

    public UserAccess( String access, String id )
    {
        this.access = access;
        this.id = id;
    }

    public UserAccess( User user, String access )
    {
        this.access = access;
        this.id = user.getUid();
    }

    public UserAccess( org.hisp.dhis.user.UserAccess userAccess )
    {
        this.access = userAccess.getAccess();
        this.id = userAccess.getUid();
    }

    public void setUser( User user )
    {
        this.id = user.getUid();
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

    public org.hisp.dhis.user.UserAccess toDtoObject()
    {
        org.hisp.dhis.user.UserAccess userAccess = new org.hisp.dhis.user.UserAccess();
        userAccess.setUid( this.id );
        userAccess.setAccess( this.access );
        User user = new User();
        user.setUid( this.id );
        userAccess.setUser( user );
        userAccess.setUid( this.id );

        return userAccess;
    }
}
