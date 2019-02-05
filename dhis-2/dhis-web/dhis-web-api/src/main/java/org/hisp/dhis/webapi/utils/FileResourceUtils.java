package org.hisp.dhis.webapi.utils;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;

/**
 * @author Lars Helge Overland
 */
public class FileResourceUtils
{
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
     * @return a valid {@link FileResource} populated with data from the provided
     *         file
     * @throws IOException if hashing fails
     *
     */
    public static FileResource build( String key, MultipartFile file, FileResourceDomain domain )
        throws IOException
    {
        return new FileResource( key, file.getName(), file.getContentType(), file.getSize(),
            ByteSource.wrap( file.getBytes() ).hash( Hashing.md5() ).toString(), domain );
    }
}
