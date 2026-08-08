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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.model.RelationshipItem;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RelationshipMapperTest {

  private static final RelationshipMapper MAPPER = Mappers.getMapper(RelationshipMapper.class);

  @Test
  void shouldMapDeletedTrueWhenRelationshipIsDeleted() {
    org.hisp.dhis.webapi.controller.tracker.view.Relationship result = map(true);

    assertTrue(result.isDeleted());
  }

  @Test
  void shouldMapDeletedFalseWhenRelationshipIsNotDeleted() {
    org.hisp.dhis.webapi.controller.tracker.view.Relationship result = map(false);

    assertFalse(result.isDeleted());
  }

  private org.hisp.dhis.webapi.controller.tracker.view.Relationship map(boolean deleted) {
    RelationshipType type = new RelationshipType();
    type.setUid("RelTypeUiAB");

    org.hisp.dhis.tracker.model.Relationship relationship =
        new org.hisp.dhis.tracker.model.Relationship();
    relationship.setUid("RelUid1234A");
    relationship.setRelationshipType(type);
    relationship.setCreated(new Date());
    relationship.setCreatedAtClient(new Date());
    relationship.setLastUpdated(new Date());
    relationship.setDeleted(deleted);
    relationship.setFrom(new RelationshipItem());
    relationship.setTo(new RelationshipItem());

    return MAPPER.map(relationship);
  }
}
