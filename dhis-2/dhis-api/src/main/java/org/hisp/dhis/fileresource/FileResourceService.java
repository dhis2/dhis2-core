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
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Halvdan Hoem Grelland
 */
public interface FileResourceService {

  @CheckForNull
  FileResource getFileResource(String uid);

  /**
   * Get the {@link FileResource} with the given ID
   *
   * @param uid the resource to fetch
   * @return the file resource
   * @throws NotFoundException when no such file resource exits
   */
  @Nonnull
  FileResource getExistingFileResource(String uid) throws NotFoundException;

  /**
   * Lookup a {@link FileResource} by uid and {@link FileResourceDomain}.
   *
   * @param uid file resource uid to lookup
   * @param domain file resource domain to lookup
   * @return the {@link FileResource} associated with the given uid and domain
   */
  Optional<FileResource> getFileResource(String uid, FileResourceDomain domain);

  List<FileResource> getFileResources(@Nonnull List<String> uids);

  List<FileResource> getExpiredFileResources(Set<FileResourceDomain> domainsToDeleteWhenUnassigned);

  /**
   * Get all unassigned File Resources by JOB_DATA FileResourceDomain, which have no associated job
   * config of scheduling type ONCE_ASAP.
   *
   * <p>The intention here is to find unassigned file resources that have no corresponding job
   * config, of scheduling type ONCE_ASAP. We assume that this means these JOB_DATA file resources
   * are no longer needed and should be cleaned up.
   *
   * @return matching FileResources
   */
  List<FileResource> getAllUnassignedByJobDataDomainWithNoJobConfig();

  /**
   * Lookup a {@link FileResource} by storage key property.
   *
   * @param storageKey key to look up
   * @return the {@link FileResource} associated with the given storage key
   */
  Optional<FileResource> findByStorageKey(@CheckForNull String storageKey);

  /**
   * Reverse lookup the objects associated with a {@link FileResource} by the storage key property.
   *
   * @param storageKey key to look up
   * @return list of objects that are associated with the {@link FileResource} of the given storage
   *     key. This is either none, most often one, but in theory can also be more than one. For
   *     example when the same data value would be associated with the same file resource value.
   */
  List<FileResourceOwner> findOwnersByStorageKey(@CheckForNull String storageKey);

  /**
   * Decides whether the given user may read the content of the given {@link FileResource}.
   *
   * <p>{@link FileResource} is not a shareable type, so it carries no sharing of its own and the
   * decision is made against the object that owns it: a {@link FileResourceDomain#DOCUMENT} is
   * readable when its {@link org.hisp.dhis.document.Document} is, a {@link
   * FileResourceDomain#MESSAGE_ATTACHMENT} when the user takes part in the conversation, and so on.
   *
   * <p>This is <em>deny by default</em>: a domain without an explicit rule is not readable. Callers
   * serving file content to a user MUST consult this method first.
   *
   * @param user the user requesting the file resource
   * @param fileResource the file resource to read
   * @return true if the user may read the file resource, false if not
   */
  boolean canRead(@Nonnull UserDetails user, @Nonnull FileResource fileResource);

  /**
   * Decides whether the given user may read the metadata (name, size, content type, checksum,
   * uploader) of the given {@link FileResource}.
   *
   * <p>This is {@link #canRead(UserDetails, FileResource)}, except for {@link
   * FileResourceDomain#DATA_VALUE}: clients such as Capture and Data Entry look up the metadata of
   * a file that belongs to a data value the user already sees, and that file was usually uploaded
   * by someone else. The content of such a file is only served by the data value APIs, which apply
   * the data access rules of the owning value.
   *
   * @param user the user requesting the file resource
   * @param fileResource the file resource to describe
   * @return true if the user may read the metadata of the file resource, false if not
   */
  boolean canReadMetadata(@Nonnull UserDetails user, @Nonnull FileResource fileResource);

  /**
   * Decides whether the given user may read the content of the given {@link
   * FileResourceDomain#DATA_VALUE} file where it is not looked up through its data value, as by the
   * data value change log (audit) API, which serves current and former values alike.
   *
   * <p>This is {@link #canRead(UserDetails, FileResource)}, or the file is the current or a former
   * value of an aggregate data value of a file value type that the user may read: its organisation
   * unit is in the user's data capture or data view hierarchy, and the user may read the data of
   * its category option combo and attribute option combo. Any other domain is not readable here.
   *
   * @param user the user requesting the file resource
   * @param fileResource the data value file resource to read
   * @return true if the user may read the file resource, false if not
   */
  boolean canReadDataValueFile(@Nonnull UserDetails user, @Nonnull FileResource fileResource);

