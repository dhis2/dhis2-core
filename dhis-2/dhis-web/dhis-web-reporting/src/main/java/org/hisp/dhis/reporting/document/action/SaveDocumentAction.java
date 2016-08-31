package org.hisp.dhis.reporting.document.action;

import com.opensymphony.xwork2.Action;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

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

/**
 * @author Lars Helge Overland
 */
public class SaveDocumentAction
    implements Action
{
    private static final Log log = LogFactory.getLog( SaveDocumentAction.class );

    private static final String HTTP_PREFIX = "http://";

    private static final String HTTPS_PREFIX = "https://";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private LocationManager locationManager;

    public void setLocationManager( LocationManager locationManager )
    {
        this.locationManager = locationManager;
    }

    private DocumentService documentService;

    public void setDocumentService( DocumentService documentService )
    {
        this.documentService = documentService;
    }

    @Autowired
    private AttributeService attributeService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String url;

    public void setUrl( String url )
    {
        this.url = url;
    }

    private Boolean external;

    public void setExternal( Boolean external )
    {
        this.external = external;
    }

    private Boolean attachment = false;

    public void setAttachment( Boolean attachment )
    {
        this.attachment = attachment;
    }

    private File file;

    public void setUpload( File file )
    {
        this.file = file;
    }

    private String fileName;

    public void setUploadFileName( String fileName )
    {
        this.fileName = fileName;
    }

    private String contentType;

    public void setUploadContentType( String contentType )
    {
        this.contentType = contentType;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        Document document = new Document();

        if ( id != null )
        {
            document = documentService.getDocument( id );
        }

        if ( !external && file != null )
        {
            log.info( "Uploading file: '" + fileName + "', content-type: '" + contentType + "'" );

            File destination = locationManager.getFileForWriting( fileName, DocumentService.DIR );

            log.info( "Destination: '" + destination.getAbsolutePath() + "'" );

            boolean fileMoved = file.renameTo( destination );

            if ( !fileMoved )
            {
                throw new RuntimeException( "File could not be moved to: '" + destination.getAbsolutePath() + "'" );
            }

            url = fileName;
            document.setUrl( url );
            document.setContentType( contentType );
        }

        else if ( external )
        {
            if ( !(url.startsWith( HTTP_PREFIX ) || url.startsWith( HTTPS_PREFIX )) )
            {
                url = HTTP_PREFIX + url;
            }

            log.info( "Document name: '" + name + "', url: '" + url + "', external: '" + external + "'" );

            document.setUrl( url );
        }

        document.setAttachment( attachment );

        document.setExternal( external );

        document.setName( name );

        documentService.saveDocument( document );

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( document, jsonAttributeValues );
        }

        documentService.saveDocument( document );

        return SUCCESS;
    }
}
