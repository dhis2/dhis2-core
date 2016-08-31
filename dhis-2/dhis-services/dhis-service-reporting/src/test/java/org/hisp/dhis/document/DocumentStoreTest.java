package org.hisp.dhis.document;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DocumentStoreTest
    extends DhisSpringTest
{
    @Resource(name="org.hisp.dhis.document.DocumentStore")
    private GenericIdentifiableObjectStore<Document> documentStore;
    
    private Document documentA;
    private Document documentB;
    private Document documentC;
    
    @Override
    public void setUpTest()
    {  
        documentA = new Document( "DocumentA", "UrlA", true, null );
        documentB = new Document( "DocumentB", "UrlB", true, null );
        documentC = new Document( "DocumentC", "UrlC", false, null );
    }

    @Test
    public void testSaveGet()
    {
        int id = documentStore.save( documentA );
        
        assertEquals( documentA, documentStore.get( id ) );
    }

    @Test
    public void testDelete()
    {
        int idA = documentStore.save( documentA );
        int idB = documentStore.save( documentB );
        
        assertNotNull( documentStore.get( idA ) );
        assertNotNull( documentStore.get( idB ) );
        
        documentStore.delete( documentA );
        
        assertNull( documentStore.get( idA ) );
        assertNotNull( documentStore.get( idB ) );
        
        documentStore.delete( documentB );

        assertNull( documentStore.get( idA ) );
        assertNull( documentStore.get( idB ) );
    }

    @Test
    public void testGetAll()
    {
        documentStore.save( documentA );
        documentStore.save( documentB );
        documentStore.save( documentC );
        
        List<Document> actual = documentStore.getAll();
        
        assertEquals( 3, actual.size() );
        assertTrue( actual.contains( documentA ) );
        assertTrue( actual.contains( documentB ) );
        assertTrue( actual.contains( documentC ) );        
    }

    @Test
    public void testGetByName()
    {
        documentStore.save( documentA );
        documentStore.save( documentB );
        documentStore.save( documentC );
        
        assertEquals( documentA, documentStore.getByName( "DocumentA" ) );
    }
}
