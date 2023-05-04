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

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.datavalue.DataValueKey;
import org.joda.time.DateTime;

public interface FileResourceStore extends IdentifiableObjectStore<FileResource>
{
    List<FileResource> getExpiredFileResources( DateTime expires );

    List<FileResource> getAllUnProcessedImages();

    /**
     * @param storageKey key to look up
     * @return the file resource with the given storage key or nothing if no
     *         such file resource exists
     */
    Optional<FileResource> findByStorageKey( @Nonnull String storageKey );

    /**
     * @param uid of the file resource
     * @param domain of the file resource
     * @return the file resource with the given uid and domain or null if no
     *         such file resource exists
     */
    FileResource findByUidAndDomain( @Nonnull String uid, @Nonnull FileResourceDomain domain );

    /**
     * Returns the organisation unit UID(s) with an image that links to the
     * {@link FileResource} with the provided uid.
     *
     * @param uid of the file resource UID for the organisation unit image
     * @return usually none or exactly one but in theory it could be multiple
     *         linked to the same file resource
     */
    List<String> findOrganisationUnitsByImageFileResource( @Nonnull String uid );

    /**
     * Lookup of user ID(s) for avatar image file resource id.
     *
     * @param uid of a file resource
     * @return UID(s) of users that have an avatar image with the given file
     *         resource uid
     */
    List<String> findUsersByAvatarFileResource( @Nonnull String uid );

    /**
     * Lookup of document ID(s) for a document file resource id.
     *
     * @param uid of a file resource
     * @return UID(s) of documents that have a file resource with the given uid
     */
    List<String> findDocumentsByFileResource( @Nonnull String uid );

    /**
     * Lookup of message ID(s) for a message attachment file resource id.
     *
     * @param uid of a file resource
     * @return UID(s) of messages that have an attachment with the given file
     *         resource uid
     */
    List<String> findMessagesByFileResource( @Nonnull String uid );

    /**
     * Lookup of data value(s) key combinations with a given file resource
     * value.
     *
     * @param uid of a file resource
     * @return data value(s) key combinations that have the given file resource
     *         as value.
     */
    List<DataValueKey> findDataValuesByFileResourceValue( @Nonnull String uid );
}
