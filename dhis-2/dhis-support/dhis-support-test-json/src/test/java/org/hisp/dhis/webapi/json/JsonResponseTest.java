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
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the basic correctness of {@link JsonResponse} which is the
 * implementation of all core interfaces of the {@link JsonValue} utility.
 *
 * @author Jan Bernitt
 */
public class JsonResponseTest
{
    @Test
    public void testCustomObjectTypeMultiMap()
    {
        JsonMultiMap<JsonNumber> multiMap = createJSON( "{'foo':[1,23], 'bar': [34,56]}" )
            .asMultiMap( JsonNumber.class );
        assertFalse( multiMap.isEmpty() );
        assertTrue( multiMap.isObject() );
        assertEquals( 23, multiMap.get( "foo" ).get( 1 ).intValue() );
        assertEquals( 34, multiMap.get( "bar" ).get( 0 ).intValue() );
    }

    @Test
    public void testObjectHas()
    {
        JsonObject response = createJSON( "{'users': {'foo':{'id':'foo'}, 'bar':[]}}" );
        assertTrue( response.has( "users" ) );
        assertTrue( response.getObject( "users" ).has( "foo", "bar" ) );
        assertFalse( response.has( "no-a-member" ) );
        assertFalse( createJSON( "[]" ).getObject( "undefined" ).has( "foo" ) );
        JsonObject bar = response.getObject( "users" ).getObject( "bar" );
        Exception ex = Assert.assertThrows( UnsupportedOperationException.class, () -> bar.has( "is-array" ) );
        Assert.assertEquals( "Path `$.users.bar` does not contain a OBJECT but a(n) ARRAY: []", ex.getMessage() );
    }

    @Test
    public void testNumber()
    {
        JsonObject response = createJSON( "{'number': 13, 'fraction': 4.2}" );

        assertEquals( 13, response.getNumber( "number" ).number() );
        assertEquals( 4.2f, response.getNumber( "fraction" ).number().floatValue(), 0.001f );
        assertTrue( response.getNumber( "number" ).exists() );
        Assert.assertNull( response.getNumber( "missing" ).number() );
    }

    @Test
    public void testIntValue()
    {
        JsonObject response = createJSON( "{'number':13}" );
        assertEquals( 13, response.getNumber( "number" ).intValue() );
        JsonNumber missing = response.getNumber( "missing" );
        Assert.assertThrows( NoSuchElementException.class, missing::intValue );
    }

    @Test
    public void testString()
    {
        JsonObject response = createJSON( "{'text': 'plain'}" );

        assertEquals( "plain", response.getString( "text" ).string() );
        assertTrue( response.getString( "text" ).exists() );
        Assert.assertNull( response.getString( "missing" ).string() );
    }

    @Test
    public void testBool()
    {
        JsonObject response = createJSON( "{'flag': true}" );

        assertTrue( response.getBoolean( "flag" ).bool() );
        assertTrue( response.getBoolean( "flag" ).exists() );
        Assert.assertNull( response.getBoolean( "missing" ).bool() );
    }

    @Test
    public void testBooleanValue()
    {
        JsonObject response = createJSON( "{'flag': true}" );

        assertTrue( response.getBoolean( "flag" ).booleanValue() );
        JsonBoolean missing = response.getBoolean( "missing" );
        Assert.assertThrows( NoSuchElementException.class, missing::booleanValue );
    }

    @Test
    public void testNotExists()
    {
        JsonObject response = createJSON( "{'flag': true}" );

        assertFalse( response.getString( "no" ).exists() );
    }

    @Test
    public void testSizeArray()
    {
        JsonObject response = createJSON( "{'numbers': [1,2,3,4]}" );

        assertEquals( 4, response.getArray( "numbers" ).size() );
        assertFalse( response.getArray( "numbers" ).isNull() );
    }

