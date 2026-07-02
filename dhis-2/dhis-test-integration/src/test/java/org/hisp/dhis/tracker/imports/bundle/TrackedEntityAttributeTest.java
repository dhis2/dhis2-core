/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackedEntityAttributeTest extends TrackerTest {

  @Autowired private TrackerPreheatService trackerPreheatService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired protected UserService _userService;

  private User importUser;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/te_with_tea_metadata.json");
    importUser = userService.getUser(ADMIN_USER_UID);
    injectAdminUser();
  }

  @Test
  void testTrackedAttributePreheater() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/te_with_tea_data.json");
    TrackerPreheat preheat =
        trackerPreheatService.preheat(
            trackerObjects, new TrackerIdSchemeParams(), userService.getUser(ADMIN_USER_UID));
    assertNotNull(preheat.get(OrganisationUnit.class, "cNEZTkdAvmg"));
    assertNotNull(preheat.get(TrackedEntityType.class, "KrYIdvLxkMb"));
    assertNotNull(preheat.get(TrackedEntityAttribute.class, "sYn3tkL3XKa"));
    assertNotNull(preheat.get(TrackedEntityAttribute.class, "TsfP85GKsU5"));
    assertNotNull(preheat.get(TrackedEntityAttribute.class, "sTGqP5JNy6E"));
  }

  @Test
  void testTrackedAttributeValueBundleImporter() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/te_with_tea_data.json");
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(3, attributeValues.size());
  }

  @Test
  void shouldSetStoredByToAuthenticatedUserForTrackedEntityAttributeValue() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/te_with_tea_data.json");
    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntities.get(0));
    for (TrackedEntityAttributeValue av : attributeValues) {
      assertEquals(importUser.getUsername(), av.getStoredBy());
    }
  }

  @Test
  void shouldUpdateExistingAttributeValueWhenImportingWithNonUidIdScheme() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/te_with_tea_data.json");
    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    // The persister must recognize the attribute value as existing (and route it to an UPDATE
    // instead of a duplicate INSERT, which would violate the composite PK) regardless of the
    // idScheme the import resolves attributes with.
    TrackerObjects update =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
                        .trackedEntity("CLR1fvPj4ic")
                        .trackedEntityType(MetadataIdentifier.ofName("Person"))
                        .orgUnit(MetadataIdentifier.ofName("Country"))
                        .attributes(
                            List.of(
                                Attribute.builder()
                                    .attribute(MetadataIdentifier.ofName("Attribute_Text"))
                                    .value("updated value")
                                    .build()))
                        .build()))
            .build();
    params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.UPDATE)
            .idSchemes(
                TrackerIdSchemeParams.builder()
                    .idScheme(TrackerIdSchemeParam.NAME)
                    .orgUnitIdScheme(TrackerIdSchemeParam.NAME)
                    .build())
            .build();

    assertNoErrors(trackerImportService.importTracker(params, update));

    TrackedEntity trackedEntity = manager.getAll(TrackedEntity.class).get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(3, attributeValues.size());
    TrackedEntityAttributeValue updatedValue =
        attributeValues.stream()
            .filter(av -> "TsfP85GKsU5".equals(av.getAttribute().getUid()))
            .findFirst()
            .orElseThrow();
    assertEquals("updated value", updatedValue.getValue());
  }

  @Test
  void shouldNotPersistAttributeValueWhenImportingEmptyValueForAttributeNotInDb() {
    String te = CodeGenerator.generateUid();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
                        .trackedEntity(te)
                        .trackedEntityType(MetadataIdentifier.ofUid("KrYIdvLxkMb"))
                        .orgUnit(MetadataIdentifier.ofUid("cNEZTkdAvmg"))
                        .attributes(
                            List.of(
                                Attribute.builder()
                                    .attribute(MetadataIdentifier.ofUid("sYn3tkL3XKa"))
                                    .value("123")
                                    .build(),
                                Attribute.builder()
                                    .attribute(MetadataIdentifier.ofUid("TsfP85GKsU5"))
                                    .value("")
                                    .build()))
                        .build()))
            .build();

    assertNoErrors(
        trackerImportService.importTracker(TrackerImportParams.builder().build(), trackerObjects));

    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, te);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertContainsOnly(
        List.of("sYn3tkL3XKa"),
        attributeValues.stream().map(av -> av.getAttribute().getUid()).toList());
  }

  @Test
  void shouldDeleteAttributeValueWhenImportingEmptyValueForExistingAttribute() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/te_with_tea_data.json");
    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    TrackerObjects update =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
                        .trackedEntity("CLR1fvPj4ic")
                        .trackedEntityType(MetadataIdentifier.ofUid("KrYIdvLxkMb"))
                        .orgUnit(MetadataIdentifier.ofUid("cNEZTkdAvmg"))
                        .attributes(
                            List.of(
                                Attribute.builder()
                                    .attribute(MetadataIdentifier.ofUid("TsfP85GKsU5"))
                                    .value("")
                                    .build()))
                        .build()))
            .build();
    params = TrackerImportParams.builder().importStrategy(TrackerImportStrategy.UPDATE).build();

    assertNoErrors(trackerImportService.importTracker(params, update));

    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "CLR1fvPj4ic");
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertContainsOnly(
        List.of("sYn3tkL3XKa", "sTGqP5JNy6E"),
        attributeValues.stream().map(av -> av.getAttribute().getUid()).toList());
  }
}
