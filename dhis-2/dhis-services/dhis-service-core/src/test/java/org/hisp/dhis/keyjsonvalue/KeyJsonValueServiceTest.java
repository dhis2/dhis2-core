package org.hisp.dhis.keyjsonvalue;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class KeyJsonValueServiceTest
    extends DhisSpringTest
{
    private final String namespace = "DOGS";
    
    @Autowired
    private KeyJsonValueService keyJsonValueService;
    
    @Test
    public void testAddGetObject()
    {
        Dog dogA = new Dog( "1", "Fido", "Brown" );
        Dog dogB = new Dog( "2", "Aldo", "Black" );
        
        keyJsonValueService.addValue( namespace, dogA.getId(), dogA );
        keyJsonValueService.addValue( namespace, dogB.getId(), dogB );
        
        dogA = keyJsonValueService.getValue( namespace, dogA.getId(), Dog.class );
        dogB = keyJsonValueService.getValue( namespace, dogB.getId(), Dog.class );
        
        assertNotNull( dogA );        
        assertEquals( "1", dogA.getId() );
        assertEquals( "Fido", dogA.getName() );
        assertNotNull( dogB );
        assertEquals( "2", dogB.getId() );
        assertEquals( "Aldo", dogB.getName() );
    }

    @Test
    public void testAddUpdateObject()
    {
        Dog dogA = new Dog( "1", "Fido", "Brown" );
        Dog dogB = new Dog( "2", "Aldo", "Black" );
        
        keyJsonValueService.addValue( namespace, dogA.getId(), dogA );
        keyJsonValueService.addValue( namespace, dogB.getId(), dogB );

        dogA = keyJsonValueService.getValue( namespace, dogA.getId(), Dog.class );
        dogB = keyJsonValueService.getValue( namespace, dogB.getId(), Dog.class );

        assertEquals( "Fido", dogA.getName() );
        assertEquals( "Aldo", dogB.getName() );
        
        dogA.setName( "Lilly" );
        dogB.setName( "Teddy" );
        
        keyJsonValueService.updateValue( namespace, dogA.getId(), dogA );
        keyJsonValueService.updateValue( namespace, dogB.getId(), dogB );

        dogA = keyJsonValueService.getValue( namespace, dogA.getId(), Dog.class );
        dogB = keyJsonValueService.getValue( namespace, dogB.getId(), Dog.class );

        assertEquals( "Lilly", dogA.getName() );
        assertEquals( "Teddy", dogB.getName() );
    }
}
