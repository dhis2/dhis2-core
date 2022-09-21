package org.hisp.dhis.utils;



import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class JsonObjectBuilder
{
    private JsonObject jsonObject;

    public JsonObjectBuilder() {
        jsonObject = new JsonObject();
    }

    public JsonObjectBuilder( JsonObject jsonObject ) {
        this.jsonObject = jsonObject;
    }

    public static JsonObjectBuilder jsonObject() {
        return new JsonObjectBuilder();
    }

    public static JsonObjectBuilder jsonObject( JsonObject jsonObject) {
        return new JsonObjectBuilder( jsonObject );
    }

    public JsonObjectBuilder addProperty(String property, String value) {
        jsonObject.addProperty( property, value );

        return this;
    }

    public JsonObjectBuilder addUserGroupAccess( ) {
        JsonArray userGroupAccesses = new JsonArray(  );

        JsonObject userGroupAccess = JsonObjectBuilder.jsonObject()
            .addProperty( "access", "rwrw----" )
            .addProperty( "userGroupId", Constants.USER_GROUP_ID )
            .addProperty( "id", Constants.USER_GROUP_ID )
            .build();

        userGroupAccesses.add( userGroupAccess );


        jsonObject.add( "userGroupAccesses", userGroupAccesses );

        return this;
    }
    public JsonObject build() {
        return this.jsonObject;
    }
}
