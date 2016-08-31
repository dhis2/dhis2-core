package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.node.annotation.NodeComplex;
import org.hisp.dhis.node.annotation.NodeSimple;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

class Value
{
    @NodeSimple
    private String value;
}

class ComplexFields
{
    @NodeComplex
    private Value property;

    @NodeComplex( value = "renamedProperty" )
    private Value propertyToBeRenamed;

    @NodeComplex( isReadable = true, isWritable = false )
    private Value readOnly;

    @NodeComplex( isReadable = false, isWritable = true )
    private Value writeOnly;

    @NodeComplex( namespace = "http://ns.example.org" )
    private Value propertyWithNamespace;

    public Value getProperty()
    {
        return property;
    }

    public void setProperty( Value property )
    {
        this.property = property;
    }
}

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class FieldComplexNodePropertyIntrospectorServiceTest
{
    private Map<String, Property> propertyMap;

    @Before
    public void setup()
    {
        propertyMap = new NodePropertyIntrospectorService().scanClass( ComplexFields.class );
    }

    @Test
    public void testContainsKey()
    {
        assertTrue( propertyMap.containsKey( "property" ) );
        assertFalse( propertyMap.containsKey( "propertyToBeRenamed" ) );
        assertTrue( propertyMap.containsKey( "renamedProperty" ) );
        assertTrue( propertyMap.containsKey( "readOnly" ) );
        assertTrue( propertyMap.containsKey( "writeOnly" ) );
        assertTrue( propertyMap.containsKey( "propertyWithNamespace" ) );
    }

    @Test
    public void testReadWrite()
    {
        assertTrue( propertyMap.get( "readOnly" ).isReadable() );
        assertFalse( propertyMap.get( "readOnly" ).isWritable() );

        assertFalse( propertyMap.get( "writeOnly" ).isReadable() );
        assertTrue( propertyMap.get( "writeOnly" ).isWritable() );

        assertNull( propertyMap.get( "readOnly" ).getSetterMethod() );
        assertNull( propertyMap.get( "writeOnly" ).getGetterMethod() );
    }

    @Test
    public void testFieldName()
    {
        assertEquals( "property", propertyMap.get( "property" ).getFieldName() );
        assertEquals( "propertyToBeRenamed", propertyMap.get( "renamedProperty" ).getFieldName() );
    }

    @Test
    public void testNamespace()
    {
        assertEquals( "http://ns.example.org", propertyMap.get( "propertyWithNamespace" ).getNamespace() );
    }

    @Test
    public void testGetter()
    {
        assertNotNull( propertyMap.get( "property" ).getGetterMethod() );
        assertNull( propertyMap.get( "renamedProperty" ).getGetterMethod() );
    }

    @Test
    public void testSetter()
    {
        assertNotNull( propertyMap.get( "property" ).getSetterMethod() );
        assertNull( propertyMap.get( "renamedProperty" ).getSetterMethod() );
    }
}
