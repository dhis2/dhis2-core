/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.setting;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonPrimitive;

/**
 * {@link Settings} are plain {@link String} key-value pairs that can be accessed as certain types
 * using of the {@code asX} helper methods.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public sealed interface Settings permits UserSettings, SystemSettings {

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
   * @return names of the settings whose value is defined as data
   */
  Set<String> keys();

  /**
   * @return a map of the names and raw values
   */
  Map<String, String> toMap();

  /**
   * @return a JSON of all public properties that are non-null with their JSON value
   */
  JsonMap<? extends JsonPrimitive> toJson();

  <E extends Enum<E>> E asEnum(String key, @Nonnull E defaultValue);

  @Nonnull
  String asString(String key, @Nonnull String defaultValue);

  @Nonnull
  Date asDate(String key, @Nonnull Date defaultValue);

  @Nonnull
  Locale asLocale(String key, @Nonnull Locale defaultValue);

  int asInt(String key, int defaultValue);

  double asDouble(String key, double defaultValue);

  boolean asBoolean(String key, boolean defaultValue);
}
