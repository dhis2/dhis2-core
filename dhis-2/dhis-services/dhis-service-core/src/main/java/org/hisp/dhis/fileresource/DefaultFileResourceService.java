/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResourceStore.DataValueFileReference;
import org.hisp.dhis.fileresource.events.BinaryFileSavedEvent;
import org.hisp.dhis.fileresource.events.FileDeletedEvent;
import org.hisp.dhis.fileresource.events.FileSavedEvent;
import org.hisp.dhis.fileresource.events.ImageFileSavedEvent;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.ObjectUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.fileresource.FileResourceService")
public class DefaultFileResourceService implements FileResourceService {
  private static final Duration UNASSIGNED_GRACE_PERIOD = Days.days(2).toStandardDuration();
  public static final Predicate<FileResource> IS_ORPHAN_PREDICATE = (fr -> !fr.isAssigned());

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final FileResourceStore fileResourceStore;

  private final PeriodService periodService;

  private final FileResourceContentStore fileResourceContentStore;

  private final ApplicationEventPublisher fileEventPublisher;

  private final EntityManager entityManager;

  private final AclService aclService;

  private final IdentifiableObjectManager idObjectManager;

  // -------------------------------------------------------------------------
  // FileResourceService implementation
  // -------------------------------------------------------------------------

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public FileResource getExistingFileResource(String uid) throws NotFoundException {
    FileResource fr = fileResourceStore.getByUid(uid);
    if (fr == null) throw new NotFoundException(FileResource.class, uid);
    return fr;
  }

