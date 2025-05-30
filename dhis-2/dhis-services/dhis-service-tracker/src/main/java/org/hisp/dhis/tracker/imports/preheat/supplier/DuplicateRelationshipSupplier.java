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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.mappers.RelationshipMapper;
import org.hisp.dhis.tracker.imports.util.RelationshipKeySupport;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DuplicateRelationshipSupplier extends AbstractPreheatSupplier {
  @Nonnull private final RelationshipService relationshipService;

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    List<org.hisp.dhis.relationship.Relationship> relationships =
        retrieveRelationshipKeys(trackerObjects.getRelationships(), preheat);

    relationships.stream()
        .map(RelationshipMapper.INSTANCE::map)
        .filter(Objects::nonNull)
        .forEach(preheat::addExistingRelationship);
  }

  private List<org.hisp.dhis.relationship.Relationship> retrieveRelationshipKeys(
      List<Relationship> relationships, TrackerPreheat preheat) {
    List<RelationshipType> relationshipTypes = preheat.getAll(RelationshipType.class);
    List<RelationshipKey> keys =
        relationships.stream()
            .filter(
                rel ->
                    RelationshipKeySupport.hasRelationshipKey(
                        rel, getRelationshipType(rel, relationshipTypes)))
            .map(
                rel ->
                    RelationshipKeySupport.getRelationshipKey(
                        rel, getRelationshipType(rel, relationshipTypes)))
            .toList();

    return relationshipService.getRelationshipsByRelationshipKeys(keys);
  }

  private RelationshipType getRelationshipType(
      Relationship rel, List<RelationshipType> relationshipTypes) {
    return relationshipTypes.stream()
        .filter(type -> rel.getRelationshipType().isEqualTo(type))
        .findAny()
        .orElse(null);
  }
}
