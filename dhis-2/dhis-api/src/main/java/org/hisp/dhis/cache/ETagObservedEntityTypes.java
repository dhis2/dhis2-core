/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.cache;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.datastatistics.DataStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;

/** Shared rules for entity types that can participate in API ETag invalidation. */
@Slf4j
public final class ETagObservedEntityTypes {

  // SystemSetting is in dhis-service-setting (not a dhis-api dependency). Resolved reflectively
  // so a rename causes a ClassNotFoundException rather than a silent mismatch.
  private static final String SYSTEM_SETTING_CLASS = "org.hisp.dhis.setting.SystemSetting";

  private static final Set<String> ADDITIONAL_OBSERVED_TYPE_NAMES = buildAdditionalTypes();

  private static Set<String> buildAdditionalTypes() {
    Set<String> types = new LinkedHashSet<>();
    types.add(Configuration.class.getName());
    types.add(FileResource.class.getName());
    types.add(UserSetting.class.getName());
    types.add(DatastoreEntry.class.getName());
    types.add(UserDatastoreEntry.class.getName());
    types.add(DataStatistics.class.getName());
    types.add(DataStatisticsEvent.class.getName());
    try {
      types.add(Class.forName(SYSTEM_SETTING_CLASS).getName());
    } catch (ClassNotFoundException e) {
      log.warn(
          "SystemSetting class not found at {}. "
              + "Was it moved or renamed? ETag cache invalidation for system settings will not work.",
          SYSTEM_SETTING_CLASS);
      types.add(SYSTEM_SETTING_CLASS);
    }
    return Set.copyOf(types);
  }

  /** Cache for isObservedType results — avoids repeated isAssignableFrom hierarchy walks. */
  private static final Map<Class<?>, Boolean> OBSERVED_TYPE_CACHE = new ConcurrentHashMap<>();

  private ETagObservedEntityTypes() {}

  public static boolean isObservedType(@Nonnull Class<?> entityType) {
    return OBSERVED_TYPE_CACHE.computeIfAbsent(
        entityType,
        type ->
            MetadataObject.class.isAssignableFrom(type)
                || ADDITIONAL_OBSERVED_TYPE_NAMES.contains(type.getName()));
  }

  public static Set<String> getAdditionalObservedTypeNames() {
    return ADDITIONAL_OBSERVED_TYPE_NAMES;
  }
}
