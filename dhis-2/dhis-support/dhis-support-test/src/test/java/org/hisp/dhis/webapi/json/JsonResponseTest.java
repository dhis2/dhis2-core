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
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonUser;
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
    public void testCustomObjectType()
    {
        JsonObject response = createJSON( "{'user': {'id':'foo'}}" );

        assertEquals( "foo", response.get( "user", JsonUser.class ).getId() );
    }

    @Test
    public void testCustomObjectTypeList()
    {
        JsonObject response = createJSON( "{'users': [ {'id':'foo'} ]}" );

        JsonList<JsonUser> users = response.getList( "users", JsonUser.class );
        assertEquals( "foo", users.get( 0 ).getId() );
    }

    @Test
    public void testCustomObjectTypeMap()
    {
        JsonObject response = createJSON( "{'users': {'foo':{'id':'foo'}, 'bar':{'id':'bar'}}}" );
        JsonMap<JsonUser> usersById = response.getMap( "users", JsonUser.class );
        assertFalse( usersById.isEmpty() );
        assertEquals( 2, usersById.size() );
        assertEquals( "foo", usersById.get( "foo" ).getId() );
    }

    @Test
    public void testDateType()
    {
        JsonObject response = createJSON( "{'user': {'lastUpdated': '2021-01-21T15:14:54.000'}}" );

        JsonUser user = response.get( "user", JsonUser.class );
        assertEquals( LocalDateTime.of( 2021, 1, 21, 15, 14, 54 ),
            user.getLastUpdated() );
        assertNull( user.getCreated() );
    }

    @Test
    public void testNumber()
    {
        JsonObject response = createJSON( "{'number': 13, 'fraction': 4.2}" );

        assertEquals( 13, response.getNumber( "number" ).number() );
        assertEquals( 4.2f, response.getNumber( "fraction" ).number().floatValue(), 0.001f );
        assertTrue( response.getNumber( "number" ).exists() );
        assertNull( response.getNumber( "missing" ).number() );
    }

    @Test
    public void testIntValue()
    {
        JsonObject response = createJSON( "{'number':13}" );
        assertEquals( 13, response.getNumber( "number" ).intValue() );
        JsonNumber missing = response.getNumber( "missing" );
        assertThrows( NoSuchElementException.class, missing::intValue );
    }

    @Test
    public void testString()
    {
        JsonObject response = createJSON( "{'text': 'plain'}" );

        assertEquals( "plain", response.getString( "text" ).string() );
        assertTrue( response.getString( "text" ).exists() );
        assertNull( response.getString( "missing" ).string() );
    }

    @Test
    public void testBool()
    {
        JsonObject response = createJSON( "{'flag': true}" );

        assertTrue( response.getBoolean( "flag" ).bool() );
        assertTrue( response.getBoolean( "flag" ).exists() );
        assertNull( response.getBoolean( "missing" ).bool() );
    }

    @Test
    public void testBooleanValue()
    {
        JsonObject response = createJSON( "{'flag': true}" );

        assertTrue( response.getBoolean( "flag" ).booleanValue() );
        JsonBoolean missing = response.getBoolean( "missing" );
        assertThrows( NoSuchElementException.class, missing::booleanValue );
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
        assertThrows( NoSuchElementException.class, missing::isArray );
    }

    @Test
    public void testIsObject()
    {
        JsonObject response = createJSON( "{'object': {}, 'notAnObject': 42 }" );

        assertTrue( response.isObject() );
        assertTrue( response.getArray( "object" ).isObject() );
        assertFalse( response.getArray( "notAnObject" ).isObject() );
        JsonArray missing = response.getArray( "missing" );
        assertThrows( NoSuchElementException.class, missing::isObject );
    }

    @Test
    public void testErrorSummary_MessageOnly()
    {
        JsonObject response = createJSON( "{'message':'my message'}" );
        assertEquals( "my message", response.as( JsonError.class ).summary() );
    }

    @Test
    public void testErrorSummary_MessageAndErrorReports()
    {
        JsonObject response = createJSON(
            "{'message':'my message','response':{'errorReports': [{'errorCode':'E4000','message':'m1'}]}}" );
        assertEquals( "my message\n" + "  E4000 m1", response.as( JsonError.class ).summary() );
    }

    @Test
    public void testErrorSummary_MessageAndObjectReports()
    {
        JsonObject response = createJSON(
            "{'message':'my message','response':{'objectReports':[{'klass':'java.lang.String','errorReports': [{'errorCode':'E4000','message':'m1'}]}]}}" );
        assertEquals( "my message\n" + "* class java.lang.String\n" + "  E4000 m1",
            response.as( JsonError.class ).summary() );
    }

    private JsonResponse createJSON( String content )
    {
        return new JsonResponse( content.replace( '\'', '"' ) );
    }
}
