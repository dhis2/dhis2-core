package org.hisp.dhis.webapi.controller;
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

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.dxf2.common.Status;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fileresource.*;
import org.hisp.dhis.schema.descriptors.ExternalFileResourceSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( ExternalFileResourceSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class ExternalFileResourceController
{

    @Autowired
    private ExternalFileResourceService externalFileResourceService;

    @Autowired
    private FileResourceService fileResourceService;

    /**
     * Returns a file associated with the externalFileResource resolved from the accessToken.
     * <p>
     * Only files contained in externalFileResources with a valid accessToken, expiration date null or in the future
     * are files allowed to be served trough this endpoint.
     *
     * @param accessToken a unique string that resolves to a given externalFileResource
     * @param response
     * @throws WebMessageException
     */
    @RequestMapping( value = "/{accessToken}", method = RequestMethod.GET )
    public void getExternalFileResource( @PathVariable String accessToken,
        HttpServletResponse response )
        throws WebMessageException
    {
        ExternalFileResource externalFileResource = externalFileResourceService
            .getExternalFileResourceByAccessToken( accessToken );

        if ( externalFileResource == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No file found with key '" + accessToken + "'" ) );
        }

        if ( externalFileResource.getExpires() != null && externalFileResource.getExpires().before( new Date() ) )
        {
            throw new WebMessageException( WebMessageUtils
                .createWebMessage( "The key you requested has expired", Status.WARNING, HttpStatus.GONE ) );
        }

        FileResource fileResource = externalFileResource.getFileResource();

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
            inputStream = fileResourceService.getFileResourceContent( fileResource ).openStream();
            IOUtils.copy( inputStream, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend. " +
                    "Depending on the provider the root cause could be network or file system related." ) );
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }

    }
}
