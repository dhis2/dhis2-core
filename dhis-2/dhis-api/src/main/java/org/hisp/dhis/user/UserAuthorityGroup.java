package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Nguyen Hong Duc
 */
@JacksonXmlRootElement( localName = "userRole", namespace = DxfNamespaces.DXF_2_0 )
public class UserAuthorityGroup
    extends BaseIdentifiableObject implements MetadataObject
{
    public static final String AUTHORITY_ALL = "ALL";

    public static final String[] CRITICAL_AUTHS = { "ALL", "F_SCHEDULING_ADMIN", "F_SYSTEM_SETTING",
        "F_SQLVIEW_PUBLIC_ADD", "F_SQLVIEW_PRIVATE_ADD", "F_SQLVIEW_DELETE", "F_SQLVIEW_EXECUTE",
        "F_SQLVIEW_MANAGEMENT", "F_USERROLE_PUBLIC_ADD", "F_USERROLE_PRIVATE_ADD", "F_USERROLE_DELETE",
        "F_USERROLE_LIST" };

    /**
     * Required and unique.
     */
    private String description;

    private Set<String> authorities = new HashSet<>();

    private Set<UserCredentials> members = new HashSet<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public UserAuthorityGroup()
    {

    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addUserCredentials( UserCredentials userCredentials )
    {
        members.add( userCredentials );
        userCredentials.getUserAuthorityGroups().add( this );
    }

    public void removeUserCredentials( UserCredentials userCredentials )
    {
        members.remove( userCredentials );
        userCredentials.getUserAuthorityGroups().remove( this );
    }

    public boolean isSuper()
    {
        return authorities != null && authorities.contains( AUTHORITY_ALL );
    }

    public boolean hasCriticalAuthorities()
    {
        return authorities != null && CollectionUtils.containsAny( authorities, Sets.newHashSet( CRITICAL_AUTHS ) );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "authorities", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "authority", namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getAuthorities()
    {
        return authorities;
    }

    public void setAuthorities( Set<String> authorities )
    {
        this.authorities = authorities;
    }

    public Set<UserCredentials> getMembers()
    {
        return members;
    }

    public void setMembers( Set<UserCredentials> members )
    {
        this.members = members;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "users", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userObject", namespace = DxfNamespaces.DXF_2_0 )
    public List<User> getUsers()
    {
        List<User> users = new ArrayList<>();

        for ( UserCredentials userCredentials : members )
        {
            if ( userCredentials.getUserInfo() != null )
            {
                users.add( userCredentials.getUserInfo() );
            }
        }

        return users;
    }
}
