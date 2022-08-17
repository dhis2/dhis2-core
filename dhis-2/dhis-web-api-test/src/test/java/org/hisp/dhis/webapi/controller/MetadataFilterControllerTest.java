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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the filters of the metadata API.
 *
 * @author Jan Bernitt
 */
class MetadataFilterControllerTest extends DhisControllerConvenienceTest
{
    private String attrId;

    private String ouId;

    @BeforeEach
    void setUp()
    {
        attrId = assertStatus( HttpStatus.CREATED, POST( "/attributes", "{" + "'name':'extra', "
            + "'valueType':'TEXT', " + "'" + Attribute.ObjectType.ORGANISATION_UNIT.getPropertyName() + "':true}" ) );
        ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{"
                + "'name':'My Unit', "
                + "'shortName':'OU1', "
                + "'openingDate': '2020-01-01',"
                + "'attributeValues':[{'attribute': {'id':'" + attrId + "'}, 'value':'test'}]"
                + "}" ) );
    }

    @Test
    void testFilter_attributeEq()
    {
        JsonArray units = GET( "/organisationUnits?filter={attr}:eq:test", attrId ).content()
            .getArray( "organisationUnits" );
        assertEquals( 1, units.size() );
        JsonObject unit = units.getObject( 0 );
        assertEquals( "My Unit", unit.getString( "displayName" ).string() );
        assertEquals( ouId, unit.getString( "id" ).string() );
    }

    @Test
    void testFields_ListAttribute()
    {
        JsonArray units = GET( "/organisationUnits?fields=id,name,{attr}", attrId ).content()
            .getArray( "organisationUnits" );
        assertEquals( 1, units.size() );
        JsonObject unit = units.getObject( 0 );
        assertEquals( ouId, unit.getString( "id" ).string() );
        assertEquals( "My Unit", unit.getString( "name" ).string() );
        assertEquals( "test", unit.getString( attrId ).string() );
    }

    @Test
    void testFields_ListAttributeWithAlias()
    {
        JsonArray units = GET( "/organisationUnits?fields=id,name,{attr}::rename(x)", attrId ).content()
            .getArray( "organisationUnits" );
        assertEquals( 1, units.size() );
        JsonObject unit = units.getObject( 0 );
        assertEquals( ouId, unit.getString( "id" ).string() );
        assertEquals( "My Unit", unit.getString( "name" ).string() );
        assertEquals( "test", unit.getString( "x" ).string() );
    }

    @Test
    void testFields_ObjectAttribute()
    {
        JsonObject unit = GET( "/organisationUnits/{uid}?fields=id,name,{attr}", ouId, attrId ).content();
        assertEquals( ouId, unit.getString( "id" ).string() );
        assertEquals( "My Unit", unit.getString( "name" ).string() );
        assertEquals( "test", unit.getString( attrId ).string() );
    }

    @Test
    void testFields_ObjectAttributeWithAlias()
    {
        JsonObject unit = GET( "/organisationUnits/{uid}?fields=id,name,{attr}::rename(x)", ouId, attrId ).content();
        assertEquals( ouId, unit.getString( "id" ).string() );
        assertEquals( "My Unit", unit.getString( "name" ).string() );
        assertEquals( "test", unit.getString( "x" ).string() );
    }
}
