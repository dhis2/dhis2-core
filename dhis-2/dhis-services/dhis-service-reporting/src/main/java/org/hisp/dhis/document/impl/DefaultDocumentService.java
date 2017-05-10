package org.hisp.dhis.document.impl;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Transactional
public class DefaultDocumentService
    implements DocumentService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private LocationManager locationManager;

    private GenericIdentifiableObjectStore<Document> documentStore;

    public void setDocumentStore( GenericIdentifiableObjectStore<Document> documentStore )
    {
        this.documentStore = documentStore;
    }

    private static final Log log = LogFactory.getLog( DefaultDocumentService.class );

    // -------------------------------------------------------------------------
    // DocumentService implementation
    // -------------------------------------------------------------------------

    @Override
    public int saveDocument( Document document )
    {
        documentStore.save( document );

        return document.getId();
    }

    @Override
    public Document getDocument( int id )
    {
        return documentStore.get( id );
    }

    @Override
    public Document getDocument( String uid )
    {
        return documentStore.getByUid( uid );
    }

    @Override
    public void deleteDocument( Document document )
    {

        // Remove files is !external
        if ( !document.isExternal() )
        {
            if ( document.getFileResource() != null )
            {
                deleteFileFromDocument( document );
                log.info( "Document " + document.getUrl() + " successfully deleted" );
            }
            else
            {
                try
                {
                    File file = locationManager.getFileForReading( document.getUrl(), DocumentService.DIR );

                    if ( file.delete() )
                    {
                        log.info( "Document " + document.getUrl() + " successfully deleted" );
                    }
                    else
                    {
                        log.warn( "Document " + document.getUrl() + " could not be deleted" );
                    }
                }
                catch ( LocationManagerException ex )
                {
                    log.warn( "An error occured while deleting " + document.getUrl() );
                }
            }
        }

        documentStore.delete( document );
    }

    @Override
    public void deleteFileFromDocument( Document document )
    {
        FileResource fileResource = document.getFileResource();

        // Remove reference to fileResource from document to avoid db constraint exception
        document.setFileResource( null );
        documentStore.save( document );

        // Delete file
        fileResourceService.deleteFileResource( fileResource.getUid() );
    }

    @Override
    public List<Document> getAllDocuments()
    {
        return documentStore.getAll();
    }

    @Override
    public List<Document> getDocumentByName( String name )
    {
        return documentStore.getAllEqName( name );
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
    public List<Document> getDocumentsBetween( int first, int max )
    {
        return documentStore.getAllOrderedName( first, max );
    }

    @Override
    public List<Document> getDocumentsBetweenByName( String name, int first, int max )
    {
        return documentStore.getAllLikeName( name, first, max );
    }
    
    @Override
    public List<Document> getDocumentsByUid( List<String> uids )
    {
        return documentStore.getByUid( uids );
    }
}
