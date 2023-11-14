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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerPreheatServiceTest extends TrackerTest {

  @Autowired private TrackerPreheatService trackerPreheatService;

  @Autowired private TrackerIdentifierCollector identifierCollector;

  @Override
  protected void initTest() {}

  @Test
  void testCollectIdentifiersEvents() throws IOException {
    TrackerImportParams params = fromJson("tracker/event_events.json");
    assertTrue(params.getTrackedEntities().isEmpty());
    assertTrue(params.getEnrollments().isEmpty());
    assertFalse(params.getEvents().isEmpty());
    Map<Class<?>, Set<String>> collectedMap = identifierCollector.collect(params);
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
    TrackerImportParams params =
        TrackerImportParams.builder()
            .idSchemes(
                TrackerIdSchemeParams.builder()
                    .idScheme(TrackerIdSchemeParam.ofAttribute("ATTR1234567"))
                    .build())
            .trackedEntities(
                Lists.newArrayList(
                    TrackedEntity.builder()
                        .trackedEntity("TEI12345678")
                        .orgUnit(MetadataIdentifier.ofCode("OU123456789"))
                        .build()))
            .build();
    assertFalse(params.getTrackedEntities().isEmpty());
    assertTrue(params.getEnrollments().isEmpty());
    assertTrue(params.getEvents().isEmpty());
    Map<Class<?>, Set<String>> collectedMap = identifierCollector.collect(params);
    assertTrue(collectedMap.containsKey(TrackedEntity.class));
    Set<String> trackedEntities = collectedMap.get(TrackedEntity.class);
    assertTrue(collectedMap.containsKey(OrganisationUnit.class));
    Set<String> organisationUnits = collectedMap.get(OrganisationUnit.class);
    assertTrue(organisationUnits.contains("OU123456789"));
    assertEquals(1, organisationUnits.size());
    assertTrue(trackedEntities.contains("TEI12345678"));
    assertEquals(1, trackedEntities.size());
  }

  @Test
  void testPreheatValidation() throws IOException {
    TrackerImportParams params = fromJson("tracker/event_events.json");
    assertTrue(params.getTrackedEntities().isEmpty());
    assertTrue(params.getEnrollments().isEmpty());
    assertFalse(params.getEvents().isEmpty());
  }

  @Test
  void testPreheatEvents() throws IOException {
    setUpMetadata("tracker/event_metadata.json");
    TrackerImportParams params = fromJson("tracker/event_events.json");
    assertTrue(params.getTrackedEntities().isEmpty());
    assertTrue(params.getEnrollments().isEmpty());
    assertFalse(params.getEvents().isEmpty());

    TrackerPreheat preheat = trackerPreheatService.preheat(params);

    assertNotNull(preheat);
    assertFalse(preheat.getAll(DataElement.class).isEmpty());
    assertFalse(preheat.getAll(OrganisationUnit.class).isEmpty());
    assertFalse(preheat.getAll(ProgramStage.class).isEmpty());
    assertFalse(preheat.getAll(CategoryOptionCombo.class).isEmpty());
    assertNotNull(preheat.get(CategoryOptionCombo.class, "XXXvX50cXC0"));
    assertNotNull(preheat.get(CategoryOption.class, "XXXrKDKCefk"));
  }
}
