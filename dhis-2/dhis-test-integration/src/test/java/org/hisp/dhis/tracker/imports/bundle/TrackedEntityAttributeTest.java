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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.EW;
import static org.hisp.dhis.common.QueryOperator.GE;
import static org.hisp.dhis.common.QueryOperator.GT;
import static org.hisp.dhis.common.QueryOperator.IEQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.LE;
import static org.hisp.dhis.common.QueryOperator.LIKE;
import static org.hisp.dhis.common.QueryOperator.LT;
import static org.hisp.dhis.common.QueryOperator.NNULL;
import static org.hisp.dhis.common.QueryOperator.NULL;
import static org.hisp.dhis.common.QueryOperator.SW;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityAttributeTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerPreheatService trackerPreheatService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private MetadataImportService metadataImportService;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/te_with_tea_metadata.json");

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void testTrackedAttributePreheater() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_with_tea_data.json");

    TrackerPreheat preheat =
        trackerPreheatService.preheat(trackerObjects, new TrackerIdSchemeParams());

    assertNotNull(preheat.get(OrganisationUnit.class, "cNEZTkdAvmg"));
    assertNotNull(preheat.get(TrackedEntityType.class, "KrYIdvLxkMb"));
    assertNotNull(preheat.get(TrackedEntityAttribute.class, "sYn3tkL3XKa"));
    assertNotNull(preheat.get(TrackedEntityAttribute.class, "TsfP85GKsU5"));
    assertNotNull(preheat.get(TrackedEntityAttribute.class, "sTGqP5JNy6E"));
  }

  @Test
  void testTrackedAttributeValueBundleImporter() throws IOException {
    testSetup.importTrackerData("tracker/te_with_tea_data.json");

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(3, attributeValues.size());
  }

  @Test
  void shouldSetMinCharactersToSearchFromImportOrDefaultToZeroIfNotSpecified() {
    List<TrackedEntityAttribute> trackedEntityAttributes =
        trackedEntityAttributeService.getAllTrackedEntityAttributes();

    assertMinCharactersToSearch(trackedEntityAttributes, "sTGqP5JNy6E", 2);
    assertMinCharactersToSearch(trackedEntityAttributes, "sYn3tkL3XKa", 0);
    assertMinCharactersToSearch(trackedEntityAttributes, "TsfP85GKsU5", 0);
  }

  @Test
  void shouldSetPreferredSearchOperatorFromImportOrNullIfNotSpecified() {
    List<TrackedEntityAttribute> trackedEntityAttributes =
        trackedEntityAttributeService.getAllTrackedEntityAttributes();

    assertPreferredSearchOperator(trackedEntityAttributes, "sTGqP5JNy6E", LIKE);
    assertPreferredSearchOperator(trackedEntityAttributes, "sYn3tkL3XKa", EQ);
    assertPreferredSearchOperator(trackedEntityAttributes, "TsfP85GKsU5", null);
  }

  @Test
  void shouldFailIfPreferredSearchOperatorIsNotPartOfTrackerOperators() {
    TrackedEntityAttribute tea =
        trackedEntityAttributeService.getTrackedEntityAttribute("sYn3tkL3XKa");
    tea.setPreferredSearchOperator(IEQ);

    ImportReport report =
        metadataImportService.importMetadata(
            new MetadataImportParams(),
            new MetadataObjects(Map.of(TrackedEntityAttribute.class, List.of(tea))));

    assertEquals(Status.ERROR, report.getStatus());
    assertStartsWith(
        "The preferred search operator `IEQ` provided for the tracked entity attribute",
        getErrorMessage(report));
  }

  @Test
  void shouldFailIfPreferredOperatorIsBlocked() {
    TrackedEntityAttribute tea =
        trackedEntityAttributeService.getTrackedEntityAttribute("sYn3tkL3XKa");
    tea.setPreferredSearchOperator(IN);

    ImportReport report =
        metadataImportService.importMetadata(
            new MetadataImportParams(),
            new MetadataObjects(Map.of(TrackedEntityAttribute.class, List.of(tea))));

    assertEquals(Status.ERROR, report.getStatus());
    assertStartsWith(
        "The preferred search operator `IN` is blocked for the selected tracked entity attribute",
        getErrorMessage(report));
  }

  @Test
  void shouldSetBlockedOperatorsFromImportOrEmptyListIfNotSpecified() {
    List<TrackedEntityAttribute> trackedEntityAttributes =
        trackedEntityAttributeService.getAllTrackedEntityAttributes();

    assertBlockedOperators(
        trackedEntityAttributes, "sTGqP5JNy6E", List.of(LE, GE, EW, IN, SW, NULL, NNULL));
    assertBlockedOperators(
        trackedEntityAttributes, "sYn3tkL3XKa", List.of(IN, NNULL, GT, LT, SW, LE, EW, NULL, GE));
    assertIsEmpty(getAttribute(trackedEntityAttributes, "TsfP85GKsU5").getBlockedSearchOperators());
  }

  @Test
  void shouldSetIndexableFlagFromImportOrDefaultToFalseIfNotSpecified() {
    List<TrackedEntityAttribute> trackedEntityAttributes =
        trackedEntityAttributeService.getAllTrackedEntityAttributes();

    assertTrigramIndexableFlag(trackedEntityAttributes, "sTGqP5JNy6E", true);
    assertTrigramIndexableFlag(trackedEntityAttributes, "sYn3tkL3XKa", false);
    assertTrigramIndexableFlag(trackedEntityAttributes, "TsfP85GKsU5", false);
  }

  private void assertMinCharactersToSearch(
      List<TrackedEntityAttribute> teas, String uid, int expected) {
    TrackedEntityAttribute tea =
        teas.stream()
            .filter(t -> t.getUid().equals(uid))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("TrackedEntityAttribute with UID " + uid + " not found"));

    assertEquals(
        expected,
        tea.getMinCharactersToSearch(),
        "Expected minCharactersToSearch for UID " + uid + " to be " + expected);
  }

  private void assertPreferredSearchOperator(
      List<TrackedEntityAttribute> teas, String uid, QueryOperator expected) {
    TrackedEntityAttribute tea =
        teas.stream()
            .filter(t -> t.getUid().equals(uid))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("TrackedEntityAttribute with UID " + uid + " not found"));

    assertEquals(
        expected,
        tea.getPreferredSearchOperator(),
        "Expected preferredSearchOperator for UID " + uid + " to be " + expected);
  }

  private void assertBlockedOperators(
      List<TrackedEntityAttribute> teas, String uid, List<QueryOperator> expectedOperators) {
    TrackedEntityAttribute tea = getAttribute(teas, uid);

    assertContainsOnly(expectedOperators, tea.getBlockedSearchOperators());
  }

  private TrackedEntityAttribute getAttribute(List<TrackedEntityAttribute> teas, String uid) {
    return teas.stream()
        .filter(t -> t.getUid().equals(uid))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("TrackedEntityAttribute with UID " + uid + " not found"));
  }

  private String getErrorMessage(ImportReport report) {
    return report.getTypeReports().stream()
        .findFirst()
        .flatMap(
            typeReport ->
                typeReport.getObjectReports().stream()
                    .findFirst()
                    .flatMap(
                        objectReport ->
                            objectReport.getErrorReports().stream()
                                .findFirst()
                                .map(ErrorReport::getMessage)))
        .orElseThrow();
  }

  private void assertTrigramIndexableFlag(
      List<TrackedEntityAttribute> teas, String uid, boolean expected) {
    TrackedEntityAttribute tea =
        teas.stream()
            .filter(t -> t.getUid().equals(uid))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("TrackedEntityAttribute with UID " + uid + " not found"));

    assertEquals(
        expected,
        tea.getTrigramIndexable(),
        "Expected trigram indexable flag for UID " + uid + " to be " + expected);
  }
}
