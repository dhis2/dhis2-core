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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Gist API allows attribute UIDs being used as properties in the
 * {@code fields} and {@code filter} parameters.
 *
 * @author Jan Bernitt
 */
class GistAttributeControllerTest extends AbstractGistControllerTest
{
    /**
     * A text attribute used in the tests
     */
    private String attrId;

    /**
     * A group having the text attribute
     */
    private String group1Id;

    /**
     * Another group having the text attribute but with a different value
     */
    private String group2Id;

    @BeforeEach
    void setUp()
    {
        // create user group with custom attribute value
        attrId = postNewAttribute( "extra", ValueType.TEXT, Attribute.ObjectType.USER_GROUP );
        group1Id = postNewUserGroupWithAttributeValue( "G1", attrId, "extra-value" );
        group2Id = postNewUserGroupWithAttributeValue( "G2", attrId, "different" );
    }

    @Test
    void testAttributeValues_SingleObjectOnlyField()
    {
        assertMapEquals( Map.of( attrId, "extra-value" ),
            GET( "/userGroups/{id}/gist?fields=attributeValues", group1Id ).content().asMap( JsonString.class ) );
    }

    @Test
    void testAttributeValues_SingleObjectProperty()
    {
        assertMapEquals( Map.of( attrId, "extra-value" ),
            GET( "/userGroups/{id}/attributeValues/gist", group1Id ).content().asMap( JsonString.class ) );
    }

    @Test
    void testAttributeValues_SingleObjectOneOfManyFields()
    {
        assertMapEquals( Map.of( attrId, "extra-value" ),
            GET( "/userGroups/{id}/gist?fields=id,name,attributeValues&headless=true", group1Id )
                .content().getMap( "attributeValues", JsonString.class ) );
    }

    @Test
    void testAttributeValues_ListOnlyField()
    {
        assertListOfMapEquals( List.of( Map.of( attrId, "extra-value" ), Map.of( attrId, "different" ) ),
            GET( "/userGroups/gist?fields=attributeValues&headless=true&order=name" ).content()
                .asList( JsonMap.class ) );
    }

    @Test
    void testAttributeValues_ListOneOfManyFields()
    {
        JsonArray list = GET( "/userGroups/gist?fields=id,name,attributeValues&headless=true&order=name" ).content();
        assertEquals( 2, list.size() );
        assertMapEquals( Map.of( attrId, "extra-value" ),
            list.getObject( 0 ).getMap( "attributeValues", JsonString.class ) );
        assertMapEquals( Map.of( attrId, "different" ),
            list.getObject( 1 ).getMap( "attributeValues", JsonString.class ) );
    }

    @Test
    void testField_ObjectSingleField()
    {
        assertEquals( "extra-value",
            GET( "/userGroups/{uid}/gist?fields={attr}", group1Id, attrId ).content().string() );
    }

    @Test
    void testField_ObjectMultipleFields()
    {
        JsonObject group = GET( "/userGroups/{uid}/gist?fields=id,name,{attr}", group1Id, attrId )
            .content();
        assertEquals( group1Id, group.getString( "id" ).string() );
        assertEquals( "G1", group.getString( "name" ).string() );
        assertEquals( "extra-value", group.getString( attrId ).string() );
    }

    @Test
    void testField_ObjectMultipleFieldsWithAlias()
    {
        JsonObject group = GET( "/userGroups/{uid}/gist?fields=id,name,{attr}::rename(extra)", group1Id, attrId )
            .content();
        assertEquals( group1Id, group.getString( "id" ).string() );
        assertEquals( "G1", group.getString( "name" ).string() );
        assertEquals( "extra-value", group.getString( "extra" ).string() );
    }

    @Test
    void testField_ListMultipleFields()
    {
        JsonArray groups = GET( "/userGroups/gist?fields=id,name,{attr}&headless=true", attrId ).content();
        assertEquals( 2,
            groups.asList( JsonObject.class ).count( Function.identity(), g -> !g.get( attrId ).isUndefined() ) );
    }