  @Override
  @Transactional(readOnly = true)
  public FileResource getFileResource(String uid) {
    return checkStorageStatus(fileResourceStore.getByUid(uid));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FileResource> getFileResource(String uid, FileResourceDomain domain) {
    return fileResourceStore.findByUidAndDomain(uid, domain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FileResource> getFileResources(@Nonnull List<String> uids) {
    return fileResourceStore.getByUid(uids).stream()
        .map(this::checkStorageStatus)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<FileResource> getExpiredFileResources(
      Set<FileResourceDomain> domainsToDeleteWhenUnassigned) {
    return fileResourceStore.getUnassignedPassedGracePeriod(
        domainsToDeleteWhenUnassigned, DefaultFileResourceService.getGracePeriod());
  }

  public static DateTime getGracePeriod() {
    return new DateTime().minus(UNASSIGNED_GRACE_PERIOD);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FileResource> getAllUnassignedByJobDataDomainWithNoJobConfig() {
    return fileResourceStore.getAllUnassignedByJobDataDomainWithNoJobConfig();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FileResource> findByStorageKey(@CheckForNull String storageKey) {
    return storageKey == null ? Optional.empty() : fileResourceStore.findByStorageKey(storageKey);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FileResourceOwner> findOwnersByStorageKey(@CheckForNull String storageKey) {
    Optional<FileResource> maybeFr = findByStorageKey(storageKey);
    if (maybeFr.isEmpty()) return List.of();
    FileResource fr = maybeFr.get();
    String uid = fr.getUid();
    return switch (fr.getDomain()) {
      case PUSH_ANALYSIS -> List.of();
      case ORG_UNIT ->
          fileResourceStore.findOrganisationUnitsByImageFileResource(uid).stream()
              .map(id -> new FileResourceOwner(FileResourceDomain.ORG_UNIT, id))
              .toList();
      case DOCUMENT ->
          fileResourceStore.findDocumentsByFileResource(uid).stream()
              .map(id -> new FileResourceOwner(FileResourceDomain.DOCUMENT, id))
              .toList();
      case MESSAGE_ATTACHMENT ->
          fileResourceStore.findMessagesByFileResource(uid).stream()
              .map(id -> new FileResourceOwner(FileResourceDomain.MESSAGE_ATTACHMENT, id))
              .toList();
      case USER_AVATAR ->
          fileResourceStore.findUsersByAvatarFileResource(uid).stream()
              .map(id -> new FileResourceOwner(FileResourceDomain.USER_AVATAR, id))
              .toList();
      case DATA_VALUE ->
          fileResourceStore.findDataValuesByFileResourceValue(uid).stream()
              .map(
                  dv ->
                      new FileResourceOwner(
                          dv.de(), dv.ou(), periodService.getPeriod(dv.pe()).getIsoDate(), dv.co()))
              .toList();
      case ICON ->
          fileResourceStore.findCustomIconByFileResource(uid).stream()
              .map(key -> new FileResourceOwner(FileResourceDomain.ICON, key))
              .toList();
      case JOB_DATA -> List.of(new FileResourceOwner(FileResourceDomain.JOB_DATA, uid));
    };
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canRead(@Nonnull UserDetails user, @Nonnull FileResource fileResource) {
    FileResourceDomain domain = fileResource.getDomain();
    if (domain == null) {
      // a file resource without a domain has no owner to authorize against
      return false;
    }

    if (user.isSuper() || fileResource.isCreatedBy(user)) {
      return true;
    }

    String uid = fileResource.getUid();
    return switch (domain) {
      // No object level rule: the content of a data value file is only served by the data value
      // API and the audit API (canReadDataValueFile), which apply the data access rules of the
      // owning data value. Its metadata is covered by canReadMetadata.
      // Note that FileResourceController refuses to serve the content of this domain outright.
      case DATA_VALUE -> false;

      // Push analysis reports are generated for a specific user and are not served here either.
      case PUSH_ANALYSIS -> false;

      // Icons and organisation unit images are public interface assets: every client renders them
      // and both are already served by their own endpoints without an object level check.
      case ICON, ORG_UNIT -> true;

      case USER_AVATAR ->
          user.isAuthorized(Authorities.F_USER_VIEW)
              || fileResourceStore.findUsersByAvatarFileResource(uid).contains(user.getUid());

      // Only the participants of the conversation the attachment belongs to. An attachment that is
      // not part of any conversation yet is reachable by its uploader through the check above.
      case MESSAGE_ATTACHMENT ->
          fileResourceStore.findMessageAttachmentParticipants(uid).contains(user.getUid());

      // The sharing of the owning document decides. An unassigned upload has no document yet and
      // is reachable by its uploader only.
      case DOCUMENT -> canReadAnyDocument(user, fileResourceStore.findDocumentsByFileResource(uid));

      // Job payloads follow the job log rule used by JobConfigurationController: the executing user
      // (the uploader, covered above), a job log reader, or a superuser.
      case JOB_DATA -> user.isAuthorized(Authorities.F_JOB_LOG_READ);
    };
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canReadMetadata(@Nonnull UserDetails user, @Nonnull FileResource fileResource) {
    // Only the content of a data value file is restricted to the data value API; its metadata
    // stays readable for the clients that resolve it from a data value.
    return fileResource.getDomain() == FileResourceDomain.DATA_VALUE || canRead(user, fileResource);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canReadDataValueFile(
      @Nonnull UserDetails user, @Nonnull FileResource fileResource) {
    if (fileResource.getDomain() != FileResourceDomain.DATA_VALUE) {
      return false;
    }
    if (canRead(user, fileResource)) {
      return true;
    }

    List<DataValueFileReference> references =
        fileResourceStore.findDataValueReferencesByFileResourceValue(fileResource.getUid()).stream()
            .filter(
                ref ->
                    user.isInUserHierarchy(ref.orgUnitPath())
                        || user.isInUserDataHierarchy(ref.orgUnitPath()))
            .toList();
    if (references.isEmpty()) {
      return false;
    }

    Set<String> comboUids = new HashSet<>();
    for (DataValueFileReference ref : references) {
      comboUids.add(ref.categoryOptionCombo());
      comboUids.add(ref.attributeOptionCombo());
    }
    Set<String> readableCombos =
        idObjectManager.getNoAcl(CategoryOptionCombo.class, comboUids).stream()
            .filter(combo -> aclService.canDataRead(user, combo))
            .map(CategoryOptionCombo::getUid)
            .collect(Collectors.toSet());
    return references.stream()
        .anyMatch(
            ref ->
                readableCombos.contains(ref.categoryOptionCombo())
                    && readableCombos.contains(ref.attributeOptionCombo()));
  }

  @Override
  @NonTransactional
  public boolean canAssign(
      @Nonnull UserDetails user,
      @Nonnull FileResource fileResource,
      @Nonnull FileResourceDomain domain) {
    return fileResource.getDomain() == domain
        && !fileResource.isAssigned()
        && (user.isSuper() || fileResource.isCreatedBy(user));
  }

  private boolean canReadAnyDocument(UserDetails user, List<String> documentUids) {
    if (documentUids.isEmpty()) {
      return false;
    }
    return idObjectManager.getNoAcl(Document.class, documentUids).stream()
        .anyMatch(document -> aclService.canRead(user, document));
  }

  @Override
  @Transactional
  public void asyncSaveFileResource(FileResource fileResource, File file) {
    validateFileResource(fileResource);

    fileResource.setStorageStatus(FileResourceStorageStatus.PENDING);
    fileResourceStore.save(fileResource);
    entityManager.flush();

    if (hasMultiDimensionImageSupport(fileResource)) {
      fileEventPublisher.publishEvent(
          new ImageFileSavedEvent(
              fileResource.getUid(), file, CurrentUserUtil.getCurrentUserDetails().getUid()));
      return;
    }

    fileEventPublisher.publishEvent(new FileSavedEvent(fileResource.getUid(), file));
  }

  @Override
  @Transactional
  public String asyncSaveFileResource(FileResource fileResource, byte[] bytes) {
    validateFileResource(fileResource);

    fileResource.setStorageStatus(FileResourceStorageStatus.PENDING);
    fileResourceStore.save(fileResource);
    entityManager.flush();

    final String uid = fileResource.getUid();

    fileEventPublisher.publishEvent(new BinaryFileSavedEvent(fileResource.getUid(), bytes));

    return uid;
  }

  @Override
  @Transactional
  public String syncSaveFileResource(FileResource fileResource, byte[] bytes)
      throws ConflictException {
    validateFileResource(fileResource);

    fileResource.setContentLength(bytes.length);
    try {
      fileResource.setContentMd5(ByteSource.wrap(bytes).hash(Hashing.md5()).toString());
    } catch (IOException ex) {
      throw new ConflictException("Failed to compute content md5 resource: " + ex.getMessage());
    }
    fileResource.setStorageStatus(FileResourceStorageStatus.PENDING);
    fileResourceStore.save(fileResource);
    entityManager.flush();

    final String uid = fileResource.getUid();

    String storageId = fileResourceContentStore.saveFileResourceContent(fileResource, bytes);
    if (storageId == null) throw new ConflictException(ErrorCode.E6102);

    return uid;
  }

  @Override
  @Transactional
  public String syncSaveFileResource(FileResource fileResource, InputStream content)
      throws ConflictException {
    try {
      return syncSaveFileResource(fileResource, IOUtils.toByteArray(content));
    } catch (IOException ex) {
      throw new ConflictException("Failed to extract bytes from input stream: " + ex.getMessage());
    }
  }

  @Override
  @Transactional
  public void deleteFileResource(String uid) {
    if (uid == null) {
      return;
    }

    FileResource fileResource = fileResourceStore.getByUid(uid);

    deleteFileResource(fileResource);
  }

  @Override
  @Transactional
  public void deleteFileResource(FileResource fileResource) {
    if (fileResource == null) {
      return;
    }

    FileResource existingResource = fileResourceStore.get(fileResource.getId());

    if (existingResource == null) {
      return;
    }

    FileDeletedEvent deleteFileEvent =
        new FileDeletedEvent(
            existingResource.getStorageKey(),
            existingResource.getContentType(),
            existingResource.getDomain());

    fileResourceStore.delete(existingResource);

    fileEventPublisher.publishEvent(deleteFileEvent);
  }

  @Override
  @Nonnull
  public InputStream getFileResourceContent(FileResource fileResource) throws ConflictException {
    String key = fileResource.getStorageKey();
    InputStream content = fileResourceContentStore.getFileResourceContent(key);
    if (content == null) throw new ConflictException(ErrorCode.E6103);
    return content;
  }

  @Override
  public long getFileResourceContentLength(FileResource fileResource) {
    return fileResourceContentStore.getFileResourceContentLength(fileResource.getStorageKey());
  }

  @Override
  public void copyFileResourceContent(FileResource fileResource, OutputStream outputStream)
      throws IOException, NoSuchElementException {
    fileResourceContentStore.copyContent(fileResource.getStorageKey(), outputStream);
  }

  @Override
  public byte[] copyFileResourceContent(FileResource fileResource)
      throws IOException, NoSuchElementException {
    return fileResourceContentStore.copyContent(fileResource.getStorageKey());
  }

  @Override
  public byte[] copyImageContent(FileResource fileResource, ImageFileDimension dimension)
      throws NoSuchElementException, BadRequestException, IOException {
    ImageFileDimension imageDimension =
        ObjectUtils.firstNonNull(dimension, ImageFileDimension.ORIGINAL);

    hasImageDimensionSupport(fileResource, imageDimension);

    return fileResourceContentStore.copyContent(imageKey(fileResource, imageDimension));
  }

  @Override
  public InputStream openContentStream(FileResource fileResource)
      throws IOException, NoSuchElementException {
    return fileResourceContentStore.openStream(fileResource.getStorageKey());
  }

  @Override
  public InputStream openContentStreamToImage(
      FileResource fileResource, ImageFileDimension dimension)
      throws IOException, NoSuchElementException, BadRequestException {
    ImageFileDimension imageDimension =
        ObjectUtils.firstNonNull(dimension, ImageFileDimension.ORIGINAL);

    hasImageDimensionSupport(fileResource, imageDimension);

    return fileResourceContentStore.openStream(imageKey(fileResource, imageDimension));
  }

  private static void hasImageDimensionSupport(
      FileResource fileResource, ImageFileDimension imageDimension) throws BadRequestException {
    if (!FileResource.isImage(fileResource.getContentType())) {
      throw new BadRequestException("File is not an image");
    }

    if (imageDimension != ImageFileDimension.ORIGINAL
        && !hasMultiDimensionImageSupport(fileResource)) {
      throw new BadRequestException("Image does not have support for multiple dimensions");
    }

    if (imageDimension != ImageFileDimension.ORIGINAL
        && !fileResource.isHasMultipleStorageFiles()) {
      throw new BadRequestException("Image is not stored using multiple dimensions");
    }
  }

  private static boolean hasMultiDimensionImageSupport(FileResource fileResource) {
    return FileResource.isImage(fileResource.getContentType())
        && FileResourceDomain.isDomainForMultipleImages(fileResource.getDomain());
  }

  private static String imageKey(FileResource fileResource, ImageFileDimension imageDimension) {
    return StringUtils.join(fileResource.getStorageKey(), imageDimension.getDimension());
  }

  @Override
  @Transactional
  public boolean fileResourceExists(String uid) {
    return fileResourceStore.getByUid(uid) != null;
  }

  @Override
  @Transactional
  public void updateFileResource(FileResource fileResource) {
    fileResourceStore.update(fileResource);
  }

  @Override
  @Transactional(readOnly = true)
  public URI getSignedGetFileResourceContentUri(String uid) {
    FileResource fileResource = getFileResource(uid);

    if (fileResource == null) {
      return null;
    }

    return fileResourceContentStore.getSignedGetContentUri(fileResource.getStorageKey());
  }

  @Override
  @Transactional(readOnly = true)
  public URI getSignedGetFileResourceContentUri(FileResource fileResource) {
    if (fileResource == null) {
      return null;
    }

    return fileResourceContentStore.getSignedGetContentUri(fileResource.getStorageKey());
  }

  @Override
  @Transactional(readOnly = true)
  public List<FileResource> getExpiredDataValueFileResources(
      FileResourceRetentionStrategy retentionStrategy) {
    DateTime retentionPeriod = DateTime.now().minus(retentionStrategy.getRetentionTime());
    return fileResourceStore.getExpiredDataValueFileResources(
        retentionPeriod, DefaultFileResourceService.getGracePeriod());
  }

  @Override
  @Transactional(readOnly = true)
  public List<FileResource> getAllUnProcessedImagesFiles() {
    return fileResourceStore.getAllUnProcessedImages();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Validates the given {@link FileResource}. Throws an exception if not.
   *
   * @param fileResource the file resource.
   * @throws IllegalQueryException if the given file resource is invalid.
   */
  private void validateFileResource(FileResource fileResource) throws IllegalQueryException {
    if (fileResource.getName() == null) {
      throw new IllegalQueryException(ErrorCode.E6100);
    }

    if (!FileResourceBlocklist.isValid(fileResource)) {
      throw new IllegalQueryException(ErrorCode.E6101);
    }
  }

  private FileResource checkStorageStatus(FileResource fileResource) {
    if (fileResource != null) {
      boolean exists =
          fileResourceContentStore.fileResourceContentExists(fileResource.getStorageKey());

      if (exists) {
        fileResource.setStorageStatus(FileResourceStorageStatus.STORED);
      } else {
        fileResource.setStorageStatus(FileResourceStorageStatus.PENDING);
      }
    }

    return fileResource;
  }
}
