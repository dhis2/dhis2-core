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
package org.hisp.dhis.webapi.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link Expected} annotation feature.
 *
 * @author Jan Bernitt
 */
class JsonExpectedTest
{

    private interface JsonFoo extends JsonObject
    {

        @Expected
        default String getBar()
        {
            return getString( "bar" ).string();
        }
    }

    private interface JsonEntry extends JsonObject
    {

        @Expected
        default String getKey()
        {
            return getString( "key" ).string();
        }

        @Expected
        default Number getValue()
        {
            return getNumber( "value" ).number();
        }
    }

    private interface JsonRoot extends JsonObject
    {

        @Expected( nullable = true )
        default JsonFoo getA()
        {
            return get( "a", JsonFoo.class );
        }

        @Expected
        default JsonFoo getB()
        {
            return get( "b", JsonFoo.class );
        }
    }

    @Test
    void testIsA()
    {
        assertTrue( createJSON( "{'bar':'x'}" ).isA( JsonFoo.class ) );
        assertTrue( createJSON( "{'key':'x', 'value': 1}" ).isA( JsonEntry.class ) );
        JsonResponse both = createJSON( "{'key':'x', 'value': 1, 'bar':'y'}" );
        assertTrue( both.isA( JsonFoo.class ) );
        assertTrue( both.isA( JsonEntry.class ) );
    }

    @Test
    void testIsA_MissingMember()
    {
        assertFalse( createJSON( "{'bar':'x'}" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "{'key':'x', 'value': 1}" ).isA( JsonFoo.class ) );
    }

    @Test
    void testIsA_WrongNodeType()
    {
        assertFalse( createJSON( "{'bar':true}" ).isA( JsonFoo.class ) );
        assertFalse( createJSON( "{'key':'x', 'value': '1'}" ).isA( JsonEntry.class ) );
    }

    @Test
    void testIsA_NotAnObject()
    {
        assertFalse( createJSON( "[]" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "'test'" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "12" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "false" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "null" ).isA( JsonEntry.class ) );
    }

    @Test
    void testAsObject()
    {
        assertAsObject( JsonFoo.class, "{'bar': 'yes'}" );
        assertAsObject( JsonEntry.class, "{'key':'x', 'value': 42}" );
        String both = "{'key':'x', 'value': 1, 'bar':'y'}";
        assertAsObject( JsonFoo.class, both );
        assertAsObject( JsonEntry.class, both );
    }

    @Test
    void testAsObject_Nullable()
    {
        assertAsObject( JsonRoot.class, "{'a':null,'b':{'bar':'x'}}" );
    }

    @Test
    void testAsObject_NotAnObjectArray()
    {
        assertAsObjectThrows( JsonFoo.class, "[]", "Expected  JsonFoo node is not an object but a ARRAY" );
    }

    @Test
    void testAsObject_NotAnObjectString()
    {
        assertAsObjectThrows( JsonFoo.class, "'nop'", "Expected  JsonFoo node is not an object but a STRING" );
    }

    @Test
    void testAsObject_NotAnObjectNumber()
    {
        assertAsObjectThrows( JsonFoo.class, "13", "Expected  JsonFoo node is not an object but a NUMBER" );
    }

    @Test
    void testAsObject_NotAnObjectBoolean()
    {
        assertAsObjectThrows( JsonFoo.class, "true", "Expected  JsonFoo node is not an object but a BOOLEAN" );
    }

    @Test
    void testAsObject_NotAnObjectNull()
    {
        assertAsObjectThrows( JsonFoo.class, "null", "Expected  JsonFoo node is not an object but a NULL" );
    }

    @Test
    void testAsObject_NotAnObjectUndefined()
    {
        assertAsObjectThrows( () -> createJSON( "{}" ).getObject( "x" ).asObject( JsonFoo.class ),
            "Expected  JsonFoo node does not exist" );
    }

    @Test
    void testAsObject_MissingMemberUndefined()
    {
        assertAsObjectThrows( JsonRoot.class, "{'b':{'bar':''}}",
            "Expected JsonRoot node member getA was not defined" );
    }

    @Test
    void testAsObject_MissingMemberRecursive()
    {
        assertAsObjectThrows( JsonRoot.class, "{'a': {}, 'b':{'bar':''}}",
            "Expected JsonFoo node member getA.getBar was not defined" );
    }

    @Test
    void testAsObject_NotAnObjectRecursive()
    {
        assertAsObjectThrows( JsonRoot.class, "{'a': [], 'b':{'bar':''}}",
            "Expected getA JsonFoo node is not an object but a ARRAY" );
    }

    private static void assertAsObjectThrows( Class<? extends JsonObject> of, String actualJson,
        String expectedMessage )
    {
        assertAsObjectThrows( () -> assertNotNull( createJSON( actualJson ).asObject( of ) ), expectedMessage );
    }

    private static void assertAsObjectThrows( Runnable test, String expectedMessage )
    {
        try
        {
            test.run();
        }
        catch ( NoSuchElementException ex )
        {
            assertEquals( expectedMessage, ex.getMessage() );
            return;
        }
        fail( "Expected NoSuchElementException with message: " + expectedMessage );
    }

    private static void assertAsObject( Class<? extends JsonObject> of, String actualJson )
    {
        JsonObject obj = createJSON( actualJson ).asObject( of );
        assertNotNull( obj );
        assertTrue( of.isInstance( obj ) );
    }

    private static JsonResponse createJSON( String content )
    {
        return new JsonResponse( content.replace( '\'', '"' ) );
    }
}