  /**
   * Decides whether the given user may assign the given {@link FileResource} to an object, such as
   * a document, a message or an icon.
   *
   * <p>Who may read a file follows from the object it is assigned to, so only a file that is not
   * assigned yet, has the domain of that object and was uploaded by the user may be assigned. A
   * superuser may assign files uploaded by other users.
   *
   * @param user the user assigning the file resource
   * @param fileResource the file resource to assign
   * @param domain the domain of the object the file resource is assigned to
   * @return true if the user may assign the file resource, false if not
   */
  boolean canAssign(
      @Nonnull UserDetails user,
      @Nonnull FileResource fileResource,
      @Nonnull FileResourceDomain domain);

  /**
   * Creates the provided file resource and stores the file content asynchronously.
   *
   * @param fileResource the resource to create
   * @param file the content stored asynchronously
   */
  void asyncSaveFileResource(FileResource fileResource, File file);

  /**
   * Creates the provided file resource and stores the content asynchronously.
   *
   * @param fileResource the resource to create
   * @param bytes the content stored asynchronously
   * @return the UID of the created file resource
   */
  String asyncSaveFileResource(FileResource fileResource, byte[] bytes);

  /**
   * Creates the provided file resource and stores the content synchronously.
   *
   * @param fileResource the resource to create
   * @param bytes the content stored synchronously
   * @return the UID of the created file resource
   */
  String syncSaveFileResource(FileResource fileResource, byte[] bytes) throws ConflictException;

  /**
   * Creates a new file resource with the provided UID, domain and content data.
   *
   * @param content the content stored synchronously
   * @return the created {@link FileResource}
   * @throws ConflictException when the content could not be stored
   */
  String syncSaveFileResource(FileResource fileResource, InputStream content)
      throws ConflictException;

  void deleteFileResource(String uid);

  void deleteFileResource(FileResource fileResource);

  @Nonnull
  InputStream getFileResourceContent(FileResource fileResource) throws ConflictException;

  /** Copy fileResource content to outputStream and Return File content length */
  void copyFileResourceContent(FileResource fileResource, OutputStream outputStream)
      throws IOException, NoSuchElementException;

  /** Copy fileResource content to a byte array */
  byte[] copyFileResourceContent(FileResource fileResource)
      throws IOException, NoSuchElementException;

  /**
   * Copies the file resource content of an image of the given dimension. Copies the image in its
   * original dimensions if the given {@code dimension} is {@code null}.
   *
   * @param dimension the dimension of the image to copy
   * @return image bytes
   * @throws BadRequestException when the file resource is not an image, does not support multiple
   *     dimensions or does not have multiple dimension files stored
   */
  byte[] copyImageContent(FileResource fileResource, ImageFileDimension dimension)
      throws BadRequestException, IOException;

  /** Opens a stream to the file resource content. */
  InputStream openContentStream(FileResource fileResource)
      throws IOException, NoSuchElementException;

  /**
   * Opens a stream to the file resource content of an image of the given dimension. Returns the
   * image in its original dimensions if the given {@code dimension} is {@code null}.
   *
   * @param dimension the dimension of the image to open
   * @return the stream to the image
   * @throws BadRequestException when the file resource is not an image, does not support multiple
   *     dimensions or does not have multiple dimension files stored
   */
  InputStream openContentStreamToImage(FileResource fileResource, ImageFileDimension dimension)
      throws IOException, NoSuchElementException, BadRequestException;

  boolean fileResourceExists(String uid);

  void updateFileResource(FileResource fileResource);

  URI getSignedGetFileResourceContentUri(String uid);

  URI getSignedGetFileResourceContentUri(FileResource fileResource);

  List<FileResource> getExpiredDataValueFileResources(
      FileResourceRetentionStrategy retentionStrategy);

  List<FileResource> getAllUnProcessedImagesFiles();

  long getFileResourceContentLength(FileResource fileResource);
}
