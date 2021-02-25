package org.hisp.dhis.webapi.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

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
        JsonObject response = new JsonResponse( "{'user': {'id':'foo'}}" );

        assertEquals( "foo", response.get( "user", JsonUser.class ).getId() );
    }

    @Test
    public void testCustomObjectTypeList()
    {
        JsonObject response = new JsonResponse( "{'users': [ {'id':'foo'} ]}" );

        JsonList<JsonUser> users = response.getList( "users", JsonUser.class );
        assertEquals( "foo", users.get( 0 ).getId() );
    }

    @Test
    public void testDateType()
    {
        JsonObject response = new JsonResponse( "{'user': {'lastUpdated': '2021-01-21T15:14:54.000'}}" );

        assertEquals( LocalDateTime.of( 2021, 1, 21, 15, 14, 54 ),
            response.get( "user", JsonUser.class ).getLastUpdated() );
    }

    @Test
    public void testNumber()
    {
        JsonObject response = new JsonResponse( "{'number': 13, 'fraction': 4.2}" );
        assertEquals( 13, response.getNumber( "number" ).intValue() );
        assertEquals( 4.2f, response.getNumber( "fraction" ).floatValue(), 0.001f );
        assertTrue( response.getNumber( "number" ).exists() );
    }

    @Test
    public void testBoolean()
    {
        JsonObject response = new JsonResponse( "{'flag': true}" );
        assertTrue( response.getBoolean( "flag" ).booleanValue() );
        assertTrue( response.getBoolean( "flag" ).exists() );
    }

    @Test
    public void testNotExists()
    {
        JsonObject response = new JsonResponse( "{'flag': true}" );

        assertFalse( response.getString( "no" ).exists() );
    }

    @Test
    public void testSizeArray()
    {
        JsonObject response = new JsonResponse( "{'numbers': [1,2,3,4]}" );

        assertEquals( 4, response.getArray( "numbers" ).size() );
        assertFalse( response.getArray( "numbers" ).isNull() );
    }

    @Test
    public void testIsNull()
    {
        JsonObject response = new JsonResponse( "{'optional': null }" );

        assertTrue( response.getArray( "optional" ).isNull() );
    }

}
