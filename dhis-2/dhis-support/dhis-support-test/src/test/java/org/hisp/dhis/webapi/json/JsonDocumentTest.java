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
import static org.junit.Assert.assertThrows;

import java.util.HashSet;
import java.util.Map;

import org.hisp.dhis.webapi.json.JsonDocument.JsonFormatException;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNode;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;
import org.hisp.dhis.webapi.json.JsonDocument.JsonPathException;
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
    public void testStringNode_Unicode()
    {
        // use an array to see that unicode skipping works as well
        JsonNode node0 = new JsonDocument( "[\"Star \\uD83D\\uDE80 ship\", 12]" ).get( "$[0]" );
        assertEquals( JsonNodeType.STRING, node0.getType() );
        assertEquals( "Star \uD83D\uDE80 ship", node0.value() );
        JsonNode node1 = new JsonDocument( "[\"Star \\uD83D\\uDE80 ship\", 12]" ).get( "$[1]" );
        assertEquals( JsonNodeType.NUMBER, node1.getType() );
        assertEquals( 12, node1.value() );
    }

    @Test
    public void testStringNode_EscapedChars()
    {
        JsonNode node = new JsonDocument( "\"\\\\\\/\\t\\r\\n\\f\\b\"" ).get( "$" );
        assertEquals( "\\/\t\r\n\f\b", node.value() );
    }

    @Test
    public void testStringNode_Unsupported()
    {
        JsonNode node = new JsonDocument( "\"hello\"" ).get( "$" );
        Exception ex = assertThrows( UnsupportedOperationException.class, node::isEmpty );
        assertEquals( "STRING node has no empty property.", ex.getMessage() );
        ex = assertThrows( UnsupportedOperationException.class, node::size );
        assertEquals( "STRING node has no size property.", ex.getMessage() );
    }

    @Test
    public void testStringNode_EOI()
    {
        JsonNode node = new JsonDocument( "\"hello" ).get( "$" );
        JsonFormatException ex = assertThrows( JsonFormatException.class, node::value );
        assertEquals( "Expected \" but reach EOI: \"hello", ex.getMessage() );
    }

    @Test
    public void testNumberNode_Integer()
    {
        JsonNode node = new JsonDocument( "123" ).get( "$" );
        assertEquals( JsonNodeType.NUMBER, node.getType() );
        assertEquals( 123, node.value() );
    }

    @Test
    public void testNumberNode_Unsupported()
    {
        JsonNode node = new JsonDocument( "1e-2" ).get( "$" );
        Exception ex = assertThrows( UnsupportedOperationException.class, node::isEmpty );
        assertEquals( "NUMBER node has no empty property.", ex.getMessage() );
        ex = assertThrows( UnsupportedOperationException.class, node::size );
        assertEquals( "NUMBER node has no size property.", ex.getMessage() );
    }

    @Test
    public void testNumberNode_EOI()
    {
        JsonNode node = new JsonDocument( "-" ).get( "$" );
        JsonFormatException ex = assertThrows( JsonFormatException.class, node::value );
        assertEquals( "Expected character but reached EOI: -", ex.getMessage() );
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
    public void testBooleanNode_Unsupported()
    {
        JsonNode node = new JsonDocument( "false" ).get( "$" );
        Exception ex = assertThrows( UnsupportedOperationException.class, node::isEmpty );
        assertEquals( "BOOLEAN node has no empty property.", ex.getMessage() );
        ex = assertThrows( UnsupportedOperationException.class, node::size );
        assertEquals( "BOOLEAN node has no size property.", ex.getMessage() );
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
        JsonDocument doc = new JsonDocument( "{\"a\": { \"b\" : [12, false] } }" );

        JsonNode root = doc.get( "$" );
        assertEquals( JsonNodeType.OBJECT, root.getType() );
        assertFalse( root.isEmpty() );
        assertEquals( 1, root.size() );

        JsonNode a = doc.get( "$.a" );
        assertEquals( JsonNodeType.OBJECT, a.getType() );
        assertFalse( a.isEmpty() );
        assertEquals( 1, a.size() );

        JsonNode ab = doc.get( "$.a.b" );
        assertEquals( JsonNodeType.ARRAY, ab.getType() );
        assertFalse( ab.isEmpty() );
        assertEquals( 2, ab.size() );
        assertEquals( "[12, false]", ab.getDeclaration() );

        JsonNode ab0 = doc.get( "$.a.b[0]" );
        assertEquals( JsonNodeType.NUMBER, ab0.getType() );
        assertEquals( 12, ab0.value() );

        JsonNode ab1 = doc.get( "$.a.b[1]" );
        assertEquals( JsonNodeType.BOOLEAN, ab1.getType() );
        assertEquals( false, ab1.value() );
    }

    /**
     * This test might look very much the same as the above but this test avoid
     * accessing the object fields or array elements before using
     * {@link JsonDocument#get(String)} to resolve inner object so that these
     * would not already be in the internal map but would need to be resolved by
     * going the path backwards.
     */
    @Test
    public void testObject_DeepAccess()
    {
        JsonDocument doc = new JsonDocument( "{\"a\": { \"b\" : [12, false] } }" );

        JsonNode root = doc.get( "$" );
        assertEquals( JsonNodeType.OBJECT, root.getType() );

        JsonNode a = doc.get( "$.a" );
        assertEquals( JsonNodeType.OBJECT, a.getType() );

        JsonNode ab = doc.get( "$.a.b" );
        assertEquals( JsonNodeType.ARRAY, ab.getType() );

        JsonNode ab0 = doc.get( "$.a.b[0]" );
        assertEquals( JsonNodeType.NUMBER, ab0.getType() );
        assertEquals( 12, ab0.value() );

        JsonNode ab1 = doc.get( "$.a.b[1]" );
        assertEquals( JsonNodeType.BOOLEAN, ab1.getType() );
        assertEquals( false, ab1.value() );
    }

    @Test
    public void testObject_NoSuchProperty()
    {
        JsonDocument doc = new JsonDocument( "{\"a\": { \"b\" : [12, false] } }" );

        JsonPathException ex = assertThrows( JsonPathException.class, () -> doc.get( ".a.notFound" ) );
        assertEquals( "Path `.a.notFound` does not exist, object `.a` does not have a property `notFound`",
            ex.getMessage() );
    }

    @Test
    public void testObject_NoSuchIndex()
    {
        JsonDocument doc = new JsonDocument( "{\"a\": { \"b\" : [12, false] } }" );

        JsonPathException ex = assertThrows( JsonPathException.class, () -> doc.get( ".a.b[3]" ) );
        assertEquals( "Path `.a.b[3]` does not exist, array `.a.b` has only `2` elements.",
            ex.getMessage() );
    }

    @Test
    public void testObject_WrongNodeTypeArray()
    {
        JsonDocument doc = new JsonDocument( "{\"a\": { \"b\" : 42 } }" );

        JsonPathException ex = assertThrows( JsonPathException.class, () -> doc.get( ".a.b[1]" ) );
        assertEquals( "Path `.a.b[1]` does not exist, parent `.a.b` is not an ARRAY but a NUMBER node.",
            ex.getMessage() );
    }

    @Test
    public void testObject_WrongNodeTypeObject()
    {
        JsonDocument doc = new JsonDocument( "{\"a\": 42 }" );

        JsonPathException ex = assertThrows( JsonPathException.class, () -> doc.get( ".a.b.[1]" ) );
        assertEquals( "Path `.a.b.[1]` does not exist, parent `.a` is not an OBJECT but a NUMBER node.",
            ex.getMessage() );
    }

    @Test
    public void testString_MissingQuotes()
    {
        JsonDocument doc = new JsonDocument( "{\"a\": hello }" );

        JsonFormatException ex = assertThrows( JsonFormatException.class, () -> doc.get( ".a" ) );
        assertEquals( "Unexpected character at position 6,\n" + "{\"a\": hello }\n" + "      ^ expected start of value",
            ex.getMessage() );
    }
}
