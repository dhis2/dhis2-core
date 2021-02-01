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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.collections.MapUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JacksonXmlRootElement( localName = "sharing", namespace = DxfNamespaces.DXF_2_0 )
public class Sharing
    implements Serializable, EmbeddedObject
{
    private static final long serialVersionUID = 6977793211734844477L;

    /**
     * Uid of the User who owns the object
     */
    @JsonProperty
    private String owner;

    @JsonProperty( "public" )
    private String publicAccess;

    @JsonProperty
    private boolean external;

    /**
     * Map of UserAccess. Key is User uid
     */
    @Builder.Default
    @JsonProperty
    private Map<String, UserAccess> users = new HashMap<>();

    /**
     * Map of UserGroupAccess. Key is UserGroup uid
     */
    @Builder.Default
    @JsonProperty
    private Map<String, UserGroupAccess> userGroups = new HashMap<>();

    public void setOwner( User user )
    {
        this.owner = user != null ? user.getUid() : null;
    }

    public void setOwner( String userId )
    {
        this.owner = userId;
    }

    @JsonIgnore
    public User getUserOwner()
    {
        User user = new User();
        user.setUid( this.owner );
        return user;
    }

    public void setUserAccesses( Set<UserAccess> userAccesses )
    {
        if ( this.users != null )
            this.users.clear();
        else
            this.users = new HashMap<>();

        userAccesses.forEach( ua -> this.addUserAccess( ua ) );
    }

    public void setDtoUserAccesses( Set<org.hisp.dhis.user.UserAccess> userAccesses )
    {
        if ( this.users != null )
            this.users.clear();
        else
            this.users = new HashMap<>();

        if ( userAccesses != null && !userAccesses.isEmpty() )
        {
            userAccesses.forEach( ua -> this.addUserAccess( new UserAccess( ua ) ) );
        }
    }

    public void setDtoUserGroupAccesses( Set<org.hisp.dhis.user.UserGroupAccess> userGroupAccesses )
    {
        if ( this.userGroups != null )
        {
            this.userGroups.clear();
        }

        else
            this.userGroups = new HashMap<>();

        if ( userGroupAccesses != null && !userGroupAccesses.isEmpty() )
        {
            userGroupAccesses.forEach( uga -> this.addUserGroupAccess( new UserGroupAccess( uga ) ) );
        }
    }

    public void setUserGroupAccess( Set<UserGroupAccess> userGroupAccesses )
    {
        if ( this.userGroups != null )
            this.userGroups.clear();
        else
            this.userGroups = new HashMap<>();
        userGroupAccesses.forEach( uga -> this.addUserGroupAccess( uga ) );
    }

    public void addUserAccess( UserAccess userAccess )
    {
        if ( userAccess != null )
        {
            this.users.put( userAccess.getId(), userAccess );
        }
    }

    public void addDtoUserAccess( org.hisp.dhis.user.UserAccess userAccess )
    {
        this.users.put( userAccess.getUid(), new UserAccess( userAccess ) );
    }

    public void addDtoUserGroupAccess( org.hisp.dhis.user.UserGroupAccess userGroupAccess )
    {
        this.userGroups.put( userGroupAccess.getUid(), new UserGroupAccess( userGroupAccess ) );
    }

    public void addUserGroupAccess( UserGroupAccess userGroupAccess )
    {
        if ( userGroupAccess == null )
        {
            userGroups = new HashMap<>();
        }

        this.userGroups.put( userGroupAccess.getId(), userGroupAccess );
    }

    public void resetUserAccesses()
    {
        if ( hasUserAccesses() )
        {
            this.users.clear();
        }
    }

    public void resetUserGroupAccesses()
    {
        if ( hasUserGroupAccesses() )
        {
            this.userGroups.clear();
        }
    }

    public boolean hasUserAccesses()
    {
        return !MapUtils.isEmpty( this.users );
    }

    public boolean hasUserGroupAccesses()
    {
        return !MapUtils.isEmpty( this.userGroups );
    }

    public Sharing copy()
    {
        return builder()
            .external( this.external )
            .publicAccess( this.publicAccess )
            .owner( this.owner )
            .users( new HashMap<>( users ) )
            .userGroups( new HashMap<>( userGroups ) ).build();
    }
}
