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
package org.hisp.dhis.i18n;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import lombok.AllArgsConstructor;

/**
 * @author Pham Thi Thuy
 * @author Nguyen Dang Quang
 * @author Anders Gjendem
 */
@AllArgsConstructor
public class I18n {
  private final ResourceBundle globalResourceBundle;

  private final ResourceBundle specificResourceBundle;

  /**
   * Get a translated String for a given key for the currently selected locale
   *
   * @param key the key for a given translation
   * @return a translated String for a given key, or the key if no translation is found.
   */
  public String getString(String key) {
    return getString(key, key);
  }

  /**
   * Get a translated String for a given key for the currently selected locale
   *
   * @param key the key for a given translation
   * @return a translated String for a given key, or the provided default value if no translation is
   *     found.
   */
  public String getString(String key, String defaultValue) {
    String translation = defaultValue;

    if (specificResourceBundle != null) {
      translation = getBundleString(specificResourceBundle, key, translation);
    }

    if (Objects.equals(defaultValue, translation) && globalResourceBundle != null) {
      translation = getBundleString(globalResourceBundle, key, translation);
    }

    return translation;
  }

  private String getBundleString(ResourceBundle bundle, String key, String defaultValue) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException ignored) {
      return defaultValue;
    }
  }
}
