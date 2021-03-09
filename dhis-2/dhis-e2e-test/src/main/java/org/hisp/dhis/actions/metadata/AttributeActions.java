

package org.hisp.dhis.actions.metadata;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.utils.DataGenerator;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AttributeActions extends RestApiActions
{
    public AttributeActions(  )
    {
        super( "/attributes" );
    }

    public String createUniqueAttribute(String valueType, String... metadataObjects) {
        JsonObject object = new JsonObject();

        object.addProperty( "name", String.format( "TA attribute %s", DataGenerator.randomString() ) );
        object.addProperty( "unique", "false" );
        for ( String metadataObject : metadataObjects
               )
        {
            object.addProperty( metadataObject + "Attribute", "true" );

        }

        object.addProperty( "valueType", valueType);

        return this.create( object );
    }
}
