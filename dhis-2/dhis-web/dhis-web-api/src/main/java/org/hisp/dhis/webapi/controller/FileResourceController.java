/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.schema.descriptors.FileResourceSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.MoreObjects;

/**
 * @author Halvdan Hoem Grelland
 */
@RestController
@RequestMapping( value = FileResourceSchemaDescriptor.API_ENDPOINT )
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class FileResourceController
{
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private FileResourceUtils fileResourceUtils;

    @Autowired
    private DhisConfigurationProvider dhisConfig;

    // -------------------------------------------------------------------------
    // Controller methods
    // -------------------------------------------------------------------------

    @GetMapping( value = "/{uid}" )
    public FileResource getFileResource( @PathVariable String uid,
        @RequestParam( required = false ) ImageFileDimension dimension )
        throws WebMessageException
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( FileResource.class, uid ) );
        }

        FileResourceUtils.setImageFileDimensions( fileResource,
            MoreObjects.firstNonNull( dimension, ImageFileDimension.ORIGINAL ) );

        return fileResource;
    }

    @GetMapping( value = "/{uid}/data" )
    public void getFileResourceData( @PathVariable String uid, HttpServletResponse response,
        @RequestParam( required = false ) ImageFileDimension dimension )
        throws WebMessageException
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( FileResource.class, uid ) );
        }

        FileResourceUtils.setImageFileDimensions( fileResource,
            MoreObjects.firstNonNull( dimension, ImageFileDimension.ORIGINAL ) );

        if ( !checkSharing( fileResource ) )
        {
            throw new WebMessageException(
                WebMessageUtils.unathorized( "You don't have access to fileResource '" + uid
                    + "' or this fileResource is not available from this endpoint" ) );
        }

        response.setContentType( fileResource.getContentType() );
        response.setHeader( HttpHeaders.CONTENT_LENGTH,
            String.valueOf( fileResourceService.getFileResourceContentLength( fileResource ) ) );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );
        HeaderUtils.setSecurityHeaders( response, dhisConfig.getProperty( ConfigurationKey.CSP_HEADER_VALUE ) );

        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            log.error( "Could not retrieve file.", e );
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend. " +
                    "Depending on the provider the root cause could be network or file system related." ) );
        }
    }

    @PostMapping
    public WebMessage saveFileResource( @RequestParam MultipartFile file,
        @RequestParam( defaultValue = "DATA_VALUE" ) FileResourceDomain domain )
        throws WebMessageException,
        IOException
    {
        FileResource fileResource = fileResourceUtils.saveFileResource( file, domain );

        WebMessage webMessage = new WebMessage( Status.OK, HttpStatus.ACCEPTED );
        webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

        return webMessage;
    }

    /**
     * Checks is the current user has access to view the fileResource.
     *
     * @return true if user has access, false if not.
     */
    private boolean checkSharing( FileResource fileResource )
    {
        User currentUser = currentUserService.getCurrentUser();

        /*
         * Serving DATA_VALUE and PUSH_ANALYSIS fileResources from this endpoint
         * doesn't make sense So we will return false if the fileResource have
         * either of these domains.
         */

        if ( fileResource.getDomain().equals( FileResourceDomain.USER_AVATAR ) )
        {
            return currentUser.isAuthorized( "F_USER_VIEW" ) || currentUser.getAvatar().equals( fileResource );
        }

        if ( fileResource.getDomain().equals( FileResourceDomain.DOCUMENT ) )
        {
            return true;
        }

        return false;
    }
}
