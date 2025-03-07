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
package org.hisp.dhis.tracker.programrule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.AttributeValueConverterService;
import org.hisp.dhis.tracker.converter.RuleEngineConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultTrackerProgramRuleServiceTest extends DhisConvenienceTest {

  @Mock private ProgramRuleEngine programRuleEngine;

  @Mock
  private RuleEngineConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService;

  @Mock
  private RuleEngineConverterService<Event, org.hisp.dhis.program.ProgramStageInstance>
      eventTrackerConverterService;

  @Mock private EventService eventService;

  @Mock private ProgramStageInstanceService programStageInstanceService;

  @Mock private TrackerBundle trackerBundle;

  @Mock private TrackerPreheat preheat;

  private DefaultTrackerProgramRuleService defaultTrackerProgramRuleService;

  private final Program trackerProgram = createProgram('T');

  private final Program eventProgram = createProgramWithoutRegistration('E');

  @BeforeEach
  void setUp() {
    defaultTrackerProgramRuleService =
        new DefaultTrackerProgramRuleService(
            programRuleEngine,
            enrollmentTrackerConverterService,
            eventTrackerConverterService,
            new AttributeValueConverterService(),
            eventService,
            programStageInstanceService);
  }

  @Test
  void shouldInvokeSpecificMethodWhenCalculatingRuleEffectsForTrackerEvents() {
    String enrollmentUid = CodeGenerator.generateUid();
    String eventUid = CodeGenerator.generateUid();
    List<Event> events = new ArrayList<>();
    Event event =
        Event.builder()
            .enrollment(enrollmentUid)
            .event(eventUid)
            .program(MetadataIdentifier.ofUid(trackerProgram.getUid()))
            .build();
    events.add(event);
    ProgramInstance programInstance = new ProgramInstance();
    programInstance.setUid(enrollmentUid);
    programInstance.setProgram(trackerProgram);
    List<ProgramStageInstance> programStageInstances = new ArrayList<>();
    programStageInstances.add(new ProgramStageInstance());

    when(preheat.getEnrollment(enrollmentUid)).thenReturn(programInstance);
    when(eventService.getEvents(any())).thenReturn(new Events());
    when(trackerBundle.getEnrollments()).thenReturn(Collections.emptyList());
    when(trackerBundle.getEvents()).thenReturn(events);
    when(trackerBundle.getPreheat()).thenReturn(preheat);
    when(eventTrackerConverterService.fromForRuleEngine(any(TrackerPreheat.class), anyList()))
        .thenReturn(programStageInstances);
    when(programRuleEngine.evaluateEnrollmentAndEvents(
            any(ProgramInstance.class), anySet(), anyList()))
        .thenReturn(Collections.emptyList());

    defaultTrackerProgramRuleService.calculateRuleEffects(trackerBundle);

    Mockito.verify(programRuleEngine, Mockito.times(1))
        .evaluateEnrollmentAndEvents(any(ProgramInstance.class), anySet(), anyList());
    Mockito.verify(programRuleEngine, Mockito.times(0))
        .evaluateProgramEvent(any(), any(), anyList());
  }

  @Test
  void shouldInvokeSpecificMethodWhenCalculatingRuleEffectsForProgramEvents() {
    String enrollmentUid = CodeGenerator.generateUid();
    String eventUid = CodeGenerator.generateUid();
    List<Event> events = new ArrayList<>();
    Event event =
        Event.builder()
            .enrollment(enrollmentUid)
            .event(eventUid)
            .program(MetadataIdentifier.ofUid(eventProgram.getUid()))
            .build();
    events.add(event);
    ProgramInstance programInstance = new ProgramInstance();
    programInstance.setUid(enrollmentUid);
    programInstance.setProgram(eventProgram);

    when(trackerBundle.getEnrollments()).thenReturn(Collections.emptyList());
    when(trackerBundle.getEvents()).thenReturn(events);
    when(trackerBundle.getPreheat()).thenReturn(preheat);
    when(preheat.getEnrollment(enrollmentUid)).thenReturn(programInstance);
    when(eventTrackerConverterService.fromForRuleEngine(any(TrackerPreheat.class), anyList()))
        .thenReturn(Collections.emptyList());
    when(programRuleEngine.evaluateProgramEvents(anySet(), any()))
        .thenReturn(Collections.emptyList());

    defaultTrackerProgramRuleService.calculateRuleEffects(trackerBundle);

    Mockito.verify(programRuleEngine, Mockito.times(0))
        .evaluateEnrollmentAndEvents(any(ProgramInstance.class), anySet(), anyList());
    Mockito.verify(programRuleEngine, Mockito.times(1)).evaluateProgramEvents(anySet(), any());
  }
}
