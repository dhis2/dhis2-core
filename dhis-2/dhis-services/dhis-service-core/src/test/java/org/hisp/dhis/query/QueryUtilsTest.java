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
package org.hisp.dhis.query;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryUtilsTest
{
    private Schema schema;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp()
        throws Exception
    {
        schema = new Schema( Attribute.class, "attribute", "attributes" );

        Property property = new Property( String.class );
        property.setName( "value1" );
        property.setSimple( true );
        schema.addProperty( property );

        property = new Property( String.class );
        property.setName( "value2" );
        property.setSimple( false );
        schema.addProperty( property );

        property = new Property( String.class );
        property.setName( "value3" );
        property.setSimple( true );
        schema.addProperty( property );

        property = new Property( Integer.class );
        property.setName( "value4" );
        property.setSimple( true );
        schema.addProperty( property );

        property = new Property( String.class );
        property.setName( "value5" );
        property.setSimple( true );
        schema.addProperty( property );

        property = new Property( String.class );
        property.setName( "value6" );
        property.setSimple( true );
        schema.addProperty( property );

        property = new Property( String.class );
        property.setName( "value7" );
        property.setSimple( true );
        schema.addProperty( property );
    }

    @Test
    public void testParseValidEnum()
    {
        assertNotNull( QueryUtils.parseValue( ValueType.class, "INTEGER" ) );
        assertNotNull( QueryUtils.parseValue( ValueType.class, "TEXT" ) );
    }

    @Test
    public void testParseValidInteger()
    {
        Integer value1 = QueryUtils.parseValue( Integer.class, "10" );
        Integer value2 = QueryUtils.parseValue( Integer.class, "100" );

        assertNotNull( value1 );
        assertNotNull( value2 );

        assertSame( 10, value1 );
        assertSame( 100, value2 );
    }

    @Test( expected = QueryParserException.class )
    public void testParseInvalidEnum()
    {
        QueryUtils.parseValue( ValueType.class, "INTEGER" );
        QueryUtils.parseValue( ValueType.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidInteger()
    {
        QueryUtils.parseValue( Integer.class, "1" );
        QueryUtils.parseValue( Integer.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidFloat()
    {
        QueryUtils.parseValue( Float.class, "1.2" );
        QueryUtils.parseValue( Float.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidDouble()
    {
        QueryUtils.parseValue( Double.class, "1.2" );
        QueryUtils.parseValue( Double.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidDate()
    {
        QueryUtils.parseValue( Date.class, "2014" );
        QueryUtils.parseValue( Date.class, "ABC" );
    }

    @Test
    public void testParseValue()
    {
        assertEquals( "'abc'", QueryUtils.parseValue( "abc" ) );
        assertEquals( "123", QueryUtils.parseValue( "123" ) );
    }

    @Test
    public void testParserNotFound()
    {
        // Given
        final Class<User> nonSupportedClass = User.class;
        final String anyValue = "wewee-4343";

        // Then
        exceptionRule.expect( QueryParserException.class );
        exceptionRule
            .expectMessage( "Unable to parse `" + anyValue + "` to `" + nonSupportedClass.getSimpleName() + "`." );

        // When
        QueryUtils.parseValue( nonSupportedClass, anyValue );
    }

    @Test
    public void testParseSelectFields()
    {
        List<String> fields = new ArrayList<>();
        fields.add( "ABC" );
        fields.add( "DEF" );

        assertEquals( "ABC,DEF", QueryUtils.parseSelectFields( fields ) );
    }

    @Test
    public void testParseSelectFieldsNull()
    {
        assertEquals( " * ", QueryUtils.parseSelectFields( null ) );
    }

    @Test
    public void testTransformCollectionValue()
    {
        assertEquals( "('x','y')", QueryUtils.convertCollectionValue( "[x,y]" ) );

        assertEquals( "(1,2)", QueryUtils.convertCollectionValue( "[1,2]" ) );
    }

    @Test
    public void testParseFilterOperator()
    {
        assertEquals( "= 5", QueryUtils.parseFilterOperator( "eq", "5" ) );

        assertEquals( "= 'ABC'", QueryUtils.parseFilterOperator( "eq", "ABC" ) );

        assertEquals( "like '%abc%'", QueryUtils.parseFilterOperator( "like", "abc" ) );

        assertEquals( " like '%abc'", QueryUtils.parseFilterOperator( "$like", "abc" ) );

        assertEquals( "in ('a','b','c')", QueryUtils.parseFilterOperator( "in", "[a,b,c]" ) );

        assertEquals( "in (1,2,3)", QueryUtils.parseFilterOperator( "in", "[1,2,3]" ) );

        assertEquals( "is not null", QueryUtils.parseFilterOperator( "!null", null ) );
    }

    @Test
    public void testConvertOrderStringsNull()
    {
        Assert.assertEquals( Collections.emptyList(), QueryUtils.convertOrderStrings( null, schema ) );
    }

    @Test
    public void testConvertOrderStrings()
    {
        List<Order> orders = QueryUtils.convertOrderStrings( Arrays.asList( "value1:asc", "value2:asc", "value3:iasc",
            "value4:desc", "value5:idesc", "value6:xdesc", "value7" ), schema );
        Assert.assertEquals( 5, orders.size() );
        Assert.assertEquals( orders.get( 0 ), Order.from( "asc", schema.getProperty( "value1" ) ) );
        Assert.assertEquals( orders.get( 1 ), Order.from( "iasc", schema.getProperty( "value3" ) ) );
        Assert.assertEquals( orders.get( 2 ), Order.from( "desc", schema.getProperty( "value4" ) ) );
        Assert.assertEquals( orders.get( 3 ), Order.from( "idesc", schema.getProperty( "value5" ) ) );
        Assert.assertEquals( orders.get( 4 ), Order.from( "asc", schema.getProperty( "value7" ) ) );
    }
}
