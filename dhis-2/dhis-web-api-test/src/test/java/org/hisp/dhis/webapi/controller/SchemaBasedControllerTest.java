/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.WebClient.ContentType;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hisp.dhis.attribute.Attribute.ObjectType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonGenerator;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonSchema;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * This tests uses the {@link JsonSchema} information the server provides to
 * create an object for each {@link org.hisp.dhis.schema.Schema} (some won't
 * work) and then delete it again.
 * <p>
 * When objects depend upon other objects these are created first.
 *
 * @author Jan Bernitt
 */
class SchemaBasedControllerTest extends DhisControllerConvenienceTest
{
    private static final Set<String> IGNORED_SCHEMAS = Set.of(
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
        "user", // generator insufficient to understand user
        "jobConfiguration", // API requires configurable=true
        "messageConversation", // needs recipients (not a required field)
        "programRuleAction", // needs DataElement and TrackedEntityAttribute
        "validationRule", // generator insufficient (embedded fields)
        "programStage", // presumably server errors/bugs
        "trackedEntityInstance", // conflict (no details)
        "predictor", // NPE in preheat when creating objects
        "analyticsDataExchange" // required JSONB objects not working
    );

    /**
     * A list of endpoints that do not support the {@code /gist} API because
     * their controller does not extend the base class that implements it.
     */
    private static final Set<String> IGNORED_GIST_ENDPOINTS = Set.of(
        "reportTable",
        "chart" );

    @Test
    void testCreateAndDeleteSchemaObjects()
    {
        JsonList<JsonSchema> schemas = GET( "/schemas" ).content().getList( "schemas", JsonSchema.class );
        JsonGenerator generator = new JsonGenerator( schemas );
        int testedSchemas = 0;
        for ( JsonSchema schema : schemas )
        {
            if ( !isExcludedFromTest( schema ) )
            {
                testedSchemas++;
                Map<String, String> objects = generator.generateObjects( schema );
                String uid = "";
                String endpoint = "";
                // create needed object(s)
                // last created is the one we want to test for schema
                // those before might be objects it depends upon that
                // need to be created first
                for ( Entry<String, String> entry : objects.entrySet() )
                {
                    endpoint = entry.getKey();
                    uid = assertStatus( HttpStatus.CREATED, POST( endpoint, entry.getValue() ) );
                }
                // run other tests that depend upon having an existing object
                testWithSchema( schema, uid );
                // delete the last created object
                // (the one belonging to the tested schema)
                assertStatus( HttpStatus.OK, DELETE( endpoint + "/" + uid ) );
            }
        }
        assertTrue( testedSchemas >= 58, "make sure we actually test schemas" );
    }

    /**
     * Uses the created instance to test the {@code /gist} endpoint list.
     */
    private void testWithSchema( JsonSchema schema, String uid )
    {
        String endpoint = schema.getRelativeApiEndpoint();
        if ( endpoint != null )
        {
            testGistAPI( schema, uid );
            testCanHaveAttributes( schema, uid );
        }
    }

    private void testGistAPI( JsonSchema schema, String uid )
    {
        if ( IGNORED_GIST_ENDPOINTS.contains( schema.getName() ) )
        {
            return;
        }
        String endpoint = schema.getRelativeApiEndpoint();
        // test gist list of object for the schema
        JsonObject gist = GET( endpoint + "/gist" ).content();
        assertTrue( gist.getObject( "pager" ).exists() );
        JsonArray list = gist.getArray( schema.getPlural() );
        assertFalse( list.isEmpty() );
        // only if there is only one we are sure its the one we created
        if ( list.size() == 1 )
        {
            assertEquals( uid, list.getObject( 0 ).getString( "id" ).string() );
        }
        // test the single object gist as well
        JsonObject object = GET( endpoint + "/" + uid + "/gist" ).content();
        assertTrue( object.exists() );
        assertEquals( uid, object.getString( "id" ).string() );
    }

    private void testCanHaveAttributes( JsonSchema schema, String uid )
    {
        ObjectType type = ObjectType.valueOf( schema.getKlass() );
        if ( type == null || type == ObjectType.MAP )
        {
            return;
        }
        String attrId = assertStatus( HttpStatus.CREATED, POST( "/attributes",
            "{'name':'" + type + "', 'valueType':'INTEGER','" + type.getPropertyName() + "':true}" ) );
        String endpoint = schema.getRelativeApiEndpoint();
        JsonObject object = GET( endpoint + "/" + uid ).content();
        assertStatus( HttpStatus.OK,
            PUT( endpoint + "/" + uid + "?mergeMode=REPLACE",
                Body( object.getObject( "attributeValues" ).node()
                    .replaceWith( "[{\"value\":42, \"attribute\":{\"id\":\"" + attrId + "\"}}]" ).getDeclaration() ),
                ContentType( MediaType.APPLICATION_JSON ) ) );
        assertEquals( "42", GET( endpoint + "/" + uid ).content().as( JsonIdentifiableObject.class )
            .getAttributeValues().get( 0 ).getValue() );
    }

    private boolean isExcludedFromTest( JsonSchema schema )
    {
        return schema.isEmbeddedObject() || !schema.isIdentifiableObject() || !schema.getApiEndpoint().exists()
            || IGNORED_SCHEMAS.contains( schema.getName() );
    }
}
