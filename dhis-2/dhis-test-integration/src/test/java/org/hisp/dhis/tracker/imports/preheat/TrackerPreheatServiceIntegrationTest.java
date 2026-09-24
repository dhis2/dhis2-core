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

import static org.hisp.dhis.tracker.test.TrackerTestBase.createEnrollment;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TestNotes;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TrackerPreheatServiceIntegrationTest extends PostgresIntegrationTestBase {

  private static final String TET_UID = "TET12345678";

  private static final String ATTRIBUTE_UID = "ATTR1234567";

  @Autowired private TrackerPreheatService trackerPreheatService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramService programService;

  @Autowired private AttributeService attributeService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TestNotes testNotes;

  private OrganisationUnit orgUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private String programAttribute;

  @BeforeEach
  void setUp() {
    // Set up placeholder OU; We add Code for testing idScheme.
    orgUnit = createOrganisationUnit('A');
    orgUnit.setCode("OUA");
    organisationUnitService.addOrganisationUnit(orgUnit);
    // Set up placeholder TET
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setUid(TET_UID);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    // Set up attribute for program, to be used for testing idScheme.
    Attribute attributeA = createAttribute('A');
    attributeA.setUid(ATTRIBUTE_UID);
    attributeA.setUnique(true);
    attributeA.setProgramAttribute(true);
    attributeService.addAttribute(attributeA);
    // Set up placeholder Program, with attributeValue
    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.setTrackedEntityType(trackedEntityType);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    programAttribute = "PROGRAM1";
    program.setAttributeValues(AttributeValues.of(Map.of(attributeA.getUid(), programAttribute)));
    programService.addProgram(program);
  }

  @Test
  void testPreheatWithDifferentIdSchemes() {
    TrackedEntity teA =
        TrackedEntity.builder()
            .trackedEntity(UID.generate())
            .orgUnit(MetadataIdentifier.ofCode("OUA"))
            .trackedEntityType(MetadataIdentifier.ofUid(TET_UID))
            .build();
    Enrollment enrollmentA =
        Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofCode("OUA"))
            .program(MetadataIdentifier.ofAttribute(ATTRIBUTE_UID, programAttribute))
            .trackedEntity(UID.of("TE123456789"))
            .build();

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .trackedEntities(Lists.newArrayList(teA))
            .enrollments(Lists.newArrayList(enrollmentA))
            .build();

    TrackerIdSchemeParams idSchemeParams =
        TrackerIdSchemeParams.builder()
            .idScheme(TrackerIdSchemeParam.UID)
            .orgUnitIdScheme(TrackerIdSchemeParam.CODE)
            .programIdScheme(TrackerIdSchemeParam.ofAttribute(ATTRIBUTE_UID))
            .build();

    TrackerPreheat preheat = trackerPreheatService.preheat(trackerObjects, idSchemeParams);

    assertNotNull(preheat);
    // asserting on specific fields instead of plain assertEquals since
    // PreheatMappers are not mapping all fields
    assertEquals("OUA", preheat.getOrganisationUnit("OUA").getCode());
    assertEquals(TET_UID, preheat.getTrackedEntityType(TET_UID).getUid());
    Program actualProgram = preheat.getProgram(programAttribute);
    assertNotNull(actualProgram);
    assertEquals(program.getUid(), actualProgram.getUid());
  }

  @Test
  void shouldPreheatOnlyNotesThatExist() {
    org.hisp.dhis.tracker.model.TrackedEntity trackedEntity =
        createTrackedEntity(orgUnit, trackedEntityType);
    manager.save(trackedEntity, false);
    org.hisp.dhis.tracker.model.Enrollment enrollment =
        createEnrollment(program, trackedEntity, orgUnit);
    manager.save(enrollment, false);
    UID existing = UID.of(testNotes.save(enrollment, "text").getUid());
    UID notExisting = UID.generate();

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    Enrollment.builder()
                        .enrollment(UID.generate())
                        .notes(
                            List.of(
                                Note.builder().note(existing).value("text").build(),
                                Note.builder().note(notExisting).value("text").build()))
                        .build()))
            .build();

    TrackerPreheat preheat =
        trackerPreheatService.preheat(trackerObjects, TrackerIdSchemeParams.builder().build());

    assertTrue(preheat.hasNote(existing));
    assertFalse(preheat.hasNote(notExisting));
  }
}
