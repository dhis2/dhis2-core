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
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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
  private final UserService userService;

  private final Set<DefaultIcon> ignoredAfterFailure = ConcurrentHashMap.newKeySet();

  @Override
  @Transactional(readOnly = true)
  public List<Icon> findNonExistingDefaultIcons() {
    Set<String> existingKeys = Set.copyOf(iconStore.getAllKeys());
    List<Icon> missingIcons = new ArrayList<>();
    for (DefaultIcon icon : DefaultIcon.values()) {
      if (!ignoredAfterFailure.contains(icon)
          && icon.getVariantKeys().stream().anyMatch(key -> !existingKeys.contains(key))) {
        for (Icon i : icon.toVariantIcons()) {
          if (!existingKeys.contains(i.getKey())) {
            missingIcons.add(i);
          }
        }
      }
    }
    return missingIcons;
  }

  @Override
  @Transactional
  public String uploadDefaultIcon(Icon icon) throws ConflictException {
    String fileResourceId = CodeGenerator.generateUid();
    Resource resource = getDefaultIconResource(icon.getKey());
    try {
      FileResource fileResource =
          FileResource.ofKey(CUSTOM_ICON, icon.getKey(), MediaType.IMAGE_PNG);
      fileResource.setUid(fileResourceId);
      fileResource.setAssigned(true);
      try (InputStream image = resource.getInputStream()) {
        fileResourceService.syncSaveFileResource(fileResource, image);
      }
      icon.setFileResource(fileResource);
      return fileResourceId;
    } catch (IOException ex) {
      ignoredAfterFailure.add(icon.getOrigin());
      throw new ConflictException("Failed to create default icon resource: " + ex.getMessage());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<Icon> getIcons(IconQueryParams params) {
    return iconStore.getIcons(params);
  }

  @Override
  @Transactional(readOnly = true)
  public long count(IconQueryParams params) {
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
  public boolean iconExists(String key) {
    return iconStore.getIconByKey(key) != null;
  }

  @Override
  @Transactional
  public void addIcon(@Nonnull Icon icon)
      throws BadRequestException, NotFoundException, SQLException {

    if (icon.getOrigin() == null && !icon.isCustom()) {
      throw new BadRequestException("Not allowed to create default icon");
    }

    validateIconDoesNotExists(icon);
    validateIconKey(icon.getKey());

    if (icon.getFileResource() != null) {
      FileResource fileResource = getFileResource(icon.getFileResource().getUid());
      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    if (currentUser != null) {
      icon.setCreatedBy(currentUser);
    }

    if (icon.getKeywords() == null) {
      icon.setKeywords(Set.of());
    }

    icon.setAutoFields();
    iconStore.save(icon);
  }

  @Override
  @Transactional
  public void updateIcon(@Nonnull Icon icon) throws BadRequestException, SQLException {
    if (!icon.isCustom()) {
      throw new BadRequestException("Not allowed to update default icon");
    }

    validateIconKeyNotNullOrEmpty(icon.getKey());

    if (icon.getKeywords() == null) {
      icon.setKeywords(Set.of());
    }

    icon.setAutoFields();
    iconStore.update(icon);
  }

  @Override
  @Transactional
  public void deleteIcon(String key) throws BadRequestException, NotFoundException {
    Icon icon = validateIconExists(key);

    if (!icon.isCustom()) {
      throw new BadRequestException("Not allowed to delete default icon");
    }

    FileResource fileResource = getFileResource(icon.getFileResource().getUid());
    fileResource.setAssigned(false);
    fileResourceService.updateFileResource(fileResource);

    iconStore.delete(icon);
  }

  private void validateIconKey(String key) throws BadRequestException {
    Matcher matcher = pattern.matcher(key.trim());

    if (!matcher.matches()) {
      throw new BadRequestException(
          String.format(
              "Icon key %s is not valid. Alphanumeric and special characters '-' and '_' are allowed",
              key));
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

  private static Resource getDefaultIconResource(String key) {
    return new ClassPathResource(String.format("%s/%s.%s", ICON_PATH, key, DefaultIcon.SUFFIX));
  }
}
