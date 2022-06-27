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

import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.UnaryOperator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.collections.MapUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JacksonXmlRootElement( localName = "sharing", namespace = DxfNamespaces.DXF_2_0 )
public class Sharing
    implements Serializable
{
    private static final long serialVersionUID = 6977793211734844477L;

    /**
     * Uid of the User who owns the object
     */
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private String owner;

    @JsonProperty( "public" )
    @JacksonXmlProperty( localName = "public", namespace = DxfNamespaces.DXF_2_0 )
    private String publicAccess;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean external;

    /**
     * Map of UserAccess. Key is User uid
     */
    @Setter
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Map<String, UserAccess> users = new HashMap<>();

    /**
     * Map of UserGroupAccess. Key is UserGroup uid
     */
    @Setter
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Map<String, UserGroupAccess> userGroups = new HashMap<>();

    public Sharing( String publicAccess, UserAccess... userAccesses )
    {
        this.publicAccess = publicAccess;

        for ( UserAccess userAccess : userAccesses )
        {
            users.put( userAccess.getId(), userAccess );
        }
    }

    public Sharing( String publicAccess, UserGroupAccess... userGroupAccesses )
    {
        this.publicAccess = publicAccess;

        for ( UserGroupAccess userGroupAccess : userGroupAccesses )
        {
            userGroups.put( userGroupAccess.getId(), userGroupAccess );
        }
    }

    public Map<String, UserAccess> getUsers()
    {
        if ( users == null )
        {
            users = new HashMap<>();
        }

        return users;
    }

    public Map<String, UserGroupAccess> getUserGroups()
    {
        if ( userGroups == null )
        {
            userGroups = new HashMap<>();
        }

        return userGroups;
    }

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
        this.users = clearOrInit( this.users );
        userAccesses.forEach( this::addUserAccess );
    }

    public void setDtoUserAccesses( Set<org.hisp.dhis.user.UserAccess> dto )
    {
        if ( dto == null )
        {
            return;
        }

        this.users = clearOrInit( this.users );
        dto.forEach( ua -> this.addUserAccess( new UserAccess( ua ) ) );
    }

    public void setDtoUserGroupAccesses( Set<org.hisp.dhis.user.UserGroupAccess> userGroupAccesses )
    {
        if ( userGroupAccesses == null )
        {
            return;
        }

        this.userGroups = clearOrInit( this.userGroups );
        userGroupAccesses.forEach( uga -> this.addUserGroupAccess( new UserGroupAccess( uga ) ) );

    }

    public void setUserGroupAccess( Set<UserGroupAccess> userGroupAccesses )
    {
        this.userGroups = clearOrInit( this.userGroups );
        userGroupAccesses.forEach( this::addUserGroupAccess );
    }

    public void addUserAccess( UserAccess userAccess )
    {
        if ( userAccess != null )
        {
            getUsers().put( userAccess.getId(), userAccess );
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
        if ( userGroups == null )
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

    /**
     * Returns a new {@link Sharing} instance where all access strings have been
     * transformed.
     *
     * @param accessTransformation A transformation for access strings that is
     *        applied to all access strings of this {@link Sharing} to produce
     *        the access strings used in the newly created {@link Sharing}
     *        object returned.
     * @return A new {@link Sharing} instance where the access strings of public
     *         access, user and group access have been transformed by the
     *         provided transformation. This {@link Sharing} is kept unchanged.
     */
    public Sharing withAccess( UnaryOperator<String> accessTransformation )
    {
        return builder()
            .external( external )
            .publicAccess( accessTransformation.apply( publicAccess ) )
            .owner( owner )
            .users( mapValues( users,
                user -> new UserAccess( accessTransformation.apply( user.getAccess() ), user.getId() ) ) )
            .userGroups( mapValues( userGroups,
                group -> new UserGroupAccess( accessTransformation.apply( group.getAccess() ), group.getId() ) ) )
            .build();
    }

    private static <K, V> Map<K, V> mapValues( Map<K, V> map, UnaryOperator<V> mapper )
    {
        if ( map == null )
        {
            return null;
        }
        return map.entrySet().stream()
            .collect( toMap( Entry::getKey, e -> mapper.apply( e.getValue() ) ) );
    }

    private static <T> Map<String, T> clearOrInit( Map<String, T> map )
    {
        if ( map != null )
        {
            map.clear();
            return map;
        }
        return new HashMap<>();
    }

    /**
     * First to positions are metadata sharing, positions 3 and 4 are data
     * sharing. This copies the positions 1 and 2 to 3 and 4:
     *
     * The pattern {@code rw--xxxx} becomes {@code rwrwxxxx}.
     *
     * For example:
     *
     * <pre>
     * r------- => r-r-----
     * rw------ => rwrw----
     * r-rw---- => r-r-----
     * </pre>
     *
     * @param access a access string which is expected to be either null or 8
     *        characters long
     * @return the provided access string except that position 1 and 2 are
     *         copied to position 3 and 4.
     */
    public static String copyMetadataToData( String access )
    {
        if ( access == null )
        {
            return null;
        }
        String metadata = access.substring( 0, 2 );
        return metadata + metadata + access.substring( 4 );
    }
}
