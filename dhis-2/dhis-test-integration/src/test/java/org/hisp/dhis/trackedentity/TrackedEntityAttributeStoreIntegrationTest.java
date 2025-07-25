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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ameen
 */
class TrackedEntityAttributeStoreIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  private TrackedEntityAttribute attributeW;

  private TrackedEntityAttribute attributeY;

  private TrackedEntityAttribute attributeZ;

  @BeforeEach
  void setUp() {
    attributeW = createTrackedEntityAttribute('W');
    attributeY = createTrackedEntityAttribute('Y');
    attributeZ = createTrackedEntityAttribute('Z');
  }

  @AfterEach
  void dropAllTrigramIndexes() {
    // DDL statements like `CREATE INDEX` are not rolled back, so here I'm cleaning up after each
    // use
    List<Long> indexedAttributes =
        trackedEntityAttributeTableManager.getAttributesWithTrigramIndex();
    indexedAttributes.forEach(attr -> trackedEntityAttributeTableManager.dropTrigramIndex(attr));
  }

  @Test
  void testGetAllIndexableAttributes() {
    attributeW.setTrigramIndexable(true);
    attributeService.addTrackedEntityAttribute(attributeW);
    attributeY.setTrigramIndexable(true);
    attributeService.addTrackedEntityAttribute(attributeY);
    attributeZ.setTrigramIndexable(true);
    attributeService.addTrackedEntityAttribute(attributeZ);

    Set<TrackedEntityAttribute> indexableAttributes =
        attributeService.getAllTrigramIndexableAttributes();

    assertContainsOnly(Set.of(attributeW, attributeY, attributeZ), indexableAttributes);
    assertTrue(indexableAttributes.contains(attributeW));
    assertTrue(indexableAttributes.contains(attributeY));
  }

  @Test
  void testCreateTrigramIndex() {
    attributeService.addTrackedEntityAttribute(attributeW);
    trackedEntityAttributeTableManager.createTrigramIndex(attributeW);
    assertHasSize(1, getTrigramIndexesInDb());
  }

  @Test
  void shouldReturnAllTrigramIndexedAttributes() {
    attributeService.addTrackedEntityAttribute(attributeY);
    trackedEntityAttributeTableManager.createTrigramIndex(attributeY);
    attributeService.addTrackedEntityAttribute(attributeZ);
    createTrigramIndexWithCustomNaming(attributeZ, "trigram_index_name");

    List<Long> attributeIds = trackedEntityAttributeTableManager.getAttributesWithTrigramIndex();

    assertContainsOnly(List.of(attributeY.getId(), attributeZ.getId()), attributeIds);
  }

  @Test
  void shouldDropTrigramIndexes() {
    attributeService.addTrackedEntityAttribute(attributeW);
    trackedEntityAttributeTableManager.createTrigramIndex(attributeW);
    attributeService.addTrackedEntityAttribute(attributeY);
    trackedEntityAttributeTableManager.createTrigramIndex(attributeY);
    attributeService.addTrackedEntityAttribute(attributeZ);
    createTrigramIndexWithCustomNaming(attributeZ, "trigram_index_name");
    assertHasSize(3, getTrigramIndexesInDb());

    trackedEntityAttributeTableManager.dropTrigramIndex(attributeW.getId());
    trackedEntityAttributeTableManager.dropTrigramIndex(attributeZ.getId());
    assertHasSize(1, getTrigramIndexesInDb());
  }

  private void createTrigramIndexWithCustomNaming(
      TrackedEntityAttribute attribute, String indexName) {
    attributeService.addTrackedEntityAttribute(attributeY);

    String query =
        String.format(
            """
                CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON trackedentityattributevalue
                USING gin (trackedentityid,lower(value) gin_trgm_ops) where trackedentityattributeid = %d
            """,
            indexName, attribute.getId());

    jdbcTemplate.execute(query);
  }

  private List<Long> getTrigramIndexesInDb() {
    return trackedEntityAttributeTableManager.getAttributesWithTrigramIndex();
  }
}
