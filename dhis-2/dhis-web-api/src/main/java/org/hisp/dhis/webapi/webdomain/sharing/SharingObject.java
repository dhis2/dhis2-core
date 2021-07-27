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
package org.hisp.dhis.webapi.webdomain.sharing;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class SharingObject
{
    private String id;

    private String name;

    private String displayName;

    private String publicAccess;

    private boolean externalAccess;

    private SharingUser user = new SharingUser();

    private List<SharingUserGroupAccess> userGroupAccesses = new ArrayList<>();

    private List<SharingUserAccess> userAccesses = new ArrayList<>();

    public SharingObject()
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
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @JsonProperty
    public String getPublicAccess()
    {
        return publicAccess;
    }

    public void setPublicAccess( String publicAccess )
    {
        this.publicAccess = publicAccess;
    }

    @JsonProperty( "externalAccess" )
    public boolean hasExternalAccess()
    {
        return externalAccess;
    }

    @JsonProperty( "externalAccess" )
    public void setExternalAccess( boolean externalAccess )
    {
        this.externalAccess = externalAccess;
    }

    @JsonProperty
    public SharingUser getUser()
    {
        return user;
    }

    public void setUser( SharingUser user )
    {
        this.user = user;
    }

    @JsonProperty
    public List<SharingUserGroupAccess> getUserGroupAccesses()
    {
        return userGroupAccesses;
    }

    public void setUserGroupAccesses( List<SharingUserGroupAccess> userGroupAccesses )
    {
        this.userGroupAccesses = userGroupAccesses;
    }

    @JsonProperty
    public List<SharingUserAccess> getUserAccesses()
    {
        return userAccesses;
    }

    public void setUserAccesses( List<SharingUserAccess> userAccesses )
    {
        this.userAccesses = userAccesses;
    }
}
