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

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
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
   * Process the bundle's relationships collection and remove relationships that are duplicated.
   *
   * <p>There are 5 cases (all cases assume the same relationship type):
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
   * result:  REL 1 or REL 2 has to be removed (does not matter which one)
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
   * result:  REL 1 or REL 2 has to be removed (does not matter which one)
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
   * result:  REL 1 or REL 2 has to be removed (does not matter which one)
   *
   * </pre>
   */
  @Override
  public void process(TrackerBundle bundle) {
    Predicate<Relationship> validRelationship =
        rel ->
            StringUtils.isNotEmpty(rel.getRelationship())
                && StringUtils.isNotEmpty(rel.getRelationshipType())
                && rel.getFrom() != null
                && rel.getTo() != null
                && getRelationshipType(rel.getRelationshipType(), bundle) != null;

    // Create a map where both key and value must be unique
    BidiMap<String, String> map = new DualHashBidiMap<>();

    // Add a pseudo hash of all relationships to the map. If the
    // relationship is
    // bidirectional, first
    // sort the Relationship Items
    bundle.getRelationships().stream()
        .filter(validRelationship)
        .forEach(rel -> map.put(rel.getRelationship(), hash(rel, bundle)));

    // Remove duplicated Relationships from the bundle, if any
    bundle
        .getRelationships()
        .removeIf(rel -> validRelationship.test(rel) && !map.containsKey(rel.getRelationship()));
  }

  private String hash(Relationship rel, TrackerBundle bundle) {
    RelationshipType relationshipType = getRelationshipType(rel.getRelationshipType(), bundle);
    return rel.getRelationshipType()
        + "-"
        + (relationshipType.isBidirectional() ? sortItems(rel) : rel.getFrom() + "-" + rel.getTo())
        + relationshipType.isBidirectional();
  }

  private String sortItems(Relationship rel) {
    return Stream.of(rel.getFrom().toString(), rel.getTo().toString())
        .sorted()
        .collect(Collectors.joining("-"));
  }

  private RelationshipType getRelationshipType(String uid, TrackerBundle bundle) {
    return bundle.getPreheat().get(RelationshipType.class, uid);
  }
}
