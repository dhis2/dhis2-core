/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dto;

import static org.hisp.dhis.utils.SharingUtils.getSafe;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

public class Sharing
{
    private String publicAccess;

    private String owner;

    private boolean external;

    private Map<String, String> userGroups;

    private Map<String, String> users;

    public Sharing()
    {
    }

    public Sharing( JsonObject object )
    {
        publicAccess = getSafe( object, "public" );
        owner = getSafe( object, "owner" );
        external = false;
        userGroups = readAccess( object, "userGroups" );
        users = readAccess( object, "users" );
    }

    private Map<String, String> readAccess( JsonObject object, String accessProperty )
    {
        if ( !object.has( "sharing" ) )
        {
            return null;
        }

        JsonObject sharingObject = object.getAsJsonObject( "sharing" );

        if ( !sharingObject.has( accessProperty ) )
        {
            return null;
        }

        JsonObject accessObject = sharingObject.getAsJsonObject( accessProperty );

        return accessObject.entrySet().stream()
            .collect( Collectors.toMap( Map.Entry::getKey,
                e -> e.getValue().getAsJsonObject().get( "access" ).getAsString() ) );
    }

    public String getPublicAccess()
    {
        return publicAccess;
    }

    public String getOwner()
    {
        return owner;
    }

    public boolean isExternal()
    {
        return external;
    }

    public Map<String, String> getUserGroups()
    {
        return userGroups;
    }

    public Map<String, String> getUsers()
    {
        return users;
    }

    public boolean hasUsers()
    {
        return users != null && !users.isEmpty();
    }

    public boolean hasUserGroups()
    {
        return userGroups != null && !userGroups.isEmpty();
    }
}
