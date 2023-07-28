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
package org.hisp.dhis.tracker.preprocess;

import static org.hisp.dhis.tracker.util.RelationshipKeySupport.getRelationshipKey;
import static org.hisp.dhis.tracker.util.RelationshipKeySupport.hasRelationshipKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for removing duplicated relationships from the Tracker import
 * payload.
 *
 * @author Luciano Fiandesio
 */
@Component
public class DuplicateRelationshipsPreProcessor implements BundlePreProcessor {

  /**
   * Process the bundle's relationships collection and remove relationships that are duplicated. to
   * preserve the order, always the second one to appear is removed.
   *
   * <p>There are 4 cases (all cases assume the same relationship type):
   *
   * <pre>
   * case 1:
   *
   * REL 1 --- TEI A
   *       --- TEI B
   *
   * REL 2 --- TEI A
   *       --- TEI B
   *
   * TYPE  --- bi = false
   *
   * result:  REL 2 has to be removed
   *
   * case 2:
   *
   * REL 1 --- TEI A
   *       --- TEI B
   *
   * REL 2 --- TEI B
   *       --- TEI A
   *
   * TYPE  --- bi = false
   *
   * result: REL 1 or REL 2 are both unique, so they are not removed
   *
   * case 3:
   *
   * REL 1 --- TEI A
   *       --- TEI B
   *
   * REL 2 --- TEI B
   *       --- TEI A
   *
   * TYPE  --- bi = true
   *
   * result:  REL 2 has to be removed
   *
   *
   * case 4:
   *
   * REL 1 --- TEI A
   *       --- TEI B
   *
   * REL 2 --- TEI A
   *       --- TEI B
   *
   * TYPE  --- bi = true
   *
   * result:  REL 2 has to be removed
   *
   * </pre>
   */
  @Override
  public void process(TrackerBundle bundle) {
    List<Relationship> distinctRelationships = new ArrayList<>();
    for (Relationship relationship : bundle.getRelationships()) {
      RelationshipType relationshipType =
          bundle.getPreheat().getRelationshipType(relationship.getRelationshipType());
      if (isInvalidRelationship(relationship, relationshipType)
          || !isDuplicate(relationship, relationshipType, distinctRelationships)) {
        distinctRelationships.add(relationship);
      }
    }

    bundle.setRelationships(distinctRelationships);
  }

  public boolean isDuplicate(
      Relationship relationship,
      RelationshipType relationshipType,
      List<Relationship> distinctRelationships) {
    List<RelationshipKey> relationshipKeys =
        distinctRelationships.stream()
            .map(r -> getRelationshipKey(r, relationshipType))
            .collect(Collectors.toList());
    RelationshipKey relationshipKey = getRelationshipKey(relationship, relationshipType);

    RelationshipKey inverseKey = null;
    if (relationshipType.isBidirectional()) {
      inverseKey = relationshipKey.inverseKey();
    }
    return Stream.of(relationshipKey, inverseKey)
        .filter(Objects::nonNull)
        .anyMatch(relationshipKeys::contains);
  }

  public boolean isInvalidRelationship(
      Relationship relationship, RelationshipType relationshipType) {
    return relationshipType == null || !hasRelationshipKey(relationship, relationshipType);
  }
}
