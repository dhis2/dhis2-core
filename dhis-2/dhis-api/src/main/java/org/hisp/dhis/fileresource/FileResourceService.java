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
package org.hisp.dhis.fileresource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Halvdan Hoem Grelland
 */
public interface FileResourceService
{
    FileResource getFileResource( String uid );

    List<FileResource> getFileResources( List<String> uids );

    List<FileResource> getOrphanedFileResources();

    void saveFileResource( FileResource fileResource, File file );

    String saveFileResource( FileResource fileResource, byte[] bytes );

    void deleteFileResource( String uid );

    void deleteFileResource( FileResource fileResource );

    InputStream getFileResourceContent( FileResource fileResource );

    /**
     * Copy fileResource content to outputStream and Return File content length
     *
     * @param fileResource
     * @param outputStream
     * @return
     * @throws IOException
     * @throws NoSuchElementException
     */
    void copyFileResourceContent( FileResource fileResource, OutputStream outputStream )
        throws IOException,
        NoSuchElementException;

    /**
     * Copy fileResource content to a byte array
     *
     * @param fileResource
     * @return a byte array of the content
     * @throws IOException
     * @throws NoSuchElementException
     */
    byte[] copyFileResourceContent( FileResource fileResource )
        throws IOException,
        NoSuchElementException;

    boolean fileResourceExists( String uid );

    void updateFileResource( FileResource fileResource );

    URI getSignedGetFileResourceContentUri( String uid );

    URI getSignedGetFileResourceContentUri( FileResource fileResource );

    List<FileResource> getExpiredFileResources( FileResourceRetentionStrategy retentionStrategy );

    List<FileResource> getAllUnProcessedImagesFiles();

    long getFileResourceContentLength( FileResource fileResource );
}
