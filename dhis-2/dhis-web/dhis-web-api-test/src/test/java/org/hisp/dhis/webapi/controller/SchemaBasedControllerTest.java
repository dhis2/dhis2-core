package org.hisp.dhis.webapi.controller;

import static java.util.Arrays.asList;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonGenerator;
import org.hisp.dhis.webapi.json.domain.JsonSchema;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * This tests uses the {@link JsonSchema} information the server provides to
 * create an object for each {@link org.hisp.dhis.schema.Schema} (some won't
 * work) and then delete it again.
 *
 * When objects depend upon other objects these are created first.
 *
 * @author Jan Bernitt
 */
public class SchemaBasedControllerTest extends DhisControllerConvenienceTest
{

    private static final Set<String> IGNORED_SCHEMAS = new HashSet<>(
        asList(
            "externalFileResource", // can't POST files
            "identifiableObject", // depends on files
            "dashboard", // uses JSONB functions (improve test setup)
            "pushanalysis", // uses dashboards (see above)
            "programInstance", // no POST endpoint
            "metadataVersion", // no POST endpoint
            "softDeletableObject", // depends on programInstance (see above)
            "relationship", // generator insufficient for embedded fields
            "relationshipType", // generator insufficient for embedded fields
            "programStageInstanceFilter", // generator insufficient
            "interpretation", // required ObjectReport not required in schema
            "user", // generator insufficient to understand userCredentials
            "jobConfiguration", // API requires configurable=true
            "messageConversation", // needs recipients (not a required field)
            "validationRule", // generator insufficient (embedded fields)

            // presumably server errors/bugs
            "trackedEntityInstance", // conflict (no details)
            "Predictor" // NPE in preheat when creating objects
        ) );

    @Test
    public void testCreateAndDeleteSchemaObjects()
    {
        JsonList<JsonSchema> schemas = GET( "/schemas" )
            .content().getList( "schemas", JsonSchema.class );

        JsonGenerator generator = new JsonGenerator( schemas );

        int testedSchemas = 0;
        for ( JsonSchema schema : schemas )
        {
            if ( !isExcludedFromTest( schema ) )
            {
                testedSchemas++;
                Map<String, String> objects = generator.generateObjects( schema );
                String id = "";
                String endpoint = "";
                // create needed object(s)
                // last created is the one we want to test for schema
                // those before might be objects it depends upon that
                // need to be created first
                for ( Entry<String, String> entry : objects.entrySet() )
                {
                    endpoint = entry.getKey();
                    id = assertStatus( HttpStatus.CREATED, POST( endpoint, entry.getValue() ) );
                }
                // delete the last created object
                // (the one belonging to the tested schema)
                assertStatus( HttpStatus.OK, DELETE( endpoint + "/" + id ) );

            }
        }
        assertTrue( "make sure we actually test schemas", testedSchemas >= 60 );
    }

    private boolean isExcludedFromTest( JsonSchema schema )
    {
        return schema.isEmbeddedObject()
            || !schema.isIdentifiableObject()
            || !schema.getApiEndpoint().exists()
            || IGNORED_SCHEMAS.contains( schema.getName() );
    }
}
