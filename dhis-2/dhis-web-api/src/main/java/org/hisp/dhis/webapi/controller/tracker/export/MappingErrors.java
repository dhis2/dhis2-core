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
package org.hisp.dhis.webapi.controller.tracker.export;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;

/**
 * MappingErrors collects and reports metadata that does not have an identifier for the requested
 * {@code idScheme}. Metadata will be reported using their {@code UID} as every metadata must have
 * one.
 */
@RequiredArgsConstructor
public class MappingErrors {
  // Maximum number of missing identifiers that are reported. This to safeguard against requests
  // with a large number of items like paging=false. Admins need to use other tools to fix their
  // metadata if such large numbers do not have identifiers for the requested idScheme.
  private static final int DISPLAY_MAX_UIDS = 20;

  private final Map<Class<?>, Set<IdentifiableObject>> errors = new HashMap<>();
  private final TrackerIdSchemeParams idSchemeParams;

  /**
   * Add the metadata to the set of errors to report it as not having an identifier for the
   * requested {@code idScheme}.
   */
  public <T extends IdentifiableObject & MetadataObject> void add(T metadata) {
    errors.computeIfAbsent(metadata.getClass(), k -> new HashSet<>()).add(metadata);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  @Override
  public String toString() {
    if (!hasErrors()) {
      return "";
    }

    StringBuilder result =
        new StringBuilder(
            "Following metadata listed using their UIDs is missing identifiers for the requested"
                + " idScheme:\n");

    AtomicBoolean hasDefaultCategory = new AtomicBoolean();

    errors.forEach(
        (metadataClass, metadatas) -> {
          if (metadatas.isEmpty()) {
            return;
          }

          result.append("\n");
          result.append(metadataClass.getSimpleName());
          result.append("[");
          result.append(idSchemeParams.getByClass(metadataClass));
          result.append("]=");

          int count = 1;
          int size = metadatas.size();
          for (IdentifiableObject metadata : metadatas) {
            if (count > DISPLAY_MAX_UIDS) {
              result.append("...");
              return;
            }

            result.append(metadata.getUid());
            if (isDefaultCategory(metadata)) {
              hasDefaultCategory.set(true);
              result.append("(default)");
            }
            if (count < size) {
              result.append(",");
            }
            count++;
          }
        });

    // default category option (combo) are guaranteed to have a UID, name and code but no attribute
    // values
    if (hasDefaultCategory.get()) {
      result.append(
          """


Data linked to default category option (combo)s cannot be exported using\
 idScheme=ATTRIBUTE as they cannot have any attribute values.""");
    }
    return result.toString();
  }

  private static boolean isDefaultCategory(IdentifiableObject metadata) {
    return metadata instanceof CategoryOptionCombo categoryOptionCombo
            && CategoryOptionCombo.DEFAULT_NAME.equals(
                categoryOptionCombo.getName()) // CategoryOptionCombo.isDefault
        // checks the CategoryCombo.name and we might not have mapped the CategoryCombo
        || metadata instanceof CategoryOption categoryOption && categoryOption.isDefault();
  }
}
