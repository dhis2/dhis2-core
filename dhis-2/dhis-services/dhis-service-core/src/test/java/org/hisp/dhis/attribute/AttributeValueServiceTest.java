package org.hisp.dhis.attribute;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.hisp.dhis.common.ValueType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AttributeValueServiceTest
    extends DhisSpringTest
{
    @Autowired
    private AttributeService attributeService;

    private AttributeValue attributeValue1;

    private AttributeValue attributeValue2;

    @Override
    protected void setUpTest()
    {
        attributeValue1 = new AttributeValue( "value 1" );
        attributeValue2 = new AttributeValue( "value 2" );

        Attribute attribute1 = new Attribute( "attribute 1", ValueType.TEXT );
        Attribute attribute2 = new Attribute( "attribute 2", ValueType.TEXT );

        attributeService.addAttribute( attribute1 );
        attributeService.addAttribute( attribute2 );

        attributeValue1.setAttribute( attribute1 );
        attributeValue2.setAttribute( attribute2 );

        attributeService.addAttributeValue( attributeValue1 );
        attributeService.addAttributeValue( attributeValue2 );
    }

    @Test
    public void testAddAttributeValue()
    {
        attributeValue1 = attributeService.getAttributeValue( attributeValue1.getId() );
        attributeValue2 = attributeService.getAttributeValue( attributeValue2.getId() );

        assertNotNull( attributeValue1 );
        assertNotNull( attributeValue2 );
    }

    @Test
    public void testUpdateAttributeValue()
    {
        attributeValue1.setValue( "updated value 1" );
        attributeValue2.setValue( "updated value 2" );

        attributeService.updateAttributeValue( attributeValue1 );
        attributeService.updateAttributeValue( attributeValue2 );

        attributeValue1 = attributeService.getAttributeValue( attributeValue1.getId() );
        attributeValue2 = attributeService.getAttributeValue( attributeValue2.getId() );

        assertNotNull( attributeValue1 );
        assertNotNull( attributeValue2 );

        assertEquals( "updated value 1", attributeValue1.getValue() );
        assertEquals( "updated value 2", attributeValue2.getValue() );
    }

    @Test
    public void testDeleteAttributeValue()
    {
        int attributeValueId1 = attributeValue1.getId();
        int attributeValueId2 = attributeValue2.getId();

        attributeService.deleteAttributeValue( attributeValue1 );
        attributeService.deleteAttributeValue( attributeValue2 );

        attributeValue1 = attributeService.getAttributeValue( attributeValueId1 );
        attributeValue2 = attributeService.getAttributeValue( attributeValueId2 );

        assertNull( attributeValue1 );
        assertNull( attributeValue2 );
    }

    @Test
    public void testGetAttributeValue()
    {
        attributeValue1 = attributeService.getAttributeValue( attributeValue1.getId() );
        attributeValue2 = attributeService.getAttributeValue( attributeValue2.getId() );

        assertNotNull( attributeValue1 );
        assertNotNull( attributeValue2 );
    }
}
