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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1326;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class BlockEntryFormAfterCompletionValidatorTest {

  private static final String PROGRAM_STAGE_UID = "programStageU";

  private BlockEntryFormAfterCompletionValidator validator;

  @Mock private TrackerPreheat preheat;

  @Mock private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    validator = new BlockEntryFormAfterCompletionValidator();
    lenient().when(bundle.getPreheat()).thenReturn(preheat);
    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
  }

  @Test
  void shouldFailWhenEventIsCompletedAndSavedStatusIsCompletedAndBlockEntryFormIsEnabled() {
    stubProgramStage(true);
    UID uid = UID.generate();
    Event event = event(uid, EventStatus.COMPLETED);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1326);
  }

  @ParameterizedTest
  @EnumSource(
      value = EventStatus.class,
      mode = Mode.EXCLUDE,
      names = {"COMPLETED"})
  void shouldPassWhenEventIsCompletedButSavedStatusIsNotCompleted(EventStatus savedStatus) {
    stubProgramStage(true);
    String uid = CodeGenerator.generateUid();
    stubSavedEvent(uid, savedStatus);
    Event event = event(uid, EventStatus.COMPLETED);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventIsCompletedAndSavedStatusIsCompletedButBlockEntryFormIsDisabled() {
    stubProgramStage(false);
    String uid = CodeGenerator.generateUid();
    stubSavedEvent(uid, EventStatus.COMPLETED);
    Event event = event(uid, EventStatus.COMPLETED);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @EnumSource(
      value = EventStatus.class,
      mode = Mode.EXCLUDE,
      names = {"COMPLETED"})
  void shouldPassWhenBlockEntryFormIsEnabledButEventIsNotCompleted(EventStatus status) {
    stubProgramStage(true);
    UID uid = UID.generate();
    Event event = event(uid, status);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldOnlyRunOnUpdate() {
    assertTrue(validator.needsToRun(TrackerImportStrategy.UPDATE));
    assertFalse(validator.needsToRun(TrackerImportStrategy.CREATE));
    assertFalse(validator.needsToRun(TrackerImportStrategy.CREATE_AND_UPDATE));
    assertFalse(validator.needsToRun(TrackerImportStrategy.DELETE));
  }

  private void stubProgramStage(boolean blockEntryForm) {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setBlockEntryForm(blockEntryForm);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);
  }

  private void stubSavedEvent(String uid, EventStatus status) {
    org.hisp.dhis.program.Event savedEvent = new org.hisp.dhis.program.Event();
    savedEvent.setStatus(status);
    when(preheat.getEvent(uid)).thenReturn(savedEvent);
  }

  private Event event(String uid, EventStatus status) {
    return Event.builder()
        .event(uid)
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
        .status(status)
        .build();
  }
}
