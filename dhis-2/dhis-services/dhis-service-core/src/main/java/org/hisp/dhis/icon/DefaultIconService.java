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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.CurrentUserService;
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
  private static final String ICON_PATH = "SVGs";

  private final CustomIconStore customIconStore;

  private final FileResourceService fileResourceService;

  private final CurrentUserService currentUserService;

  private final Map<String, DefaultIcon> defaultIcons =
      Arrays.stream(DefaultIcon.Icons.values())
          .map(DefaultIcon.Icons::getVariants)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(DefaultIcon::getKey, Function.identity()));

  @Override
  @Transactional(readOnly = true)
  public List<Icon> getIcons() {
    return Stream.concat(defaultIcons.values().stream(), customIconStore.getAllIcons().stream())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Icon> getIcons(String[] keywords) {
    return Stream.concat(
            defaultIcons.values().stream()
                .filter(icon -> Set.of(icon.getKeywords()).containsAll(List.of(keywords)))
                .toList()
                .stream(),
            customIconStore.getIconsByKeywords(keywords).stream())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Icon getIcon(String key) throws NotFoundException {
    if (defaultIcons.containsKey(key)) {
      return defaultIcons.get(key);
    }

    return getCustomIcon(key);
  }

  @Override
  @Transactional(readOnly = true)
  public CustomIcon getCustomIcon(String key) throws NotFoundException {
    CustomIcon customIcon = customIconStore.getIconByKey(key);
    if (customIcon == null) {
      throw new NotFoundException(String.format("Icon not found: %s", key));
    }

    return customIcon;
  }

  @Override
  public Resource getDefaultIconResource(String key) throws NotFoundException {
    if (defaultIcons.containsKey(key)) {
      return new ClassPathResource(
          String.format("%s/%s.%s", ICON_PATH, key, DefaultIcon.Icons.SUFFIX));
    }

    throw new NotFoundException(String.format("No default icon found with key %s.", key));
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getKeywords() {
    return Stream.concat(
            defaultIcons.values().stream().map(Icon::getKeywords).flatMap(Arrays::stream),
            customIconStore.getKeywords().stream())
        .collect(Collectors.toSet());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean iconExists(String key) {
    return defaultIcons.get(key) != null || customIconStore.getIconByKey(key) != null;
  }

  @Override
  @Transactional
  public void addCustomIcon(CustomIcon customIcon) throws BadRequestException, NotFoundException {
    validateIconDoesNotExists(customIcon.getKey());
    FileResource fileResource = getFileResource(customIcon.getFileResourceUid());
    customIconStore.save(customIcon, fileResource, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional
  public void updateCustomIcon(String key, String description, String[] keywords)
      throws BadRequestException, NotFoundException {
    CustomIcon icon = validateCustomIconExists(key);

    if (description == null && keywords == null) {
      throw new BadRequestException(
          String.format(
              "Can't update icon %s if none of description and keywords are present in the request",
              key));
    }

    if (description != null) {
      icon.setDescription(description);
    }

    if (keywords != null) {
      icon.setKeywords(keywords);
    }

    customIconStore.update(icon);
  }

  @Override
  @Transactional
  public void deleteCustomIcon(String key) throws BadRequestException, NotFoundException {
    CustomIcon icon = validateCustomIconExists(key);
    getFileResource(icon.getFileResourceUid()).setAssigned(false);
    customIconStore.delete(key);
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
  }

  private FileResource getFileResource(String fileResourceUid)
      throws BadRequestException, NotFoundException {
    if (Strings.isNullOrEmpty(fileResourceUid)) {
      throw new BadRequestException("File resource id not specified.");
    }

    Optional<FileResource> fileResource =
        fileResourceService.getFileResource(fileResourceUid, CUSTOM_ICON);
    if (fileResource.isEmpty()) {
      throw new NotFoundException(
          String.format("File resource %s does not exist", fileResourceUid));
    }

    return fileResource.get();
  }

  private CustomIcon validateCustomIconExists(String key)
      throws NotFoundException, BadRequestException {
    validateIconKeyNotNullOrEmpty(key);
    return getCustomIcon(key);
  }
}
