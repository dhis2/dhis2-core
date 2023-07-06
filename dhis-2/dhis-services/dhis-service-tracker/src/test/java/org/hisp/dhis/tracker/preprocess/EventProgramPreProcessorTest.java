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
package org.hisp.dhis.tracker.preprocess;

import static org.hisp.dhis.DhisConvenienceTest.createCategoryCombo;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Collections;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
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
    when(preheat.get(ProgramStage.class, PROGRAM_STAGE_WITH_REGISTRATION))
        .thenReturn(programStageWithRegistration());

    TrackerBundle bundle =
        TrackerBundle.builder()
            .events(Collections.singletonList(trackerEventWithProgramStage()))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat).put(TrackerIdSchemeParam.UID, programWithRegistration());
    assertEquals(PROGRAM_WITH_REGISTRATION, bundle.getEvents().get(0).getProgram());
  }

  @Test
  void testProgramEventIsEnhancedWithProgram() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.get(ProgramStage.class, PROGRAM_STAGE_WITHOUT_REGISTRATION))
        .thenReturn(programStageWithoutRegistration());

    TrackerBundle bundle =
        TrackerBundle.builder()
            .events(Collections.singletonList(programEventWithProgramStage()))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    verify(preheat).put(TrackerIdSchemeParam.UID, programWithoutRegistration());
    assertEquals(PROGRAM_WITHOUT_REGISTRATION, bundle.getEvents().get(0).getProgram());
  }

  @Test
  void testTrackerEventWithProgramAndProgramStageIsNotProcessed() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Event event = completeTrackerEvent();
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    verify(preheat, never()).get(Program.class, PROGRAM_WITH_REGISTRATION);
    verify(preheat, never()).get(ProgramStage.class, PROGRAM_STAGE_WITH_REGISTRATION);
    assertEquals(PROGRAM_WITH_REGISTRATION, bundle.getEvents().get(0).getProgram());
    assertEquals(PROGRAM_STAGE_WITH_REGISTRATION, bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testProgramStageHasNoReferenceToProgram() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("LGSWs20XFvy");
    when(preheat.get(ProgramStage.class, "LGSWs20XFvy")).thenReturn(programStage);

    Event event = new Event();
    event.setProgramStage(programStage.getUid());
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    verify(preheat, never()).put(TrackerIdSchemeParam.UID, programStage.getProgram());
  }

  @Test
  void testProgramEventIsEnhancedWithProgramStage() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.get(Program.class, PROGRAM_WITHOUT_REGISTRATION))
        .thenReturn(programWithoutRegistrationWithProgramStages());

    Event event = programEventWithProgram();
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    verify(preheat).put(TrackerIdSchemeParam.UID, programStageWithoutRegistration());
    assertEquals(PROGRAM_STAGE_WITHOUT_REGISTRATION, bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testTrackerEventIsNotEnhancedWithProgramStage() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);
    when(preheat.get(Program.class, PROGRAM_WITH_REGISTRATION))
        .thenReturn(programWithRegistrationWithProgramStages());
    Event event = trackerEventWithProgram();
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertEquals(PROGRAM_WITH_REGISTRATION, bundle.getEvents().get(0).getProgram());
    assertNull(bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testProgramEventWithProgramAndProgramStageIsNotProcessed() {
    TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Event event = completeProgramEvent();
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    verify(preheat, never()).get(Program.class, PROGRAM_WITHOUT_REGISTRATION);
    verify(preheat, never()).get(ProgramStage.class, PROGRAM_STAGE_WITHOUT_REGISTRATION);
    assertEquals(PROGRAM_WITHOUT_REGISTRATION, bundle.getEvents().get(0).getProgram());
    assertEquals(PROGRAM_STAGE_WITHOUT_REGISTRATION, bundle.getEvents().get(0).getProgramStage());
  }

  @Test
  void testEventWithOnlyCOsIsEnhancedWithAOC() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Event event = completeTrackerEvent();
    event.setProgram(program.getUid());
    event.setAttributeCategoryOptions("123;235");
    when(preheat.get(Program.class, event.getProgram())).thenReturn(program);
    CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo('A');
    when(preheat.getCategoryOptionComboIdentifier(categoryCombo, "123;235"))
        .thenReturn(categoryOptionCombo.getCode());

    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertEquals(
        categoryOptionCombo.getCode(), bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals("123;235", bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithOnlyCOsIsNotEnhancedWithAOCIfItCantBeFound() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Event event = completeTrackerEvent();
    event.setProgram(program.getUid());
    event.setAttributeCategoryOptions("123;235");
    when(preheat.get(Program.class, event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertNull(bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals("123;235", bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithOnlyCOsIsNotEnhancedWithAOCIfProgramCantBeFound() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Event event = completeTrackerEvent();
    event.setProgram(program.getUid());
    event.setAttributeCategoryOptions("123;235");

    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertNull(bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals("123;235", bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithAOCAndCOsIsNotEnhancedWithAOC() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Event event = completeTrackerEvent();
    event.setProgram(program.getUid());
    event.setAttributeOptionCombo("9871");
    event.setAttributeCategoryOptions("123;235");
    when(preheat.get(Program.class, event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertEquals("9871", bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals("123;235", bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithOnlyAOCIsLeftUnchanged() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Event event = completeTrackerEvent();
    event.setProgram(program.getUid());
    event.setAttributeOptionCombo("9871");
    when(preheat.get(Program.class, event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertEquals("9871", bundle.getEvents().get(0).getAttributeOptionCombo());
  }

  @Test
  void testEventWithNoAOCAndNoCOsIsNotEnhancedWithAOC() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Event event = completeTrackerEvent();
    event.setProgram(program.getUid());
    when(preheat.get(Program.class, event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preprocessor.process(bundle);

    assertNull(bundle.getEvents().get(0).getAttributeOptionCombo());
    assertNull(bundle.getEvents().get(0).getAttributeCategoryOptions());
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

  private Event programEventWithProgram() {
    Event event = new Event();
    event.setProgram(PROGRAM_WITHOUT_REGISTRATION);
    return event;
  }

  private Event programEventWithProgramStage() {
    Event event = new Event();
    event.setProgramStage(PROGRAM_STAGE_WITHOUT_REGISTRATION);
    return event;
  }

  private Event completeProgramEvent() {
    Event event = new Event();
    event.setProgramStage(PROGRAM_STAGE_WITHOUT_REGISTRATION);
    event.setProgram(PROGRAM_WITHOUT_REGISTRATION);
    return event;
  }

  private Event trackerEventWithProgramStage() {
    Event event = new Event();
    event.setProgramStage(PROGRAM_STAGE_WITH_REGISTRATION);
    return event;
  }

  private Event trackerEventWithProgram() {
    Event event = new Event();
    event.setProgram(PROGRAM_WITH_REGISTRATION);
    return event;
  }

  private Event completeTrackerEvent() {
    Event event = new Event();
    event.setProgramStage(PROGRAM_STAGE_WITH_REGISTRATION);
    event.setProgram(PROGRAM_WITH_REGISTRATION);
    return event;
  }
}
