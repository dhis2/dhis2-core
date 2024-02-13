/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.user.UserDetails;

public interface CustomIconStore {
  /**
   * Returns a custom icon that contains a given key
   *
   * @param key of the icon
   * @return the custom icon matching the key, or null instead
   */
  CustomIcon getIconByKey(String key);

  /**
   * Returns a list of custom icons that contain all the specified keywords
   *
   * @param iconOperationParams contains query params for CustomIcon
   * @return the list of custom icons that contain all the keywords
   */
  Stream<CustomIcon> getIcons(IconOperationParams iconOperationParams);

  /**
   * Returns a list with all the custom icon keywords
   *
   * @return a list with all the custom icon keywords
   */
  Set<String> getKeywords();

  /**
   * Persists a custom icon to the database
   *
   * @param customIcon Icon to be saved
   * @param fileResource file resource linked to the custom icon
   * @param createdByUser user that created the custom icon
   */
  void save(CustomIcon customIcon, FileResource fileResource, UserDetails createdByUser);

  /**
   * Deletes a custom icon from the database
   *
   * @param customIconKey Key of the icon to be deleted
   */
  void delete(String customIconKey);

  /**
   * Updates a custom icon from the database
   *
   * @param customIcon Icon to be updated
   */
  void update(CustomIcon customIcon);

  long count(IconOperationParams iconOperationParams);
}
