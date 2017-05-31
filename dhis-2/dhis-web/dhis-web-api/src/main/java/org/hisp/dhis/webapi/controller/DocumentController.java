package org.hisp.dhis.webapi.controller;

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

import com.google.common.io.ByteSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.schema.descriptors.DocumentSchemaDescriptor;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DocumentSchemaDescriptor.API_ENDPOINT )
public class DocumentController
    extends AbstractCrudController<Document>
{

    private final static Log log = LogFactory.getLog( DocumentController.class );

    @Autowired
    private DocumentService documentService;

    @Autowired
    private LocationManager locationManager;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private ContextUtils contextUtils;

    @RequestMapping( value = "/{uid}/data", method = RequestMethod.GET )
    public void getDocumentContent( @PathVariable( "uid" ) String uid, HttpServletResponse response )
        throws Exception
    {
        Document document = documentService.getDocument( uid );

        if ( document == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Document not found for uid: " + uid ) );
        }

        if ( document.isExternal() )
        {
            response.sendRedirect( response.encodeRedirectURL( document.getUrl() ) );
        }
        else if ( document.getFileResource() != null )
        {
            FileResource fileResource = document.getFileResource();

            ByteSource content = fileResourceService.getFileResourceContent( fileResource );

            if ( content == null )
            {
                throw new WebMessageException(
                    WebMessageUtils.notFound( "The referenced file could not be found" ) );
            }

            // ---------------------------------------------------------------------
            // Attempt to build signed URL request for content and redirect
            // ---------------------------------------------------------------------

            URI signedGetUri = fileResourceService.getSignedGetFileResourceContentUri( fileResource.getUid() );

            if ( signedGetUri != null )
            {
                response.setStatus( HttpServletResponse.SC_TEMPORARY_REDIRECT );
                response.setHeader( HttpHeaders.LOCATION, signedGetUri.toASCIIString() );

                return;
            }

            // ---------------------------------------------------------------------
            // Build response and return
            // ---------------------------------------------------------------------

            response.setContentType( fileResource.getContentType() );
            response.setContentLength( new Long( fileResource.getContentLength() ).intValue() );
            response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );

            // ---------------------------------------------------------------------
            // Request signing is not available, stream content back to client
            // ---------------------------------------------------------------------

            InputStream inputStream = null;

            try
            {
                inputStream = content.openStream();
                IOUtils.copy( inputStream, response.getOutputStream() );
            }
            catch ( IOException e )
            {
                log.error( "Could not retrieve file.", e );
                throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                    "There was an exception when trying to fetch the file from the storage backend. " +
                        "Depending on the provider the root cause could be network or file system related." ) );
            }
            finally
            {
                IOUtils.closeQuietly( inputStream );
            }
        }
        else
        {
            contextUtils.configureResponse( response, document.getContentType(), CacheStrategy.CACHE_TWO_WEEKS,
                document.getUrl(),
                document.getAttachment() == null ? false : document.getAttachment() );

            InputStream in = null;

            try
            {
                in = locationManager.getInputStream( document.getUrl(), DocumentService.DIR );
                IOUtils.copy( in, response.getOutputStream() );
            }
            catch ( IOException e )
            {
                log.error( "Could not retrieve file.", e );
                throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                    "There was an exception when trying to fetch the file from the storage backend. " +
                        "Depending on the provider the root cause could be network or file system related." ) );
            }
            finally
            {
                IOUtils.closeQuietly( in );
            }

        }
    }
}
