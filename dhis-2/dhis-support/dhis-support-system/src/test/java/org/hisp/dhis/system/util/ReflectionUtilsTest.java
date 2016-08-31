package org.hisp.dhis.system.util;

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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.hisp.dhis.system.util.ReflectionUtils.*;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class ReflectionUtilsTest
{
    private DataElement dataElementA;

    @Before
    public void before()
    {
        dataElementA = new DataElement();
        dataElementA.setId( 8 );
        dataElementA.setName( "NameA" );
        dataElementA.setAggregationType( AggregationType.SUM );
    }

    @Test
    public void testGetId()
    {
        assertEquals( 8, getId( dataElementA ) );
    }

    @Test
    public void testGetProperty()
    {
        assertEquals( "NameA", getProperty( dataElementA, "name" ) );
        assertNull( getProperty( dataElementA, "color" ) );
    }

    @Test
    public void testSetProperty()
    {
        setProperty( dataElementA, "shortName", "ShortNameA" );

        assertEquals( "ShortNameA", dataElementA.getShortName() );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testSetPropertyException()
    {
        setProperty( dataElementA, "color", "Blue" );
    }

    @Test
    public void testGetClassName()
    {
        assertEquals( "DataElement", getClassName( dataElementA ) );
    }

    @Test
    public void testIsCollection()
    {
        List<Object> colA = new ArrayList<>();
        Collection<DataElement> colB = new HashSet<>();
        Collection<DataElement> colC = new ArrayList<>();

        assertTrue( isCollection( colA ) );
        assertTrue( isCollection( colB ) );
        assertTrue( isCollection( colC ) );
        assertFalse( isCollection( dataElementA ) );
    }
}
