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

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1128;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class UpdatableFieldsValidatorTest {

  private static final String TRACKED_ENTITY_TYPE_ID = "TrackedEntityTypeId";

  private static final String PROGRAM_ID = "ProgramId";

  private static final String PROGRAM_STAGE_ID = "ProgramStageId";

  private static final UID TRACKED_ENTITY_ID = UID.generate();

  private static final UID ENROLLMENT_ID = UID.generate();

  private static final UID EVENT_UID = UID.generate();

  private UpdatableFieldsValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new UpdatableFieldsValidator();

    when(bundle.getImportStrategy()).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.TrackedEntity.class)))
        .thenReturn(TrackerImportStrategy.UPDATE);
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Enrollment.class)))
        .thenReturn(TrackerImportStrategy.UPDATE);
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Event.class)))
        .thenReturn(TrackerImportStrategy.UPDATE);

    when(preheat.getTrackedEntity(TRACKED_ENTITY_ID)).thenReturn(trackedEntity());
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(getEnrollment());
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(event());

    when(bundle.getPreheat()).thenReturn(preheat);

    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
  }

  @Test
  void verifyEventValidationSuccess() {
    org.hisp.dhis.tracker.imports.domain.Event event = validEvent();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEventValidationFailsWhenUpdateProgramStage() {
    org.hisp.dhis.tracker.imports.domain.Event event = validEvent();
    event.setProgramStage(MetadataIdentifier.ofUid("NewProgramStageId"));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1128, "programStage");
  }

  @Test
  void verifyEventValidationFailsWhenUpdateEnrollment() {
    org.hisp.dhis.tracker.imports.domain.Event event = validEvent();
    event.setEnrollment(UID.generate());

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1128, "enrollment");
  }

  private org.hisp.dhis.tracker.imports.domain.Event validEvent() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(EVENT_UID)
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
        .enrollment(ENROLLMENT_ID)
        .build();
  }

  private TrackedEntity trackedEntity() {
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_ID);

    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TRACKED_ENTITY_ID.getValue());
    trackedEntity.setTrackedEntityType(trackedEntityType);
    return trackedEntity;
  }

  private Enrollment getEnrollment() {
    Program program = new Program();
    program.setUid(PROGRAM_ID);

    Enrollment enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT_ID.getValue());
    enrollment.setProgram(program);
    enrollment.setTrackedEntity(trackedEntity());
    return enrollment;
  }

  private TrackerEvent event() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_ID);

    TrackerEvent event = new TrackerEvent();
    event.setUid(EVENT_UID.getValue());
    event.setEnrollment(getEnrollment());
    event.setProgramStage(programStage);
    return event;
  }
}
