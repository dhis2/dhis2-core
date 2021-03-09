package org.hisp.dhis.actions.tracker;




import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RelationshipActions
    extends RestApiActions
{
    public RelationshipActions()
    {
        super( "/relationships" );
    }

    public JsonObject createRelationshipBody( String relationshipTypeId, String fromEntity, String fromEntityId, String toEntity,
        String toEntityId )
    {
        JsonObject relationship = new JsonObject();
        relationship.addProperty( "relationshipType", relationshipTypeId );

        JsonObject from = new JsonObject();
        JsonObject fromEntityObj = new JsonObject();
        fromEntityObj.addProperty( fromEntity, fromEntityId );
        from.add( fromEntity, fromEntityObj );

        relationship.add( "from", from );

        JsonObject to = new JsonObject();
        JsonObject toEntityObj = new JsonObject();
        toEntityObj.addProperty( toEntity, toEntityId );
        to.add( toEntity, toEntityObj );

        relationship.add( "to", to );

        return relationship;

    }
}
