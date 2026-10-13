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
package org.hisp.dhis.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Verifies the JPA annotation mapping of {@link OptionGroupSet} (formerly HBM). */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class OptionGroupSetStoreTest extends PostgresIntegrationTestBase {

  @Autowired private OptionGroupSetStore store;
  @Autowired private OptionGroupStore groupStore;
  @Autowired private IdentifiableObjectStore<OptionSet> optionSetStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("members keep their list order and sort_order is 1-based")
  void testMembersOrderPersisted() {
    OptionGroup ogC = saveGroup("C");
    OptionGroup ogA = saveGroup("A");
    OptionGroup ogB = saveGroup("B");

    OptionGroupSet ogs = new OptionGroupSet("OgsOrder");
    ogs.addOptionGroup(ogC);
    ogs.addOptionGroup(ogA);
    ogs.addOptionGroup(ogB);
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertEquals(List.of(ogC.getUid(), ogA.getUid(), ogB.getUid()), memberUids(reloaded));

    List<Integer> sortOrders =
        jdbcTemplate.queryForList(
            "select sort_order from optiongroupsetmembers"
                + " where optiongroupsetid = ? order by sort_order",
            Integer.class,
            id);
    assertEquals(List.of(1, 2, 3), sortOrders);
  }

  @Test
  @DisplayName("members can be reordered and removed")
  void testMembersUpdate() {
    OptionGroup ogA = saveGroup("D");
    OptionGroup ogB = saveGroup("E");
    OptionGroup ogC = saveGroup("F");

    OptionGroupSet ogs = new OptionGroupSet("OgsUpdate");
    ogs.addOptionGroup(ogA);
    ogs.addOptionGroup(ogB);
    ogs.addOptionGroup(ogC);
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet loaded = store.get(id);
    loaded.getMembers().remove(0);
    loaded.getMembers().add(loaded.getMembers().remove(0));
    store.update(loaded);

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertEquals(List.of(ogC.getUid(), ogB.getUid()), memberUids(reloaded));
  }

  @Test
  @DisplayName("deleting a group set removes its join rows but keeps the groups")
  void testDeleteKeepsGroups() {
    OptionGroup og = saveGroup("H");
    OptionGroupSet ogs = new OptionGroupSet("OgsDelete");
    ogs.addOptionGroup(og);
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    store.delete(store.get(id));

    clearSession();

    assertNull(store.get(id));
    assertNotNull(groupStore.get(og.getId()));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from optiongroupsetmembers where optiongroupsetid = ?",
            Integer.class,
            id));
  }

  @Test
  @DisplayName("scalar fields round-trip, description is unbounded text")
  void testScalarFieldsPersisted() {
    String longDescription = "d".repeat(1000);
    OptionGroupSet ogs = new OptionGroupSet("OgsScalar");
    ogs.setCode("OGS_CODE_S");
    ogs.setDescription(longDescription);
    ogs.setDataDimension(false);
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertEquals(ogs.getUid(), reloaded.getUid());
    assertEquals("OGS_CODE_S", reloaded.getCode());
    assertEquals("OgsScalar", reloaded.getName());
    assertEquals(longDescription, reloaded.getDescription());
    assertFalse(reloaded.isDataDimension());
    assertNotNull(reloaded.getCreated());
    assertNotNull(reloaded.getLastUpdated());
  }

  @Test
  @DisplayName("dataDimension defaults to true")
  void testDataDimensionDefault() {
    OptionGroupSet ogs = new OptionGroupSet("OgsDataDimension");
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    assertTrue(store.get(id).isDataDimension());
  }

  @Test
  @DisplayName("optionSet many-to-one round-trips")
  void testOptionSetPersisted() {
    OptionSet optionSet = createOptionSet('S');
    optionSet.setValueType(ValueType.TEXT);
    optionSetStore.save(optionSet);

    OptionGroupSet ogs = new OptionGroupSet("OgsOptionSet");
    ogs.setOptionSet(optionSet);
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertNotNull(reloaded.getOptionSet());
    assertEquals(optionSet.getUid(), reloaded.getOptionSet().getUid());
  }

  @Test
  @DisplayName("createdBy is persisted to the userid column")
  void testCreatedByPersisted() {
    OptionGroupSet ogs = new OptionGroupSet("OgsCreatedBy");
    ogs.setCreatedBy(getAdminUser());
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertNotNull(reloaded.getCreatedBy());
    assertEquals(getAdminUser().getUid(), reloaded.getCreatedBy().getUid());
  }

  @Test
  @DisplayName("sharing and translations (jsonb) round-trip")
  void testJsonbColumnsPersisted() {
    OptionGroupSet ogs = new OptionGroupSet("OgsJsonb");
    ogs.getSharing().setPublicAccess("rw------");
    ogs.setTranslations(Set.of(new Translation(Locale.of("fr"), "NAME", "Ensemble")));
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertEquals("rw------", reloaded.getSharing().getPublicAccess());
    assertEquals(1, reloaded.getTranslations().size());
    assertEquals("Ensemble", reloaded.getTranslations().iterator().next().getValue());
  }

  @Test
  @DisplayName("shortName and attributeValues have no column and are not supported")
  void testUnsupportedProperties() {
    OptionGroupSet ogs = new OptionGroupSet("OgsUnsupported");
    ogs.setShortName("OgsShort");
    ogs.setAttributeValues(
        AttributeValues.of(Map.<CharSequence, CharSequence>of("hQKI6KcEu5t", "avalue")));
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertNull(reloaded.getShortName());
    assertTrue(reloaded.getAttributeValues().isEmpty());
  }

  @Test
  @DisplayName("id is generated (SEQUENCE) on save")
  void testIdGeneration() {
    OptionGroupSet ogs = new OptionGroupSet("OgsIdGen");
    store.save(ogs);
    assertTrue(ogs.getId() > 0);
    assertNotNull(ogs.getUid());
  }

  @Test
  @DisplayName("equals is based on identity fields, not members")
  void testEquals() {
    OptionGroupSet ogs = new OptionGroupSet("OgsEquals");
    ogs.addOptionGroup(saveGroup("Q"));
    store.save(ogs);
    long id = ogs.getId();

    clearSession();

    OptionGroupSet reloaded = store.get(id);
    assertEquals(ogs, reloaded);
    assertEquals(ogs.hashCode(), reloaded.hashCode());
  }

  private OptionGroup saveGroup(String suffix) {
    OptionGroup og = new OptionGroup("OgsMember" + suffix);
    og.setShortName("OgsMember" + suffix);
    groupStore.save(og);
    return og;
  }

  private static List<String> memberUids(OptionGroupSet ogs) {
    return ogs.getMembers().stream().map(OptionGroup::getUid).toList();
  }
}
