

package org.hisp.dhis.actions.metadata;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.JsonObjectBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SharingActions extends RestApiActions
{
    public SharingActions( )
    {
        super( "/sharing" );
    }

    public void setupSharingForConfiguredUserGroup(String type, String id) {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add( "object",JsonObjectBuilder.jsonObject().addUserGroupAccess().build());

        this.post( jsonObject, new QueryParamsBuilder().add( "type=" + type  ).add( "id=" + id ) ).validate().statusCode( 200 );
    }

}
