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
import org.hisp.dhis.common.ValueType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AttributeServiceTest
    extends DhisSpringTest
{
    @Autowired
    private AttributeService attributeService;

    @Test
    public void testAddAttribute()
    {
        Attribute attribute = new Attribute();
        attribute.setValueType( ValueType.TEXT );
        attribute.setName( "attribute1" );

        attributeService.addAttribute( attribute );
        attribute = attributeService.getAttribute( attribute.getId() );

        assertNotNull( attribute );
        assertEquals( ValueType.TEXT, attribute.getValueType() );
        assertEquals( "attribute1", attribute.getName() );
    }

    @Test
    public void testUpdateAttribute()
    {
        Attribute attribute = new Attribute();
        attribute.setValueType( ValueType.TEXT );
        attribute.setName( "attribute1" );

        attributeService.addAttribute( attribute );

        attribute.setValueType( ValueType.INTEGER );
        attribute.setName( "attribute2" );

        attributeService.updateAttribute( attribute );
        attribute = attributeService.getAttribute( attribute.getId() );

        assertEquals( ValueType.INTEGER, attribute.getValueType() );
        assertEquals( "attribute2", attribute.getName() );
    }

    @Test
    public void testDeleteAttribute()
    {
        Attribute attribute = new Attribute();
        attribute.setValueType( ValueType.TEXT );
        attribute.setName( "attribute1" );

        attributeService.addAttribute( attribute );
        attribute = attributeService.getAttribute( attribute.getId() );

        assertNotNull( attribute );

        int attributeId = attribute.getId();

        attributeService.deleteAttribute( attribute );
        attribute = attributeService.getAttribute( attributeId );

        assertNull( attribute );
    }

    @Test
    public void testGetAttribute()
    {
        Attribute attribute = new Attribute();
        attribute.setValueType( ValueType.TEXT );
        attribute.setName( "attribute1" );

        attributeService.addAttribute( attribute );
        attribute = attributeService.getAttribute( attribute.getId() );

        assertNotNull( attribute );
    }

    @Test
    public void testGetAllAttributes()
    {
        Attribute attribute1 = new Attribute();
        attribute1.setValueType( ValueType.TEXT );
        attribute1.setName( "attribute1" );

        Attribute attribute2 = new Attribute();
        attribute2.setValueType( ValueType.TEXT );
        attribute2.setName( "attribute2" );

        attributeService.addAttribute( attribute1 );
        attributeService.addAttribute( attribute2 );

        assertEquals( 2, attributeService.getAllAttributes().size() );
    }
}
