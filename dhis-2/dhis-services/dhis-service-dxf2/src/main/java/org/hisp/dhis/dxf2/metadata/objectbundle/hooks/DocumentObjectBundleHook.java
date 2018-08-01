package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Kristian Wærstad <kristian@dhis2.com>
 */
public class DocumentObjectBundleHook extends AbstractObjectBundleHook {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private LocationManager locationManager;

    private static final Log log = LogFactory.getLog( DocumentObjectBundleHook.class );

    @Override
    public List<ErrorReport> validate (IdentifiableObject object, ObjectBundle bundle)
    {

        if ( !Document.class.isInstance( object ) )
        {
            return new ArrayList<>();
        }

        List<ErrorReport> errors = new ArrayList<>();

        Document document = (Document) object;

        // FileResource fileResource = fileResourceService.getFileResource( document.getUrl() );
        FileResource fileResource = fileResourceService.getFileResourceByName( document.getUrl() );

        // Should have an url value - Valid url if external or file resource name if file upload
        if ( document.getUrl() == null )
        {
            errors.add( new ErrorReport( Document.class, ErrorCode.E4000, "url" ) );
        }
        // Should have a valid url if external
        else if ( document.isExternal() && !URL_PATTERN.matcher( document.getUrl() ).matches() )
        {
            errors.add( new ErrorReport( Document.class, ErrorCode.E4004, "url", document.getUrl() ) );
        }
        // If not external should have an associated fileResource
        else if ( !document.isExternal() && fileResource == null )
        {
            errors.add( new ErrorReport( Document.class, ErrorCode.E4015, "url", document.getUrl() ) );
        }
        // If file upload, fileResource should be unique (when creating new)
        else if ( !document.isExternal() && fileResource.isAssigned() && bundle.getImportMode().isCreate() )
        {
            errors.add( new ErrorReport( Document.class, ErrorCode.E4016, "url", document.getUrl() ) );
        }

        return errors;
    }

    @Override
    public void preDelete( IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        Document persistedDocument = (Document) persistedObject;
        if ( !persistedDocument.isExternal() )
        {
            if ( persistedDocument.getFileResource() != null )
            {
                documentService.deleteFileFromDocument( persistedDocument );
                log.info( "Document " + persistedDocument.getUrl() + " successfully deleted" );
            }
            else
            {
                try
                {
                    File file = locationManager.getFileForReading( persistedDocument.getUrl(), DocumentService.DIR );

                    if ( file.delete() )
                    {
                        log.info( "Document " + persistedDocument.getUrl() + " successfully deleted" );
                    }
                    else
                    {
                        log.warn( "Document " + persistedDocument.getUrl() + " could not be deleted" );
                    }
                }
                catch ( LocationManagerException ex )
                {
                    log.warn( "An error occured while deleting " + persistedDocument.getUrl() );
                }
            }
        }
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !Document.class.isInstance( object ) || ((Document) object).isExternal()) return;
        Document document = (Document) object;
        Document persistedDocument = (Document) persistedObject;
        if(!document.getUrl().equalsIgnoreCase( persistedDocument.getUrl() ))
        {
            documentService.deleteFileFromDocument( persistedDocument );
        }
    }

    @Override
    public void postCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !Document.class.isInstance( object ) )
        {
            return;
        }

        Document document = (Document) object;

        saveDocument( document );
    }

    @Override
    public void postUpdate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !Document.class.isInstance( object ) )
        {
            return;
        }

        Document document = (Document) object;

        saveDocument( document );
    }

    private void saveDocument( Document document )
    {
        if ( !document.isExternal() )
        {
            FileResource fileResource = fileResourceService.getFileResourceByName( document.getUrl() );
            fileResource.setDomain( FileResourceDomain.DOCUMENT );
            fileResource.setAssigned( true );
            document.setFileResource( fileResource );
            fileResourceService.updateFileResource( fileResource );
        }

        idObjectManager.save( document );
    }
}
