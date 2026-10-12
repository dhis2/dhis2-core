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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the lookup of stored unique attribute values done during the tracker import preheat, see
 * {@link TrackedEntityAttributeValueService#getUniqueAttributeValues}.
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class UniqueAttributeValueLookupTest extends PostgresIntegrationTestBase {

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private TrackedEntityType trackedEntityType;

  private TrackedEntityAttribute globalAttribute;

  private TrackedEntityAttribute scopedAttribute;

  @BeforeEach
  void setUp() {
    orgUnitA = createOrganisationUnit('A');
    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitA);
    manager.save(orgUnitB);

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);

    globalAttribute = createTrackedEntityAttribute('G');
    globalAttribute.setUnique(true);
    manager.save(globalAttribute);

    scopedAttribute = createTrackedEntityAttribute('S');
    scopedAttribute.setUnique(true);
    scopedAttribute.setOrgunitScope(true);
    manager.save(scopedAttribute);
  }

  @Test
  void shouldFindValuesInAnyOrgUnitIgnoringCaseWhenAttributeIsUniqueInTheSystem() {
    TrackedEntity trackedEntityA = trackedEntityWithValue(orgUnitA, globalAttribute, "Value1");
    TrackedEntity trackedEntityB = trackedEntityWithValue(orgUnitB, globalAttribute, "value2");
    trackedEntityWithValue(orgUnitB, globalAttribute, "value3");

    List<UniqueAttributeValueMatch> matches =
        attributeValueService.getUniqueAttributeValues(
            globalAttribute, Set.of("VALUE1", "value2", "not stored"));

    assertContainsOnly(
        List.of(
            new UniqueAttributeValueMatch(UID.of(trackedEntityA), "Value1", orgUnitA.getId()),
            new UniqueAttributeValueMatch(UID.of(trackedEntityB), "value2", orgUnitB.getId())),
        matches);
  }

  @Test
  void shouldFindOnlyRequestedOrgUnitValuePairsWhenAttributeIsUniqueInOrgUnit() {
    TrackedEntity trackedEntityA1 = trackedEntityWithValue(orgUnitA, scopedAttribute, "Value1");
    // value1 is only requested for orgUnitA, so this one is not a match
    trackedEntityWithValue(orgUnitB, scopedAttribute, "value1");
    TrackedEntity trackedEntityB2 = trackedEntityWithValue(orgUnitB, scopedAttribute, "value2");
    // value2 is only requested for orgUnitB, so this one is not a match
    trackedEntityWithValue(orgUnitA, scopedAttribute, "value2");

    List<UniqueAttributeValueMatch> matches =
        attributeValueService.getUniqueAttributeValues(
            scopedAttribute,
            Map.of(orgUnitA.getId(), Set.of("value1"), orgUnitB.getId(), Set.of("VALUE2")));

    assertContainsOnly(
        List.of(
            new UniqueAttributeValueMatch(UID.of(trackedEntityA1), "Value1", orgUnitA.getId()),
            new UniqueAttributeValueMatch(UID.of(trackedEntityB2), "value2", orgUnitB.getId())),
        matches);
  }

  @Test
  void shouldNotFindValuesOfAnotherAttribute() {
    trackedEntityWithValue(orgUnitA, globalAttribute, "value1");

    assertIsEmpty(
        attributeValueService.getUniqueAttributeValues(
            scopedAttribute, Map.of(orgUnitA.getId(), Set.of("value1"))));
  }

  @Test
  void shouldReturnEmptyWhenThereAreNoValuesToLookUp() {
    trackedEntityWithValue(orgUnitA, globalAttribute, "value1");

    assertIsEmpty(attributeValueService.getUniqueAttributeValues(globalAttribute, Set.of()));
    assertIsEmpty(attributeValueService.getUniqueAttributeValues(scopedAttribute, Map.of()));
  }

  @Test
  void shouldFindAllValuesWhenLookupIsSplitAcrossSeveralQueries() {
    TrackedEntity globalMatch = trackedEntityWithValue(orgUnitA, globalAttribute, "value1");
    TrackedEntity scopedMatch = trackedEntityWithValue(orgUnitB, scopedAttribute, "value1");

    Set<String> values = new HashSet<>(Set.of("value1"));
    IntStream.range(0, 20_000).forEach(i -> values.add("not stored " + i));
    // org unit ids that do not exist, pushing the real pair past the first query
    Map<Long, Set<String>> valuesByOrgUnitId = new HashMap<>();
    IntStream.range(0, 20_000)
        .forEach(i -> valuesByOrgUnitId.put(-1L - i, Set.of("value1", "value2")));
    // more values than one query takes, all in the same org unit
    Set<String> orgUnitBValues = new HashSet<>(Set.of("value1"));
    IntStream.range(0, 20_000).forEach(i -> orgUnitBValues.add("not stored " + i));
    valuesByOrgUnitId.put(orgUnitB.getId(), orgUnitBValues);

    assertContainsOnly(
        List.of(new UniqueAttributeValueMatch(UID.of(globalMatch), "value1", orgUnitA.getId())),
        attributeValueService.getUniqueAttributeValues(globalAttribute, values));
    assertContainsOnly(
        List.of(new UniqueAttributeValueMatch(UID.of(scopedMatch), "value1", orgUnitB.getId())),
        attributeValueService.getUniqueAttributeValues(scopedAttribute, valuesByOrgUnitId));
  }

  @Test
  void shouldRunOneQueryRegardlessOfTheNumberOfMatchingTrackedEntities() {
    Set<String> globalValues = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      trackedEntityWithValue(orgUnitA, globalAttribute, "global" + i);
      trackedEntityWithValue(i % 2 == 0 ? orgUnitA : orgUnitB, scopedAttribute, "scoped" + i);
      globalValues.add("global" + i);
    }
    Map<Long, Set<String>> scopedValues =
        Map.of(
            orgUnitA.getId(), Set.of("scoped0", "scoped2", "scoped4", "scoped6", "scoped8"),
            orgUnitB.getId(), Set.of("scoped1", "scoped3", "scoped5", "scoped7", "scoped9"));
    // start from an empty session, as a tracked entity already in the session is not loaded again
    clearSession();

    QueryCountDataSourceProxy.clearCapturedSql();
    List<UniqueAttributeValueMatch> globalMatches =
        attributeValueService.getUniqueAttributeValues(globalAttribute, globalValues);
    long globalQueries = QueryCountDataSourceProxy.countCapturedSqlMatching("select");

    QueryCountDataSourceProxy.clearCapturedSql();
    List<UniqueAttributeValueMatch> scopedMatches =
        attributeValueService.getUniqueAttributeValues(scopedAttribute, scopedValues);
    long scopedQueries = QueryCountDataSourceProxy.countCapturedSqlMatching("select");

    assertEquals(10, globalMatches.size());
    assertEquals(10, scopedMatches.size());
    assertEquals(1, globalQueries, "expected a single query, not one per matching tracked entity");
    assertEquals(1, scopedQueries, "expected a single query, not one per matching tracked entity");
  }

  private TrackedEntity trackedEntityWithValue(
      OrganisationUnit orgUnit, TrackedEntityAttribute attribute, String value) {
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    manager.save(trackedEntity);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(attribute, trackedEntity, value));
    return trackedEntity;
  }
}
