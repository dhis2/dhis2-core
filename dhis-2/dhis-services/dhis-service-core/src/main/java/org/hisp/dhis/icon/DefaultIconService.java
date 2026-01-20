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
package org.hisp.dhis.icon;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.fileresource.FileResourceDomain.ICON;
import static org.hisp.dhis.util.DateUtils.dateTimeIsValid;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
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

  private static final String ICON_KEY_PATTERN_REGEX = "^[a-zA-Z0-9_+-]+$";
  private static final Pattern ICON_KEY_PATTERN = Pattern.compile(ICON_KEY_PATTERN_REGEX);

  private static final String ICON_PATH = "SVGs";
  private static final String MEDIA_TYPE_SVG = "image/svg+xml";

  private final IconStore iconStore;
  private final FileResourceService fileResourceService;
  private final FileResourceContentStore fileResourceContentStore;
  private final UserService userService;

  private final Set<DefaultIcon> ignoredAfterFailure = ConcurrentHashMap.newKeySet();

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Map<DefaultIcon, List<AddIconRequest>> findNonExistingDefaultIcons() {
    Set<String> existingKeys = Set.copyOf(iconStore.getAllKeys());
    Map<DefaultIcon, List<AddIconRequest>> missingIcons = new EnumMap<>(DefaultIcon.class);
    for (DefaultIcon origin : DefaultIcon.values()) {
      if (!ignoredAfterFailure.contains(origin)
          && origin.getVariantKeys().stream().anyMatch(key -> !existingKeys.contains(key))) {
        for (AddIconRequest add : origin.toVariantIcons()) {
          if (!existingKeys.contains(add.getKey())) {
            missingIcons.computeIfAbsent(origin, key -> new ArrayList<>(3)).add(add);
          }
        }
      }
    }
    return missingIcons;
  }

  @Nonnull
  @Override
  @Transactional
  public String addDefaultIconImage(@Nonnull String key, @Nonnull DefaultIcon origin)
      throws ConflictException {
    String fileResourceId = CodeGenerator.generateUid();
    Resource resource = getDefaultIconResource(key);
    try {
      FileResource fr = FileResource.ofKey(ICON, key, MEDIA_TYPE_SVG);
      fr.setUid(fileResourceId);
      Optional<FileResource> existing = fileResourceService.findByStorageKey(fr.getStorageKey());
      if (existing.isPresent()) fr = existing.get();
      fr.setAssigned(true);
      try (InputStream image = resource.getInputStream()) {
        fileResourceService.syncSaveFileResource(fr, image);
      }
      return fr.getUid();
    } catch (IOException ex) {
      ignoredAfterFailure.add(origin);
      throw new ConflictException("Failed to create default icon resource: " + ex.getMessage());
    }
  }

  @Override
  @Transactional
  public int repairPhantomDefaultIcons() throws ConflictException {
    int c = 0;
    List<String> keys =
        Stream.of(DefaultIcon.values()).flatMap(i -> i.getVariantKeys().stream()).toList();
    IconQueryParams params = new IconQueryParams();
    params.setPaging(false);
    params.setKeys(keys);
    try {
      List<Icon> icons = getIcons(params);
      for (Icon i : icons) {
        if (!fileResourceContentStore.fileResourceContentExists(
            i.getFileResource().getStorageKey())) {
          try (InputStream image = getDefaultIconResource(i.getKey()).getInputStream()) {
            fileResourceService.syncSaveFileResource(i.getFileResource(), image);
          }
          c++;
        }
      }
    } catch (Exception ex) {
      ConflictException e = new ConflictException("Repair failed");
      e.initCause(ex);
      throw e;
    }
    return c;
  }

  private static Resource getDefaultIconResource(String key) {
    return new ClassPathResource(String.format("%s/%s.%s", ICON_PATH, key, DefaultIcon.SUFFIX));
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<Icon> getIcons(@Nonnull IconQueryParams params) throws BadRequestException {
    validateQuery(params);
    return iconStore.getIcons(params);
  }

  @Override
  @Transactional(readOnly = true)
  public long count(@Nonnull IconQueryParams params) throws BadRequestException {
    validateQuery(params);
    return iconStore.count(params);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Icon getIcon(@Nonnull String key) throws NotFoundException {
    Icon icon = iconStore.getIconByKey(key);
    if (icon == null) throw new NotFoundException(Icon.class, key);
    FileResource image = icon.getFileResource();
    if (image == null) throw new NotFoundException(Icon.class, key);
    if (!fileResourceContentStore.fileResourceContentExists(image.getStorageKey()))
      throw new NotFoundException(Icon.class, key);
    return icon;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean iconExists(@Nonnull String key) {
    try {
      return getIcon(key) != null;
    } catch (NotFoundException ex) {
      return false;
    }
  }

  @Nonnull
  @Override
  @Transactional
  public Icon addIcon(@Nonnull AddIconRequest request, @CheckForNull DefaultIcon origin)
      throws BadRequestException, NotFoundException {
    validateIconKey(request.getKey());
    validateIconDoesNotExists(request.getKey());

    String fileResourceId = request.getFileResourceId();
    FileResource image = fileResourceService.getFileResource(fileResourceId);
    if (image == null || FileResourceStorageStatus.PENDING == image.getStorageStatus())
      throw new NotFoundException(FileResource.class, fileResourceId);

    image.setAssigned(true);
    fileResourceService.updateFileResource(image);

    Icon icon = new Icon();
    icon.setKey(request.getKey());
    icon.setDescription(request.getDescription());
    icon.setKeywords(request.getKeywords());
    icon.setCustom(origin == null);
    icon.setFileResource(image);

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    if (currentUser != null) {
      icon.setCreatedBy(currentUser);
    }

    icon.setAutoFields();
    iconStore.save(icon);
    return icon;
  }

  @Override
  @Transactional
  public void updateIcon(@CheckForNull String key, @Nonnull UpdateIconRequest request)
      throws BadRequestException, NotFoundException {
    if (key == null) throw new NotFoundException(Icon.class, key);
    Icon icon = getModifiableIcon(key, "Not allowed to update default icon");

    icon.setDescription(request.getDescription());
    icon.setKeywords(request.getKeywords());
    icon.setAutoFields();

    iconStore.update(icon);
  }

  @Override
  @Transactional
  public void deleteIcon(@CheckForNull String key) throws BadRequestException, NotFoundException {
    if (key == null) throw new NotFoundException(Icon.class, key);
    Icon icon = getModifiableIcon(key, "Not allowed to delete default icon");

    FileResource image = icon.getFileResource();
    if (image != null) {
      image.setAssigned(false);
      fileResourceService.updateFileResource(image);
    }
    iconStore.delete(icon);
  }

  @Override
  @Transactional
  public int deleteOrphanDefaultIcons() {
    return iconStore.deleteOrphanDefaultIcons();
  }

  @Nonnull
  private Icon getModifiableIcon(@Nonnull String key, String message)
      throws NotFoundException, BadRequestException {
    Icon icon = iconStore.getIconByKey(key);
    if (icon == null) throw new NotFoundException(Icon.class, key);
    if (!icon.isCustom()) throw new BadRequestException(message);
    return icon;
  }

  private void validateIconKey(String key) throws BadRequestException {
    if (Strings.isNullOrEmpty(key)) throw new BadRequestException("Icon key not specified.");

    Matcher matcher = ICON_KEY_PATTERN.matcher(key.trim());

    if (!matcher.matches()) {
      throw new BadRequestException(
          String.format(
              "Icon key %s is not valid. Alphanumeric and special characters '-' and '_' are allowed",
              key));
    }
  }

  private void validateIconDoesNotExists(String key) throws BadRequestException {
    if (iconStore.getIconByKey(key) != null)
      throw new BadRequestException(String.format("Icon with key %s already exists.", key));
  }

  private void validateQuery(IconQueryParams params) throws BadRequestException {
    validateDate(params.getCreatedStartDate(), "createdStartDate %s is not valid");
    validateDate(params.getCreatedEndDate(), "createdEndDate %s is not valid");
    validateDate(params.getLastUpdatedStartDate(), "lastUpdatedStartDate %s is not valid");
    validateDate(params.getLastUpdatedEndDate(), "lastUpdatedEndDate %s is not valid");

    validateOrderBy(params);
  }

  private static void validateOrderBy(IconQueryParams params) throws BadRequestException {
    List<OrderCriteria> orders = params.getOrder();
    if (orders == null || orders.isEmpty()) return;
    Set<String> valid = Set.of("created", "lastUpdated", "key");
    for (OrderCriteria order : orders)
      if (!valid.contains(order.getField()))
        throw new BadRequestException(
            "Not a valid order property %s, valid are: %s".formatted(order.getField(), valid));
    if (orders.stream().map(OrderCriteria::getField).collect(toUnmodifiableSet()).size()
        < orders.size()) throw new BadRequestException("Cannot use same order more than once");
  }

  private static void validateDate(Date date, String template) throws BadRequestException {
    if (date != null && !dateTimeIsValid(toLongDate(date)))
      throw new BadRequestException(String.format(template, date));
  }
}
