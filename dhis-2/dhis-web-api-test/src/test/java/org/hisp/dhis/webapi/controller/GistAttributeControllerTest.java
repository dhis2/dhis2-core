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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
        attrId = assertStatus( HttpStatus.CREATED, POST( "/attributes", "{" + "'name':'extra', "
            + "'valueType':'TEXT', " + "'" + Attribute.ObjectType.USER_GROUP.getPropertyName() + "':true}" ) );
        group1Id = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/",
                "{"
                    + "'name':'G1', "
                    + "'attributeValues':[{'attribute': {'id':'" + attrId + "'}, 'value':'extra-value'}]"
                    + "}" ) );
        group2Id = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/",
                "{"
                    + "'name':'G2', "
                    + "'attributeValues':[{'attribute': {'id':'" + attrId + "'}, 'value':'different'}]"
                    + "}" ) );
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
}
