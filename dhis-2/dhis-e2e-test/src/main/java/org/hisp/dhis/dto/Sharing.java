package org.hisp.dhis.dto;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.utils.SharingUtils;

import java.util.Map;
import java.util.stream.Collectors;

import static org.hisp.dhis.utils.SharingUtils.getSafe;

public class Sharing
{
    private String publicAccess;
    private String owner;
    private boolean external;
    private Map<String, String> userGroups;
    private Map<String, String> users;

    public Sharing() {
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
            .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().getAsJsonObject().get( "access" ).getAsString() ) );
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
}