    @Test
    void testField_ListMultipleFieldsWithAlias()
    {
        JsonArray groups = GET( "/userGroups/gist?fields=id,name,{attr}::rename(extra)&headless=true", attrId )
            .content();
        assertEquals( 2,
            groups.asList( JsonObject.class ).count( Function.identity(), g -> !g.get( "extra" ).isUndefined() ) );
    }

    @Test
    void testField_ObjectGeoJsonPlainValue()
    {
        String geoAttrId = postNewAttribute( "geo", ValueType.GEOJSON, Attribute.ObjectType.USER_GROUP );
        String geoJsonValue = "{'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }";
        String geoGroupId = postNewUserGroupWithAttributeValue( "gg", geoAttrId, geoJsonValue.replace( "'", "\\\"" ) );
        JsonObject group = GET( "/userGroups/{uid}/gist?fields=id,name,{attr}::rename(geo)", geoGroupId, geoAttrId )
            .content();
        assertTrue( group.get( "geo" ).isObject() );
        assertEquals( "MultiPolygon", group.getString( "geo.type" ).string() );
    }

    /**
     * Pluck extracts the attribute value directly in the database using JSONB
     * functions
     */
    @Test
    void testField_ObjectGeoJsonPlainValuePluck()
    {
        String geoAttrId = postNewAttribute( "geo", ValueType.GEOJSON, Attribute.ObjectType.USER_GROUP );
        String geoJsonValue = "{'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }";
        String geoGroupId = postNewUserGroupWithAttributeValue( "gg", geoAttrId, geoJsonValue.replace( "'", "\\\"" ) );
        JsonObject group = GET( "/userGroups/{uid}/gist?fields=id,name,{attr}::rename(geo)::pluck", geoGroupId,
            geoAttrId )
                .content();
        assertTrue( group.get( "geo" ).isObject() );
        assertEquals( "MultiPolygon", group.getString( "geo.type" ).string() );
    }

    @Test
    void testFilter_Eq()
    {
        String url = "/userGroups/gist?fields=id,name&headless=true&filter={attr}:eq:extra-value";
        assertEquals( 1, GET( url, attrId ).content().size() );
    }

    @Test
    void testFilter_NotEq()
    {
        String url = "/userGroups/gist?fields=id,name,{attr}&headless=true&filter={attr}:neq:extra-value";
        JsonArray groups = GET( url, attrId, attrId ).content();
        assertEquals( 1, groups.size() );
        assertEquals( "different", groups.getObject( 0 ).getString( attrId ).string() );
    }

    private String postNewUserGroupWithAttributeValue( String name, String attrId, String value )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/",
                "{"
                    + "'name':'" + name + "', "
                    + "'attributeValues':[{'attribute': {'id':'" + attrId + "'}, 'value':'" + value + "'}]"
                    + "}" ) );
    }

    private String postNewAttribute( String name, ValueType valueType, Attribute.ObjectType objectType )
    {
        return assertStatus( HttpStatus.CREATED, POST( "/attributes", "{" + "'name':'" + name + "', "
            + "'valueType':'" + valueType.name() + "', " + "'" + objectType.getPropertyName() + "':true}" ) );
    }

    private static void assertListOfMapEquals( List<Map<String, String>> expected, JsonList<?> actual )
    {
        assertEquals( expected.size(), actual.size() );
        int i = 0;
        for ( JsonValue e : actual )
        {
            assertMapEquals( expected.get( i++ ), e.asMap( JsonString.class ) );
        }
    }

    private static void assertMapEquals( Map<String, String> expected, JsonMap<JsonString> actual )
    {
        assertEquals( expected, toMap( actual ) );
    }

    private static Map<String, String> toMap( JsonMap<JsonString> actual )
    {
        Map<String, String> res = new HashMap<>();
        for ( String key : actual.keys() )
        {
            res.put( key, actual.get( key ).string() );
        }
        return res;
    }
}
