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
package org.hisp.dhis.webapi.utils;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_HEADER_VALUE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;

/**
 * @author Lars Helge Overland
 */
@Component
@Slf4j
public class FileResourceUtils
{
    @Autowired
    private FileResourceService fileResourceService;

    /**
     * Transfers the given multipart file content to a local temporary file.
     *
     * @param multipartFile the multipart file.
     * @return a temporary local file.
     * @throws IOException if the file content could not be transferred.
     */
    public static File toTempFile( MultipartFile multipartFile )
        throws IOException
    {
        File tmpFile = Files.createTempFile( "org.hisp.dhis", ".tmp" ).toFile();
        tmpFile.deleteOnExit();
        multipartFile.transferTo( tmpFile );
        return tmpFile;
    }

    /**
     * Indicates whether the content type represented by the given string is a
     * valid, known content type.
     *
     * @param contentType the content type string.
     * @return true if the content is valid, false if not.
     */
    public static boolean isValidContentType( String contentType )
    {
        try
        {
            MimeTypeUtils.parseMimeType( contentType );
        }
        catch ( InvalidMimeTypeException ignored )
        {
            return false;
        }

        return true;
    }

    /**
     *
     * Builds a {@link FileResource} from a {@link MultipartFile}.
     *
     * @param key the key to associate to the {@link FileResource}
     * @param file a {@link MultipartFile}
     * @param domain a {@link FileResourceDomain}
     * @return a valid {@link FileResource} populated with data from the
     *         provided file
     * @throws IOException if hashing fails
     *
     */
    public static FileResource build( String key, MultipartFile file, FileResourceDomain domain )
        throws IOException
    {
        return new FileResource( key, file.getName(), file.getContentType(), file.getSize(),
            ByteSource.wrap( file.getBytes() ).hash( Hashing.md5() ).toString(), domain );
    }

    public static void setImageFileDimensions( FileResource fileResource, ImageFileDimension dimension )
    {
        if ( FileResource.IMAGE_CONTENT_TYPES.contains( fileResource.getContentType() ) &&
            FileResourceDomain.isDomainForMultipleImages( fileResource.getDomain() ) )
        {
            if ( fileResource.isHasMultipleStorageFiles() )
            {
                fileResource
                    .setStorageKey( StringUtils.join( fileResource.getStorageKey(), dimension.getDimension() ) );
            }
        }
    }

    public void configureFileResourceResponse( HttpServletResponse response, FileResource fileResource,
        DhisConfigurationProvider dhisConfig )
        throws WebMessageException
    {
        response.setContentType( fileResource.getContentType() );
        response.setContentLengthLong( fileResource.getContentLength() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );
        HeaderUtils.setSecurityHeaders( response, dhisConfig.getProperty( CSP_HEADER_VALUE ) );

        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend. "
                    + "Depending on the provider the root cause could be network or file system related." ) );
        }
    }

    public FileResource saveFileResource( MultipartFile file, FileResourceDomain domain )
        throws WebMessageException,
        IOException
    {
        return saveFileResource( null, file, domain );
    }

    public FileResource saveFileResource( String uid, MultipartFile file, FileResourceDomain domain )
        throws WebMessageException,
        IOException
    {
        String filename = StringUtils.defaultIfBlank( FilenameUtils.getName( file.getOriginalFilename() ),
            FileResource.DEFAULT_FILENAME );

        String contentType = file.getContentType();
        contentType = FileResourceUtils.isValidContentType( contentType ) ? contentType
            : FileResource.DEFAULT_CONTENT_TYPE;

        long contentLength = file.getSize();

        log.info( "File uploaded with filename: '{}', original filename: '{}', content type: '{}', content length: {}",
            filename, file.getOriginalFilename(), file.getContentType(), contentLength );

        if ( contentLength <= 0 )
        {
            throw new WebMessageException( conflict( "Could not read file or file is empty." ) );
        }

        ByteSource bytes = new MultipartFileByteSource( file );

        String contentMd5 = bytes.hash( Hashing.md5() ).toString();

        FileResource fileResource = new FileResource( filename, contentType, contentLength, contentMd5, domain );
        fileResource.setUid( uid );

        File tmpFile = toTempFile( file );

        if ( uid != null && fileResourceService.fileResourceExists( uid ) )
        {
            throw new WebMessageException( conflict( ErrorCode.E1119, FileResource.class.getSimpleName(), uid ) );
        }
        fileResourceService.saveFileResource( fileResource, tmpFile );
        return fileResource;
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    private class MultipartFileByteSource
        extends
        ByteSource
    {
        private MultipartFile file;

        public MultipartFileByteSource( MultipartFile file )
        {
            this.file = file;
        }

        @Override
        public InputStream openStream()
            throws IOException
        {
            try
            {
                return file.getInputStream();
            }
            catch ( IOException ioe )
            {
                return new NullInputStream( 0 );
            }
        }
    }
}
