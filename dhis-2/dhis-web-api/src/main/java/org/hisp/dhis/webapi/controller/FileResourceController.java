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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.schema.descriptors.FileResourceSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@OpenApi.Tags( "system" )
@RestController
@RequestMapping( value = FileResourceSchemaDescriptor.API_ENDPOINT )
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class FileResourceController extends AbstractFullReadOnlyController<FileResource>
{
    private final FileResourceService fileResourceService;

    private final FileResourceUtils fileResourceUtils;

    private final DhisConfigurationProvider dhisConfig;

    /**
     * Overridden to only use it when {@code fields} parameter is present.
     */
    @OpenApi.Ignore
    @Override
    @GetMapping( value = "/{uid}", params = "fields" )
    public ResponseEntity<?> getObject( @PathVariable String uid, Map<String, String> rpParameters,
        @CurrentUser User currentUser, HttpServletRequest request,
        HttpServletResponse response )
        throws ForbiddenException,
        NotFoundException
    {
        return super.getObject( uid, rpParameters, currentUser, request, response );
    }

    @GetMapping( value = "/{uid}" )
    public FileResource getFileResource( @PathVariable String uid,
        @RequestParam( required = false ) ImageFileDimension dimension )
        throws NotFoundException
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null )
        {
            throw new NotFoundException( FileResource.class, uid );
        }

        FileResourceUtils.setImageFileDimensions( fileResource,
            MoreObjects.firstNonNull( dimension, ImageFileDimension.ORIGINAL ) );

        return fileResource;
    }

    @GetMapping( value = "/{uid}/data" )
    public void getFileResourceData( @PathVariable String uid, HttpServletResponse response,
        @RequestParam( required = false ) ImageFileDimension dimension, @CurrentUser User currentUser )
        throws NotFoundException,
        ForbiddenException,
        WebMessageException
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null )
        {
            throw new NotFoundException( FileResource.class, uid );
        }

        FileResourceUtils.setImageFileDimensions( fileResource,
            MoreObjects.firstNonNull( dimension, ImageFileDimension.ORIGINAL ) );

        if ( !checkSharing( fileResource, currentUser ) )
        {
            throw new ForbiddenException(
                "You don't have access to fileResource '" + uid
                    + "' or this fileResource is not available from this endpoint" );
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
            throw new WebMessageException( error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend. " +
                    "Depending on the provider the root cause could be network or file system related." ) );
        }
    }

    @PostMapping
    public WebMessage saveFileResource( @RequestParam MultipartFile file,
        @RequestParam( defaultValue = "DATA_VALUE" ) FileResourceDomain domain,
        @RequestParam( required = false ) String uid )
        throws WebMessageException,
        IOException
    {
        FileResource fileResource = fileResourceUtils.saveFileResource( uid, file, domain );

        WebMessage webMessage = new WebMessage( Status.OK, HttpStatus.ACCEPTED );
        webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

        return webMessage;
    }

    /**
     * Checks is the current user has access to view the fileResource.
     *
     * @return true if user has access, false if not.
     */
    private boolean checkSharing( FileResource fileResource, User currentUser )
    {
        /*
         * Serving DATA_VALUE and PUSH_ANALYSIS fileResources from this endpoint
         * doesn't make sense So we will return false if the fileResource have
         * either of these domains.
         */
        FileResourceDomain domain = fileResource.getDomain();
        if ( domain == FileResourceDomain.DATA_VALUE || domain == FileResourceDomain.PUSH_ANALYSIS )
        {
            return false;
        }

        if ( domain == FileResourceDomain.USER_AVATAR )
        {
            return currentUser.isAuthorized( "F_USER_VIEW" ) || fileResource.equals( currentUser.getAvatar() );
        }

        return true;
    }
}
