package org.hisp.dhis.fileresource;

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

import java.io.File;
import java.net.URI;

/**
 * @author Halvdan Hoem Grelland
 */
public interface FileResourceContentStore
{
    /**
     * Get the content bytes of a FileResource from the file store.
     * @param key the key.
     * @return a ByteSource which provides a stream to the content or null if the content cannot be found or read.
     */
    ByteSource getFileResourceContent( String key );

    /**
     * Save the contents of the byte array to the file store.
     *
     * @param fileResource the FileResource object. Must be complete and include the storageKey,
     *                     contentLength, contentMd5 and name.
     * @param bytes the byte array.
     * @return the key on success or null if saving failed.
     */
    String saveFileResourceContent( FileResource fileResource, byte[] bytes );

    /**
     * Save the contents of the File to the file store.
     *
     * @param fileResource the FileResource object.
     * @param file the File. Will be consumed upon deletion.
     * @return the key on success or null if saving failed.
     */
    String saveFileResourceContent( FileResource fileResource, File file);

    /**
     * Delete the content bytes of a file resource.
     * @param key the key.
     */
    void deleteFileResourceContent( String key );

    /**
     * Check existence of a file.
     * @param key key of the file.
     * @return true if the file exists in the file store, false otherwise.
     */
    boolean fileResourceContentExists( String key );

    /**
     * Create a signed GET request which gives access to the content.
     * @param key the key.
     * @return a URI containing the signed GET request or null if signed requests are not supported.
     */
    URI getSignedGetContentUri( String key );
}
