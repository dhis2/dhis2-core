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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.createWebMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.springframework.http.HttpStatus;

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

  private final Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> errors =
      new HashMap<>();
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

    boolean hasDefaultCategory = false;

    for (Entry<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> entry :
        errors.entrySet()) {
      Set<IdentifiableObject> metadatas = entry.getValue();
      if (metadatas.isEmpty()) {
        continue;
      }

      appendMetadataPrefix(result, entry);

      if (appendUids(result, metadatas)) {
        hasDefaultCategory = true;
      }
    }

    // default category option (combo) are guaranteed to have a UID, name and code but no attribute
    // values
    if (hasDefaultCategory) {
      result.append(
"""


Data linked to default category option (combo)s cannot be exported using\
 idScheme=ATTRIBUTE as they cannot have any attribute values.""");
    }
    return result.toString();
  }

  private void appendMetadataPrefix(
      StringBuilder result,
      Entry<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> entry) {
    Class<? extends IdentifiableObject> metadataClass = entry.getKey();
    result.append("\n");
    result.append(metadataClass.getSimpleName());
    result.append("[");
    result.append(idSchemeParams.getByClass(metadataClass));
    result.append("]=");
  }

  private boolean appendUids(StringBuilder result, Set<IdentifiableObject> metadatas) {
    int count = 1;
    int size = metadatas.size();
    boolean hasDefaultCategory = false;
    for (IdentifiableObject metadata : metadatas) {
      if (count > DISPLAY_MAX_UIDS) {
        result.append("...");
        continue;
      }

      result.append(metadata.getUid());

      if (isDefaultCategory(metadata)) {
        hasDefaultCategory = true;
        result.append("(default)");
      }

      if (count < size) {
        result.append(",");
      }
      count++;
    }
    return hasDefaultCategory;
  }

  private static boolean isDefaultCategory(IdentifiableObject metadata) {
    return metadata instanceof CategoryOptionCombo categoryOptionCombo
            && categoryOptionCombo.isDefault()
        || metadata instanceof CategoryOption categoryOption && categoryOption.isDefault();
  }

  public static void ensureNoMappingErrors(MappingErrors errors) throws WebMessageException {
    if (errors.hasErrors()) {
      throw new WebMessageException(
          createWebMessage(
              "Not all metadata has an identifier for the requested idScheme. Either change the"
                  + " requested idScheme or add the missing identifiers to the metadata.",
              errors.toString(),
              org.hisp.dhis.feedback.Status.ERROR,
              HttpStatus.UNPROCESSABLE_ENTITY));
    }
  }
}
