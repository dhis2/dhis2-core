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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
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
class AssignDataValueExecutorTest extends TestBase {
  private static final UID RULE_UID = UID.of("TvctPPhpD8u");

  private static final UID EVENT_UID = UID.generate();

  private static final UID SECOND_EVENT_UID = UID.generate();

  private static final UID DATA_ELEMENT_UID = UID.of("h4w96yEMlzO");

  private static final String DATA_ELEMENT_CODE = "DataElementCode";

  private static final String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

  private static final UID OPTION_SET_DATA_ELEMENT_UID = UID.of("NpsdDv6kKSO");

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

  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private SystemSettings settings;
  @Mock private OptionService optionService;

  @BeforeEach
  void setUpTest() {
    firstProgramStage = createProgramStage('A', 0);
    firstProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementA = createDataElement('A');
    dataElementA.setUid(DATA_ELEMENT_UID.getValue());
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
    optionSetDataElement.setUid(OPTION_SET_DATA_ELEMENT_UID.getValue());
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
    when(preheat.getDataElement(DATA_ELEMENT_UID.getValue())).thenReturn(dataElementA);
    when(preheat.getDataElement(OPTION_SET_DATA_ELEMENT_UID.getValue()))
        .thenReturn(optionSetDataElement);

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getRuleEngineAssignOverwrite()).thenReturn(false);
  }

  @Test
  void shouldFailWhenAssignedValueIsInvalidOptionAndDataValueIsValidOption() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            INVALID_OPTION_VALUE,
            OPTION_SET_DATA_ELEMENT_UID,
            eventWithOptionDataValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithOptionDataValue);

    Optional<DataValue> dataValue =
        findDataValueByUid(bundle, EVENT_UID, OPTION_SET_DATA_ELEMENT_UID);

    assertDataValueWasNotAssignedAndErrorIsPresent(VALID_OPTION_VALUE, dataValue, warning);
  }

  @Test
  void shouldAssignDataValueWhenAssignedValueIsInvalidOptionAndOverwriteIsTrue() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(settings.getRuleEngineAssignOverwrite()).thenReturn(true);
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            INVALID_OPTION_VALUE,
            OPTION_SET_DATA_ELEMENT_UID,
            eventWithOptionDataValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithOptionDataValue);

    Optional<DataValue> dataValue =
        findDataValueByUid(bundle, EVENT_UID, OPTION_SET_DATA_ELEMENT_UID);

    assertDataValueWasAssignedAndWarningIsPresent(INVALID_OPTION_VALUE, dataValue, warning);
  }

  @Test
  void shouldAssignDataValueValueForEventsWhenDataValueIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    Event eventWithDataValueNOTSet = getEventWithDataValueNOTSet();
    List<Event> events = List.of(eventWithDataValueNOTSet);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_UID,
            eventWithDataValueNOTSet.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithDataValueNOTSet);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, SECOND_EVENT_UID, DATA_ELEMENT_UID);

    assertDataValueWasAssignedAndWarningIsPresent(DATAELEMENT_NEW_VALUE, dataValue, warning);
  }

  @Test
  void shouldNotAssignDataValueValueForEventsWhenDataValueIsAlreadyPresent() {
    Event eventWithDataValueSet = getEventWithDataValueSet();
    List<Event> events = List.of(eventWithDataValueSet);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_UID,
            eventWithDataValueSet.getDataValues());

    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, eventWithDataValueSet);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, EVENT_UID, DATA_ELEMENT_UID);

    assertDataValueWasNotAssignedAndErrorIsPresent(DATAELEMENT_OLD_VALUE, dataValue, error);
  }

  @Test
  void shouldNotAssignDataValueValueForEventsWhenDataValueIsAlreadyPresentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getDataElement(DATA_ELEMENT_UID.getValue())).thenReturn(dataElementA);
    Event eventWithDataValueSet = getEventWithDataValueSet(idSchemes);
    List<Event> events = List.of(eventWithDataValueSet);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_UID,
            eventWithDataValueSet.getDataValues());

    Optional<ProgramRuleIssue> error = executor.executeRuleAction(bundle, eventWithDataValueSet);

    Optional<DataValue> dataValue = findDataValueByCode(bundle, EVENT_UID, DATA_ELEMENT_CODE);

    assertDataValueWasNotAssignedAndErrorIsPresent(DATAELEMENT_OLD_VALUE, dataValue, error);
  }

  @Test
  void shouldAssignDataValueValueForEventsWhenDataValueIsAlreadyPresentAndHasTheSameValue() {
    Event eventWithDataValueSetSameValue = getEventWithDataValueSetSameValue();
    List<Event> events = List.of(eventWithDataValueSetSameValue);
    bundle.setEvents(events);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_UID,
            eventWithDataValueSetSameValue.getDataValues());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, eventWithDataValueSetSameValue);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, EVENT_UID, DATA_ELEMENT_UID);

    assertDataValueWasAssignedAndWarningIsPresent(DATAELEMENT_NEW_VALUE, dataValue, warning);
  }

  @Test
  void
      shouldAssignDataValueValueForEventsWhenDataValueIsAlreadyPresentAndSystemSettingToOverwriteIsTrue() {
    Event eventWithDataValueSet = getEventWithDataValueSet();
    List<Event> events = List.of(eventWithDataValueSet);
    bundle.setEvents(events);
    when(settings.getRuleEngineAssignOverwrite()).thenReturn(true);

    AssignDataValueExecutor executor =
        new AssignDataValueExecutor(
            settingsProvider,
            RULE_UID,
            DATAELEMENT_NEW_VALUE,
            DATA_ELEMENT_UID,
            eventWithDataValueSet.getDataValues());

    Optional<ProgramRuleIssue> warning = executor.executeRuleAction(bundle, eventWithDataValueSet);

    Optional<DataValue> dataValue = findDataValueByUid(bundle, EVENT_UID, DATA_ELEMENT_UID);

    assertDataValueWasAssignedAndWarningIsPresent(DATAELEMENT_NEW_VALUE, dataValue, warning);
  }

  private Optional<DataValue> findDataValueByUid(
      TrackerBundle bundle, UID eventUid, UID dataValueUid) {
    Event event = bundle.findEventByUid(eventUid).get();
    return event.getDataValues().stream()
        .filter(dv -> dv.getDataElement().equals(MetadataIdentifier.ofUid(dataValueUid.getValue())))
        .findAny();
  }

  private Optional<DataValue> findDataValueByCode(
      TrackerBundle bundle, UID eventUid, String dataValueCode) {
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
    return TrackerEvent.builder()
        .event(EVENT_UID)
        .status(EventStatus.ACTIVE)
        .dataValues(getDataValues())
        .build();
  }

  private Event getEventWithDataValueSet(TrackerIdSchemeParams idSchemes) {
    return TrackerEvent.builder()
        .event(EVENT_UID)
        .status(EventStatus.ACTIVE)
        .dataValues(getDataValues(idSchemes))
        .build();
  }

  private Event getEventWithDataValueSetSameValue() {
    return TrackerEvent.builder()
        .event(EVENT_UID)
        .status(EventStatus.ACTIVE)
        .dataValues(getDataValuesSameValue())
        .build();
  }

  private Event getEventWithOptionSetDataValueWithValidValue() {
    return TrackerEvent.builder()
        .event(EVENT_UID)
        .status(EventStatus.ACTIVE)
        .dataValues(getOptionSetDataValues())
        .build();
  }

  private Event getEventWithDataValueNOTSet() {
    return TrackerEvent.builder().event(SECOND_EVENT_UID).status(EventStatus.COMPLETED).build();
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
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID.getValue()))
            .value(DATAELEMENT_OLD_VALUE)
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getDataValuesSameValue() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID.getValue()))
            .value(DATAELEMENT_NEW_VALUE)
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getOptionSetDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid(OPTION_SET_DATA_ELEMENT_UID.getValue()))
            .value(VALID_OPTION_VALUE)
            .build();
    return Set.of(dataValue);
  }
}
