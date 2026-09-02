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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class OptionGroupStoreTest extends PostgresIntegrationTestBase {
  @Autowired private OptionGroupStore store;

  @Autowired private OptionGroupSetStore optionGroupSetStore;

  @Autowired private OptionStore optionStore;

  @Autowired private IdentifiableObjectStore<OptionSet> optionSetStore;

  private OptionGroup optionGroupA;

  private OptionGroup optionGroupB;

  private OptionGroup optionGroupC;

  @BeforeEach
  public void setUpTest() {
    optionGroupA = new OptionGroup("OptionGroupA");
    optionGroupA.setShortName("ShortNameA");
    optionGroupB = new OptionGroup("OptionGroupB");
    optionGroupB.setShortName("ShortNameB");
    optionGroupC = new OptionGroup("OptionGroupC");
    optionGroupC.setShortName("ShortNameC");
  }

  @Test
  void tetAddOptionGroup() {
    store.save(optionGroupA);
    long idA = optionGroupA.getId();
    store.save(optionGroupB);
    long idB = optionGroupB.getId();
    store.save(optionGroupC);
    long idC = optionGroupC.getId();
    assertEquals(optionGroupA, store.get(idA));
    assertEquals(optionGroupB, store.get(idB));
    assertEquals(optionGroupC, store.get(idC));
  }

  @Test
  void testDeleteOptionGroup() {
    store.save(optionGroupA);
    long idA = optionGroupA.getId();
    store.save(optionGroupB);
    long idB = optionGroupB.getId();
    store.delete(optionGroupA);
    assertNull(store.get(idA));
    assertNotNull(store.get(idB));
  }

  @Test
  void genericGetAll() {
    store.save(optionGroupA);
    store.save(optionGroupB);
    store.save(optionGroupC);
    Collection<OptionGroup> objects = store.getAll();
    assertNotNull(objects);
    assertEquals(3, objects.size());
    assertTrue(objects.contains(optionGroupA));
    assertTrue(objects.contains(optionGroupB));
    assertTrue(objects.contains(optionGroupC));
  }

  @Test
  void testGetByOptionId() {
    OptionSet optionSet = createOptionSet('A');
    optionSet.setValueType(ValueType.TEXT);
    optionSetStore.save(optionSet);

    Option option = createOption('A');
    option.setOptionSet(optionSet);
    optionStore.save(option);

    optionGroupA.setOptionSet(optionSet);
    optionGroupA.addOption(option);
    store.save(optionGroupA);

    assertNotNull(store.getOptionGroupsByOptionId(option.getUid()));
  }

  @Test
  void testDeleteOption() {
    OptionSet optionSet = createOptionSet('A');
    optionSet.setValueType(ValueType.TEXT);
    optionSetStore.save(optionSet);
    Option option = createOption('A');
    option.setOptionSet(optionSet);
    optionStore.save(option);

    optionGroupA.setOptionSet(optionSet);
    optionGroupA.addOption(option);
    store.save(optionGroupA);

    optionStore.delete(option);
    optionGroupA = store.get(optionGroupA.getId());
    assertTrue(optionGroupA.getMembers().isEmpty());
  }

  // -------------------------------------------------------------------------
  // JPA migration verification (HBM -> annotations)
  // -------------------------------------------------------------------------

  private Option saveOption(char uniqueChar, OptionSet optionSet) {
    Option option = createOption(uniqueChar);
    option.setOptionSet(optionSet);
    optionStore.save(option);
    return option;
  }

  @Test
  @DisplayName("JPA: members round-trip through the optiongroupmembers join table")
  void testJpaMembersJoinTable() {
    OptionSet optionSet = createOptionSet('J');
    optionSet.setValueType(ValueType.TEXT);
    optionSetStore.save(optionSet);
    Option o1 = saveOption('1', optionSet);
    Option o2 = saveOption('2', optionSet);

    OptionGroup group = new OptionGroup("JpaMembersGroup");
    group.setShortName("JpaMembersGroup");
    group.addOption(o1);
    group.addOption(o2);
    store.save(group);
    long id = group.getId();

    clearSession(); // force reload from DB

    OptionGroup reloaded = store.get(id);
    assertNotNull(reloaded);
    Set<String> memberUids = new HashSet<>();
    reloaded.getMembers().forEach(o -> memberUids.add(o.getUid()));
    assertEquals(2, memberUids.size());
    assertTrue(memberUids.containsAll(Set.of(o1.getUid(), o2.getUid())));
  }

  @Test
  @DisplayName("JPA: createdBy IS persisted (optiongroup has a userid column)")
  void testJpaCreatedByPersisted() {
    OptionGroup group = new OptionGroup("JpaCreatedByGroup");
    group.setShortName("JpaCreatedByGroup");
    group.setCreatedBy(getAdminUser());
    store.save(group);
    long id = group.getId();

    clearSession();

    OptionGroup reloaded = store.get(id);
    assertNotNull(reloaded.getCreatedBy(), "createdBy must persist to the userid column");
    assertEquals(getAdminUser().getUid(), reloaded.getCreatedBy().getUid());
  }

  @Test
  @DisplayName("JPA: shortName and description (text-typed) round-trip")
  void testJpaScalarFieldsPersist() {
    OptionGroup group = new OptionGroup("JpaScalarGroup");
    group.setShortName("JpaScalarGroup");
    // Longer than 255 chars - only round-trips correctly if @Type(type = "text") was kept
    // alongside @Column(columnDefinition = "text"); columnDefinition alone still binds as
    // StringType and truncates the schema-reported length back to 255 (see gotcha #14).
    String longDescription = "d".repeat(1000);
    group.setDescription(longDescription);
    store.save(group);
    long id = group.getId();

    clearSession();

    OptionGroup reloaded = store.get(id);
    assertEquals("JpaScalarGroup", reloaded.getShortName());
    assertEquals(longDescription, reloaded.getDescription());
  }

  @Test
  @DisplayName("JPA: optionSet many-to-one round-trip")
  void testJpaOptionSetPersisted() {
    OptionSet optionSet = createOptionSet('K');
    optionSet.setValueType(ValueType.TEXT);
    optionSetStore.save(optionSet);

    OptionGroup group = new OptionGroup("JpaOptionSetGroup");
    group.setShortName("JpaOptionSetGroup");
    group.setOptionSet(optionSet);
    store.save(group);
    long id = group.getId();

    clearSession();

    OptionGroup reloaded = store.get(id);
    assertNotNull(reloaded.getOptionSet());
    assertEquals(optionSet.getUid(), reloaded.getOptionSet().getUid());
  }

  @Test
  @DisplayName("JPA: id is generated (SEQUENCE) on save")
  void testJpaIdGeneration() {
    OptionGroup group = new OptionGroup("JpaIdGenGroup");
    group.setShortName("JpaIdGenGroup");
    store.save(group);
    assertTrue(group.getId() > 0, "id must be generated on save");
    assertNotNull(group.getUid());
  }

  @Test
  @DisplayName(
      "JPA: OptionGroup round-trips as a member of the still-HBM-mapped OptionGroupSet ordered list")
  void testJpaOptionGroupAsHbmOptionGroupSetMember() {
    OptionGroup groupA = new OptionGroup("JpaGroupSetMemberA");
    groupA.setShortName("JpaGroupSetMemberA");
    OptionGroup groupB = new OptionGroup("JpaGroupSetMemberB");
    groupB.setShortName("JpaGroupSetMemberB");
    store.save(groupA);
    store.save(groupB);

    OptionGroupSet groupSet = new OptionGroupSet("JpaGroupSet");
    groupSet.addOptionGroup(groupA);
    groupSet.addOptionGroup(groupB);
    optionGroupSetStore.save(groupSet);
    long groupSetId = groupSet.getId();

    clearSession(); // force reload from DB - exercises the HBM <list> -> JPA entity edge

    OptionGroupSet reloaded = optionGroupSetStore.get(groupSetId);
    assertNotNull(reloaded);
    // Order must be preserved (HBM <list-index column="sort_order" base="1">).
    assertEquals(
        List.of(groupA.getUid(), groupB.getUid()),
        reloaded.getMembers().stream().map(OptionGroup::getUid).toList());
  }

  @Test
  @DisplayName(
      "JPA: preserved HBM cascade=\"all\" on members - deleting the group also deletes its member Option")
  void testJpaMembersCascadeAllDeletesOptions() {
    OptionSet optionSet = createOptionSet('L');
    optionSet.setValueType(ValueType.TEXT);
    optionSetStore.save(optionSet);
    Option option = saveOption('3', optionSet);
    long optionId = option.getId();

    OptionGroup group = new OptionGroup("JpaCascadeGroup");
    group.setShortName("JpaCascadeGroup");
    group.addOption(option);
    store.save(group);
    long id = group.getId();

    store.delete(store.get(id));

    clearSession();

    assertNull(
        optionStore.get(optionId),
        "cascade=\"all\" carried over from the HBM <set> must delete the member Option too");
  }
}
