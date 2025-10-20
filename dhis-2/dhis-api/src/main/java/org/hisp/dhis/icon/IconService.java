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

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;

/**
 * @author Kristian Wærstad
 */
public interface IconService {

  /**
   * The {@link Icon}s returned by this method are not persisted in DB.
   *
   * @return an {@link Icon} for all {@link DefaultIcon}s variants that do not yet exist
   */
  @Nonnull
  Map<DefaultIcon, List<AddIconRequest>> findNonExistingDefaultIcons();

  /**
   * To creates the {@link org.hisp.dhis.fileresource.FileResource} for the default icon provided
   *
   * @param key of the default icon to create (includes variant)
   * @param origin of th {@link DefaultIcon} this represents
   * @return the UID of the crated {@link org.hisp.dhis.fileresource.FileResource}
   */
  @Nonnull
  String addDefaultIconImage(@Nonnull String key, @Nonnull DefaultIcon origin)
      throws ConflictException;

  /**
   * A phantom default icon is an icon that exists as {@link
   * org.hisp.dhis.fileresource.FileResource} but for some reason has lost its file in the store. To
   * repair the icon the file is re-uploaded.
   *
   * @return the number of {@link DefaultIcon} {@link org.hisp.dhis.fileresource.FileResource}s that
   *     were repaired.
   * @throws ConflictException when an exception occurred during repair
   */
  int repairPhantomDefaultIcons() throws ConflictException;

  /**
   * Get the count of Icons based on filters provided in {@link IconQueryParams}
   *
   * @param params filters
   * @return total count
   */
  long count(@Nonnull IconQueryParams params) throws BadRequestException;

  /**
   * Get list of Icons based on filters provided in {@link IconQueryParams}
   *
   * @param params filters to build query
   * @return list of Icons
   */
  @Nonnull
  List<Icon> getIcons(@Nonnull IconQueryParams params) throws BadRequestException;

  /**
   * Gets the icon associated to a key, if it exists
   *
   * @param key key of the icon to find
   * @return custom icon associated to the key, if found
   * @throws NotFoundException if no icon exists with the provided key
   */
  @Nonnull
  Icon getIcon(@Nonnull String key) throws NotFoundException;

  /**
   * Checks whether an icon with a given key exists, either default or custom
   *
   * @param key key of the icon
   * @return true if the icon exists, false otherwise
   */
  boolean iconExists(@Nonnull String key);

  /**
   * Persists the provided icon to the database
   *
   * @param request details the icon to be created
   * @param origin in case the icon represents a variant of a {@link DefaultIcon}
   * @throws BadRequestException when an icon already exists with the same key or the file resource
   *     id is not specified
   * @throws NotFoundException when no file resource with the provided id exists
   * @throws BadRequestException when another icon with the same key already exists
   * @return the created and persisted {@link Icon}
   */
  @Nonnull
  Icon addIcon(@Nonnull AddIconRequest request, @CheckForNull DefaultIcon origin)
      throws BadRequestException, NotFoundException;

  /**
   * Updated the provided icon
   *
   * @param request the icon to be updated
   */
  void updateIcon(@CheckForNull String key, @Nonnull UpdateIconRequest request)
      throws BadRequestException, NotFoundException;

  /**
   * Deletes a given Icon
   *
   * @param key of the icon to be deleted
   * @throws BadRequestException when icon key is not specified
   * @throws NotFoundException when no icon with the provided key exists
   */
  void deleteIcon(@CheckForNull String key) throws BadRequestException, NotFoundException;

  /**
   * @return number of icons deleted because they were not custom but refer to a non-existing {@link
   *     org.hisp.dhis.fileresource.FileResource}
   */
  int deleteOrphanDefaultIcons();
}
