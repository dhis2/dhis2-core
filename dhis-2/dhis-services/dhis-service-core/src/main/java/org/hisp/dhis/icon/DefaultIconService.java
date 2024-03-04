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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
@Service("org.hisp.dhis.icon.IconService")
public class DefaultIconService implements IconService {

  private static final String CUSTOM_ICON_KEY_PATTERN = "^[a-zA-Z0-9_+-]+$";

  private static final Pattern pattern = Pattern.compile(CUSTOM_ICON_KEY_PATTERN);

  private static final String ICON_PATH = "SVGs";

  private final IconStore iconStore;

  private final FileResourceService fileResourceService;

  @Override
  @Transactional(readOnly = true)
  public Set<Icon> getIcons(IconOperationParams params) {
    return iconStore.getIcons(params);
  }

  @Override
  @Transactional(readOnly = true)
  public long count(IconOperationParams params) {
    return iconStore.count(params);
  }

  @Override
  @Transactional(readOnly = true)
  public Icon getIcon(String key) throws NotFoundException {
    Icon icon = iconStore.getIconByKey(key);
    if (icon == null) {
      throw new NotFoundException(String.format("Icon not found: %s", key));
    }

    return icon;
  }

  @Override
  @Transactional(readOnly = true)
  public Icon getIconByUid(String uid) throws NotFoundException {
    Icon icon = iconStore.getByUid(uid);
    if (icon == null) {
      throw new NotFoundException(String.format("Icon not found: %s", uid));
    }

    return icon;
  }

  @Override
  @Transactional(readOnly = true)
  public Resource getIconResource(String key) throws NotFoundException {

    if (iconExists(key)) {
      return new ClassPathResource(String.format("%s/%s.%s", ICON_PATH, key, DefaultIcon.SUFFIX));
    }

    throw new NotFoundException(String.format("No Icon found with key %s.", key));
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getKeywords() {
    return iconStore.getKeywords();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean iconExists(String key) {
    return iconStore.getIconByKey(key) != null;
  }

  @Override
  @Transactional
  public void addIcon(Icon icon) throws BadRequestException, NotFoundException {

    if (icon == null) {
      throw new BadRequestException("Icon cannot be null.");
    }

    validateIconDoesNotExists(icon);
    validateIconKey(icon.getKey());

    if (icon.getCustom()) {
      FileResource fileResource = getFileResource(icon.getFileResource().getUid());
      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
    }

    iconStore.save(icon);
  }

  @Override
  @Transactional
  public void updateIcon(Icon icon) throws BadRequestException {

    if (icon == null) {
      throw new BadRequestException("Icon cannot be null.");
    }

    validateIconKeyNotNullOrEmpty(icon.getKey());

    icon.setAutoFields();
    iconStore.update(icon);
  }

  @Override
  @Transactional
  public void deleteIcon(Icon icon) throws BadRequestException, NotFoundException {

    Icon persistedIcon = validateIconExists(icon);

    if (persistedIcon.getCustom()) {
      FileResource fileResource = getFileResource(persistedIcon.getFileResource().getUid());
      fileResource.setAssigned(false);
      fileResourceService.updateFileResource(fileResource);
    }

    iconStore.delete(persistedIcon);
  }

  private void validateIconKey(String key) throws BadRequestException {
    Matcher matcher = pattern.matcher(key.trim());

    if (!matcher.matches()) {
      throw new BadRequestException(
          String.format(
              "Icon key %s is not valid. Alphanumeric and special characters are allowed", key));
    }
  }

  private void validateIconDoesNotExists(Icon icon) throws BadRequestException {

    if (icon == null) {
      throw new BadRequestException("Icon cannot be null.");
    }

    validateIconDoesNotExists(icon.getKey());
  }

  private void validateIconDoesNotExists(String key) throws BadRequestException {
    validateIconKeyNotNullOrEmpty(key);

    if (iconExists(key)) {
      throw new BadRequestException(String.format("Icon with key %s already exists.", key));
    }
  }

  private void validateIconKeyNotNullOrEmpty(String key) throws BadRequestException {
    if (Strings.isNullOrEmpty(key)) {
      throw new BadRequestException("Icon key not specified.");
    }

    validateIconKey(key);
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

  private Icon validateIconExists(String key) throws NotFoundException, BadRequestException {
    validateIconKeyNotNullOrEmpty(key);
    return getIcon(key);
  }

  private Icon validateIconExists(Icon icon) throws NotFoundException, BadRequestException {
    if (icon == null) {
      throw new BadRequestException("Icon cannot be null.");
    }

    return validateIconExists(icon.getKey());
  }
}
