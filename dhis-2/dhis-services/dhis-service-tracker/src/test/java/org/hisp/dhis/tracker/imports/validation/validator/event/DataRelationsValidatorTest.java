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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1313;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasNoError;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Optional;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class DataRelationsValidatorTest extends TestBase {

  private static final String PROGRAM_UID = "PROGRAM_UID";

  private static final String PROGRAM_STAGE_ID = "PROGRAM_STAGE_ID";

  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final String TE_TYPE_ID = "TE_TYPE_ID";

  private static final UID ENROLLMENT_ID = UID.generate();

  private DataRelationsValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    validator = new DataRelationsValidator();

    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void eventValidationFailsWhenEventAndProgramStageProgramDontMatch() {
    OrganisationUnit orgUnit = organisationUnit(ORG_UNIT_ID);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))).thenReturn(orgUnit);
    Program program = programWithRegistration(PROGRAM_UID, orgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    when(preheat.getProgramWithOrgUnitsMap())
        .thenReturn(Collections.singletonMap(PROGRAM_UID, Collections.singletonList(ORG_UNIT_ID)));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID)))
        .thenReturn(
            programStage(
                PROGRAM_STAGE_ID, programWithRegistration(CodeGenerator.generateUid(), orgUnit)));

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .enrollment(ENROLLMENT_ID)
            .program(MetadataIdentifier.ofUid(program))
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1089);
  }

  @Test
  void eventValidationFailsWhenEventAndEnrollmentProgramDontMatch() {
    OrganisationUnit orgUnit = organisationUnit(ORG_UNIT_ID);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))).thenReturn(orgUnit);
    Program program = programWithRegistration(PROGRAM_UID, orgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    when(preheat.getProgramWithOrgUnitsMap())
        .thenReturn(Collections.singletonMap(PROGRAM_UID, Collections.singletonList(ORG_UNIT_ID)));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID)))
        .thenReturn(programStage(PROGRAM_STAGE_ID, program));
    when(preheat.getEnrollment(ENROLLMENT_ID))
        .thenReturn(
            enrollment(
                ENROLLMENT_ID, programWithRegistration(CodeGenerator.generateUid(), orgUnit)));

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(program))
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .enrollment(ENROLLMENT_ID)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1079);
  }

  @Test
  void eventValidationFailsWhenEventAndProgramOrganisationUnitDontMatch() {
    OrganisationUnit orgUnit = organisationUnit(ORG_UNIT_ID);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))).thenReturn(orgUnit);
    OrganisationUnit anotherOrgUnit = organisationUnit(CodeGenerator.generateUid());
    Program program = programWithRegistration(PROGRAM_UID, anotherOrgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    when(preheat.getProgramWithOrgUnitsMap())
        .thenReturn(
            Collections.singletonMap(
                PROGRAM_UID, Collections.singletonList(anotherOrgUnit.getUid())));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID)))
        .thenReturn(programStage(PROGRAM_STAGE_ID, program));
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(enrollment(ENROLLMENT_ID, program));

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(program))
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .enrollment(ENROLLMENT_ID)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1029);
  }

  @Test
  void eventOfProgramWithRegistrationInEnrollmentWithoutTrackedEntity() {
    OrganisationUnit orgUnit = setupOrgUnit();

    setupProgram(orgUnit);

    Event event = eventBuilder().enrollment(ENROLLMENT_ID).build();

    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(null);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1313);
  }

  @Test
  void eventOfProgramWithRegistrationInEnrollmentWithTrackedEntityInBundle() {
    OrganisationUnit orgUnit = setupOrgUnit();

    Enrollment enrollment = enrollment(ENROLLMENT_ID, new TrackedEntity());

    setupProgram(orgUnit, enrollment);

    Event event = eventBuilder().enrollment(ENROLLMENT_ID).build();

    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(null);

    org.hisp.dhis.tracker.imports.domain.Enrollment e =
        new org.hisp.dhis.tracker.imports.domain.Enrollment();
    e.setTrackedEntity(UID.generate());

    when(bundle.findEnrollmentByUid(ENROLLMENT_ID)).thenReturn(Optional.of(e));

    validator.validate(reporter, bundle, event);

    assertHasNoError(reporter, event, E1313);
  }

  @Test
  void eventOfProgramWithRegistrationInEnrollmentWithTrackedEntityInPreheat() {
    OrganisationUnit orgUnit = setupOrgUnit();

    setupProgram(orgUnit);

    Event event = eventBuilder().enrollment(ENROLLMENT_ID).build();

    validator.validate(reporter, bundle, event);

    assertHasNoError(reporter, event, E1313);
  }

  @Test
  void eventOfProgramWithRegistrationInEnrollmentWithoutTrackedEntityInPreheat() {
    OrganisationUnit orgUnit = setupOrgUnit();

    Enrollment enrollment = enrollment(ENROLLMENT_ID);

    setupProgram(orgUnit, enrollment);

    Event event = eventBuilder().enrollment(ENROLLMENT_ID).build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1313);
  }

  private OrganisationUnit organisationUnit(String uid) {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(uid);
    return organisationUnit;
  }

  private Program programWithRegistration(String uid, OrganisationUnit orgUnit) {
    return program(uid, ProgramType.WITH_REGISTRATION, 'A', orgUnit, trackedEntityType(TE_TYPE_ID));
  }

  private Program program(
      String uid,
      ProgramType type,
      char uniqueCharacter,
      OrganisationUnit orgUnit,
      TrackedEntityType teType) {
    Program program = createProgram(uniqueCharacter);
    program.setUid(uid);
    program.setProgramType(type);
    program.setOrganisationUnits(Sets.newHashSet(orgUnit));
    program.setTrackedEntityType(teType);
    return program;
  }

  private TrackedEntityType trackedEntityType(String uid) {
    return trackedEntityType(uid, 'A');
  }

  private TrackedEntityType trackedEntityType(String uid, char uniqueChar) {
    TrackedEntityType trackedEntityType = createTrackedEntityType(uniqueChar);
    trackedEntityType.setUid(uid);
    return trackedEntityType;
  }

  private ProgramStage programStage(String uid, Program program) {
    ProgramStage programStage = createProgramStage('A', program);
    programStage.setUid(uid);
    return programStage;
  }

  private Enrollment enrollment(UID uid, Program program) {
    Enrollment enrollment = enrollment(uid);
    enrollment.setProgram(program);
    enrollment.setTrackedEntity(new TrackedEntity());
    return enrollment;
  }

  private Enrollment enrollment(UID uid, TrackedEntity trackedEntity) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(uid.getValue());
    enrollment.setTrackedEntity(trackedEntity);
    return enrollment;
  }

  private Enrollment enrollment(UID uid) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(uid.getValue());
    return enrollment;
  }

  private Program setupProgram(OrganisationUnit orgUnit) {
    Program program = programWithRegistration(PROGRAM_UID, orgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    when(preheat.getProgramWithOrgUnitsMap())
        .thenReturn(Collections.singletonMap(PROGRAM_UID, Collections.singletonList(ORG_UNIT_ID)));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID)))
        .thenReturn(programStage(PROGRAM_STAGE_ID, program));
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(enrollment(ENROLLMENT_ID, program));
    return program;
  }

  private Program setupProgram(OrganisationUnit orgUnit, Enrollment enrollment) {
    Program program = programWithRegistration(PROGRAM_UID, orgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);
    when(preheat.getProgramWithOrgUnitsMap())
        .thenReturn(Collections.singletonMap(PROGRAM_UID, Collections.singletonList(ORG_UNIT_ID)));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID)))
        .thenReturn(programStage(PROGRAM_STAGE_ID, program));
    enrollment.setProgram(program);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(enrollment);
    return program;
  }

  private OrganisationUnit setupOrgUnit() {
    OrganisationUnit orgUnit = organisationUnit(ORG_UNIT_ID);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))).thenReturn(orgUnit);
    return orgUnit;
  }

  private TrackerEvent.TrackerEventBuilder eventBuilder() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.ofUid(PROGRAM_UID))
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
        .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .enrollment(ENROLLMENT_ID);
  }
}
