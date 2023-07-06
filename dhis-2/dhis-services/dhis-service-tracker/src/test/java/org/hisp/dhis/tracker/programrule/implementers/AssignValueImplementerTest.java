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
package org.hisp.dhis.tracker.programrule.implementers;

import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.rules.models.AttributeType.TRACKED_ENTITY_ATTRIBUTE;
import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AssignValueImplementerTest extends DhisConvenienceTest {

  private static final String TRACKED_ENTITY_ID = "TrackedEntityUid";

  private static final String FIRST_ENROLLMENT_ID = "ActiveEnrollmentUid";

  private static final String SECOND_ENROLLMENT_ID = "CompletedEnrollmentUid";

  private static final String FIRST_EVENT_ID = "EventUid";

  private static final String SECOND_EVENT_ID = "CompletedEventUid";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String DATA_ELEMENT_CODE = "DataElementCode";

  private static final String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

  private static final String ATTRIBUTE_ID = "AttributeId";

  private static final String ATTRIBUTE_CODE = "AttributeCode";

  private static final String DATA_ELEMENT_OLD_VALUE = "1";

  private static final String DATA_ELEMENT_NEW_VALUE_PAYLOAD = "23";

  private static final String DATA_ELEMENT_NEW_VALUE = "23.0";

  private static final String TEI_ATTRIBUTE_OLD_VALUE = "10.0";

  private static final String TEI_ATTRIBUTE_NEW_VALUE = "24.0";

  private static final String OPTION_SET_DATA_ELEMENT_ID = "OptionSetDataElementId";

  private static final String VALID_OPTION_VALUE = "10";

  private static final String INVALID_OPTION_VALUE = "0";

  private static ProgramStage firstProgramStage;

  private static ProgramStage secondProgramStage;

  private static DataElement dataElementA;

  private static DataElement dataElementB;

  private static TrackedEntityAttribute attributeA;

  private static DataElement optionSetDataElement;

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private SystemSettingManager systemSettingManager;

  @InjectMocks private AssignValueImplementer implementerToTest;

  @BeforeEach
  void setUpTest() {
    firstProgramStage = createProgramStage('A', 0);
    firstProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    attributeA = createTrackedEntityAttribute('A');
    attributeA.setUid(ATTRIBUTE_ID);
    attributeA.setCode(ATTRIBUTE_CODE);
    attributeA.setValueType(ValueType.NUMBER);
    dataElementA = createDataElement('A');
    dataElementA.setUid(DATA_ELEMENT_ID);
    dataElementA.setCode(DATA_ELEMENT_CODE);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(firstProgramStage, dataElementA, 0);
    firstProgramStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElementA));
    secondProgramStage = createProgramStage('B', 0);
    secondProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementB = createDataElement('B');
    dataElementB.setUid(ANOTHER_DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementB =
        createProgramStageDataElement(secondProgramStage, dataElementB, 0);
    secondProgramStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElementB));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(firstProgramStage)))
        .thenReturn(firstProgramStage);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(secondProgramStage)))
        .thenReturn(secondProgramStage);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(dataElementA.getUid())))
        .thenReturn(dataElementA);
    when(preheat.getTrackedEntityAttribute(attributeA.getUid())).thenReturn(attributeA);
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(attributeA.getUid())))
        .thenReturn(attributeA);
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
    when(preheat.get(ProgramStage.class, firstProgramStage.getUid())).thenReturn(firstProgramStage);
    when(preheat.get(ProgramStage.class, secondProgramStage.getUid()))
        .thenReturn(secondProgramStage);
    when(preheat.get(DataElement.class, dataElementA.getUid())).thenReturn(dataElementA);
    when(preheat.get(DataElement.class, optionSetDataElement.getUid()))
        .thenReturn(optionSetDataElement);
    when(preheat.get(TrackedEntityAttribute.class, attributeA.getUid())).thenReturn(attributeA);
    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.FALSE);
  }

  @Test
  void testAssignDataElementValueForEventsWhenDataElementIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(dataElementA.getUid())).thenReturn(dataElementA);
    Event eventWithDataValueNOTSet = getEventWithDataValueNOTSet();
    List<Event> events = Lists.newArrayList(eventWithDataValueNOTSet);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(bundle, getRuleEventEffects(), eventWithDataValueNOTSet);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(SECOND_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElementA))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(DATA_ELEMENT_NEW_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(1, eventIssues.size());
    assertEquals(WARNING, eventIssues.get(0).getIssueType());
  }

  @Test
  void testAssignDataElementValueForEventsWhenDataElementIsEmptyAndFromDifferentProgramStage() {
    Event eventWithDataValueNOTSetInDifferentProgramStage =
        getEventWithDataValueNOTSetInDifferentProgramStage();
    List<Event> events = Lists.newArrayList(eventWithDataValueNOTSetInDifferentProgramStage);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), eventWithDataValueNOTSetInDifferentProgramStage);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(SECOND_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElementA))
            .findAny();
    assertTrue(newDataValue.isEmpty());
    assertTrue(eventIssues.isEmpty());
  }

  @Test
  void testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresent() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(dataElementA.getUid())).thenReturn(dataElementA);
    Event eventWithDataValueSet = getEventWithDataValueSet();
    List<Event> events = Lists.newArrayList(eventWithDataValueSet);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(bundle, getRuleEventEffects(), eventWithDataValueSet);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElementA))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(DATA_ELEMENT_OLD_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(1, eventIssues.size());
    assertEquals(ERROR, eventIssues.get(0).getIssueType());
    assertEquals(TrackerErrorCode.E1307, eventIssues.get(0).getIssueCode());
  }

  @Test
  void testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getDataElement(dataElementA.getUid())).thenReturn(dataElementA);

    Event eventWithDataValueSet = getEventWithDataValueSet(idSchemes);
    List<Event> events = Lists.newArrayList(eventWithDataValueSet);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(bundle, getRuleEventEffects(), eventWithDataValueSet);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElementA))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(DATA_ELEMENT_OLD_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(1, eventIssues.size());
    assertEquals(TrackerErrorCode.E1307, eventIssues.get(0).getIssueCode());
    assertEquals(ERROR, eventIssues.get(0).getIssueType());
  }

  @Test
  void testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresentAndHasSameValue() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(dataElementA.getUid())).thenReturn(dataElementA);
    Event eventWithDataValueSetSameValue = getEventWithDataValueSetSameValue();
    List<Event> events = Lists.newArrayList(eventWithDataValueSetSameValue);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), eventWithDataValueSetSameValue);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElementA))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(DATA_ELEMENT_NEW_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(1, eventIssues.size());
    assertEquals(WARNING, eventIssues.get(0).getIssueType());
  }

  @Test
  void
      testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresentAndSystemSettingToOverwriteIsTrue() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(dataElementA.getUid())).thenReturn(dataElementA);
    Event eventWithDataValueSet = getEventWithDataValueSet();
    List<Event> events = Lists.newArrayList(eventWithDataValueSet);
    bundle.setEvents(events);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(bundle, getRuleEventEffects(), eventWithDataValueSet);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElementA))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(DATA_ELEMENT_NEW_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(1, eventIssues.size());
    assertEquals(WARNING, eventIssues.get(0).getIssueType());
  }

  @Test
  void shouldFailWhenAssignedValueIsInvalidOptionAndDataValueIsValidOption() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(optionSetDataElement.getUid())).thenReturn(optionSetDataElement);
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(
            bundle, getInvalidOptionSetRuleEventEffects(), eventWithOptionDataValue);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(optionSetDataElement))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(VALID_OPTION_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(ERROR, eventIssues.get(0).getIssueType());
  }

  @Test
  void shouldAssignDataValueWhenAssignedValueIsValidOptionAndDataValueIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(optionSetDataElement.getUid())).thenReturn(optionSetDataElement);
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueNOTSet();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(
            bundle, getValidOptionSetRuleEventEffects(), eventWithOptionDataValue);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(SECOND_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(optionSetDataElement))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertEquals(VALID_OPTION_VALUE, newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(WARNING, eventIssues.get(0).getIssueType());
  }

  @Test
  void shouldAssignDataValueWhenAssignedValueIsInvalidOptionAndDataValueIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(optionSetDataElement.getUid())).thenReturn(optionSetDataElement);
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueNOTSet();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(
            bundle, getInvalidOptionSetRuleEventEffects(), eventWithOptionDataValue);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(SECOND_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(optionSetDataElement))
            .findAny();
    assertTrue(newDataValue.isEmpty());
    assertEquals(1, eventIssues.size());
    assertEquals(WARNING, eventIssues.get(0).getIssueType());
  }

  @Test
  void shouldAssignNullDataValueWhenAssignedValueIsInvalidOptionAndOverwriteIsTrue() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(optionSetDataElement.getUid())).thenReturn(optionSetDataElement);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);
    Event eventWithOptionDataValue = getEventWithOptionSetDataValueWithValidValue();
    List<Event> events = List.of(eventWithOptionDataValue);
    bundle.setEvents(events);

    List<ProgramRuleIssue> eventIssues =
        implementerToTest.validateEvent(
            bundle, getInvalidOptionSetRuleEventEffects(), eventWithOptionDataValue);

    Event event =
        bundle.getEvents().stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT_ID))
            .findAny()
            .get();
    Optional<DataValue> newDataValue =
        event.getDataValues().stream()
            .filter(dv -> dv.getDataElement().isEqualTo(optionSetDataElement))
            .findAny();
    assertTrue(newDataValue.isPresent());
    assertNull(newDataValue.get().getValue());
    assertEquals(1, eventIssues.size());
    assertEquals(WARNING, eventIssues.get(0).getIssueType());
  }

  @Test
  void testAssignAttributeValueForEnrollmentsWhenAttributeIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    List<TrackedEntity> trackedEntities =
        Lists.newArrayList(getTrackedEntitiesWithAttributeNOTSet());
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeNOTSet);
    bundle.setTrackedEntities(trackedEntities);
    bundle.setEnrollments(enrollments);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeNOTSet);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(SECOND_ENROLLMENT_ID))
            .findAny()
            .get();
    Optional<Attribute> attribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    assertTrue(attribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(WARNING, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresent() {
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeSet);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(FIRST_ENROLLMENT_ID))
            .findAny()
            .get();
    Optional<Attribute> attribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    assertTrue(attribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_OLD_VALUE, attribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(ERROR, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentUsingIdSchemeCode() {

    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attributeA);
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet(idSchemes);
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeSet);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(FIRST_ENROLLMENT_ID))
            .findAny()
            .get();
    Optional<Attribute> attribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofCode(ATTRIBUTE_CODE)))
            .findAny();
    assertTrue(attribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_OLD_VALUE, attribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(ERROR, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTei() {
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeNOTSet);
    List<TrackedEntity> trackedEntities = Lists.newArrayList(getTrackedEntitiesWithAttributeSet());
    bundle.setEnrollments(enrollments);
    bundle.setTrackedEntities(trackedEntities);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeNOTSet);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(SECOND_ENROLLMENT_ID))
            .findAny()
            .get();
    TrackedEntity trackedEntity =
        bundle.getTrackedEntities().stream()
            .filter(e -> e.getTrackedEntity().equals(TRACKED_ENTITY_ID))
            .findAny()
            .get();
    Optional<Attribute> enrollmentAttribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    Optional<Attribute> teiAttribute =
        trackedEntity.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    assertFalse(enrollmentAttribute.isPresent());
    assertTrue(teiAttribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_OLD_VALUE, teiAttribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(ERROR, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void
      testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTeiAndCanBeOverwritten() {
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeNOTSet);
    List<TrackedEntity> trackedEntities = Lists.newArrayList(getTrackedEntitiesWithAttributeSet());
    bundle.setEnrollments(enrollments);
    bundle.setTrackedEntities(trackedEntities);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeNOTSet);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(SECOND_ENROLLMENT_ID))
            .findAny()
            .get();
    TrackedEntity trackedEntity =
        bundle.getTrackedEntities().stream()
            .filter(e -> e.getTrackedEntity().equals(TRACKED_ENTITY_ID))
            .findAny()
            .get();
    Optional<Attribute> enrollmentAttribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    Optional<Attribute> teiAttribute =
        trackedEntity.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    assertFalse(enrollmentAttribute.isPresent());
    assertTrue(teiAttribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_NEW_VALUE, teiAttribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(WARNING, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndHasTheSameValue() {
    Enrollment enrollmentWithAttributeSetSameValue = getEnrollmentWithAttributeSetSameValue();
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeSetSameValue);
    bundle.setEnrollments(enrollments);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeSetSameValue);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(FIRST_ENROLLMENT_ID))
            .findAny()
            .get();
    Optional<Attribute> attribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    assertTrue(attribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(WARNING, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void
      testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndSystemSettingToOverwriteIsTrue() {
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
    List<Enrollment> enrollments = Lists.newArrayList(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);

    List<ProgramRuleIssue> enrollmentIssues =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), enrollmentWithAttributeSet);

    Enrollment enrollment =
        bundle.getEnrollments().stream()
            .filter(e -> e.getEnrollment().equals(FIRST_ENROLLMENT_ID))
            .findAny()
            .get();
    Optional<Attribute> attribute =
        enrollment.getAttributes().stream()
            .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(ATTRIBUTE_ID)))
            .findAny();
    assertTrue(attribute.isPresent());
    assertEquals(TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(1, enrollmentIssues.size());
    assertEquals(WARNING, enrollmentIssues.get(0).getIssueType());
  }

  @Test
  void testIsEqual() {
    assertTrue(implementerToTest.isEqual("first_dose", "first_dose", ValueType.TEXT));
    assertTrue(implementerToTest.isEqual("2020-01-01", "2020-01-01", ValueType.DATE));
    assertTrue(implementerToTest.isEqual("true", "true", ValueType.BOOLEAN));
    assertTrue(implementerToTest.isEqual("26.4", "26.4", ValueType.TEXT));
    assertTrue(implementerToTest.isEqual("24.8", "24.8", ValueType.NUMBER));
    assertTrue(implementerToTest.isEqual("32", "32", ValueType.INTEGER));

    assertFalse(implementerToTest.isEqual("first_dose", "second_dose", ValueType.TEXT));
    assertFalse(implementerToTest.isEqual("2020-01-01", "2020-01-02", ValueType.DATE));
    assertFalse(implementerToTest.isEqual("true", "false", ValueType.BOOLEAN));
    assertFalse(implementerToTest.isEqual("26.4", "26.5", ValueType.TEXT));
    assertFalse(implementerToTest.isEqual("24.8", "24.9", ValueType.NUMBER));
    assertFalse(implementerToTest.isEqual("32", "33", ValueType.INTEGER));
  }

  @Test
  void testIsEqualDataTypeIntegrity() {
    assertFalse(implementerToTest.isEqual("first_dose", "46.2", ValueType.NUMBER));
    assertFalse(implementerToTest.isEqual("24", "second_dose", ValueType.NUMBER));
    assertFalse(implementerToTest.isEqual(null, "46.2", ValueType.NUMBER));
    assertFalse(implementerToTest.isEqual("26.4", null, ValueType.NUMBER));
    assertFalse(implementerToTest.isEqual("first_dose", null, ValueType.TEXT));
    assertFalse(implementerToTest.isEqual(null, "second_dose", ValueType.TEXT));
  }

  private Event getEventWithOptionSetDataValueWithValidValue() {
    return Event.builder()
        .event(FIRST_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(secondProgramStage))
        .dataValues(getOptionSetDataValues())
        .build();
  }

  private Set<DataValue> getOptionSetDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid(OPTION_SET_DATA_ELEMENT_ID))
            .value(VALID_OPTION_VALUE)
            .build();
    return Sets.newHashSet(dataValue);
  }

  private Event getEventWithDataValueSet() {
    Event event = new Event();
    event.setEvent(FIRST_EVENT_ID);
    event.setStatus(EventStatus.ACTIVE);
    event.setProgramStage(MetadataIdentifier.ofUid(firstProgramStage));
    event.setDataValues(getEventDataValues());
    return event;
  }

  private Event getEventWithDataValueSet(TrackerIdSchemeParams idSchemes) {

    return Event.builder()
        .event(FIRST_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(idSchemes.toMetadataIdentifier(firstProgramStage))
        .dataValues(getEventDataValues(idSchemes))
        .build();
  }

  private Event getEventWithDataValueSetSameValue() {
    Event event = new Event();
    event.setEvent(FIRST_EVENT_ID);
    event.setStatus(EventStatus.ACTIVE);
    event.setProgramStage(MetadataIdentifier.ofUid(firstProgramStage));
    event.setDataValues(getEventDataValuesSameValue());
    return event;
  }

  private Event getEventWithDataValueNOTSet() {
    Event event = new Event();
    event.setEvent(SECOND_EVENT_ID);
    event.setStatus(EventStatus.ACTIVE);
    event.setProgramStage(MetadataIdentifier.ofUid(firstProgramStage));
    return event;
  }

  private Event getEventWithOptionSetDataValueNOTSet() {
    Event event = new Event();
    event.setEvent(SECOND_EVENT_ID);
    event.setStatus(EventStatus.ACTIVE);
    event.setProgramStage(MetadataIdentifier.ofUid(secondProgramStage));
    return event;
  }

  private Event getEventWithDataValueNOTSetInDifferentProgramStage() {
    return Event.builder()
        .event(SECOND_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(secondProgramStage))
        .build();
  }

  private Set<DataValue> getEventDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_OLD_VALUE)
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .build();
    return Sets.newHashSet(dataValue);
  }

  private Set<DataValue> getEventDataValues(TrackerIdSchemeParams idSchemes) {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_OLD_VALUE)
            .dataElement(idSchemes.toMetadataIdentifier(dataElementA))
            .build();
    return Sets.newHashSet(dataValue);
  }

  private Set<DataValue> getEventDataValuesSameValue() {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_NEW_VALUE_PAYLOAD)
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .build();
    return Sets.newHashSet(dataValue);
  }

  private Enrollment getEnrollmentWithAttributeSet() {
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(FIRST_ENROLLMENT_ID);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setAttributes(getAttributes());
    return enrollment;
  }

  private Enrollment getEnrollmentWithAttributeSet(TrackerIdSchemeParams idSchemes) {
    return Enrollment.builder()
        .enrollment(FIRST_ENROLLMENT_ID)
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes(idSchemes))
        .build();
  }

  private Enrollment getEnrollmentWithAttributeSetSameValue() {
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(FIRST_ENROLLMENT_ID);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setAttributes(getAttributesSameValue());
    return enrollment;
  }

  private TrackedEntity getTrackedEntitiesWithAttributeSet() {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setTrackedEntity(TRACKED_ENTITY_ID);
    trackedEntity.setAttributes(getAttributes());
    return trackedEntity;
  }

  private TrackedEntity getTrackedEntitiesWithAttributeNOTSet() {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setTrackedEntity(TRACKED_ENTITY_ID);
    return trackedEntity;
  }

  private Enrollment getEnrollmentWithAttributeNOTSet() {
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(SECOND_ENROLLMENT_ID);
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    enrollment.setTrackedEntity(TRACKED_ENTITY_ID);
    return enrollment;
  }

  private List<Attribute> getAttributes(TrackerIdSchemeParams idSchemes) {
    Attribute attribute =
        Attribute.builder()
            .attribute(idSchemes.toMetadataIdentifier(attributeA))
            .value(TEI_ATTRIBUTE_OLD_VALUE)
            .build();
    return Lists.newArrayList(attribute);
  }

  private List<Attribute> getAttributes() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_ID))
            .value(TEI_ATTRIBUTE_OLD_VALUE)
            .build();
    return Lists.newArrayList(attribute);
  }

  private List<Attribute> getAttributesSameValue() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_ID))
            .value(TEI_ATTRIBUTE_NEW_VALUE)
            .build();
    return Lists.newArrayList(attribute);
  }

  private List<RuleEffect> getInvalidOptionSetRuleEventEffects() {
    RuleAction actionAssign =
        RuleActionAssign.create(
            null, INVALID_OPTION_VALUE, optionSetDataElement.getUid(), DATA_ELEMENT);
    return Lists.newArrayList(RuleEffect.create("", actionAssign, INVALID_OPTION_VALUE));
  }

  private List<RuleEffect> getValidOptionSetRuleEventEffects() {
    RuleAction actionAssign =
        RuleActionAssign.create(
            null, VALID_OPTION_VALUE, optionSetDataElement.getUid(), DATA_ELEMENT);
    return Lists.newArrayList(RuleEffect.create("", actionAssign, VALID_OPTION_VALUE));
  }

  private List<RuleEffect> getRuleEventEffects() {
    RuleAction actionAssign =
        RuleActionAssign.create(null, DATA_ELEMENT_NEW_VALUE, dataElementA.getUid(), DATA_ELEMENT);
    return Lists.newArrayList(RuleEffect.create("", actionAssign, DATA_ELEMENT_NEW_VALUE));
  }

  private List<RuleEffect> getRuleEnrollmentEffects() {
    RuleAction actionAssign =
        RuleActionAssign.create(
            null, TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, TRACKED_ENTITY_ATTRIBUTE);
    return Lists.newArrayList(RuleEffect.create("", actionAssign, TEI_ATTRIBUTE_NEW_VALUE));
  }
}
