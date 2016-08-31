package org.hisp.dhis;

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

import java.util.Collection;

import org.hisp.dhis.common.GenericStore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@SuppressWarnings( "unchecked" )
public abstract class GenericDhisSpringTest<T>
    extends DhisSpringTest
{
    protected GenericStore<T> genericStore;
    
    @Override
    public final void setUpTest()
        throws Exception
    {
        genericStore = (GenericStore<T>) getBean( getGenericBeanId() );
        
        setUpGenericTest();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public final void genericSaveGet()
    {
        int idA = genericStore.save( getObjects()[0] );
        int idB = genericStore.save( getObjects()[1] );
        int idC = genericStore.save( getObjects()[2] );
        
        assertEquals( getObjects()[0], genericStore.get( idA ) );
        assertEquals( getObjects()[1], genericStore.get( idB ) );
        assertEquals( getObjects()[2], genericStore.get( idC ) );
    }

    @Test
    public final void genericLoadGet()
    {
        int idA = genericStore.save( getObjects()[0] );
        int idB = genericStore.save( getObjects()[1] );
        int idC = genericStore.save( getObjects()[2] );
        
        assertEquals( getObjects()[0], genericStore.get( idA ) );
        assertEquals( getObjects()[1], genericStore.get( idB ) );
        assertEquals( getObjects()[2], genericStore.get( idC ) );
    }
    
    @Test
    public final void genericDelete()
    {
        int idA = genericStore.save( getObjects()[0] );
        int idB = genericStore.save( getObjects()[1] );
        int idC = genericStore.save( getObjects()[2] );
        
        assertNotNull( genericStore.get( idA ) );
        assertNotNull( genericStore.get( idB ) );
        assertNotNull( genericStore.get( idC ) );
        
        genericStore.delete( getObjects()[0] );
        
        assertNull( genericStore.get( idA ) );
        assertNotNull( genericStore.get( idB ) );
        assertNotNull( genericStore.get( idC ) );

        genericStore.delete( getObjects()[1] );

        assertNull( genericStore.get( idA ) );
        assertNull( genericStore.get( idB ) );
        assertNotNull( genericStore.get( idC ) );

        genericStore.delete( getObjects()[2] );

        assertNull( genericStore.get( idA ) );
        assertNull( genericStore.get( idB ) );
        assertNull( genericStore.get( idC ) );
    }
    
    @Test
    public final void genericGetAll()
    {
        genericStore.save( getObjects()[0] );
        genericStore.save( getObjects()[1] );
        genericStore.save( getObjects()[2] );
        
        Collection<T> objects = genericStore.getAll();
        
        assertNotNull( objects );
        assertEquals( 3, objects.size() );
        assertTrue( objects.contains( getObjects()[0] ) );
        assertTrue( objects.contains( getObjects()[1] ) );
        assertTrue( objects.contains( getObjects()[2] ) );        
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    /**
     * Method to override in order to set up test fixture.
     */
    protected void setUpGenericTest()
        throws Exception
    {   
    }
    
    /**
     * Should return 3 objects of the class which is to be tested.
     */
    protected abstract T[] getObjects();
    
    /**
     * Should return the Spring bean identifier of the store which is to be tested.
     */
    protected abstract String getGenericBeanId();
}
