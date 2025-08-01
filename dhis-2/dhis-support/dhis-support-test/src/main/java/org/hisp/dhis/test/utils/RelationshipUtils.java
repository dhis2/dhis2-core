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
package org.hisp.dhis.test.utils;

import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;

public class RelationshipUtils {

  private RelationshipUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Generates a key of a relationship. The key consists of three parts: The relationship type's
   * uid, from's uid and to's uid, split by an underscore.
   *
   * @param relationship the relationship to generate a key for
   * @return a key
   */
  public static String generateRelationshipKey(Relationship relationship) {
    return getRelationshipKey(relationship).asString();
  }

  /**
   * Generates an inverted key of a relationship. The inverted key consists of three parts: The
   * relationship type's uid, to's uid and from's uid, split by an underscore.
   *
   * @param relationship the relationship to generate an inverted key for
   * @return an inverted key
   */
  public static String generateRelationshipInvertedKey(Relationship relationship) {
    return getRelationshipKey(relationship).inverseKey().asString();
  }

  /**
   * Extracts the uid of the entity represented in a RelationshipItem. A RelationshipItem should
   * only have a single entity represented, and this method will return the first non null entity it
   * fields.
   *
   * @param relationshipItem to extract uid of
   * @return a uid
   */
  public static String extractRelationshipItemUid(RelationshipItem relationshipItem) {
    IdentifiableObject identifiableObject =
        ObjectUtils.firstNonNull(
            relationshipItem.getTrackedEntity(),
            relationshipItem.getEnrollment(),
            relationshipItem.getTrackerEvent(),
            relationshipItem.getSingleEvent());

    return identifiableObject.getUid();
  }

  private static RelationshipKey.RelationshipItemKey getRelationshipItemKey(
      RelationshipItem relationshipItem) {
    if (Objects.nonNull(relationshipItem)) {
      return RelationshipKey.RelationshipItemKey.builder()
          .trackedEntity(getUidOrNull(relationshipItem.getTrackedEntity()))
          .enrollment(getUidOrNull(relationshipItem.getEnrollment()))
          .trackerEvent(getUidOrNull(relationshipItem.getTrackerEvent()))
          .singleEvent(getUidOrNull(relationshipItem.getSingleEvent()))
          .build();
    }
    throw new IllegalStateException("Unable to determine uid for relationship item");
  }

  private static UID getUidOrNull(IdentifiableObject baseIdentifiableObject) {
    return Objects.isNull(baseIdentifiableObject) ? null : UID.of(baseIdentifiableObject);
  }

  private static RelationshipKey getRelationshipKey(Relationship relationship) {
    return RelationshipKey.of(
        relationship.getRelationshipType().getUid(),
        getRelationshipItemKey(relationship.getFrom()),
        getRelationshipItemKey(relationship.getTo()));
  }
}
