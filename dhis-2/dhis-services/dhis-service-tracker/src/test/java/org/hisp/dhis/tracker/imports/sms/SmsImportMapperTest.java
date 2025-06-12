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
package org.hisp.dhis.tracker.imports.sms;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.tracker.imports.sms.SmsImportMapper.map;
import static org.hisp.dhis.tracker.imports.sms.SmsImportMapper.mapProgramAttributeValues;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.smscompression.SmsConsts.SmsEnrollmentStatus;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.models.EnrollmentSmsSubmission;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SmsAttributeValue;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsEvent;
import org.hisp.dhis.smscompression.models.TrackerEventSmsSubmission;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class SmsImportMapperTest extends TestBase {
  @Test
  void mapEnrollmentWithNewTrackedEntityAndOnlyMandatoryFields() {
    EnrollmentSmsSubmission input = new EnrollmentSmsSubmission();
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setTrackerProgram(CodeGenerator.generateUid());
    input.setTrackedEntityInstance(CodeGenerator.generateUid());
    input.setTrackedEntityType(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setEnrollmentStatus(SmsEnrollmentStatus.COMPLETED);

    TrackerObjects actual = map(input, program(), null, "francis");

    TrackerObjects expected =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    Enrollment.builder()
                        .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
                        .program(MetadataIdentifier.ofUid(input.getTrackerProgram().getUid()))
                        .trackedEntity(UID.of(input.getTrackedEntityInstance().getUid()))
                        .enrollment(UID.of(input.getEnrollment().getUid()))
                        .status(EnrollmentStatus.COMPLETED)
                        .build()))
            .trackedEntities(
                List.of(
                    TrackedEntity.builder()
                        .trackedEntity(UID.of(input.getTrackedEntityInstance().getUid()))
                        .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
                        .trackedEntityType(
                            MetadataIdentifier.ofUid(input.getTrackedEntityType().getUid()))
                        .build()))
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void mapEnrollmentWithNewTrackedEntityAndAttributes() {
    EnrollmentSmsSubmission input = new EnrollmentSmsSubmission();
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setTrackerProgram(CodeGenerator.generateUid());
    input.setTrackedEntityInstance(CodeGenerator.generateUid());
    input.setTrackedEntityType(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setEnrollmentStatus(SmsEnrollmentStatus.COMPLETED);
    // non-program attribute values are mapped onto the tracked entity
    input.setValues(List.of(new SmsAttributeValue("fN8skWVI8JS", "soap")));

    TrackerObjects actual = map(input, program("YjToz9y10ZZ"), null, "francis");

    TrackerObjects expected =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    Enrollment.builder()
                        .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
                        .program(MetadataIdentifier.ofUid(input.getTrackerProgram().getUid()))
                        .trackedEntity(UID.of(input.getTrackedEntityInstance().getUid()))
                        .enrollment(UID.of(input.getEnrollment().getUid()))
                        .status(EnrollmentStatus.COMPLETED)
                        .build()))
            .trackedEntities(
                List.of(
                    TrackedEntity.builder()
                        .trackedEntity(UID.of(input.getTrackedEntityInstance().getUid()))
                        .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
                        .trackedEntityType(
                            MetadataIdentifier.ofUid(input.getTrackedEntityType().getUid()))
                        .attributes(
                            List.of(
                                Attribute.builder()
                                    .attribute(MetadataIdentifier.ofUid("fN8skWVI8JS"))
                                    .value("soap")
                                    .build()))
                        .build()))
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void mapEnrollmentWithNewTrackedEntityAndOptionalNonCollectionFields() {
    EnrollmentSmsSubmission input = new EnrollmentSmsSubmission();
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setTrackerProgram(CodeGenerator.generateUid());
    input.setTrackedEntityInstance(CodeGenerator.generateUid());
    input.setTrackedEntityType(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setEnrollmentStatus(SmsEnrollmentStatus.CANCELLED);

    Date enrollmentDate = DateUtils.getDate(2024, 9, 2, 10, 15);
    input.setEnrollmentDate(enrollmentDate);
    Date occurredDate = DateUtils.getDate(2024, 9, 3, 16, 23);
    input.setIncidentDate(occurredDate);
    input.setCoordinates(new GeoPoint(48.8575f, 2.3514f));

    TrackerObjects actual = map(input, program(), null, "francis");

    List<Enrollment> expected =
        List.of(
            Enrollment.builder()
                .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
                .program(MetadataIdentifier.ofUid(input.getTrackerProgram().getUid()))
                .trackedEntity(UID.of(input.getTrackedEntityInstance().getUid()))
                .enrollment(UID.of(input.getEnrollment().getUid()))
                .enrolledAt(enrollmentDate.toInstant())
                .occurredAt(occurredDate.toInstant())
                .status(EnrollmentStatus.CANCELLED)
                .geometry(new GeometryFactory().createPoint(new Coordinate(2.3514f, 48.8575f)))
                .build());
    assertEquals(expected, actual.getEnrollments());
  }

  @Test
  void mapEnrollmentWithEvents() {
    EnrollmentSmsSubmission input = new EnrollmentSmsSubmission();
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setTrackerProgram(CodeGenerator.generateUid());
    input.setTrackedEntityInstance(CodeGenerator.generateUid());
    input.setTrackedEntityType(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setEnrollmentStatus(SmsEnrollmentStatus.CANCELLED);

    SmsEvent smsEvent = new SmsEvent();
    smsEvent.setOrgUnit(input.getOrgUnit().getUid());
    smsEvent.setProgramStage(CodeGenerator.generateUid());
    smsEvent.setEventStatus(SmsEventStatus.SCHEDULE);
    smsEvent.setAttributeOptionCombo(CodeGenerator.generateUid());
    smsEvent.setEvent(CodeGenerator.generateUid());
    // The coc has to be set so the sms-compression library can encode the data value. Not sure why
    // that is necessary though.
    smsEvent.setValues(
        List.of(new SmsDataValue(CodeGenerator.generateUid(), "oHvZHthw9Y0", "hello")));
    input.setEvents(List.of(smsEvent));

    TrackerObjects actual = map(input, program(), null, "francis");

    List<Event> expected =
        List.of(
            TrackerEvent.builder()
                .event(UID.of(smsEvent.getEvent().getUid()))
                .orgUnit(MetadataIdentifier.ofUid(smsEvent.getOrgUnit().getUid()))
                .programStage(MetadataIdentifier.ofUid(smsEvent.getProgramStage().getUid()))
                .attributeOptionCombo(
                    MetadataIdentifier.ofUid(smsEvent.getAttributeOptionCombo().getUid()))
                .status(EventStatus.SCHEDULE)
                .storedBy("francis")
                .dataValues(
                    Set.of(
                        DataValue.builder()
                            .dataElement(MetadataIdentifier.ofUid("oHvZHthw9Y0"))
                            .value("hello")
                            .storedBy("francis")
                            .build()))
                .enrollment(UID.of(input.getEnrollment().getUid()))
                .build());
    assertEquals(expected, actual.getEvents());
  }

  @Test
  void mapEnrollmentWithExistingTrackedEntity() {
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid("uE1OF7DDawz");
    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid("cCR4QVathUM");
    org.hisp.dhis.trackedentity.TrackedEntity trackedEntity =
        new org.hisp.dhis.trackedentity.TrackedEntity();
    trackedEntity.setUid(CodeGenerator.generateUid());
    trackedEntity.setTrackedEntityAttributeValues(
        Set.of(
            new TrackedEntityAttributeValue(tea1, trackedEntity, "oneWillBeDeleted"),
            new TrackedEntityAttributeValue(tea2, trackedEntity, "twoWillBeUpdated")));

    EnrollmentSmsSubmission input = new EnrollmentSmsSubmission();
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setTrackerProgram(CodeGenerator.generateUid());
    input.setTrackedEntityInstance(trackedEntity.getUid());
    input.setTrackedEntityType(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setEnrollmentStatus(SmsEnrollmentStatus.CANCELLED);
    // attribute values are only mapped onto the tracked entity
    input.setValues(
        List.of(
            new SmsAttributeValue("cCR4QVathUM", "twoWasUpdated"),
            new SmsAttributeValue("zjOPAEZyQxu", "threeWasAdded")));

    TrackerObjects actual = map(input, program("YjToz9y10ZZ"), trackedEntity, "francis");

    List<Attribute> expected =
        List.of(
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("uE1OF7DDawz"))
                .value(null)
                .build(),
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("cCR4QVathUM"))
                .value("twoWasUpdated")
                .build(),
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("zjOPAEZyQxu"))
                .value("threeWasAdded")
                .build());
    assertNotEmpty(actual.getTrackedEntities());
    assertContainsOnly(expected, actual.getTrackedEntities().get(0).getAttributes());
  }

  @Test
  void
      mapSmsAttributeValuesToProgramAttributesGivenTETAndProgramAttributesAndNoExistingAttributes() {
    List<SmsAttributeValue> input =
        List.of(
            new SmsAttributeValue("uE1OF7DDawz", "firstTrackedEntityTypeAttribute"),
            new SmsAttributeValue("YjToz9y10ZZ", "firstProgramAttribute"),
            new SmsAttributeValue("fN8skWVI8JS", "secondProgramAttribute"));
    Set<String> programAttributes = Set.of("YjToz9y10ZZ", "fN8skWVI8JS");

    List<Attribute> actual = mapProgramAttributeValues(input, programAttributes, Set.of());

    List<Attribute> expected =
        List.of(
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("YjToz9y10ZZ"))
                .value("firstProgramAttribute")
                .build(),
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("fN8skWVI8JS"))
                .value("secondProgramAttribute")
                .build());
    assertContainsOnly(expected, actual);
  }

  @Test
  void mapSmsAttributeValuesToProgramAttributesGivenTETAndProgramAttributesAndExistingAttributes() {
    List<SmsAttributeValue> input =
        List.of(
            new SmsAttributeValue("uE1OF7DDawz", "firstTrackedEntityTypeAttribute"),
            new SmsAttributeValue("YjToz9y10ZZ", "firstProgramAttribute"),
            new SmsAttributeValue("fN8skWVI8JS", "secondProgramAttribute"));
    Set<String> existingAttributeValues =
        Set.of("uE1OF7DDawz", "cCR4QVathUM", "fN8skWVI8JS", "IKfH08xtgWG");
    Set<String> programAttributes = Set.of("YjToz9y10ZZ", "fN8skWVI8JS", "IKfH08xtgWG");

    List<Attribute> actual =
        mapProgramAttributeValues(input, programAttributes, existingAttributeValues);

    List<Attribute> expected =
        List.of(
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("YjToz9y10ZZ"))
                .value("firstProgramAttribute")
                .build(),
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("fN8skWVI8JS"))
                .value("secondProgramAttribute")
                .build(),
            Attribute.builder()
                .attribute(MetadataIdentifier.ofUid("IKfH08xtgWG"))
                .value(null)
                .build());
    assertContainsOnly(expected, actual);
  }

  @Test
  void mapEventWithMandatoryFields() {
    TrackerEventSmsSubmission input = new TrackerEventSmsSubmission();
    input.setEvent(CodeGenerator.generateUid());
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setProgramStage(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setAttributeOptionCombo(CodeGenerator.generateUid());
    input.setEventStatus(SmsEventStatus.COMPLETED);

    TrackerObjects actual = map(input, "francis");

    Event expected =
        TrackerEvent.builder()
            .event(UID.of(input.getEvent().getUid()))
            .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
            .programStage(MetadataIdentifier.ofUid(input.getProgramStage().getUid()))
            .enrollment(UID.of(input.getEnrollment().getUid()))
            .attributeOptionCombo(
                MetadataIdentifier.ofUid(input.getAttributeOptionCombo().getUid()))
            .status(EventStatus.COMPLETED)
            .storedBy("francis")
            .build();
    assertContainsOnly(List.of(expected), actual.getEvents());
  }

  @Test
  void mapEventWithOptionalFields() {
    TrackerEventSmsSubmission input = new TrackerEventSmsSubmission();
    input.setEvent(CodeGenerator.generateUid());
    input.setOrgUnit(CodeGenerator.generateUid());
    input.setProgramStage(CodeGenerator.generateUid());
    input.setEnrollment(CodeGenerator.generateUid());
    input.setAttributeOptionCombo(CodeGenerator.generateUid());
    input.setEventStatus(SmsEventStatus.ACTIVE);
    Date occurredDate = DateUtils.getDate(2024, 10, 7, 14, 12);
    input.setEventDate(occurredDate);
    Date scheduledDate = DateUtils.getDate(2024, 10, 9, 14, 12);
    input.setDueDate(scheduledDate);
    input.setCoordinates(new GeoPoint(48.8575f, 2.3514f));
    // The coc has to be set so the sms-compression library can encode the data value. Not sure why
    // that is necessary though.
    input.setValues(List.of(new SmsDataValue(CodeGenerator.generateUid(), "oHvZHthw9Y0", "hello")));

    TrackerObjects actual = map(input, "francis");

    Event expected =
        TrackerEvent.builder()
            .event(UID.of(input.getEvent().getUid()))
            .orgUnit(MetadataIdentifier.ofUid(input.getOrgUnit().getUid()))
            .programStage(MetadataIdentifier.ofUid(input.getProgramStage().getUid()))
            .enrollment(UID.of(input.getEnrollment().getUid()))
            .attributeOptionCombo(
                MetadataIdentifier.ofUid(input.getAttributeOptionCombo().getUid()))
            .status(EventStatus.ACTIVE)
            .storedBy("francis")
            .occurredAt(occurredDate.toInstant())
            .scheduledAt(scheduledDate.toInstant())
            .geometry(new GeometryFactory().createPoint(new Coordinate(2.3514f, 48.8575f)))
            .dataValues(
                Set.of(
                    DataValue.builder()
                        .dataElement(MetadataIdentifier.ofUid("oHvZHthw9Y0"))
                        .value("hello")
                        .storedBy("francis")
                        .build()))
            .build();
    assertContainsOnly(List.of(expected), actual.getEvents());
  }

  private static Program program(String... programAttributes) {
    Program program = new Program();
    program.setProgramAttributes(
        Arrays.stream(programAttributes)
            .map(
                a -> {
                  TrackedEntityAttribute tea = new TrackedEntityAttribute();
                  tea.setUid(a);
                  return new ProgramTrackedEntityAttribute(null, tea);
                })
            .toList());
    return program;
  }
}
