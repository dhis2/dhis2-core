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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonSchema;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link SchemaController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class SchemaControllerTest extends DhisControllerConvenienceTest
{
    @Test
    void testValidateSchema()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/schemas/organisationUnit", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testValidateSchema_NoSuchType()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Type xyz does not exist.",
            POST( "/schemas/xyz", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testFieldFilteringNameKlass()
    {
        var schema = GET( "/schemas/organisationUnit?fields=name,klass" ).content( HttpStatus.OK )
            .as( JsonSchema.class );
        assertNotNull( schema.getKlass() );
        assertNotNull( schema.getName() );
        assertNull( schema.getSingular() );
        assertNull( schema.getPlural() );
        assertFalse( schema.get( "properties" ).exists() );
    }

    @Test
    void testFieldFilteringDefaultPropertiesExpansion()
    {
        var schema = GET( "/schemas/organisationUnit?fields=name,klass,properties" ).content( HttpStatus.OK )
            .as( JsonSchema.class );
        assertNotNull( schema.getKlass() );
        assertNotNull( schema.getName() );
        assertNull( schema.getSingular() );
        assertNull( schema.getPlural() );
        assertTrue( schema.get( "properties" ).exists() );
        assertFalse( schema.getProperties().isEmpty() );
        assertNotNull( schema.getProperties().get( 0 ).getName() );
        assertNotNull( schema.getProperties().get( 0 ).getKlass() );
        assertNotNull( schema.getProperties().get( 0 ).getFieldName() );
    }

    @Test
    void testFieldFilteringAllSchemas()
    {
        var schemas = GET( "/schemas?fields=name,klass" ).content( HttpStatus.OK ).as( JsonObject.class )
            .getList( "schemas", JsonSchema.class );
        for ( JsonSchema schema : schemas )
        {
            assertNotNull( schema.getKlass() );
            assertNotNull( schema.getName() );
            assertNull( schema.getSingular() );
            assertNull( schema.getPlural() );
        }
    }
}
