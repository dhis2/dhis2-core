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

import java.util.List;
import java.util.Set;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DataIntegrityTrackedEntityAttributesCreatedPartialTrigramIndexesControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  @Autowired private TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  private TrackedEntityAttribute teaA;
  private TrackedEntityAttribute teaB;
  private Set<TrackedEntityAttribute> attributes;

  private static final String TRIGRAM_INDEX_CREATE_QUERY =
      "CREATE INDEX IF NOT EXISTS in_gin_teavalue_%d ON "
          + "trackedentityattributevalue USING gin (trackedentityid,lower(value) gin_trgm_ops) where trackedentityattributeid = %d";

  @BeforeEach
  void setUp() {
    teaA = createTrackedEntityAttribute('A');
    teaA.setName("teaA");
    teaB = createTrackedEntityAttribute('B');
    teaB.setName("teaB");
    attributes = Set.of(teaA, teaB);

    manager.save(teaA);
    manager.save(teaB);
  }

  @AfterEach
  void dropAllTrigramIndexes() {
    // DDL statements like `CREATE INDEX` are not rolled back, so here I'm cleaning up after each
    // use
    List<Long> indexedAttributes =
        trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndex();
    indexedAttributes.forEach(attr -> trackedEntityAttributeTableManager.dropTrigramIndex(attr));
  }

  @Test
  void testTrackedEntityAttributesWithNoTrigramIndexesCreated() {
    assertHasNoDataIntegrityIssues(
        "trackedEntityAttributes", "tracked_entity_attributes_created_trigram_indexes", true);
  }

  @Test
  void testTrackedEntityAttributesWithOneHavingATrigramIndexCreated() {
    createTrigramIndexes(Set.of(teaA));

    assertHasDataIntegrityIssues(
        "trackedEntityAttributes",
        "tracked_entity_attributes_created_trigram_indexes",
        50,
        Set.of(teaA.getUid()),
        Set.of(teaA.getName()),
        Set.of(),
        true);
  }

  @Test
  void testTrackedEntityAttributesWithAllHavingATrigramIndexCreated() {
    createTrigramIndexes(attributes);

    assertHasDataIntegrityIssues(
        "trackedEntityAttributes",
        "tracked_entity_attributes_created_trigram_indexes",
        100,
        Set.of(teaA.getUid(), teaB.getUid()),
        Set.of(teaA.getName(), teaB.getName()),
        Set.of(),
        true);
  }

  public void createTrigramIndexes(Set<TrackedEntityAttribute> attributes) {
    attributes.forEach(
        attr -> {
          String query = String.format(TRIGRAM_INDEX_CREATE_QUERY, attr.getId(), attr.getId());
          jdbcTemplate.execute(query);
        });
  }
}
