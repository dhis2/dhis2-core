/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.dataelementgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataElementGroupStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementGroupStore store;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("retrieving DataElementGroups by DataElement returns expected entries")
  void dataSetElementByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    createDataElementGroupAndSave('a', deW, deX);
    createDataElementGroupAndSave('b', deY);
    createDataElementGroupAndSave('c', deZ);

    // when
    List<DataElementGroup> dataElementGroups = store.getByDataElement(List.of(deW, deX, deY));

    // then
    assertEquals(2, dataElementGroups.size());
    assertTrue(
        dataElementGroups.stream()
            .flatMap(deg -> deg.getMembers().stream())
            .toList()
            .containsAll(List.of(deW, deX, deY)));
  }

  private DataElement createDataElementAndSave(char c) {
    CategoryCombo cc = createCategoryCombo(c);
    manager.save(cc);

    DataElement de = createDataElement(c, cc);
    manager.save(de);
    return de;
  }

  private void createDataElementGroupAndSave(char c, DataElement... de) {
    DataElementGroup deg = createDataElementGroup(c);
    for (DataElement d : de) deg.addDataElement(d);
    manager.save(deg);
  }

  // -------------------------------------------------------------------------
  // JPA migration verification (HBM -> annotations)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("JPA: members round-trip through the dataelementgroupmembers join table")
  void testJpaMembersJoinTable() {
    DataElement deA = createDataElementAndSave('A');
    DataElement deB = createDataElementAndSave('B');
    DataElementGroup deg = createDataElementGroup('M', deA, deB);
    store.save(deg);
    long id = deg.getId();

    clearSession(); // force reload from DB

    DataElementGroup reloaded = store.get(id);
    assertNotNull(reloaded);
    Set<String> memberUids = new HashSet<>();
    reloaded.getMembers().forEach(de -> memberUids.add(de.getUid()));
    assertEquals(2, memberUids.size());
    assertTrue(memberUids.containsAll(Set.of(deA.getUid(), deB.getUid())));
  }

  @Test
  @DisplayName(
      "JPA: groupSets (inverse mappedBy) round-trips against the still-HBM-mapped "
          + "DataElementGroupSet owning side")
  void testJpaGroupSetsInverseSide() {
    DataElementGroup deg = createDataElementGroup('G');
    manager.save(deg);

    DataElementGroupSet degs = createDataElementGroupSet('S');
    degs.addDataElementGroup(deg);
    manager.save(degs);
    long id = deg.getId();

    clearSession(); // force reload from DB

    DataElementGroup reloaded = store.get(id);
    assertNotNull(reloaded);
    assertEquals(1, reloaded.getGroupSets().size());
    assertEquals(degs.getUid(), reloaded.getGroupSets().iterator().next().getUid());
  }

  @Test
  @DisplayName("JPA: createdBy IS persisted (dataelementgroup has a userid column)")
  void testJpaCreatedByPersisted() {
    DataElementGroup deg = createDataElementGroup('U');
    deg.setCreatedBy(getAdminUser());
    store.save(deg);
    long id = deg.getId();

    clearSession();

    DataElementGroup reloaded = store.get(id);
    assertNotNull(reloaded.getCreatedBy(), "createdBy must persist to the userid column");
    assertEquals(getAdminUser().getUid(), reloaded.getCreatedBy().getUid());
  }

  @Test
  @DisplayName("JPA: name, shortName and description (text) round-trip")
  void testJpaScalarFieldsPersist() {
    DataElementGroup deg = createDataElementGroup('T');
    deg.setDescription("a description");
    store.save(deg);
    long id = deg.getId();

    clearSession();

    DataElementGroup reloaded = store.get(id);
    assertEquals(deg.getName(), reloaded.getName());
    assertEquals(deg.getShortName(), reloaded.getShortName());
    assertEquals("a description", reloaded.getDescription());
  }

  @Test
  @DisplayName("JPA: attributeValues (jsonb) round-trip")
  void testJpaAttributeValuesPersisted() {
    DataElementGroup deg = createDataElementGroup('V');
    deg.setAttributeValues(
        AttributeValues.of(Map.<CharSequence, CharSequence>of("hQKI6KcEu5t", "avalue")));
    store.save(deg);
    long id = deg.getId();

    clearSession();

    DataElementGroup reloaded = store.get(id);
    assertFalse(reloaded.getAttributeValues().isEmpty());
    assertEquals("avalue", reloaded.getAttributeValues().get("hQKI6KcEu5t"));
  }

  @Test
  @DisplayName("JPA: id is generated (SEQUENCE) on save")
  void testJpaIdGeneration() {
    DataElementGroup deg = createDataElementGroup('I');
    store.save(deg);
    assertTrue(deg.getId() > 0, "id must be generated on save");
    assertNotNull(deg.getUid());
  }
}
