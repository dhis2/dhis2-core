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
package org.hisp.dhis.tracker.imports.validation.validator.relationship;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;

/**
 * @author Enrico Colasante
 */
class ValidationUtils {

  private ValidationUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static TrackerType relationshipItemValueType(RelationshipItem item) {
    if (item.getTrackedEntity() != null) {
      return TrackerType.TRACKED_ENTITY;
    } else if (item.getEnrollment() != null) {
      return TrackerType.ENROLLMENT;
    } else if (item.getEvent() != null) {
      return TrackerType.EVENT;
    }
    return null;
  }

  public static UID getUidFromRelationshipItem(RelationshipItem item) {
    return ObjectUtils.firstNonNull(item.getTrackedEntity(), item.getEnrollment(), item.getEvent());
  }
}
