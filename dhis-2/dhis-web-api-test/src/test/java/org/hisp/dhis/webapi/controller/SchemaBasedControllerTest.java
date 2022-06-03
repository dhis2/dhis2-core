/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static java.util.Arrays.asList;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonObject;
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
            "programRuleAction", // needs DataElement and TrackedEntityAttribute
                                 // (not a required field)
            "validationRule", // generator insufficient (embedded fields)
            "programStage", // required Program not required in schema
            // presumably server errors/bugs
            "trackedEntityInstance", // conflict (no details)
            "predictor" // NPE in preheat when creating objects
        ) );

    /**
     * A list of endpoints that do not support the {@code /gist} API because
     * their controller does not extend the base class that implements it.
     */
    private static final Set<String> IGNORED_GIST_ENDPOINTS = new HashSet<>( asList(
        "reportTable", // no /gist API
        "chart" // no /gist API
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

                // run other tests that depend upon having an existing object
                testWithSchema( schema, id );

                // delete the last created object
                // (the one belonging to the tested schema)
                assertStatus( HttpStatus.OK, DELETE( endpoint + "/" + id ) );
            }
        }
        assertTrue( "make sure we actually test schemas", testedSchemas >= 58 );
    }

    /**
     * Uses the created instance to test the {@code /gist} endpoint list.
     */
    private void testWithSchema( JsonSchema schema, String id )
    {
        String endpoint = schema.getRelativeApiEndpoint();
        if ( endpoint == null || IGNORED_GIST_ENDPOINTS.contains( schema.getName() ) )
        {
            return;
        }

        // test gist list of object for the schema
        JsonObject gist = GET( endpoint + "/gist" ).content();
        assertTrue( gist.getObject( "pager" ).exists() );
        JsonArray list = gist.getArray( schema.getPlural() );
        assertFalse( list.isEmpty() );
        // only if there is only one we are sure its the one we created
        if ( list.size() == 1 )
        {
            assertEquals( id, list.getObject( 0 ).getString( "id" ).string() );
        }

        // test the single object gist as well
        JsonObject object = GET( endpoint + "/" + id + "/gist" ).content();
        assertTrue( object.exists() );
        assertEquals( id, object.getString( "id" ).string() );
    }

    private boolean isExcludedFromTest( JsonSchema schema )
    {
        return schema.isEmbeddedObject()
            || !schema.isIdentifiableObject()
            || !schema.getApiEndpoint().exists()
            || IGNORED_SCHEMAS.contains( schema.getName() );
    }
}
