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
package org.hisp.dhis.tracker.programrule.executor.event;

import static org.hisp.dhis.tracker.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1301;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1314;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetMandatoryFieldExecutorTest extends DhisConvenienceTest {
  private static final String ACTIVE_EVENT_ID = "EventUid";

  private static final String COMPLETED_EVENT_ID = "CompletedEventUid";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String DATA_ELEMENT_VALUE = "1.0";

  private static final String RULE_UID = "Rule uid";

  private static ProgramStage programStage;

  private static DataElement dataElement;

  private final SetMandatoryFieldExecutor executor =
      new SetMandatoryFieldExecutor(RULE_UID, DATA_ELEMENT_ID);

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  public static Stream<Arguments> transactionsCreatingDataValues() {
    return Stream.of(
        Arguments.of(EventStatus.SCHEDULE, EventStatus.ACTIVE),
        Arguments.of(EventStatus.SCHEDULE, EventStatus.COMPLETED),
        Arguments.of(EventStatus.SCHEDULE, EventStatus.VISITED),
        Arguments.of(EventStatus.OVERDUE, EventStatus.ACTIVE),
        Arguments.of(EventStatus.OVERDUE, EventStatus.COMPLETED),
        Arguments.of(EventStatus.OVERDUE, EventStatus.VISITED),
        Arguments.of(EventStatus.SKIPPED, EventStatus.ACTIVE),
        Arguments.of(EventStatus.SKIPPED, EventStatus.COMPLETED),
        Arguments.of(EventStatus.SKIPPED, EventStatus.VISITED));
  }

  public static Stream<Arguments> transactionsNotCreatingDataValues() {
    return Stream.of(
        Arguments.of(EventStatus.ACTIVE, EventStatus.ACTIVE),
        Arguments.of(EventStatus.ACTIVE, EventStatus.COMPLETED),
        Arguments.of(EventStatus.ACTIVE, EventStatus.VISITED),
        Arguments.of(EventStatus.VISITED, EventStatus.ACTIVE),
        Arguments.of(EventStatus.VISITED, EventStatus.COMPLETED),
        Arguments.of(EventStatus.VISITED, EventStatus.VISITED),
        Arguments.of(EventStatus.COMPLETED, EventStatus.ACTIVE),
        Arguments.of(EventStatus.COMPLETED, EventStatus.COMPLETED),
        Arguments.of(EventStatus.COMPLETED, EventStatus.VISITED));
  }

  @BeforeEach
  void setUpTest() {
    programStage = createProgramStage('A', 0);
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElement = createDataElement('A');
    dataElement.setUid(DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(programStage, dataElement, 0);
    programStage.setProgramStageDataElements(Set.of(programStageDataElementA));

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
  }

  @Test
  void shouldPassValidationWhenMandatoryFieldIsPresentAndStrategyIsCreate() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithMandatoryValueSet();
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.CREATE);
    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, event);

    assertTrue(error.isEmpty());
  }

  @Test
  void shouldPassValidationWhenMandatoryFieldIsPresentAndStrategyIsUpdate() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithMandatoryValueSet();
    when(preheat.getEvent(event.getUid()))
        .thenReturn(new org.hisp.dhis.program.ProgramStageInstance());
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.UPDATE);
    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, event);

    assertTrue(error.isEmpty());
  }

  @Test
  void shouldFailValidationWhenMandatoryFieldIsDeletedAndStrategyIsCreate() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithDeleteMandatoryValue();
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.CREATE);
    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, event);

    assertFalse(error.isEmpty());
    assertEquals(error(RULE_UID, E1314, dataElement.getUid()), error.get());
  }

  @Test
  void shouldFailValidationWhenMandatoryFieldIsDeletedAndStrategyIsUpdate() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithDeleteMandatoryValue();
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.UPDATE);
    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, event);

    assertFalse(error.isEmpty());
    assertEquals(error(RULE_UID, E1314, dataElement.getUid()), error.get());
  }

  @ParameterizedTest
  @MethodSource("transactionsNotCreatingDataValues")
  void shouldPassValidationWhenMandatoryFieldIsNotPresentInPayloadButPresentInDB(
      EventStatus savedStatus, EventStatus newStatus) {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithMandatoryValueNOTSet(savedStatus);
    when(preheat.getEvent(event.getUid()))
        .thenReturn(eventWithMandatoryValue(event.getUid(), newStatus));
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.UPDATE);
    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, event);

    assertTrue(error.isEmpty());
  }

  @Test
  void shouldPassValidationWhenMandatoryFieldIsPresentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithMandatoryValueSet(idSchemes);
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.CREATE);
    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, event);

    assertTrue(error.isEmpty());
  }

  @Test
  void shouldFailValidationWhenOneMandatoryFieldIsNotPresentAndStrategyIsCreate() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    bundle.setEvents(List.of(getEventWithMandatoryValueSet(), getEventWithMandatoryValueNOTSet()));
    bundle.setStrategy(getEventWithMandatoryValueNOTSet(), TrackerImportStrategy.CREATE);
    bundle.setStrategy(getEventWithMandatoryValueSet(), TrackerImportStrategy.CREATE);
    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEventWithMandatoryValueSet());
    assertTrue(error.isEmpty());

    error = executor.executeRuleAction(bundle, getEventWithMandatoryValueNOTSet());

    assertFalse(error.isEmpty());
    assertEquals(error(RULE_UID, E1301, dataElement.getUid()), error.get());
  }

  @ParameterizedTest
  @MethodSource("transactionsCreatingDataValues")
  void shouldFailValidationWhenMandatoryFieldIsNotPresentAndDataValuesAreCreated(
      EventStatus savedStatus, EventStatus newStatus) {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    Event event = getEventWithMandatoryValueNOTSet(newStatus);
    when(preheat.getEvent(event.getUid())).thenReturn(event(event.getUid(), savedStatus));
    bundle.setEvents(List.of(event));
    bundle.setStrategy(event, TrackerImportStrategy.UPDATE);
    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEventWithMandatoryValueNOTSet());

    assertFalse(error.isEmpty());
    assertEquals(error(RULE_UID, E1301, dataElement.getUid()), error.get());
  }

  private org.hisp.dhis.program.ProgramStageInstance eventWithMandatoryValue(
      String uid, EventStatus status) {
    org.hisp.dhis.program.ProgramStageInstance event =
        new org.hisp.dhis.program.ProgramStageInstance();
    event.setUid(uid);
    event.setStatus(status);
    event.setEventDataValues(Set.of(new EventDataValue(DATA_ELEMENT_ID, DATA_ELEMENT_VALUE)));
    return event;
  }

  private org.hisp.dhis.program.ProgramStageInstance event(String uid, EventStatus status) {
    org.hisp.dhis.program.ProgramStageInstance event =
        new org.hisp.dhis.program.ProgramStageInstance();
    event.setUid(uid);
    event.setStatus(status);
    return event;
  }

  private Event getEventWithDeleteMandatoryValue() {
    return Event.builder()
        .event(ACTIVE_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(programStage))
        .dataValues(getNullEventDataValues())
        .build();
  }

  private Event getEventWithMandatoryValueSet(TrackerIdSchemeParams idSchemes) {
    return Event.builder()
        .event(ACTIVE_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(idSchemes.toMetadataIdentifier(programStage))
        .dataValues(getActiveEventDataValues(idSchemes))
        .build();
  }

  private Event getEventWithMandatoryValueSet() {
    return Event.builder()
        .event(ACTIVE_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(programStage))
        .dataValues(getActiveEventDataValues())
        .build();
  }

  private Event getEventWithMandatoryValueNOTSet() {
    return Event.builder()
        .event(COMPLETED_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(programStage))
        .build();
  }

  private Event getEventWithMandatoryValueNOTSet(EventStatus status) {
    return Event.builder()
        .event(COMPLETED_EVENT_ID)
        .status(status)
        .programStage(MetadataIdentifier.ofUid(programStage))
        .build();
  }

  private Set<DataValue> getActiveEventDataValues(TrackerIdSchemeParams idSchemes) {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_VALUE)
            .dataElement(idSchemes.toMetadataIdentifier(dataElement))
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getActiveEventDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_VALUE)
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getNullEventDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .value(null)
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .build();
    return Set.of(dataValue);
  }
}
