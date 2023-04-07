package org.hisp.dhis.dto;

import com.google.gson.JsonObject;

import java.util.Map;

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
        publicAccess = object.get( "publicAccess" ).getAsString();
        owner = object.get( "owner" ).getAsString();
        external = object.get( "external" ).getAsBoolean();
        userGroups = readAccess( object.get( "userGroups" ).getAsJsonObject() );
        users = readAccess( object.get( "users" ).getAsJsonObject() );
    }

    private Map<String, String> readAccess( JsonObject object )
    {
        Map<String, String> access = Map.of();

        object.keySet().forEach( key -> {
            JsonObject accessObject = object.get( key ).getAsJsonObject();
            access.put( accessObject.get( "id" ).getAsString(), accessObject.get( "access" ).getAsString() );
        } );

        return access;
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
