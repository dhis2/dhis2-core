package org.hisp.dhis.webapi.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Tests the {@link Expected} annotation feature.
 *
 * @author Jan Bernitt
 */
public class JsonExpectedTest
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
    public void testIsA()
    {
        assertTrue( createJSON( "{'bar':'x'}" ).isA( JsonFoo.class ) );
        assertTrue( createJSON( "{'key':'x', 'value': 1}" ).isA( JsonEntry.class ) );
        JsonResponse both = createJSON( "{'key':'x', 'value': 1, 'bar':'y'}" );
        assertTrue( both.isA( JsonFoo.class ) );
        assertTrue( both.isA( JsonEntry.class ) );
    }

    @Test
    public void testIsA_MissingMember()
    {
        assertFalse( createJSON( "{'bar':'x'}" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "{'key':'x', 'value': 1}" ).isA( JsonFoo.class ) );
    }

    @Test
    public void testIsA_WrongNodeType()
    {
        assertFalse( createJSON( "{'bar':true}" ).isA( JsonFoo.class ) );
        assertFalse( createJSON( "{'key':'x', 'value': '1'}" ).isA( JsonEntry.class ) );
    }

    @Test
    public void testIsA_NotAnObject()
    {
        assertFalse( createJSON( "[]" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "'test'" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "12" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "false" ).isA( JsonEntry.class ) );
        assertFalse( createJSON( "null" ).isA( JsonEntry.class ) );
    }

    @Test
    public void testAsObject()
    {
        assertAsObject( JsonFoo.class, "{'bar': 'yes'}" );
        assertAsObject( JsonEntry.class, "{'key':'x', 'value': 42}" );
        String both = "{'key':'x', 'value': 1, 'bar':'y'}";
        assertAsObject( JsonFoo.class, both );
        assertAsObject( JsonEntry.class, both );
    }

    @Test
    public void testAsObject_Nullable()
    {
        assertAsObject( JsonRoot.class, "{'a':null,'b':{'bar':'x'}}" );
    }

    @Test
    public void testAsObject_NotAnObjectArray()
    {
        assertAsObjectThrows( JsonFoo.class, "[]", "Expected  JsonFoo node is not an object but a ARRAY" );
    }

    @Test
    public void testAsObject_NotAnObjectString()
    {
        assertAsObjectThrows( JsonFoo.class, "'nop'", "Expected  JsonFoo node is not an object but a STRING" );
    }

    @Test
    public void testAsObject_NotAnObjectNumber()
    {
        assertAsObjectThrows( JsonFoo.class, "13", "Expected  JsonFoo node is not an object but a NUMBER" );
    }

    @Test
    public void testAsObject_NotAnObjectBoolean()
    {
        assertAsObjectThrows( JsonFoo.class, "true", "Expected  JsonFoo node is not an object but a BOOLEAN" );
    }

    @Test
    public void testAsObject_NotAnObjectNull()
    {
        assertAsObjectThrows( JsonFoo.class, "null", "Expected  JsonFoo node is not an object but a NULL" );
    }

    @Test
    public void testAsObject_NotAnObjectUndefined()
    {
        assertAsObjectThrows( () -> createJSON( "{}" ).getObject( "x" ).asObject( JsonFoo.class ),
            "Expected  JsonFoo node does not exist" );
    }

    @Test
    public void testAsObject_MissingMemberUndefined()
    {
        assertAsObjectThrows( JsonRoot.class, "{'b':{'bar':''}}",
            "Expected JsonRoot node member getA was not defined" );
    }

    @Test
    public void testAsObject_MissingMemberRecursive()
    {
        assertAsObjectThrows( JsonRoot.class, "{'a': {}, 'b':{'bar':''}}",
            "Expected JsonFoo node member getA.getBar was not defined" );
    }

    @Test
    public void testAsObject_NotAnObjectRecursive()
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
