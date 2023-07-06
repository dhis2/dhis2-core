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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleEffect;
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
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetMandatoryFieldValidatorTest extends DhisConvenienceTest {

  private static final String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

  private static final String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

  private static final String FIRST_EVENT_ID = "EventUid";

  private static final String SECOND_EVENT_ID = "CompletedEventUid";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String ATTRIBUTE_ID = "AttributeId";

  private static final String ATTRIBUTE_CODE = "AttributeCode";

  private static final String TEI_ID = "TeiId";

  private static final String DATA_ELEMENT_VALUE = "1.0";

  private static final String ATTRIBUTE_VALUE = "23.0";

  private static ProgramStage firstProgramStage;

  private static ProgramStage secondProgramStage;

  private static DataElement dataElementA;

  private static DataElement dataElementB;

  private TrackedEntityAttribute attribute;

  private final SetMandatoryFieldValidator implementerToTest = new SetMandatoryFieldValidator();

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUpTest() {
    firstProgramStage = createProgramStage('A', 0);
    firstProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementA = createDataElement('A');
    dataElementA.setUid(DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(firstProgramStage, dataElementA, 0);
    firstProgramStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElementA));
    secondProgramStage = createProgramStage('B', 0);
    secondProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementB = createDataElement('B');
    ProgramStageDataElement programStageDataElementB =
        createProgramStageDataElement(secondProgramStage, dataElementB, 0);
    secondProgramStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElementB));

    attribute = createTrackedEntityAttribute('A');
    attribute.setUid(ATTRIBUTE_ID);
    attribute.setCode(ATTRIBUTE_CODE);

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
  }

  @Test
  void testValidateOkMandatoryFieldsForEvents() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElementA);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(firstProgramStage)))
        .thenReturn(firstProgramStage);
    bundle.setEvents(Lists.newArrayList(getEventWithMandatoryValueSet()));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), getEventWithMandatoryValueSet());

    assertTrue(errors.isEmpty());
  }

  @Test
  void testValidateOkMandatoryFieldsForEventsUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElementA);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(firstProgramStage)))
        .thenReturn(firstProgramStage);
    bundle.setEvents(Lists.newArrayList(getEventWithMandatoryValueSet(idSchemes)));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), getEventWithMandatoryValueSet(idSchemes));

    assertTrue(errors.isEmpty());
  }

  @Test
  void testValidateWithErrorMandatoryFieldsForEvents() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElementA);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(firstProgramStage)))
        .thenReturn(firstProgramStage);
    bundle.setEvents(
        Lists.newArrayList(getEventWithMandatoryValueSet(), getEventWithMandatoryValueNOTSet()));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), getEventWithMandatoryValueSet());
    assertTrue(errors.isEmpty());

    errors =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), getEventWithMandatoryValueNOTSet());

    assertFalse(errors.isEmpty());
    errors.forEach(
        e -> {
          assertEquals("RULE_DATA_VALUE", e.getRuleUid());
          assertEquals(TrackerErrorCode.E1301, e.getIssueCode());
          assertEquals(IssueType.ERROR, e.getIssueType());
          assertEquals(Lists.newArrayList(dataElementA.getUid()), e.getArgs());
        });
  }

  @Test
  void testValidateOkMandatoryFieldsForValidEventAndNotValidEventInDifferentProgramStage() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElementA);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(firstProgramStage)))
        .thenReturn(firstProgramStage);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(secondProgramStage)))
        .thenReturn(secondProgramStage);
    bundle.setEvents(
        Lists.newArrayList(
            getEventWithMandatoryValueSet(),
            getEventWithMandatoryValueNOTSetInDifferentProgramStage()));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEvent(
            bundle, getRuleEventEffects(), getEventWithMandatoryValueSet());

    assertTrue(errors.isEmpty());

    errors =
        implementerToTest.validateEvent(
            bundle,
            getRuleEventEffects(),
            getEventWithMandatoryValueNOTSetInDifferentProgramStage());

    assertTrue(errors.isEmpty());
  }

  @Test
  void testValidateOkMandatoryFieldsForEnrollment() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attribute);
    bundle.setEnrollments(Lists.newArrayList(getEnrollmentWithMandatoryAttributeSet()));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), getEnrollmentWithMandatoryAttributeSet());

    assertTrue(errors.isEmpty());
  }

  @Test
  void testValidateOkMandatoryFieldsForEnrollmentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attribute);
    bundle.setEnrollments(Lists.newArrayList(getEnrollmentWithMandatoryAttributeSet(idSchemes)));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), getEnrollmentWithMandatoryAttributeSet(idSchemes));

    assertTrue(errors.isEmpty());
  }

  @Test
  void testValidateWithErrorMandatoryFieldsForEnrollments() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attribute);
    bundle.setEnrollments(
        Lists.newArrayList(
            getEnrollmentWithMandatoryAttributeSet(), getEnrollmentWithMandatoryAttributeNOTSet()));

    List<ProgramRuleIssue> errors =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), getEnrollmentWithMandatoryAttributeSet());

    assertTrue(errors.isEmpty());

    errors =
        implementerToTest.validateEnrollment(
            bundle, getRuleEnrollmentEffects(), getEnrollmentWithMandatoryAttributeNOTSet());

    errors.forEach(
        e -> {
          assertEquals("RULE_ATTRIBUTE", e.getRuleUid());
          assertEquals(TrackerErrorCode.E1306, e.getIssueCode());
          assertEquals(IssueType.ERROR, e.getIssueType());
          assertEquals(Lists.newArrayList(ATTRIBUTE_ID), e.getArgs());
        });
  }

  private Event getEventWithMandatoryValueSet(TrackerIdSchemeParams idSchemes) {
    return Event.builder()
        .event(FIRST_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(idSchemes.toMetadataIdentifier(firstProgramStage))
        .dataValues(getActiveEventDataValues(idSchemes))
        .build();
  }

  private Event getEventWithMandatoryValueSet() {
    return Event.builder()
        .event(FIRST_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(firstProgramStage))
        .dataValues(getActiveEventDataValues())
        .build();
  }

  private Event getEventWithMandatoryValueNOTSet() {
    Event event = new Event();
    event.setEvent(SECOND_EVENT_ID);
    event.setStatus(EventStatus.ACTIVE);
    event.setProgramStage(MetadataIdentifier.ofUid(firstProgramStage));
    return event;
  }

  private Event getEventWithMandatoryValueNOTSetInDifferentProgramStage() {
    Event event = new Event();
    event.setEvent(SECOND_EVENT_ID);
    event.setStatus(EventStatus.ACTIVE);
    event.setProgramStage(MetadataIdentifier.ofUid(secondProgramStage));
    return event;
  }

  private Set<DataValue> getActiveEventDataValues(TrackerIdSchemeParams idSchemes) {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_VALUE)
            .dataElement(idSchemes.toMetadataIdentifier(dataElementA))
            .build();
    return Sets.newHashSet(dataValue);
  }

  private Set<DataValue> getActiveEventDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_VALUE)
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .build();
    return Sets.newHashSet(dataValue);
  }

  private Enrollment getEnrollmentWithMandatoryAttributeSet(TrackerIdSchemeParams idSchemes) {
    return Enrollment.builder()
        .enrollment(ACTIVE_ENROLLMENT_ID)
        .trackedEntity(TEI_ID)
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes(idSchemes))
        .build();
  }

  private Enrollment getEnrollmentWithMandatoryAttributeSet() {
    return Enrollment.builder()
        .enrollment(ACTIVE_ENROLLMENT_ID)
        .trackedEntity(TEI_ID)
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes())
        .build();
  }

  private Enrollment getEnrollmentWithMandatoryAttributeNOTSet() {
    return Enrollment.builder()
        .enrollment(COMPLETED_ENROLLMENT_ID)
        .trackedEntity(TEI_ID)
        .status(EnrollmentStatus.COMPLETED)
        .build();
  }

  private List<Attribute> getAttributes(TrackerIdSchemeParams idSchemes) {
    return Lists.newArrayList(getAttribute(idSchemes));
  }

  private List<Attribute> getAttributes() {
    return Lists.newArrayList(getAttribute());
  }

  private Attribute getAttribute(TrackerIdSchemeParams idSchemes) {
    return Attribute.builder()
        .attribute(idSchemes.toMetadataIdentifier(attribute))
        .value(ATTRIBUTE_VALUE)
        .build();
  }

  private Attribute getAttribute() {
    return Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_ID))
        .value(ATTRIBUTE_VALUE)
        .build();
  }

  private List<RuleEffect> getRuleEventEffects() {
    RuleAction ruleActionSetMandatoryDataValue =
        RuleActionSetMandatoryField.create(DATA_ELEMENT_ID, DATA_ELEMENT);
    return Lists.newArrayList(
        RuleEffect.create("RULE_DATA_VALUE", ruleActionSetMandatoryDataValue));
  }

  private List<RuleEffect> getRuleEnrollmentEffects() {
    RuleAction ruleActionSetMandatoryAttribute =
        RuleActionSetMandatoryField.create(ATTRIBUTE_ID, TRACKED_ENTITY_ATTRIBUTE);
    return Lists.newArrayList(RuleEffect.create("RULE_ATTRIBUTE", ruleActionSetMandatoryAttribute));
  }
}
