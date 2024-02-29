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
package org.hisp.dhis.icon;

import static org.hisp.dhis.fileresource.FileResourceDomain.CUSTOM_ICON;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Wærstad
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.icon.CustomIconService")
public class DefaultCustomIconService implements CustomIconService {
  private static final String ICON_PATH = "SVGs";

  private final CustomIconStore customIconStore;

  private final FileResourceService fileResourceService;

  @Override
  @Transactional(readOnly = true)
  public Set<CustomIcon> getCustomIcons(CustomIconOperationParams iconOperationParams) {
    return customIconStore.getCustomIcons(iconOperationParams);
  }

  @Override
  @Transactional(readOnly = true)
  public long count(CustomIconOperationParams iconOperationParams) {
    return customIconStore.count(iconOperationParams);
  }

  @Override
  @Transactional(readOnly = true)
  public CustomIcon getCustomIcon(String key) throws NotFoundException {
    CustomIcon customIcon = customIconStore.getCustomIconByKey(key);
    if (customIcon == null) {
      throw new NotFoundException(String.format("CustomIcon not found: %s", key));
    }

    return customIcon;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<CustomIcon> getCustomIconsByKeywords(Set<String> keywords) {

    if (keywords == null || keywords.isEmpty()) {
      return Set.of();
    }

    return customIconStore.getCustomIconsByKeywords(keywords);
  }

  @Override
  @Transactional(readOnly = true)
  public CustomIcon getCustomIconByUid(String uid) throws NotFoundException {
    CustomIcon customIcon = customIconStore.getByUid(uid);
    if (customIcon == null) {
      throw new NotFoundException(String.format("CustomIcon not found: %s", uid));
    }

    return customIcon;
  }

  @Override
  @Transactional
  public Resource getCustomIconResource(String key) throws NotFoundException {

    if (customIconExists(key)) {
      return new ClassPathResource(String.format("%s/%s.%s", ICON_PATH, key, Icon.SUFFIX));
    }

    throw new NotFoundException(String.format("No CustomIcon found with key %s.", key));
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getKeywords() {
    return customIconStore.getKeywords();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean customIconExists(String key) {
    return customIconStore.getCustomIconByKey(key) != null;
  }

  @Override
  @Transactional
  public void addCustomIcon(CustomIcon customIcon) throws BadRequestException, NotFoundException {
    validateIconDoesNotExists(customIcon);

    if (customIcon.getCustom()) {
      FileResource fileResource = getFileResource(customIcon.getFileResource().getUid());
      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
    }

    customIconStore.save(customIcon);
  }

  @Override
  @Transactional
  public void updateCustomIcon(CustomIcon customIcon) throws BadRequestException {

    if (customIcon == null) {
      throw new BadRequestException("CustomIcon cannot be null.");
    }

    validateIconKeyNotNullOrEmpty(customIcon.getKey());

    customIcon.setAutoFields();
    customIconStore.update(customIcon);
  }

  @Override
  @Transactional
  public void deleteCustomIcon(CustomIcon customIcon)
      throws BadRequestException, NotFoundException {

    CustomIcon persistedCustomIcon = validateCustomIconExists(customIcon);

    if (persistedCustomIcon.getCustom()) {
      FileResource fileResource = getFileResource(persistedCustomIcon.getFileResource().getUid());
      fileResource.setAssigned(false);
      fileResourceService.updateFileResource(fileResource);
    }

    customIconStore.delete(persistedCustomIcon);
  }

  private void validateIconDoesNotExists(CustomIcon customIcon) throws BadRequestException {

    if (customIcon == null) {
      throw new BadRequestException("CustomIcon cannot be null.");
    }

    validateIconDoesNotExists(customIcon.getKey());
  }

  private void validateIconDoesNotExists(String key) throws BadRequestException {
    validateIconKeyNotNullOrEmpty(key);

    if (customIconExists(key)) {
      throw new BadRequestException(String.format("CustomIcon with key %s already exists.", key));
    }
  }

  private void validateIconKeyNotNullOrEmpty(String key) throws BadRequestException {
    if (Strings.isNullOrEmpty(key)) {
      throw new BadRequestException("CustomIcon key not specified.");
    }
  }

  private FileResource getFileResource(String fileResourceUid)
      throws BadRequestException, NotFoundException {
    if (Strings.isNullOrEmpty(fileResourceUid)) {
      throw new BadRequestException("FileResource id not specified.");
    }

    Optional<FileResource> fileResource =
        fileResourceService.getFileResource(fileResourceUid, CUSTOM_ICON);
    if (fileResource.isEmpty()) {
      throw new NotFoundException(String.format("FileResource %s does not exist", fileResourceUid));
    }

    return fileResource.get();
  }

  private CustomIcon validateCustomIconExists(String key)
      throws NotFoundException, BadRequestException {
    validateIconKeyNotNullOrEmpty(key);
    return getCustomIcon(key);
  }

  private CustomIcon validateCustomIconExists(CustomIcon customIcon)
      throws NotFoundException, BadRequestException {
    if (customIcon == null) {
      throw new BadRequestException("CustomIcon cannot be null.");
    }

    return validateCustomIconExists(customIcon.getKey());
  }
}
