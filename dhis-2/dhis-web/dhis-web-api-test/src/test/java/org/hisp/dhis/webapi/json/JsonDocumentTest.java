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
package org.hisp.dhis.webapi.json;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Map;

import org.hisp.dhis.webapi.json.JsonDocument.JsonNode;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;
import org.junit.Test;

/**
 * Tests the fundamental properties of the {@link JsonDocument} JSON path
 * extractor.
 *
 * @author Jan Bernitt
 */
public class JsonDocumentTest
{

    @Test
    public void testStringNode()
    {
        JsonNode node = new JsonDocument( "\"hello\"" ).get( "$" );
        assertEquals( JsonNodeType.STRING, node.getType() );
        assertEquals( "hello", node.value() );
        assertEquals( 0, node.startIndex() );
    }

    @Test
    public void testNumberNode_Integer()
    {
        JsonNode node = new JsonDocument( "123" ).get( "$" );
        assertEquals( JsonNodeType.NUMBER, node.getType() );
        assertEquals( 123, node.value() );
    }

    @Test
    public void testNumberNode_Long()
    {
        JsonNode node = new JsonDocument( "2147483648" ).get( "$" );
        assertEquals( JsonNodeType.NUMBER, node.getType() );
        assertEquals( 2147483648L, node.value() );
    }

    @Test
    public void testBooleanNode_True()
    {
        JsonNode node = new JsonDocument( "true" ).get( "$" );
        assertEquals( JsonNodeType.BOOLEAN, node.getType() );
        assertEquals( true, node.value() );
    }

    @Test
    public void testBooleanNode_False()
    {
        JsonNode node = new JsonDocument( "false" ).get( "$" );
        assertEquals( JsonNodeType.BOOLEAN, node.getType() );
        assertEquals( false, node.value() );
    }

    @Test
    public void testNullNode()
    {
        JsonNode node = new JsonDocument( "null" ).get( "$" );
        assertEquals( JsonNodeType.NULL, node.getType() );
        assertNull( node.value() );
    }

    @Test
    public void testArray_Numbers()
    {
        JsonNode node = new JsonDocument( "[1, 2 ,3]" ).get( "$" );
        assertEquals( JsonNodeType.ARRAY, node.getType() );
        assertFalse( node.isEmpty() );
        assertEquals( 3, node.size() );
    }

    @Test
    public void testObject_Flat()
    {
        JsonNode root = new JsonDocument( "{\"a\":1, \"bb\":true , \"ccc\":null }" ).get( "$" );
        assertEquals( JsonNodeType.OBJECT, root.getType() );
        assertFalse( root.isEmpty() );
        assertEquals( 3, root.size() );
        Map<String, JsonNode> value = root.object();
        assertEquals( new HashSet<>( asList( "a", "bb", "ccc" ) ), value.keySet() );
    }

    @Test
    public void testObject_Deep()
    {
        JsonDocument content = new JsonDocument( "{\"a\": { \"b\" : [12, false] } }" );

        JsonNode root = content.get( "$" );
        assertEquals( JsonNodeType.OBJECT, root.getType() );
        assertFalse( root.isEmpty() );
        assertEquals( 1, root.size() );

        JsonNode a = content.get( "$.a" );
        assertEquals( JsonNodeType.OBJECT, a.getType() );
        assertFalse( a.isEmpty() );
        assertEquals( 1, a.size() );

        JsonNode ab = content.get( "$.a.b" );
        assertEquals( JsonNodeType.ARRAY, ab.getType() );
        assertFalse( ab.isEmpty() );
        assertEquals( 2, ab.size() );
        assertEquals( "[12, false]", ab.getDeclaration() );

        JsonNode ab0 = content.get( "$.a.b[0]" );
        assertEquals( JsonNodeType.NUMBER, ab0.getType() );
        assertEquals( 12, ab0.value() );

        JsonNode ab1 = content.get( "$.a.b[1]" );
        assertEquals( JsonNodeType.BOOLEAN, ab1.getType() );
        assertEquals( false, ab1.value() );
    }
}
