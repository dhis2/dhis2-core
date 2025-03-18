/*
 * Copyright (c) 2004-2023, University of Oslo
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

public interface IconStore {

  /**
   * Persist {@link Icon}
   *
   * @param icon to persist
   */
  void save(Icon icon);

  /**
   * Remove {@link Icon}
   *
   * @param icon to remove
   */
  void delete(Icon icon);

  /**
   * Update {@link Icon}
   *
   * @param icon to update
   */
  void update(Icon icon);

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
   * @return all existing keys, a key includes the variant suffix
   */
  List<String> getAllKeys();

  /**
   * Returns an icon that contains a given key
   *
   * @param key of the icon
   * @return the custom icon matching the key, or null instead
   */
  Icon getIconByKey(String key);

  /**
   * @return number of icons deleted because they were not custom but refer to a non-existing {@link
   *     org.hisp.dhis.fileresource.FileResource}
   */
  int deleteOrphanDefaultIcons();
}
