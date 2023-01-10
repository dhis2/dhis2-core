/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.document.impl;

import java.util.List;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.document.DocumentStore;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
@RequiredArgsConstructor
@Service( "org.hisp.dhis.document.DocumentService" )
public class DefaultDocumentService
    implements DocumentService
{
    private final FileResourceService fileResourceService;

    private final DocumentStore documentStore;

    // -------------------------------------------------------------------------
    // DocumentService implementation
    // -------------------------------------------------------------------------

    @Override
    public long saveDocument( Document document )
    {
        documentStore.save( document );

        return document.getId();
    }

    @Override
    public Document getDocument( long id )
    {
        return documentStore.get( id );
    }

    @Override
    public Document getDocument( String uid )
    {
        return documentStore.getByUid( uid );
    }

    @Override
    public void deleteFileFromDocument( Document document )
    {
        FileResource fileResource = document.getFileResource();

        // Remove reference to fileResource from document to avoid db constraint
        // exception
        document.setFileResource( null );
        documentStore.save( document );

        // Delete file
        fileResourceService.deleteFileResource( fileResource.getUid() );
    }

    @Override
    public void deleteDocument( Document document )
    {
        documentStore.delete( document );
    }

    @Override
    public List<Document> getAllDocuments()
    {
        return documentStore.getAll();
    }

    @Override
    public int getDocumentCount()
    {
        return documentStore.getCount();
    }

    @Override
    public int getDocumentCountByName( String name )
    {
        return documentStore.getCountLikeName( name );
    }

    @Override
    public List<Document> getDocumentsByUid( @Nonnull List<String> uids )
    {
        return documentStore.getByUid( uids );
    }

    @Override
    public long getCountDocumentByUser( User user )
    {
        return documentStore.getCountByUser( user );
    }
}