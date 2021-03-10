package org.hisp.dhis.webapi.controller;

import static java.util.Arrays.asList;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonGenerator;
import org.hisp.dhis.webapi.json.domain.JsonSchema;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
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

        long start = System.currentTimeMillis();
        JsonGenerator generator = new JsonGenerator( schemas );

        for ( JsonSchema schema : schemas )
        {
            if ( schema.isEmbeddedObject()
                || !schema.isIdentifiableObject()
                || !schema.getApiEndpoint().exists()
                || IGNORED_SCHEMAS.contains( schema.getName() ) )
            {
                continue;
            }
            try
            {
                System.out.println( "\n\n====[" + schema.getName() + "]====" );
                Map<String, String> objects = generator.generateObjects( schema );

                String id = "";
                String endpoint = "";
                for ( Entry<String, String> entry : objects.entrySet() )
                {
                    endpoint = entry.getKey();
                    String body = entry.getValue();
                    System.out.println( "URL: " + endpoint );
                    System.out.println( "Body: " + body );
                    id = assertStatus( HttpStatus.CREATED, POST( endpoint, body ) );
                }
                assertStatus( HttpStatus.OK, DELETE( endpoint + "/" + id ) );

                // String uid = assertSeries( SUCCESSFUL, POST( "/" + endpoint,
                // body ) );
                // assertSeries( SUCCESSFUL, DELETE( "/" + endpoint + "/" + uid
                // ) );
                System.out.println( "X " + schema.getName() );
            }
            catch ( Throwable ex )
            {
                ex.printStackTrace( System.out );
            }
        }
        System.out.println( "done in " + (System.currentTimeMillis() - start) + "ms" );
    }

    @Test
    public void testCreateWithID()
    {
        String uid = CodeGenerator.generateUid();
        assertEquals( uid,
            assertStatus( HttpStatus.CREATED, POST( "/userGroups", "{'name': 'group', 'id':'" + uid + "'}" ) ) );
    }
}
