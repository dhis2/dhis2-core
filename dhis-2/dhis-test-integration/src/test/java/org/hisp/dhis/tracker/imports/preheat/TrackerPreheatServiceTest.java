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
package org.hisp.dhis.tracker.imports.preheat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
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
class TrackerPreheatServiceTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerPreheatService trackerPreheatService;

  @Autowired private TrackerIdentifierCollector identifierCollector;

  @BeforeAll
  void setup() throws IOException {
    testSetup.importMetadata("tracker/event_metadata.json");
  }

  @Test
  void testCollectIdentifiersEvents() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/event_events.json");
    assertTrue(trackerObjects.getTrackedEntities().isEmpty());
    assertTrue(trackerObjects.getEnrollments().isEmpty());
    assertFalse(trackerObjects.getEvents().isEmpty());
    Map<Class<?>, Set<String>> collectedMap = identifierCollector.collect(trackerObjects);
    assertTrue(collectedMap.containsKey(DataElement.class));
    assertTrue(collectedMap.containsKey(ProgramStage.class));
    assertTrue(collectedMap.containsKey(OrganisationUnit.class));
    assertTrue(collectedMap.containsKey(CategoryOptionCombo.class));
    assertTrue(collectedMap.containsKey(CategoryOption.class));
    Set<String> dataElements = collectedMap.get(DataElement.class);
    assertTrue(dataElements.contains("DSKTW8qFP0z"));
    assertTrue(dataElements.contains("VD2olWcRozZ"));
    assertTrue(dataElements.contains("WS3e6pInnuA"));
    assertTrue(dataElements.contains("h7hXjNVMiRl"));
    assertTrue(dataElements.contains("KCLriKKezWO"));
    assertTrue(dataElements.contains("A8qxpcalLtf"));
    assertTrue(dataElements.contains("zal5vkVEpV0"));
    assertTrue(dataElements.contains("kAfSfT69m0E"));
    assertTrue(dataElements.contains("wIUShyK7lIa"));
    assertTrue(dataElements.contains("ICfA0klVxkd"));
    assertTrue(dataElements.contains("xaPkxL28aPx"));
    assertTrue(dataElements.contains("Kvm949EFjjv"));
    assertTrue(dataElements.contains("xaQ4gqDYGL9"));
    assertTrue(dataElements.contains("MqGMwzt7M7w"));
    assertTrue(dataElements.contains("WdTLfnn4S1I"));
    assertTrue(dataElements.contains("hxUvZqnmBDv"));
    assertTrue(dataElements.contains("JXF90RhgNiI"));
    assertTrue(dataElements.contains("gfEoDU4GtXK"));
    assertTrue(dataElements.contains("qw67QlOlzdp"));
    Set<String> categoryCombos = collectedMap.get(CategoryOptionCombo.class);
    assertTrue(categoryCombos.contains("HllvX50cXC0"));
    Set<String> categoryOptions = collectedMap.get(CategoryOption.class);
    assertTrue(categoryOptions.contains("xYerKDKCefk"));
  }

  @Test
  void testCollectIdentifiersAttributeValues() {
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .trackedEntities(
                Lists.newArrayList(
                    TrackedEntity.builder()
                        .trackedEntity(UID.of("TE012345678"))
                        .orgUnit(MetadataIdentifier.ofCode("OU123456789"))
                        .build()))
            .build();
    assertFalse(trackerObjects.getTrackedEntities().isEmpty());
    assertTrue(trackerObjects.getEnrollments().isEmpty());
    assertTrue(trackerObjects.getEvents().isEmpty());
    Map<Class<?>, Set<String>> collectedMap = identifierCollector.collect(trackerObjects);
    assertTrue(collectedMap.containsKey(TrackedEntity.class));
    Set<String> trackedEntities = collectedMap.get(TrackedEntity.class);
    assertTrue(collectedMap.containsKey(OrganisationUnit.class));
    Set<String> organisationUnits = collectedMap.get(OrganisationUnit.class);
    assertTrue(organisationUnits.contains("OU123456789"));
    assertEquals(1, organisationUnits.size());
    assertTrue(trackedEntities.contains("TE012345678"));
    assertEquals(1, trackedEntities.size());
  }

  @Test
  void testPreheatValidation() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/event_events.json");
    assertTrue(trackerObjects.getTrackedEntities().isEmpty());
    assertTrue(trackerObjects.getEnrollments().isEmpty());
    assertFalse(trackerObjects.getEvents().isEmpty());
  }

  @Test
  void testPreheatEvents() throws IOException {
    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerObjects trackerObjects = testSetup.fromJson("tracker/event_events.json");

    TrackerPreheat preheat =
        trackerPreheatService.preheat(trackerObjects, new TrackerIdSchemeParams());

    assertNotNull(preheat);
    assertFalse(preheat.getAll(DataElement.class).isEmpty());
    assertFalse(preheat.getAll(OrganisationUnit.class).isEmpty());
    assertFalse(preheat.getAll(ProgramStage.class).isEmpty());
    assertFalse(preheat.getAll(CategoryOptionCombo.class).isEmpty());
    assertNotNull(preheat.get(CategoryOptionCombo.class, "HllvX50cXC0"));
    assertNotNull(preheat.get(CategoryOption.class, "XXXrKDKCefk"));
  }
}
