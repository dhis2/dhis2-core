/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.common.QueryOperator.EW;
import static org.hisp.dhis.common.QueryOperator.LIKE;

import java.util.Set;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataIntegrityTrackedEntityAttributesIndexableButBlockingTrigramOperatorsControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private TrackedEntityAttribute teaA;
  private TrackedEntityAttribute teaB;

  @BeforeEach
  void setUp() {
    teaA = createTrackedEntityAttribute('A');
    teaA.setName("teaA");
    teaA.setTrigramIndexable(true);
    teaB = createTrackedEntityAttribute('B');
    teaA.setName("teaB");
    teaB.setTrigramIndexable(true);

    manager.save(teaA);
    manager.save(teaB);
  }

  @Test
  void testTrackedEntityAttributesNotBlockingTrigramOperators() {
    assertHasNoDataIntegrityIssues(
        "trackedEntityAttributes",
        "indexable_tracked_entity_attributes_block_trigram_operators",
        true);
  }

  @Test
  void testTrackedEntityAttributesBlockingTrigramOperatorsInOneTrackedEntityAttribute() {
    blockTrigramOperators(Set.of(teaA));

    assertHasDataIntegrityIssues(
        "trackedEntityAttributes",
        "indexable_tracked_entity_attributes_block_trigram_operators",
        50,
        Set.of(teaA.getUid()),
        Set.of(teaA.getName()),
        Set.of(),
        true);
  }

  @Test
  void testTrackedEntityAttributesBlockingTrigramOperatorsInAllTrackedEntityAttribute() {
    blockTrigramOperators(Set.of(teaA, teaB));

    assertHasDataIntegrityIssues(
        "trackedEntityAttributes",
        "indexable_tracked_entity_attributes_block_trigram_operators",
        100,
        Set.of(teaA.getUid(), teaB.getUid()),
        Set.of(teaA.getName(), teaB.getName()),
        Set.of(),
        true);
  }

  private void blockTrigramOperators(Set<TrackedEntityAttribute> attributes) {
    attributes.forEach(
        attr -> {
          attr.setBlockedSearchOperators(Set.of(LIKE, EW));
          manager.update(attr);
        });
  }
}
