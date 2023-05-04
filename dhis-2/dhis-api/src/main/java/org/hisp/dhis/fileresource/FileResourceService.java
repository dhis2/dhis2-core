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
import java.util.Optional;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author Halvdan Hoem Grelland
 */
public interface FileResourceService
{
    FileResource getFileResource( String uid );

    /**
     * Lookup a {@link FileResource} by uid and {@link FileResourceDomain}.
     *
     * @param uid file resource uid to lookup
     * @param domain file resource domain to lookup
     * @return the {@link FileResource} associated with the given uid and domain
     */
    FileResource getFileResource( String uid, FileResourceDomain domain );

    List<FileResource> getFileResources( @Nonnull List<String> uids );

    List<FileResource> getOrphanedFileResources();

    /**
     * Lookup a {@link FileResource} by storage key property.
     *
     * @param storageKey key to look up
     * @return the {@link FileResource} associated with the given storage key
     */
    Optional<FileResource> findByStorageKey( @CheckForNull String storageKey );

    /**
     * Reverse lookup the objects associated with a {@link FileResource} by the
     * storage key property.
     *
     * @param storageKey key to look up
     * @return list of objects that are associated with the {@link FileResource}
     *         of the given storage key. This is either none, most often one,
     *         but in theory can also be more than one. For example when the
     *         same data value would be associated with the same file resource
     *         value.
     */
    List<FileResourceOwner> findOwnersByStorageKey( @CheckForNull String storageKey );

    void saveFileResource( FileResource fileResource, File file );

    String saveFileResource( FileResource fileResource, byte[] bytes );

    void deleteFileResource( String uid );

    void deleteFileResource( FileResource fileResource );

    InputStream getFileResourceContent( FileResource fileResource );

    /**
     * Copy fileResource content to outputStream and Return File content length
     */
    void copyFileResourceContent( FileResource fileResource, OutputStream outputStream )
        throws IOException,
        NoSuchElementException;

    /**
     * Copy fileResource content to a byte array
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
