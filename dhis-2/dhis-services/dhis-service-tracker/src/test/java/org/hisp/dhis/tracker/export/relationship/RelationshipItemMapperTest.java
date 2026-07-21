/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.relationship;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RelationshipItemMapperTest {

  private static final RelationshipItemMapper MAPPER =
      Mappers.getMapper(RelationshipItemMapper.class);

  @Test
  void shouldMapDeletedTrueWhenRelationshipIsDeleted() {
    Relationship result = map(true);

    assertTrue(result.isDeleted());
  }

  @Test
  void shouldMapDeletedFalseWhenRelationshipIsNotDeleted() {
    Relationship result = map(false);

    assertFalse(result.isDeleted());
  }

  private Relationship map(boolean deleted) {
    TrackedEntity from = new TrackedEntity();
    from.setUid("TeUid1234AB");
    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setTrackedEntity(from);

    TrackedEntity to = new TrackedEntity();
    to.setUid("TeUid5678CD");
    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntity(to);

    RelationshipType type = new RelationshipType();
    type.setUid("RelTypeUiAB");

    Relationship relationship = new Relationship();
    relationship.setUid("RelUid1234A");
    relationship.setRelationshipType(type);
    relationship.setCreated(new Date());
    relationship.setCreatedAtClient(new Date());
    relationship.setLastUpdated(new Date());
    relationship.setDeleted(deleted);
    relationship.setFrom(fromItem);
    relationship.setTo(toItem);

    return MAPPER.map(RelationshipFields.all(), relationship);
  }
}
