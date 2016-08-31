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

import org.hisp.dhis.node.annotation.NodeCollection;
import org.hisp.dhis.node.annotation.NodeRoot;
import org.hisp.dhis.node.annotation.NodeSimple;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@NodeRoot( value = "collectionItem" ) class Item
{
    @NodeSimple
    private String value;
}

class CollectionFields
{
    @NodeCollection
    private List<String> property = new ArrayList<>();

    @NodeCollection( value = "renamedProperty" )
    private List<String> propertyToBeRenamed = new ArrayList<>();

    @NodeCollection( isReadable = true, isWritable = false )
    private List<String> readOnly = new ArrayList<>();

    @NodeCollection( isReadable = false, isWritable = true )
    private List<String> writeOnly = new ArrayList<>();

    @NodeCollection( namespace = "http://ns.example.org" )
    private List<String> propertyWithNamespace = new ArrayList<>();

    public List<String> getProperty()
    {
        return property;
    }

    public void setProperty( List<String> property )
    {
        this.property = property;
    }

    @NodeCollection
    private List<Item> items1 = new ArrayList<>();

    @NodeCollection( value = "items", itemName = "item" )
    private List<Item> items2 = new ArrayList<>();
}

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class FieldCollectionNodePropertyIntrospectorServiceTest
{
    private Map<String, Property> propertyMap;

    @Before
    public void setup()
    {
        propertyMap = new NodePropertyIntrospectorService().scanClass( CollectionFields.class );
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
        assertTrue( propertyMap.containsKey( "items1" ) );
        assertTrue( propertyMap.containsKey( "items" ) );
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
        assertEquals( "items2", propertyMap.get( "items" ).getFieldName() );
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

    @Test
    public void testItemName()
    {
        assertEquals( "collectionItem", propertyMap.get( "items1" ).getName() );
        assertEquals( "items1", propertyMap.get( "items1" ).getCollectionName() );

        assertEquals( "item", propertyMap.get( "items" ).getName() );
        assertEquals( "items", propertyMap.get( "items" ).getCollectionName() );
    }

    @Test
    public void testItemKlass()
    {
        assertTrue( Item.class.equals( propertyMap.get( "items1" ).getItemKlass() ) );
    }
}
