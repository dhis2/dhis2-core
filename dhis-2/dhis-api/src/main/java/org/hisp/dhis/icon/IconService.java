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

import java.util.Collection;
import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * @author Kristian Wærstad
 */
public interface IconService {
  /**
   * Gets data about all the icons in the system
   *
   * @return a collection of data about all the icons in the system
   */
  Collection<IconData> getIcons();

  /**
   * Gets info about the icons in the system tagged with all the keywords in a collection
   *
   * @param keywords collection of keywords
   * @return a collection of matching icons
   */
  Collection<IconData> getIcons(Collection<String> keywords);

  /**
   * Gets the info of the icon associated with a specific key if there is one
   *
   * @param key key of the icon
   * @return icon data associated with the key if there is one
   */
  Optional<IconData> getIcon(String key);

  /**
   * Gets the icon with the correct key if one exists
   *
   * @param key key of the icon
   * @return the icon resource
   */
  Optional<Resource> getIconResource(String key);

  /**
   * Gets a collection of all unique keywords assigned to icons
   *
   * @return collection of uniquee keywords
   */
  Collection<String> getKeywords();
}
