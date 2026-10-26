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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetStore;
import org.hisp.dhis.dataelement.DataElementGroupStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Verifies the JPA annotation mapping of {@link DataElementGroupSet} (formerly HBM). */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataElementGroupSetStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementGroupSetStore store;
  @Autowired private DataElementGroupStore groupStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("members keep their list order and sort_order is 1-based")
  void testMembersOrderPersisted() {
    DataElementGroup degC = saveGroup('C');
    DataElementGroup degA = saveGroup('A');
    DataElementGroup degB = saveGroup('B');

    DataElementGroupSet degs = createDataElementGroupSet('O');
    degs.addDataElementGroup(degC);
    degs.addDataElementGroup(degA);
    degs.addDataElementGroup(degB);
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet reloaded = store.get(id);
    assertEquals(List.of(degC.getUid(), degA.getUid(), degB.getUid()), memberUids(reloaded));

    List<Integer> sortOrders =
        jdbcTemplate.queryForList(
            "select sort_order from dataelementgroupsetmembers"
                + " where dataelementgroupsetid = ? order by sort_order",
            Integer.class,
            id);
    assertEquals(List.of(1, 2, 3), sortOrders);
  }

  @Test
  @DisplayName("members can be reordered and removed")
  void testMembersUpdate() {
    DataElementGroup degA = saveGroup('D');
    DataElementGroup degB = saveGroup('E');
    DataElementGroup degC = saveGroup('F');

    DataElementGroupSet degs = createDataElementGroupSet('U');
    degs.addDataElementGroup(degA);
    degs.addDataElementGroup(degB);
    degs.addDataElementGroup(degC);
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet loaded = store.get(id);
    DataElementGroup first = loaded.getMembers().get(0);
    loaded.removeDataElementGroup(first);
    loaded.getMembers().add(loaded.getMembers().remove(0));
    store.update(loaded);

    clearSession();

    DataElementGroupSet reloaded = store.get(id);
    assertEquals(List.of(degC.getUid(), degB.getUid()), memberUids(reloaded));
  }

  @Test
  @DisplayName("members owning side is visible from DataElementGroup.groupSets (inverse side)")
  void testInverseGroupSets() {
    DataElementGroup deg = saveGroup('G');
    DataElementGroupSet degs = createDataElementGroupSet('G');
    degs.addDataElementGroup(deg);
    store.save(degs);
    long groupId = deg.getId();

    clearSession();

    DataElementGroup reloaded = groupStore.get(groupId);
    assertEquals(1, reloaded.getGroupSets().size());
    assertEquals(degs.getUid(), reloaded.getGroupSets().iterator().next().getUid());
  }

  @Test
  @DisplayName("deleting a group set removes its join rows but keeps the groups")
  void testDeleteKeepsGroups() {
    DataElementGroup deg = saveGroup('H');
    DataElementGroupSet degs = createDataElementGroupSet('H');
    degs.addDataElementGroup(deg);
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet loaded = store.get(id);
    loaded.removeAllDataElementGroups();
    store.delete(loaded);

    clearSession();

    assertNull(store.get(id));
    assertNotNull(groupStore.get(deg.getId()));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from dataelementgroupsetmembers where dataelementgroupsetid = ?",
            Integer.class,
            id));
  }

  @Test
  @DisplayName("scalar fields round-trip, description is unbounded text")
  void testScalarFieldsPersisted() {
    String longDescription = "d".repeat(1000);
    DataElementGroupSet degs = createDataElementGroupSet('S');
    degs.setCode("DEGS_CODE_S");
    degs.setDescription(longDescription);
    degs.setCompulsory(true);
    degs.setDataDimension(false);
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet reloaded = store.get(id);
    assertEquals(degs.getUid(), reloaded.getUid());
    assertEquals("DEGS_CODE_S", reloaded.getCode());
    assertEquals(degs.getName(), reloaded.getName());
    assertEquals(degs.getShortName(), reloaded.getShortName());
    assertEquals(longDescription, reloaded.getDescription());
    assertTrue(reloaded.isCompulsory());
    assertFalse(reloaded.isDataDimension());
    assertNotNull(reloaded.getCreated());
    assertNotNull(reloaded.getLastUpdated());
  }

  @Test
  @DisplayName("null compulsory column reads back as false")
  void testNullCompulsory() {
    DataElementGroupSet degs = createDataElementGroupSet('N');
    degs.setCompulsory(null);
    store.save(degs);
    long id = degs.getId();

    clearSession();

    assertFalse(store.get(id).isCompulsory());
  }

  @Test
  @DisplayName("createdBy is persisted to the userid column")
  void testCreatedByPersisted() {
    DataElementGroupSet degs = createDataElementGroupSet('B');
    degs.setCreatedBy(getAdminUser());
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet reloaded = store.get(id);
    assertNotNull(reloaded.getCreatedBy());
    assertEquals(getAdminUser().getUid(), reloaded.getCreatedBy().getUid());
  }

  @Test
  @DisplayName("attributeValues, sharing and translations (jsonb) round-trip")
  void testJsonbColumnsPersisted() {
    DataElementGroupSet degs = createDataElementGroupSet('J');
    degs.setAttributeValues(
        AttributeValues.of(Map.<CharSequence, CharSequence>of("hQKI6KcEu5t", "avalue")));
    degs.getSharing().setPublicAccess("rw------");
    degs.setTranslations(Set.of(new Translation(Locale.of("fr"), "NAME", "Ensemble")));
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet reloaded = store.get(id);
    assertEquals("avalue", reloaded.getAttributeValues().get("hQKI6KcEu5t"));
    assertEquals("rw------", reloaded.getSharing().getPublicAccess());
    assertEquals(1, reloaded.getTranslations().size());
    assertEquals("Ensemble", reloaded.getTranslations().iterator().next().getValue());
  }

  @Test
  @DisplayName("id is generated (SEQUENCE) on save")
  void testIdGeneration() {
    DataElementGroupSet degs = createDataElementGroupSet('I');
    store.save(degs);
    assertTrue(degs.getId() > 0);
  }

  @Test
  @DisplayName("equals is based on identity fields, not members")
  void testEquals() {
    DataElementGroupSet degs = createDataElementGroupSet('Q');
    store.save(degs);
    long id = degs.getId();

    clearSession();

    DataElementGroupSet reloaded = store.get(id);
    assertEquals(degs, reloaded);
    assertEquals(degs.hashCode(), reloaded.hashCode());
  }

  private DataElementGroup saveGroup(char c) {
    DataElementGroup deg = createDataElementGroup(c);
    groupStore.save(deg);
    return deg;
  }

  private static List<String> memberUids(DataElementGroupSet degs) {
    List<String> uids = new ArrayList<>();
    degs.getMembers().forEach(deg -> uids.add(deg.getUid()));
    return uids;
  }
}
