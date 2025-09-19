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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.QueryOperator.EW;
import static org.hisp.dhis.common.QueryOperator.LIKE;
import static org.hisp.dhis.common.QueryOperator.NNULL;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertMapEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TrackedEntityAttributeControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  private TrackedEntityAttribute teaA;

  private TrackedEntityAttribute teaB;

  private TrackedEntityAttribute teaC;

  private TrackedEntityAttribute teaD;

  private static final String TRIGRAM_INDEX_CREATE_QUERY =
      "CREATE INDEX IF NOT EXISTS in_gin_teavalue_%d ON "
          + "trackedentityattributevalue USING gin (trackedentityid,lower(value) gin_trgm_ops) where trackedentityattributeid = %d";

  @BeforeEach
  void setUp() {
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);

    Program program = createProgram('A');
    manager.save(program);

    teaA = createTrackedEntityAttribute('A');
    teaA.setTrigramIndexable(true);
    teaB = createTrackedEntityAttribute('B');
    teaB.setTrigramIndexable(true);
    teaC = createTrackedEntityAttribute('C');
    teaC.setTrigramIndexable(true);
    teaC.setBlockedSearchOperators(Set.of(LIKE, EW));
    teaD = createTrackedEntityAttribute('D');
    manager.save(teaA);
    manager.save(teaB);
    manager.save(teaC);
    manager.save(teaD);
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
  void getAllAttributes() {
    JsonObject json = GET("/trackedEntityAttributes").content(HttpStatus.OK);

    assertAttributeList(
        json, Set.of(teaA.getName(), teaB.getName(), teaC.getName(), teaD.getName()));
  }

  @Test
  void getAttributesWithNameFilter() {
    JsonObject json =
        GET("/trackedEntityAttributes?filter=name:in:[AttributeB,AttributeC]")
            .content(HttpStatus.OK);

    assertAttributeList(json, Set.of(teaB.getName(), teaC.getName()));
  }

  @Test
  void shouldContainPreferredSearchOperatorWhenSet() {
    teaA.setPreferredSearchOperator(LIKE);
    manager.update(teaA);

    JsonObject json =
        GET("/trackedEntityAttributes?filter=name:in:[AttributeA]&fields=*").content(HttpStatus.OK);

    assertAttributePreferredOperator(json, Set.of(LIKE.name()));
  }

  @Test
  void shouldContainBlockedSearchOperatorsWhenSet() {
    teaA.setBlockedSearchOperators(Set.of(LIKE, QueryOperator.NNULL));
    manager.update(teaA);

    JsonObject json =
        GET("/trackedEntityAttributes?filter=name:in:[AttributeA]&fields=*").content(HttpStatus.OK);

    assertAttributeBlockedOperators(json, Set.of(LIKE.name(), NNULL.name()));
  }

  @Test
  void shouldReturnCollectionOfAttributesWithExpectedTrigramIndexFieldWhenFetchingAllFields() {
    createTrigramIndexes(Set.of(teaA, teaB));

    JsonObject json = GET("/trackedEntityAttributes?fields=*").content(HttpStatus.OK);

    assertAttributeTrigramIndexedField(
        json,
        Map.of(
            teaA.getUid(), true, teaB.getUid(), true, teaC.getUid(), false, teaD.getUid(), false));
  }

  @Test
  void shouldReturnCollectionOfAttributesWithExpectedTrigramIndexFieldWhenFetchingTrigramField() {
    createTrigramIndexes(Set.of(teaA, teaB));

    JsonObject json =
        GET("/trackedEntityAttributes?fields=id,trigramIndexed").content(HttpStatus.OK);

    assertAttributeTrigramIndexedField(
        json,
        Map.of(
            teaA.getUid(), true, teaB.getUid(), true, teaC.getUid(), false, teaD.getUid(), false));
  }

  @Test
  void shouldReturnSingleAttributeWithExpectedTrigramIndexField() {
    createTrigramIndexes(Set.of(teaA));

    JsonObject jsonA =
        GET("/trackedEntityAttributes/" + teaA.getUid() + "?fields=*").content(HttpStatus.OK);
    JsonObject jsonB =
        GET("/trackedEntityAttributes/" + teaB.getUid() + "?fields=id,trigramIndexed")
            .content(HttpStatus.OK);

    assertEquals(teaA.getUid(), jsonA.getString("id").string());
    assertTrue(jsonA.getBoolean("trigramIndexed").booleanValue());
    assertEquals(teaB.getUid(), jsonB.getString("id").string());
    assertFalse(jsonB.getBoolean("trigramIndexed").booleanValue());
  }

  private static void assertAttributeList(JsonObject actualJson, Set<String> expected) {
    assertFalse(actualJson.isEmpty());
    assertEquals(expected.size(), actualJson.getArray("trackedEntityAttributes").size());
    assertEquals(
        expected,
        actualJson
            .getArray("trackedEntityAttributes")
            .projectAsList(e -> e.asObject().getString("displayName"))
            .stream()
            .map(JsonString::string)
            .collect(Collectors.toSet()));
  }

  private static void assertAttributePreferredOperator(
      JsonObject actualJson, Set<String> expected) {
    assertFalse(actualJson.isEmpty());
    assertEquals(expected.size(), actualJson.getArray("trackedEntityAttributes").size());
    assertEquals(
        expected,
        actualJson
            .getArray("trackedEntityAttributes")
            .projectAsList(e -> e.asObject().getString("preferredSearchOperator"))
            .stream()
            .map(JsonString::string)
            .collect(Collectors.toSet()));
  }

  private static void assertAttributeBlockedOperators(JsonObject actualJson, Set<String> expected) {
    assertFalse(actualJson.isEmpty());

    Set<String> actual =
        actualJson
            .getArray("trackedEntityAttributes")
            .projectAsList(e -> e.asObject().getArray("blockedSearchOperators"))
            .stream()
            .flatMap(arr -> arr.stream().map(val -> val.as(JsonString.class).string()))
            .collect(Collectors.toSet());

    assertContainsOnly(expected, actual);
  }

  private static void assertAttributeTrigramIndexedField(
      JsonObject actualJson, Map<String, Boolean> expected) {
    Map<String, Boolean> actual =
        actualJson
            .getArray("trackedEntityAttributes")
            .projectAsList(v -> v.as(JsonObject.class))
            .stream()
            .collect(
                Collectors.toMap(
                    o -> o.getString("id").string(),
                    o -> o.getBoolean("trigramIndexed").booleanValue()));

    assertMapEquals(expected, actual);
  }

  public void createTrigramIndexes(Set<TrackedEntityAttribute> attributes) {
    attributes.forEach(
        attr -> {
          String query = String.format(TRIGRAM_INDEX_CREATE_QUERY, attr.getId(), attr.getId());
          jdbcTemplate.execute(query);
        });
  }
}
