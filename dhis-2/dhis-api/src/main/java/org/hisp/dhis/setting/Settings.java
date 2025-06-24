/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.setting;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;

/**
 * {@link Settings} are plain {@link String} key-value pairs that can be accessed as certain types
 * using of the {@code asX} helper methods.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public sealed interface Settings permits UserSettings, SystemSettings {

  /**
   * Resolves the key name by a Java property name as used in the settings interface
   *
   * @param property as derived from a default method in a {@link Settings} interface
   * @return the key name for the given property name
   */
  @Nonnull
  static String getKey(@Nonnull String property) {
    String key = LazySettings.keyOf(property);
    return key == null ? property : key;
  }

  /**
   * Converts Java types to a setting raw {@link String} value
   *
   * @param value a {@link Boolean}, {@link Number}, {@link String}, {@link Date}, {@link Locale} or
   *     enum
   * @return the equivalent raw {@link String} setting value. This is the value that can be parsed
   *     back into the given Java type
   */
  @Nonnull
  static String valueOf(@CheckForNull Serializable value) {
    if (value == null) return "";
    if (value instanceof Date d)
      return String.valueOf(d.getTime()); // Date.toString() is not lossless and can't be used
    if (value instanceof Enum<?> e)
      return e.name(); // just in case toString() was overridden this should be closest to historic
    // behaviour
    return value.toString();
  }

  /**
   * Note that this only entails the names that have a defined value in the database. Settings that
   * are not defined in data and which do fall back to their default value are not included. This
   * can be used to test if a default would take an effect in case logic needs to combine several
   * settings based on them being defined.
   *
   * @return names of the settings whose value is explicitly defined
   */
  Set<String> keys();

  /**
   * This can be seen as the inverse operation to constructing a settings object from a map.
   *
   * @return a map of the names and raw values that are explicitly defined (this excludes any value
   *     only provided a value because of defaults)
   */
  Map<String, String> toMap();

  /**
   * @param includeConfidential true to include, false to exclude confidential settings
   * @return a JSON of all settings including those only defined through defaults
   */
  JsonMap<JsonMixed> toJson(boolean includeConfidential);

  /**
   * @param includeConfidential true to include, false to exclude confidential settings
   * @param keys the keys to extract, empty set includes all
   * @return a JSON of the settings for the requested keys; if a confidential key is included by
   *     #includeConfidential is {@code false} the key is not included in the result
   */
  JsonMap<JsonMixed> toJson(boolean includeConfidential, @Nonnull Set<String> keys);

  @Nonnull
  <E extends Enum<?>> E asEnum(@Nonnull String key, @Nonnull E defaultValue);

  @Nonnull
  String asString(@Nonnull String key, @Nonnull String defaultValue);

  @Nonnull
  Date asDate(@Nonnull String key, @Nonnull Date defaultValue);

  @Nonnull
  Locale asLocale(@Nonnull String key, @Nonnull Locale defaultValue);

  int asInt(@Nonnull String key, int defaultValue);

  double asDouble(@Nonnull String key, double defaultValue);

  boolean asBoolean(@Nonnull String key, boolean defaultValue);

  boolean isValid(String key, String value);
}
