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
