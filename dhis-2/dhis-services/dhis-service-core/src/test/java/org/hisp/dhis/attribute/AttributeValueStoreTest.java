package org.hisp.dhis.attribute;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AttributeValueStoreTest
    extends DhisSpringTest
{
    @Autowired
    private AttributeValueStore attributeValueStore;

    @Autowired
    private AttributeStore attributeStore;

    @Autowired
    private IdentifiableObjectManager manager;

    private AttributeValue avA;
    private AttributeValue avB;

    private Attribute atA;

    @Override
    protected void setUpTest()
    {
        atA = new Attribute();
        atA.setName( "attribute_simple" );
        atA.setValueType( ValueType.TEXT );

        attributeStore.save( atA );

        avA = new AttributeValue( "value 1" );
        avA.setAttribute( atA );

        avB = new AttributeValue( "value 2" );
        avB.setAttribute( atA );

        attributeValueStore.save( avA );
        attributeValueStore.save( avB );
    }

    @Test
    public void testGetAttribute()
    {
        AttributeValue av = attributeValueStore.get( avA.getId() );

        assertNotNull( av );
        assertNotNull( av.getAttribute() );
    }

    @Test
    public void testGetValue()
    {
        AttributeValue av = attributeValueStore.get( avA.getId() );

        assertNotNull( av );
        assertEquals( "value 1", av.getValue() );
    }

    @Test
    public void testGetAllByAttribute()
    {
        assertEquals( 2, attributeValueStore.getAllByAttribute( atA ).size() );
    }

    @Test
    public void testGetAllByAttributeAndValue()
    {
        assertEquals( 0, attributeValueStore.getAllByAttributeAndValue( atA, "null" ).size() );
        assertEquals( 1, attributeValueStore.getAllByAttributeAndValue( atA, "value 1" ).size() );
        assertEquals( 1, attributeValueStore.getAllByAttributeAndValue( atA, "value 2" ).size() );
    }

    @Test
    public void testIsAttributeValueUnique()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.getAttributeValues().add( avA );
        DataElement dataElementB = createDataElement( 'B' );

        manager.save( dataElementA );
        manager.save( dataElementB );

        assertTrue( attributeValueStore.isAttributeValueUnique( dataElementA, avA ) );
        assertFalse( attributeValueStore.isAttributeValueUnique( dataElementB, avA ) );
    }
}