    @Test
    public void testStringValues()
    {
        JsonObject response = createJSON( "{'letters': ['a','b','c']}" );

        assertEquals( asList( "a", "b", "c" ), response.getArray( "letters" ).stringValues() );
    }

    @Test
    public void testNumberValues()
    {
        JsonObject response = createJSON( "{'digits': [1,2,3]}" );

        assertEquals( asList( 1, 2, 3 ), response.getArray( "digits" ).numberValues() );
    }

    @Test
    public void testBoolValues()
    {
        JsonObject response = createJSON( "{'flags': [true, false, true]}" );

        assertEquals( asList( true, false, true ), response.getArray( "flags" ).boolValues() );
    }

    @Test
    public void testIsNull()
    {
        JsonObject response = createJSON( "{'optional': null }" );

        assertTrue( response.getArray( "optional" ).isNull() );
    }

    @Test
    public void testIsArray()
    {
        JsonObject response = createJSON( "{'array': [], 'notAnArray': 42 }" );

        assertTrue( createJSON( "[]" ).isArray() );
        assertTrue( response.getArray( "array" ).isArray() );
        assertFalse( response.getArray( "notAnArray" ).isArray() );
        JsonArray missing = response.getArray( "missing" );
        Assert.assertThrows( NoSuchElementException.class, missing::isArray );
    }

    @Test
    public void testIsObject()
    {
        JsonObject response = createJSON( "{'object': {}, 'notAnObject': 42 }" );

        assertTrue( response.isObject() );
        assertTrue( response.getArray( "object" ).isObject() );
        assertFalse( response.getArray( "notAnObject" ).isObject() );
        JsonArray missing = response.getArray( "missing" );
        Assert.assertThrows( NoSuchElementException.class, missing::isObject );
    }

    @Test
    public void testBooleanNode()
    {
        JsonObject response = createJSON( "{'a': true }" );
        assertEquals( "true", response.getBoolean( "a" ).node().getDeclaration() );
    }

    @Test
    public void testNumberNode()
    {
        JsonObject response = createJSON( "{'a': 42 }" );
        assertEquals( "42", response.getNumber( "a" ).node().getDeclaration() );
    }

    @Test
    public void testStringNode()
    {
        JsonObject response = createJSON( "{'a': 'hello, again' }" );
        assertEquals( "\"hello, again\"", response.getString( "a" ).node().getDeclaration() );
    }

    @Test
    public void testArrayNode()
    {
        JsonObject response = createJSON( "{'a': ['hello, again', 12] }" );
        assertEquals( "[\"hello, again\", 12]", response.getArray( "a" ).node().getDeclaration() );
    }

    @Test
    public void testObjectNode()
    {
        JsonObject response = createJSON( "{'a': ['hello, again', 12] }" );
        assertEquals( "{\"a\": [\"hello, again\", 12] }", response.node().getDeclaration() );
    }

    @Test
    public void testToString()
    {
        JsonList<JsonNumber> list = createJSON( "[12,42]" ).asList( JsonNumber.class );
        assertEquals( "[12,42]", list.toString() );
        JsonMap<JsonNumber> map = createJSON( "{'a':12,'b':42}" ).asMap( JsonNumber.class );
        assertEquals( "{\"a\":12,\"b\":42}", map.toString() );
    }

    @Test
    public void testToString_NonExistingPath()
    {
        JsonList<JsonNumber> list = createJSON( "[12,42]" ).getObject( "non-existing" ).asList( JsonNumber.class );
        assertEquals( "Path `.non-existing` does not exist, parent `` is not an OBJECT but a ARRAY node.",
            list.toString() );
    }

    @Test
    public void testToString_MalformedJson()
    {
        JsonMap<JsonNumber> map = createJSON( "{'a:12}" ).asMap( JsonNumber.class );
        assertEquals( "Expected \" but reach EOI: {\"a:12}", map.toString() );
    }

    private JsonResponse createJSON( String content )
    {
        return new JsonResponse( content.replace( '\'', '"' ) );
    }
}
