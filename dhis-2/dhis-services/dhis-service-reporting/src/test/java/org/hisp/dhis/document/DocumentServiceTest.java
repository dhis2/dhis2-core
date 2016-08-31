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

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DocumentServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DocumentService documentService;

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
        int id = documentService.saveDocument( documentA );

        assertEquals( documentA, documentService.getDocument( id ) );
    }

    @Test
    public void testDelete()
    {
        int idA = documentService.saveDocument( documentA );
        int idB = documentService.saveDocument( documentB );

        assertNotNull( documentService.getDocument( idA ) );
        assertNotNull( documentService.getDocument( idB ) );

        documentService.deleteDocument( documentA );

        assertNull( documentService.getDocument( idA ) );
        assertNotNull( documentService.getDocument( idB ) );

        documentService.deleteDocument( documentB );

        assertNull( documentService.getDocument( idA ) );
        assertNull( documentService.getDocument( idB ) );
    }

    @Test
    public void testGetAll()
    {
        documentService.saveDocument( documentA );
        documentService.saveDocument( documentB );
        documentService.saveDocument( documentC );

        List<Document> actual = documentService.getAllDocuments();

        assertEquals( 3, actual.size() );
        assertTrue( actual.contains( documentA ) );
        assertTrue( actual.contains( documentB ) );
        assertTrue( actual.contains( documentC ) );
    }

    @Test
    public void testGetByName()
    {
        documentService.saveDocument( documentA );
        documentService.saveDocument( documentB );
        documentService.saveDocument( documentC );

        assertEquals( documentA, documentService.getDocumentByName( "DocumentA" ).get( 0 ) );
    }
}
