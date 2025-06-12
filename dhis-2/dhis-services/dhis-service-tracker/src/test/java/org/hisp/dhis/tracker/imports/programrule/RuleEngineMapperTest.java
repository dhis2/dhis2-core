/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import kotlinx.datetime.Instant;
import kotlinx.datetime.LocalDate;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleEngineRuleEngineMapperTest extends TestBase {
  private static final String SAMPLE_VALUE_A = "textValueA";

  private static final Date NOW = new Date();
  private static final Date TOMORROW = DateUtils.addDays(NOW, 1);
  private static final Date YESTERDAY = DateUtils.addDays(NOW, -1);
  private static final Date AFTER_TOMORROW = DateUtils.addDays(NOW, 2);

  private DataElement dataElement;

  private Program program;

  private ProgramStage programStage;

  private OrganisationUnit organisationUnit;

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttribute numericTrackedEntityAttribute;

  private TrackedEntityAttribute booleanTrackedEntityAttribute;

  @BeforeEach
  void setUp() {

    program = createProgram('P');
    programStage = createProgramStage('S', program);
    organisationUnit = createOrganisationUnit('A');

    dataElement = createDataElement('D');
    dataElement.setValueType(ValueType.TEXT);

    trackedEntityAttribute = createTrackedEntityAttribute('Z');
    numericTrackedEntityAttribute = createTrackedEntityAttribute('X', ValueType.INTEGER);
    booleanTrackedEntityAttribute = createTrackedEntityAttribute('Y', ValueType.BOOLEAN);
  }

  @Test
  void shouldMapEventsToRuleEventWhenOrganisationUnitCodeIsNull() {
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    orgUnit.setCode(null);
    Event event = dbEvent();
    event.setOrganisationUnit(orgUnit);

    List<RuleEvent> ruleEvent = RuleEngineMapper.mapSavedEvents(List.of(event));

    assertEquals(1, ruleEvent.size());
    assertEvent(event, ruleEvent.get(0));
  }

  @Test
  void shouldMapPayloadEventsToRuleEvents() {
    org.hisp.dhis.tracker.imports.domain.Event eventA = payloadEvent();
    org.hisp.dhis.tracker.imports.domain.Event eventB = payloadEvent();

    TrackerPreheat trackerPreheat = new TrackerPreheat();
    trackerPreheat.put(programStage);
    trackerPreheat.put(TrackerIdSchemeParam.UID, organisationUnit);
    trackerPreheat.put(TrackerIdSchemeParam.UID, dataElement);

    List<RuleEvent> ruleEvents =
        RuleEngineMapper.mapPayloadEvents(trackerPreheat, List.of(eventA, eventB));

    assertEquals(2, ruleEvents.size());
    assertEvent(
        eventA,
        ruleEvents.stream()
            .filter(e -> e.getEvent().equals(eventA.getUid().getValue()))
            .findFirst()
            .get());
    assertEvent(
        eventB,
        ruleEvents.stream()
            .filter(e -> e.getEvent().equals(eventB.getUid().getValue()))
            .findFirst()
            .get());
  }

  @Test
  void shouldMapDBEventsToRuleEvents() {
    Event eventA = dbEvent();
    Event eventB = dbEvent();

    List<RuleEvent> ruleEvents = RuleEngineMapper.mapSavedEvents(List.of(eventA, eventB));

    assertEquals(2, ruleEvents.size());
    assertEvent(
        eventA,
        ruleEvents.stream().filter(e -> e.getEvent().equals(eventA.getUid())).findFirst().get());
    assertEvent(
        eventB,
        ruleEvents.stream().filter(e -> e.getEvent().equals(eventB.getUid())).findFirst().get());
  }

  @Test
  void shouldMapPayloadEnrollmentToRuleEnrollment() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment = payloadEnrollment();

    TrackerPreheat trackerPreheat = new TrackerPreheat();
    trackerPreheat.put(program);
    trackerPreheat.put(TrackerIdSchemeParam.UID, organisationUnit);
    trackerPreheat.put(TrackerIdSchemeParam.UID, dataElement);

    RuleAttributeValue trackedEntityAttributeValue =
        new RuleAttributeValue(trackedEntityAttribute.getUid(), SAMPLE_VALUE_A);

    RuleEnrollment ruleEnrollment =
        RuleEngineMapper.mapPayloadEnrollment(
            trackerPreheat, enrollment, List.of(trackedEntityAttributeValue));

    assertEnrollment(enrollment, ruleEnrollment);
  }

  @Test
  void shouldMapDBEnrollmentToRuleEnrollment() {
    Enrollment enrollment = dbEnrollment();
    RuleAttributeValue trackedEntityAttributeValue =
        new RuleAttributeValue(trackedEntityAttribute.getUid(), SAMPLE_VALUE_A);

    RuleEnrollment ruleEnrollment =
        RuleEngineMapper.mapSavedEnrollment(enrollment, List.of(trackedEntityAttributeValue));

    assertEnrollment(enrollment, ruleEnrollment);
  }

  @Test
  void shouldMapDBEnrollmentToRuleEnrollmentWhenOrganisationUnitCodeIsNull() {
    OrganisationUnit organisationUnitWithNullCode = createOrganisationUnit('A');
    organisationUnitWithNullCode.setCode(null);
    Enrollment enrollment = dbEnrollment();
    enrollment.setOrganisationUnit(organisationUnitWithNullCode);
    RuleAttributeValue trackedEntityAttributeValue =
        new RuleAttributeValue(trackedEntityAttribute.getUid(), SAMPLE_VALUE_A);

    RuleEnrollment ruleEnrollment =
        RuleEngineMapper.mapSavedEnrollment(enrollment, List.of(trackedEntityAttributeValue));

    assertEnrollment(enrollment, ruleEnrollment);
  }

  @Test
  void shouldMapAttributes() {
    Attribute textAttribute = attribute(trackedEntityAttribute, SAMPLE_VALUE_A);
    Attribute nullTextAttribute = attribute(trackedEntityAttribute, null);
    Attribute numericAttribute = attribute(numericTrackedEntityAttribute, "3");
    Attribute nullNumericAttribute = attribute(numericTrackedEntityAttribute, null);
    Attribute booleanAttribute = attribute(booleanTrackedEntityAttribute, "true");
    Attribute nullBooleanAttribute = attribute(booleanTrackedEntityAttribute, null);

    List<Attribute> attributes =
        List.of(
            textAttribute,
            nullTextAttribute,
            numericAttribute,
            nullNumericAttribute,
            booleanAttribute,
            nullBooleanAttribute);

    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(
        TrackerIdSchemeParam.UID,
        List.of(
            trackedEntityAttribute, numericTrackedEntityAttribute, booleanTrackedEntityAttribute));

    List<RuleAttributeValue> ruleAttributes = RuleEngineMapper.mapAttributes(preheat, attributes);

    assertContainsOnly(
        List.of(
            new RuleAttributeValue(trackedEntityAttribute.getUid(), SAMPLE_VALUE_A),
            new RuleAttributeValue(trackedEntityAttribute.getUid(), ""),
            new RuleAttributeValue(numericTrackedEntityAttribute.getUid(), "3"),
            new RuleAttributeValue(numericTrackedEntityAttribute.getUid(), "0"),
            new RuleAttributeValue(booleanTrackedEntityAttribute.getUid(), "true"),
            new RuleAttributeValue(booleanTrackedEntityAttribute.getUid(), "false")),
        ruleAttributes);
  }

  private void assertEvent(Event event, RuleEvent ruleEvent) {
    assertEquals(event.getUid(), ruleEvent.getEvent());
    assertEquals(event.getProgramStage().getUid(), ruleEvent.getProgramStage());
    assertEquals(event.getProgramStage().getName(), ruleEvent.getProgramStageName());
    assertEquals(event.getStatus().name(), ruleEvent.getStatus().name());
    assertDates(event.getOccurredDate(), ruleEvent.getEventDate());
    assertNull(ruleEvent.getCompletedDate());
    assertDates(event.getCreated(), ruleEvent.getCreatedDate());
    assertEquals(event.getOrganisationUnit().getUid(), ruleEvent.getOrganisationUnit());
    assertEquals(event.getOrganisationUnit().getCode(), ruleEvent.getOrganisationUnitCode());
    assertDataValue(event.getEventDataValues().iterator().next(), ruleEvent.getDataValues().get(0));
  }

  private void assertEvent(org.hisp.dhis.tracker.imports.domain.Event event, RuleEvent ruleEvent) {
    assertEquals(event.getUid().getValue(), ruleEvent.getEvent());
    assertEquals(event.getProgramStage().getIdentifier(), ruleEvent.getProgramStage());
    assertNotNull(ruleEvent.getProgramStageName());
    assertEquals(event.getStatus().name(), ruleEvent.getStatus().name());
    assertDates(event.getOccurredAt(), ruleEvent.getEventDate());
    assertNull(ruleEvent.getCompletedDate());
    assertNotNull(ruleEvent.getCreatedDate());
    assertEquals(event.getOrgUnit().getIdentifier(), ruleEvent.getOrganisationUnit());
    assertNotNull(ruleEvent.getOrganisationUnitCode());
    assertDataValue(event.getDataValues().iterator().next(), ruleEvent.getDataValues().get(0));
  }

  private void assertDates(java.time.Instant expected, Instant actual) {
    assertEquals(expected.toEpochMilli(), actual.getValue$kotlinx_datetime().toEpochMilli());
  }

  private void assertDates(Date expected, Instant actual) {
    assertEquals(expected.getTime(), actual.getValue$kotlinx_datetime().toEpochMilli());
  }

  private void assertDataValue(EventDataValue expectedDataValue, RuleDataValue actualDataValue) {
    assertEquals(expectedDataValue.getValue(), actualDataValue.getValue());
    assertEquals(expectedDataValue.getDataElement(), actualDataValue.getDataElement());
  }

  private void assertDataValue(DataValue expectedDataValue, RuleDataValue actualDataValue) {
    assertEquals(expectedDataValue.getValue(), actualDataValue.getValue());
    assertEquals(
        expectedDataValue.getDataElement().getIdentifier(), actualDataValue.getDataElement());
  }

  private void assertEnrollment(
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment, RuleEnrollment ruleEnrollment) {
    assertEquals(enrollment.getUid().getValue(), ruleEnrollment.getEnrollment());
    assertNotNull(ruleEnrollment.getProgramName());
    assertDates(enrollment.getOccurredAt(), ruleEnrollment.getIncidentDate());
    assertDates(enrollment.getEnrolledAt(), ruleEnrollment.getEnrollmentDate());
    assertEquals(enrollment.getStatus().name(), ruleEnrollment.getStatus().name());
    assertEquals(enrollment.getOrgUnit().getIdentifier(), ruleEnrollment.getOrganisationUnit());
    assertNotNull(ruleEnrollment.getOrganisationUnitCode());
    assertEquals(SAMPLE_VALUE_A, ruleEnrollment.getAttributeValues().get(0).getValue());
  }

  private void assertEnrollment(Enrollment enrollment, RuleEnrollment ruleEnrollment) {
    assertEquals(enrollment.getUid(), ruleEnrollment.getEnrollment());
    assertEquals(enrollment.getProgram().getName(), ruleEnrollment.getProgramName());
    assertDates(enrollment.getOccurredDate(), ruleEnrollment.getIncidentDate());
    assertDates(enrollment.getEnrollmentDate(), ruleEnrollment.getEnrollmentDate());
    assertEquals(enrollment.getStatus().name(), ruleEnrollment.getStatus().name());
    assertEquals(enrollment.getOrganisationUnit().getUid(), ruleEnrollment.getOrganisationUnit());
    assertEquals(
        enrollment.getOrganisationUnit().getCode(), ruleEnrollment.getOrganisationUnitCode());
    assertEquals(SAMPLE_VALUE_A, ruleEnrollment.getAttributeValues().get(0).getValue());
  }

  private void assertDates(java.time.Instant expected, LocalDate actual) {
    assertEquals(DateUtils.toMediumDate(DateUtils.fromInstant(expected)), actual.toString());
  }

  private void assertDates(Date expected, LocalDate actual) {
    assertEquals(DateUtils.toMediumDate(expected), actual.toString());
  }

  private Attribute attribute(TrackedEntityAttribute trackedEntityAttribute, String value) {
    return Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(trackedEntityAttribute))
        .value(value)
        .build();
  }

  private TrackedEntity trackedEntity() {
    return createTrackedEntity(
        'I', organisationUnit, trackedEntityAttribute, createTrackedEntityType('W'));
  }

  private org.hisp.dhis.tracker.imports.domain.Enrollment payloadEnrollment() {
    return org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
        .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
        .status(EnrollmentStatus.ACTIVE)
        .enrolledAt(NOW.toInstant())
        .occurredAt(TOMORROW.toInstant())
        .program(MetadataIdentifier.ofUid(program))
        .trackedEntity(UID.generate())
        .enrollment(UID.generate())
        .build();
  }

  private Enrollment dbEnrollment() {
    Enrollment enrollment = new Enrollment(NOW, TOMORROW, trackedEntity(), program);
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setAutoFields();
    return enrollment;
  }

  private Event dbEvent() {
    Event event = new Event(dbEnrollment(), programStage);
    event.setUid(CodeGenerator.generateUid());
    event.setOrganisationUnit(organisationUnit);
    event.setAutoFields();
    event.setScheduledDate(YESTERDAY);
    event.setOccurredDate(AFTER_TOMORROW);
    event.setEventDataValues(Sets.newHashSet(eventDataValue(dataElement.getUid(), SAMPLE_VALUE_A)));
    return event;
  }

  private EventDataValue eventDataValue(String dataElementUid, String value) {
    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setDataElement(dataElementUid);
    eventDataValue.setValue(value);
    eventDataValue.setAutoFields();
    return eventDataValue;
  }

  private org.hisp.dhis.tracker.imports.domain.Event payloadEvent() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .enrollment(UID.generate())
        .event(UID.generate())
        .programStage(MetadataIdentifier.ofUid(programStage.getUid()))
        .orgUnit(MetadataIdentifier.ofUid(organisationUnit.getUid()))
        .scheduledAt(YESTERDAY.toInstant())
        .occurredAt(AFTER_TOMORROW.toInstant())
        .dataValues(
            Set.of(
                DataValue.builder()
                    .dataElement(MetadataIdentifier.ofUid(dataElement.getUid()))
                    .value(SAMPLE_VALUE_A)
                    .build()))
        .build();
  }
}
