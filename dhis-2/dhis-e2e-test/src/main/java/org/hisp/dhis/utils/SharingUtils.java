package org.hisp.dhis.utils;

import com.google.gson.JsonObject;

import java.util.Map;

public class SharingUtils
{
    public static JsonObject createSharingObject( String publicAccess )
    {
        return createSharingObject( null, publicAccess, Map.of(), Map.of() );
    }
    public static JsonObject createSharingObject( String owner, String publicAccess, Map<String, String> users, Map<String, String> userGroups )
    {
        JsonObject sharing = new JsonObject();

        if ( publicAccess != null )
        {
            sharing.addProperty( "public", publicAccess );
        }

        if ( owner != null )
        {
            sharing.addProperty( "owner", owner );
        }

        sharing.addProperty( "external", false );

        if ( userGroups.size() > 0 )
        {
            JsonObject userGroupObject = new JsonObject();
            userGroups.keySet().forEach( uid -> userGroupObject.add( uid, createAccessObject( uid, userGroups.get( uid ) ) ) );
        }

        if ( users.size() > 0 )
        {
            JsonObject userObject = new JsonObject();
            users.keySet().forEach( uid -> userObject.add( uid, createAccessObject( uid, users.get( uid ) ) ) );
        }

        return sharing;
    }

    public static JsonObject createAccessObject( String uid , String accessString )
    {
        JsonObject access = new JsonObject();
        access.addProperty( "id", uid );
        access.addProperty( "access", accessString );

        return access;
    }
}
