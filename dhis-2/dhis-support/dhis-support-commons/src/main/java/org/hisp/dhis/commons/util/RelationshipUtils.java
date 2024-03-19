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
package org.hisp.dhis.commons.util;

import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;

public class RelationshipUtils {

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
            relationshipItem.getEvent());

    return identifiableObject.getUid();
  }

  private static RelationshipKey.RelationshipItemKey getRelationshipItemKey(
      RelationshipItem relationshipItem) {
    if (Objects.nonNull(relationshipItem)) {
      return RelationshipKey.RelationshipItemKey.builder()
          .trackedEntity(getUidOrEmptyString(relationshipItem.getTrackedEntity()))
          .enrollment(getUidOrEmptyString(relationshipItem.getEnrollment()))
          .event(getUidOrEmptyString(relationshipItem.getEvent()))
          .build();
    }
    throw new IllegalStateException("Unable to determine uid for relationship item");
  }

  private static String getUidOrEmptyString(BaseIdentifiableObject baseIdentifiableObject) {
    return Objects.isNull(baseIdentifiableObject)
        ? ""
        : StringUtils.trimToEmpty(baseIdentifiableObject.getUid());
  }

  private static RelationshipKey getRelationshipKey(Relationship relationship) {
    return RelationshipKey.of(
        relationship.getRelationshipType().getUid(),
        getRelationshipItemKey(relationship.getFrom()),
        getRelationshipItemKey(relationship.getTo()));
  }
}
