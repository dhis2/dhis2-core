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

import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;

/**
 * @author Kristian Wærstad
 */
public interface IconService {

  /** To create list of default icons which are not persisted in database. */
  void createDefaultIcons();

  /**
   * Get the count of Icons based on filters provided in {@link IconQueryParams}
   *
   * @param params filters
   * @return total count
   */
  long count(IconQueryParams params);

  /**
   * Get list of Icons based on filters provided in {@link IconQueryParams}
   *
   * @param params filters to build query
   * @return list of Icons
   */
  List<Icon> getIcons(IconQueryParams params);

  /**
   * Gets the icon associated to a key, if it exists
   *
   * @param key key of the icon to find
   * @return custom icon associated to the key, if found
   * @throws NotFoundException if no custom icon exists with the provided key
   */
  Icon getIcon(String key) throws NotFoundException;

  /**
   * Checks whether an icon with a given key exists, either default or custom
   *
   * @param key key of the icon
   * @return true if the icon exists, false otherwise
   */
  boolean iconExists(String key);

  /**
   * Persists the provided icon to the database
   *
   * @param icon the icon to be persisted
   * @throws BadRequestException when an icon already exists with the same key or the file resource
   *     id is not specified
   * @throws NotFoundException when no file resource with the provided id exists
   */
  void addIcon(@Nonnull Icon icon) throws BadRequestException, NotFoundException, SQLException;

  /**
   * Updated the provided icon
   *
   * @param icon the icon to be updated
   */
  void updateIcon(@Nonnull Icon icon) throws BadRequestException, NotFoundException, SQLException;

  /**
   * Deletes a given Icon
   *
   * @param key of the icon to be deleted
   * @throws BadRequestException when icon key is not specified
   * @throws NotFoundException when no icon with the provided key exists
   */
  void deleteIcon(String key) throws BadRequestException, NotFoundException;
}
