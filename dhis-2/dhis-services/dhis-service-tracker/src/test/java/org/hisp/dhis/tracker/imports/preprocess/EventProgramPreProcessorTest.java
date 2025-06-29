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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Collections;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Enrico Colasante
 */
class EventProgramPreProcessorTest {

  private static final String PROGRAM_STAGE_WITH_REGISTRATION = "PROGRAM_STAGE_WITH_REGISTRATION";

  private static final String PROGRAM_STAGE_WITHOUT_REGISTRATION =
      "PROGRAM_STAGE_WITHOUT_REGISTRATION";

  private static final String PROGRAM_WITH_REGISTRATION = "PROGRAM_WITH_REGISTRATION";

  private static final String PROGRAM_WITHOUT_REGISTRATION = "PROGRAM_WITHOUT_REGISTRATION";

  private TrackerPreheat preheat;

  private EventProgramPreProcessor preprocessor;

  @BeforeEach
  void setUp() {
    preheat = mock(TrackerPreheat.class);

    this.preprocessor = new EventProgramPreProcessor();
  }

  @Test
  void testTrackerEventIsEnhancedWithProgram() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(programStageWithRegistration());

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(trackerEventWithProgramStage()))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat).put(programWithRegistration());
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION),
        bundle.getEvents().get(0).getProgram());
  }

  @Test
  void testSingleEventIsEnhancedWithProgram() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITHOUT_REGISTRATION)))
        .thenReturn(programStageWithoutRegistration());

    TrackerBundle bundle =
        TrackerBundle.builder()
            .singleEvents(Collections.singletonList(singleEventWithProgramStage()))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat).put(programWithoutRegistration());
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION),
        bundle.getEvents().get(0).getProgram());
  }

  @Test
  void testTrackerEventWithProgramAndProgramStageIsNotProcessed() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    TrackerEvent event = completeTrackerEvent().build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat, never()).getProgram(PROGRAM_WITH_REGISTRATION);
    verify(preheat, never()).getProgramStage(PROGRAM_STAGE_WITH_REGISTRATION);
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION),
        bundle.getEvents().get(0).getProgram());
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_STAGE_WITH_REGISTRATION),
        bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testProgramStageHasNoReferenceToProgram() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("LGSWs20XFvy");
    when(preheat.getProgramStage("LGSWs20XFvy")).thenReturn(programStage);

    TrackerEvent event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.EMPTY_UID)
            .programStage(MetadataIdentifier.ofUid(programStage))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .attributeCategoryOptions(Collections.emptySet())
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat, never()).put(programStage.getProgram());
  }

  @Test
  void testSingleEventIsEnhancedWithProgramStage() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION)))
        .thenReturn(programWithoutRegistrationWithProgramStages());

    TrackerBundle bundle =
        TrackerBundle.builder()
            .singleEvents(Collections.singletonList(singleEventWithProgram()))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat).put(programStageWithoutRegistration());
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_STAGE_WITHOUT_REGISTRATION),
        bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testTrackerEventIsNotEnhancedWithProgramStage() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION)))
        .thenReturn(programWithRegistrationWithProgramStages());
    TrackerEvent event = trackerEventWithProgram();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION),
        bundle.getEvents().get(0).getProgram());
    assertEquals(MetadataIdentifier.EMPTY_UID, bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void shouldNotProcessEventWhenEventIsInvalidWithNoProgramAndNoProgramStage() {
    SingleEvent event = invalidSingleEventWithNoProgramAndNoProgramStage();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .singleEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat, never()).getProgram(PROGRAM_WITH_REGISTRATION);
    verify(preheat, never()).getProgramStage(PROGRAM_STAGE_WITH_REGISTRATION);
    assertNull(bundle.getEvents().get(0).getProgram());
    assertNull(bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testSingleEventWithProgramAndProgramStageIsNotProcessed() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    SingleEvent event = completeSingleEvent();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .singleEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat, never()).getProgram(PROGRAM_WITHOUT_REGISTRATION);
    verify(preheat, never()).getProgramStage(PROGRAM_STAGE_WITHOUT_REGISTRATION);
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION),
        bundle.getEvents().get(0).getProgram());
    assertEquals(
        MetadataIdentifier.ofUid(PROGRAM_STAGE_WITHOUT_REGISTRATION),
        bundle.getEvents().get(0).getProgramStage());
  }

  private ProgramStage programStageWithRegistration() {
    ProgramStage programStage = createProgramStage('A', 1, false);
    programStage.setUid(PROGRAM_STAGE_WITH_REGISTRATION);
    programStage.setProgram(programWithRegistration());
    return programStage;
  }

  private ProgramStage programStageWithoutRegistration() {
    ProgramStage programStage = createProgramStage('A', 1, false);
    programStage.setUid(PROGRAM_STAGE_WITHOUT_REGISTRATION);
    programStage.setProgram(programWithoutRegistration());
    return programStage;
  }

  private Program programWithRegistrationWithProgramStages() {
    Program program = createProgram('A');
    program.setUid(PROGRAM_WITH_REGISTRATION);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setProgramStages(Sets.newHashSet(programStageWithRegistration()));
    return program;
  }

  private Program programWithoutRegistrationWithProgramStages() {
    Program program = createProgram('B');
    program.setUid(PROGRAM_WITHOUT_REGISTRATION);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    program.setProgramStages(Sets.newHashSet(programStageWithoutRegistration()));
    return program;
  }

  private Program programWithRegistration() {
    Program program = createProgram('A');
    program.setUid(PROGRAM_WITH_REGISTRATION);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    return program;
  }

  private Program programWithoutRegistration() {
    Program program = createProgram('B');
    program.setUid(PROGRAM_WITHOUT_REGISTRATION);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    return program;
  }

  private SingleEvent invalidSingleEventWithNoProgramAndNoProgramStage() {
    return SingleEvent.builder()
        .event(UID.generate())
        .program(null)
        .programStage(null)
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private SingleEvent singleEventWithProgram() {
    return SingleEvent.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION))
        .programStage(MetadataIdentifier.EMPTY_UID)
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private SingleEvent singleEventWithProgramStage() {
    return SingleEvent.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.EMPTY_UID)
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITHOUT_REGISTRATION))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private SingleEvent completeSingleEvent() {
    return SingleEvent.builder()
        .event(UID.generate())
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITHOUT_REGISTRATION))
        .program(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private TrackerEvent trackerEventWithProgramStage() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.EMPTY_UID)
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITH_REGISTRATION))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private TrackerEvent trackerEventWithProgram() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION))
        .programStage(MetadataIdentifier.EMPTY_UID)
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private TrackerEvent.TrackerEventBuilder completeTrackerEvent() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITH_REGISTRATION))
        .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID);
  }
}
