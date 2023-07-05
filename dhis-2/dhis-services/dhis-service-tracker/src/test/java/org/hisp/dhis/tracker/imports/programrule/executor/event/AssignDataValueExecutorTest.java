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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.IssueType;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AssignDataValueExecutorTest extends DhisConvenienceTest {
  private static final String EVENT_ID = "EventId";

  private static final String SECOND_EVENT_ID = "SecondEventId";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String DATA_ELEMENT_CODE = "DataElementCode";

  private static final String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

  private static final String OPTION_SET_DATA_ELEMENT_ID = "OptionSetDataElementId";

  private static final String DATAELEMENT_OLD_VALUE = "10.0";

  private static final String DATAELEMENT_NEW_VALUE = "24.0";

  private static final String VALID_OPTION_VALUE = "10";

  private static final String INVALID_OPTION_VALUE = "0";

  private static ProgramStage firstProgramStage;

  private static ProgramStage secondProgramStage;

  private static DataElement dataElementA;

  private static DataElement dataElementB;

  private static DataElement optionSetDataElement;

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private SystemSettingManager systemSettingManager;

  @BeforeEach
  void setUpTest() {
    firstProgramStage = createProgramStage('A', 0);
    firstProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementA = createDataElement('A');
    dataElementA.setUid(DATA_ELEMENT_ID);
    dataElementA.setCode(DATA_ELEMENT_CODE);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(firstProgramStage, dataElementA, 0);
    firstProgramStage.setProgramStageDataElements(Set.of(programStageDataElementA));
    secondProgramStage = createProgramStage('B', 0);
    secondProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementB = createDataElement('B');
    dataElementB.setUid(ANOTHER_DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementB =
        createProgramStageDataElement(secondProgramStage, dataElementB, 0);
    optionSetDataElement = createDataElement('P');
    optionSetDataElement.setUid(OPTION_SET_DATA_ELEMENT_ID);
    OptionSet optionSet = new OptionSet();
    Option option = new Option("ten", "10");
    optionSet.setOptions(List.of(option));
    optionSet.setValueType(ValueType.TEXT);
    optionSetDataElement.setOptionSet(optionSet);
    ProgramStageDataElement programStageDataElementOptionSet =
        createProgramStageDataElement(secondProgramStage, optionSetDataElement, 0);
    secondProgramStage.setProgramStageDataElements(
        Set.of(programStageDataElementB, programStageDataElementOptionSet));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(firstProgramStage)))
        .thenReturn(firstProgramStage);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(secondProgramStage)))
        .thenReturn(secondProgramStage);
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElementA);
    when(preheat.getDataElement(OPTION_SET_DATA_ELEMENT_ID)).thenReturn(optionSetDataElement);

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.FALSE);
  }

  @Test
  void shouldFailWhenAssignedValueIsInvalidOptionAndDataValueIsValidOption() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            INVALID_OPTION_VALUE,
            OPTION_SET_DATA_ELEMENT_ID,
            eventWithOptionDataValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithOptionDataValue);

    Optional<DataValue> dataValue =
        findDataValueByUid(bundle, EVENT_ID, OPTION_SET_DATA_ELEMENT_ID);

    assertDataValueWasNotAssignedAndErrorIsPresent(VALID_OPTION_VALUE, dataValue, warning);
  }

  @Test
  void shouldAssignDataValueWhenAssignedValueIsValidOptionAndDataValueIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            VALID_OPTION_VALUE,
            OPTION_SET_DATA_ELEMENT_ID,
            eventWithOptionDataValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithOptionDataValue);

    Optional<DataValue> dataValue =
        findDataValueByUid(bundle, EVENT_ID, OPTION_SET_DATA_ELEMENT_ID);

    assertTrue(dataValue.isPresent());
    assertEquals(VALID_OPTION_VALUE, dataValue.get().getValue());
    assertTrue(warning.isPresent());
    assertEquals(WARNING, warning.get().getIssueType());
  }

  @Test
  void shouldAssignDataValueWhenAssignedValueIsInvalidOptionAndDataValueIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    Event eventWithOptionDataValue = getEventWithDataValueNOTSet();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            INVALID_OPTION_VALUE,
            OPTION_SET_DATA_ELEMENT_ID,
            eventWithOptionDataValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithOptionDataValue);

    Optional<DataValue> dataValue =
        findDataValueByUid(bundle, SECOND_EVENT_ID, OPTION_SET_DATA_ELEMENT_ID);

    assertAll(
        () -> assertTrue(dataValue.isEmpty()),
        () -> assertTrue(warning.isPresent()),
        () -> assertEquals(WARNING, warning.get().getIssueType()));
  }

  @Test
  void shouldAssignNullDataValueWhenAssignedValueIsInvalidOptionAndOverwriteIsTrue() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            INVALID_OPTION_VALUE,
            OPTION_SET_DATA_ELEMENT_ID,
            eventWithOptionDataValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithOptionDataValue);

    Optional<DataValue> dataValue =
        findDataValueByUid(bundle, EVENT_ID, OPTION_SET_DATA_ELEMENT_ID);

    assertDataValueWasAssignedAndWarningIsPresent(null, dataValue, warning);
  }

  @Test
  void shouldAssignDataValueValueForEventsWhenDataValueIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    Event eventWithDataValueNOTSet = getEventWithDataValueNOTSet();
    List<Event> events = List.of(eventWithDataValueNOTSet);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_ID,
            eventWithDataValueNOTSet.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithDataValueNOTSet);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, SECOND_EVENT_ID, DATA_ELEMENT_ID);

    assertDataValueWasAssignedAndWarningIsPresent(DATAELEMENT_NEW_VALUE, dataValue, warning);
  }

  @Test
  void shouldNotAssignDataValueValueForEventsWhenDataValueIsAlreadyPresent() {
    Event eventWithDataValueSet = getEventWithDataValueSet();
    List<Event> events = List.of(eventWithDataValueSet);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_ID,
            eventWithDataValueSet.getDataValues());

    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, eventWithDataValueSet);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, EVENT_ID, DATA_ELEMENT_ID);

    assertDataValueWasNotAssignedAndErrorIsPresent(DATAELEMENT_OLD_VALUE, dataValue, error);
  }

  @Test
  void shouldNotAssignDataValueValueForEventsWhenDataValueIsAlreadyPresentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElementA);
    Event eventWithDataValueSet = getEventWithDataValueSet(idSchemes);
    List<Event> events = List.of(eventWithDataValueSet);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_ID,
            eventWithDataValueSet.getDataValues());

    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, eventWithDataValueSet);

    Optional<DataValue> dataValue = findDataValueByCode(bundle, EVENT_ID, DATA_ELEMENT_CODE);

    assertDataValueWasNotAssignedAndErrorIsPresent(DATAELEMENT_OLD_VALUE, dataValue, error);
  }

  @Test
  void shouldAssignDataValueValueForEventsWhenDataValueIsAlreadyPresentAndHasTheSameValue() {
    Event eventWithDataValueSetSameValue = getEventWithDataValueSetSameValue();
    List<Event> events = List.of(eventWithDataValueSetSameValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_ID,
            eventWithDataValueSetSameValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithDataValueSetSameValue);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, EVENT_ID, DATA_ELEMENT_ID);

    assertDataValueWasAssignedAndWarningIsPresent(DATAELEMENT_NEW_VALUE, dataValue, warning);
  }

  @Test
  void
      shouldAssignDataValueValueForEventsWhenDataValueIsAlreadyPresentAndSystemSettingToOverwriteIsTrue() {
    Event eventWithDataValueSet = getEventWithDataValueSet();
    List<Event> events = List.of(eventWithDataValueSet);
    bundle.setEvents(events);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            systemSettingManager,
            "",
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_ID,
            eventWithDataValueSet.getDataValues());

    Optional<ProgramRuleIssue> warning = executor.executeRuleAction(bundle, eventWithDataValueSet);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, EVENT_ID, DATA_ELEMENT_ID);

    assertDataValueWasAssignedAndWarningIsPresent(DATAELEMENT_NEW_VALUE, dataValue, warning);
  }

  private Optional<DataValue> findDataValueByUid(
      TrackerBundle bundle, String eventUid, String dataValueUid) {
    Event event = bundle.findEventByUid(eventUid).get();
    return event.getDataValues().stream()
        .filter(dv -> dv.getDataElement().equals(MetadataIdentifier.ofUid(dataValueUid)))
        .findAny();
  }

  private Optional<DataValue> findDataValueByCode(
      TrackerBundle bundle, String eventUid, String dataValueCode) {
    Event event = bundle.findEventByUid(eventUid).get();
    return event.getDataValues().stream()
        .filter(dv -> dv.getDataElement().equals(MetadataIdentifier.ofCode(dataValueCode)))
        .findAny();
  }

  private void assertDataValueWasAssignedAndWarningIsPresent(
      String dataValue, Optional<DataValue> dataElement, Optional<ProgramRuleIssue> warning) {
    assertDataValueWasAssignedAndValidationIsPresent(dataValue, dataElement, warning, WARNING);
  }

  private void assertDataValueWasNotAssignedAndErrorIsPresent(
      String dataValue, Optional<DataValue> dataElement, Optional<ProgramRuleIssue> error) {
    assertDataValueWasAssignedAndValidationIsPresent(dataValue, dataElement, error, ERROR);
  }

  private void assertDataValueWasAssignedAndValidationIsPresent(
      String dataValue,
      Optional<DataValue> dataElement,
      Optional<ProgramRuleIssue> warning,
      IssueType issueType) {
    assertTrue(dataElement.isPresent());
    assertEquals(dataValue, dataElement.get().getValue());
    assertTrue(warning.isPresent());
    assertEquals(issueType, warning.get().getIssueType());
  }

  private Event getEventWithDataValueSet() {
    return Event.builder()
        .event(EVENT_ID)
        .status(EventStatus.ACTIVE)
        .dataValues(getDataValues())
        .build();
  }

  private Event getEventWithDataValueSet(TrackerIdSchemeParams idSchemes) {
    return Event.builder()
        .event(EVENT_ID)
        .status(EventStatus.ACTIVE)
        .dataValues(getDataValues(idSchemes))
        .build();
  }

  private Event getEventWithDataValueSetSameValue() {
    return Event.builder()
        .event(EVENT_ID)
        .status(EventStatus.ACTIVE)
        .dataValues(getDataValuesSameValue())
        .build();
  }

  private Event getEventWithOptionSetDataValueWithValidValue() {
    return Event.builder()
        .event(EVENT_ID)
        .status(EventStatus.ACTIVE)
        .dataValues(getOptionSetDataValues())
        .build();
  }

  private Event getEventWithDataValueNOTSet() {
    return Event.builder().event(SECOND_EVENT_ID).status(EventStatus.COMPLETED).build();
  }

  private Set<DataValue> getDataValues(TrackerIdSchemeParams idSchemes) {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(idSchemes.toMetadataIdentifier(dataElementA))
            .value(DATAELEMENT_OLD_VALUE)
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .value(DATAELEMENT_OLD_VALUE)
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getDataValuesSameValue() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .value(DATAELEMENT_NEW_VALUE)
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getOptionSetDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid(OPTION_SET_DATA_ELEMENT_ID))
            .value(VALID_OPTION_VALUE)
            .build();
    return Set.of(dataValue);
  }
}
