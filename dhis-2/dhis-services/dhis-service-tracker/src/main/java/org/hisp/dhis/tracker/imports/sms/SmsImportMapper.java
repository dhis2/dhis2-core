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

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SmsConsts.SmsEnrollmentStatus;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.models.EnrollmentSmsSubmission;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SimpleEventSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsAttributeValue;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsEvent;
import org.hisp.dhis.smscompression.models.TrackerEventSmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.system.util.SmsUtils;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * SmsImportMapper maps tracker SMS to {@link org.hisp.dhis.tracker.imports.domain.TrackerObjects}
 * so they can be imported into tracker. This class should only include pure functions that do not
 * need any other dependency.
 *
 * <p>There are two types of SMS tracker supports
 *
 * <ol>
 *   <li>compression based SMS types found in {@link org.hisp.dhis.smscompression.models} and
 *   <li>command based SMS which are not compressed and do not have dedicated types
 * </ol>
 *
 * Package private mapper functions are the ones used by the SMS processing code. Compression based
 * SMS mapper functions are prefixed with {@code map} while command based SMS ones are prefixed with
 * {@code mapCommand}.
 *
 * <p>Note that all "id" fields in a compressed SMS are mandatory, meaning you will get an NPE if
 * you try to encode an sms using {@link org.hisp.dhis.smscompression}. Android will therefore
 * always send an id even for newly created entities. Some fields like dates or collections are
 * optional.
 *
 * <p>The output {@link org.hisp.dhis.tracker.imports.domain.TrackerObjects} need to flattened as
 * that is what the tracker import expects.
 */
class SmsImportMapper {
  private static final GeometryFactory geometryFactory = new GeometryFactory();

  private SmsImportMapper() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * {@link EnrollmentSmsSubmission} can create or update a tracked entity, enrollment with tracked
   * entity attributes. It can also create or update events with data values. Refer to attribute
   * mappers for the tracked entity and enrollment to understand how attributes are translated.
   */
  @Nonnull
  static TrackerObjects map(
      @Nonnull EnrollmentSmsSubmission submission,
      @Nonnull Program program,
      @CheckForNull org.hisp.dhis.trackedentity.TrackedEntity trackedEntity,
      @Nonnull String username) {
    Set<String> programAttributes =
        emptyIfNull(program.getProgramAttributes()).stream()
            .map(ProgramTrackedEntityAttribute::getAttribute)
            .map(TrackedEntityAttribute::getUid)
            .collect(Collectors.toSet());

    Set<String> existingAttributeValues = Set.of();
    if (trackedEntity != null) {
      existingAttributeValues =
          CollectionUtils.emptyIfNull(trackedEntity.getTrackedEntityAttributeValues()).stream()
              .map(TrackedEntityAttributeValue::getAttribute)
              .map(TrackedEntityAttribute::getUid)
              .collect(Collectors.toSet());
    }

    return TrackerObjects.builder()
        .trackedEntities(
            List.of(mapTrackedEntity(submission, programAttributes, existingAttributeValues)))
        .enrollments(
            List.of(mapToEnrollment(submission, programAttributes, existingAttributeValues)))
        .events(
            emptyIfNull(submission.getEvents()).stream()
                .map(e -> mapToEvent(e, username, submission.getEnrollment()))
                .toList())
        .build();
  }

  @Nonnull
  private static TrackedEntity mapTrackedEntity(
      EnrollmentSmsSubmission submission,
      Set<String> programAttributes,
      Set<String> existingAttributeValues) {
    return TrackedEntity.builder()
        .orgUnit(metadataUid(submission.getOrgUnit()))
        .trackedEntity(UID.of(submission.getTrackedEntityInstance().getUid()))
        .trackedEntityType(metadataUid(submission.getTrackedEntityType()))
        .attributes(
            mapTrackedEntityTypeAttributes(
                submission.getValues(), existingAttributeValues, programAttributes))
        .build();
  }

  /**
   * mapTrackedEntityTypeAttributes works like {@link #mapProgramAttributeValues(List, Set, Set)}
   * only using non-program attributes which are assumed to be tracked entity type attributes.
   */
  @Nonnull
  private static List<Attribute> mapTrackedEntityTypeAttributes(
      @CheckForNull List<SmsAttributeValue> smsAttributeValues,
      @Nonnull Set<String> existingAttributeValues,
      @Nonnull Set<String> programAttributes) {
    List<SmsAttributeValue> smsTrackedEntityTypeAttributeValues =
        emptyIfNull(smsAttributeValues).stream()
            .filter(av -> !programAttributes.contains(av.getAttribute().getUid()))
            .toList();
    Set<String> existingTrackedEntityTypeAttributeValues =
        existingAttributeValues.stream()
            .filter(Predicate.not(programAttributes::contains))
            .collect(Collectors.toSet());
    return mapAttributeValues(
        smsTrackedEntityTypeAttributeValues, existingTrackedEntityTypeAttributeValues);
  }

  /**
   * mapAttributeValues translates {@link SmsAttributeValue}s to {@link Attribute}s for the tracker
   * importer. Any TEAV that is not present in the SMS will turn into an element to delete the TEAV
   * {@code {"attribute": "uid", "value": null}}. This logic was part of the {@link
   * EnrollmentSMSListener} processing before we migrated it to the tracker importer. It might have
   * been added as users cannot add an attribute with a null value in the {@link
   * EnrollmentSmsSubmission} model. That is speculation as users might be able to send an empty ""
   * value which is also understood as a deletion request for attributes.
   */
  @Nonnull
  private static List<Attribute> mapAttributeValues(
      @Nonnull List<SmsAttributeValue> smsAttributeValues,
      @Nonnull Set<String> existingAttributeValues) {
    Map<String, String> attributeValues = new HashMap<>();

    for (String attributeUid : existingAttributeValues) {
      attributeValues.put(attributeUid, null);
    }

    for (SmsAttributeValue smsAttributeValue : smsAttributeValues) {
      // either add a new attribute value or update an existing one
      attributeValues.put(smsAttributeValue.getAttribute().getUid(), smsAttributeValue.getValue());
    }

    return attributeValues.entrySet().stream()
        .map(
            entry ->
                Attribute.builder()
                    .attribute(MetadataIdentifier.ofUid(entry.getKey()))
                    .value(entry.getValue())
                    .build())
        .toList();
  }

  @Nonnull
  private static Enrollment mapToEnrollment(
      @Nonnull EnrollmentSmsSubmission submission,
      @Nonnull Set<String> programAttributes,
      @Nonnull Set<String> existingAttributeValues) {
    return Enrollment.builder()
        .orgUnit(metadataUid(submission.getOrgUnit()))
        .program(metadataUid(submission.getTrackerProgram()))
        .trackedEntity(UID.of(submission.getTrackedEntityInstance().getUid()))
        .enrollment(UID.of(submission.getEnrollment().getUid()))
        .enrolledAt(toInstant(submission.getEnrollmentDate()))
        .occurredAt(toInstant(submission.getIncidentDate()))
        .status(map(submission.getEnrollmentStatus()))
        .geometry(map(submission.getCoordinates()))
        .attributes(
            mapProgramAttributeValues(
                submission.getValues(), programAttributes, existingAttributeValues))
        .build();
  }

  /**
   * mapProgramAttributeValues translates {@link EnrollmentSmsSubmission#getValues()} into {@link
   * Enrollment#getAttributes()}. The tracker importer only accepts program attributes in {@link
   * Enrollment#getAttributes()}. Since attribute values in the sms can be tracked entity type
   * and/or program attributes they have to be split. Tracked entity type attributes go to the
   * {@link TrackedEntity#getAttributes()} while program attributes go to the {@link
   * Enrollment#getAttributes()}. The confusion in our tracker model is that a program attribute
   * <em>can</em> also be a tracked entity attribute. This is why its legal to put such attributes
   * into both attribute collections. This is what Capture app currently does. This only works in
   * the Capture app as it can guarantee that tracker programs will always contain all the tracked
   * entity type attributes of the programs tracked entity as its program attributes.
   *
   * @param smsAttributeValues all attribute values of an {@link EnrollmentSmsSubmission}
   * @param programAttributes program attributes of the tracker program the {@link
   *     EnrollmentSmsSubmission} is for
   * @param existingAttributeValues program and tracked entity type attribute values of an existing
   *     tracked entity
   * @return enrollment (program) attributes for the tracker importer
   */
  @Nonnull
  static List<Attribute> mapProgramAttributeValues(
      @CheckForNull List<SmsAttributeValue> smsAttributeValues,
      @Nonnull Set<String> programAttributes,
      @Nonnull Set<String> existingAttributeValues) {
    List<SmsAttributeValue> smsProgramAttributeValues =
        emptyIfNull(smsAttributeValues).stream()
            .filter(av -> programAttributes.contains(av.getAttribute().getUid()))
            .toList();
    Set<String> existingProgramAttributeValues =
        existingAttributeValues.stream()
            .filter(programAttributes::contains)
            .collect(Collectors.toSet());
    return mapAttributeValues(smsProgramAttributeValues, existingProgramAttributeValues);
  }

  @Nonnull
  private static Event mapToEvent(
      @Nonnull SmsEvent submission, @Nonnull String username, @Nonnull Uid enrollment) {
    return TrackerEvent.builder()
        .event(UID.of(submission.getEvent().getUid()))
        .enrollment(UID.of(enrollment.getUid()))
        .orgUnit(metadataUid(submission.getOrgUnit()))
        .programStage(metadataUid(submission.getProgramStage()))
        .attributeOptionCombo(metadataUid(submission.getAttributeOptionCombo()))
        .storedBy(username)
        .occurredAt(toInstant(submission.getEventDate()))
        .scheduledAt(toInstant(submission.getDueDate()))
        .status(map(submission.getEventStatus()))
        .geometry(map(submission.getCoordinates()))
        .dataValues(map(submission.getValues(), username))
        .build();
  }

  @Nonnull
  static TrackerObjects map(
      @Nonnull TrackerEventSmsSubmission submission, @Nonnull String username) {
    return TrackerObjects.builder().events(List.of(mapEvent(submission, username))).build();
  }

  @Nonnull
  private static Event mapEvent(
      @Nonnull TrackerEventSmsSubmission submission, @Nonnull String username) {
    return TrackerEvent.builder()
        .event(UID.of(submission.getEvent().getUid()))
        .enrollment(UID.of(submission.getEnrollment().getUid()))
        .orgUnit(metadataUid(submission.getOrgUnit()))
        .programStage(metadataUid(submission.getProgramStage()))
        .attributeOptionCombo(metadataUid(submission.getAttributeOptionCombo()))
        .storedBy(username)
        .occurredAt(toInstant(submission.getEventDate()))
        .scheduledAt(toInstant(submission.getDueDate()))
        .status(map(submission.getEventStatus()))
        .geometry(map(submission.getCoordinates()))
        .dataValues(map(submission.getValues(), username))
        .build();
  }

  @Nonnull
  static TrackerObjects map(
      @Nonnull SimpleEventSmsSubmission submission, @Nonnull String username) {
    return TrackerObjects.builder().events(List.of(mapEvent(submission, username))).build();
  }

  @Nonnull
  private static Event mapEvent(
      @Nonnull SimpleEventSmsSubmission submission, @Nonnull String username) {
    return TrackerEvent.builder()
        .event(UID.of(submission.getEvent().getUid()))
        .orgUnit(metadataUid(submission.getOrgUnit()))
        .program(metadataUid(submission.getEventProgram()))
        .attributeOptionCombo(metadataUid(submission.getAttributeOptionCombo()))
        .storedBy(username)
        .occurredAt(toInstant(submission.getEventDate()))
        .scheduledAt(toInstant(submission.getDueDate()))
        .status(map(submission.getEventStatus()))
        .geometry(map(submission.getCoordinates()))
        .dataValues(map(submission.getValues(), username))
        .build();
  }

  @Nonnull
  private static MetadataIdentifier metadataUid(@Nonnull Uid uid) {
    return MetadataIdentifier.ofUid(uid.getUid());
  }

  @CheckForNull
  private static MetadataIdentifier metadataUid(@CheckForNull IdentifiableObject uid) {
    return uid != null ? MetadataIdentifier.ofUid(uid.getUid()) : null;
  }

  @CheckForNull
  private static Instant toInstant(@CheckForNull Date date) {
    return date != null ? date.toInstant() : null;
  }

  @Nonnull
  private static Set<DataValue> map(
      @CheckForNull List<SmsDataValue> dataValues, @Nonnull String username) {
    return emptyIfNull(dataValues).stream()
        .map(
            dv ->
                DataValue.builder()
                    .dataElement(metadataUid(dv.getDataElement()))
                    .value(dv.getValue())
                    .storedBy(username)
                    .build())
        .collect(Collectors.toSet());
  }

  @CheckForNull
  private static Point map(@CheckForNull GeoPoint coordinates) {
    if (coordinates == null) {
      return null;
    }

    return geometryFactory.createPoint(
        new Coordinate(coordinates.getLongitude(), coordinates.getLatitude()));
  }

  private static EnrollmentStatus map(SmsEnrollmentStatus status) {
    return switch (status) {
      case ACTIVE -> EnrollmentStatus.ACTIVE;
      case COMPLETED -> EnrollmentStatus.COMPLETED;
      case CANCELLED -> EnrollmentStatus.CANCELLED;
    };
  }

  private static EventStatus map(SmsEventStatus status) {
    return switch (status) {
      case ACTIVE -> EventStatus.ACTIVE;
      case COMPLETED -> EventStatus.COMPLETED;
      case VISITED -> EventStatus.VISITED;
      case SCHEDULE -> EventStatus.SCHEDULE;
      case OVERDUE -> EventStatus.OVERDUE;
      case SKIPPED -> EventStatus.SKIPPED;
    };
  }

  /** Maps command SMS of parser type {@code ParserType.PROGRAM_STAGE_DATAENTRY_PARSER}. */
  static @Nonnull TrackerObjects mapCommand(
      @Nonnull IncomingSms sms,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> dataValues,
      @Nonnull String orgUnit,
      @Nonnull String username,
      @Nonnull CategoryService dataElementCategoryService,
      @Nonnull String trackedEntity,
      @CheckForNull UID enrollmentUid) {
    List<Enrollment> enrollments = List.of();
    if (enrollmentUid == null) {
      enrollmentUid = UID.generate();
      Instant now = Instant.now();
      Enrollment enrollment =
          Enrollment.builder()
              .enrollment(enrollmentUid)
              .trackedEntity(UID.of(trackedEntity))
              .program(metadataUid(smsCommand.getProgram()))
              .orgUnit(MetadataIdentifier.ofUid(orgUnit))
              .occurredAt(now)
              .enrolledAt(now)
              .status(EnrollmentStatus.ACTIVE)
              .build();
      enrollments = List.of(enrollment);
    }

    Event event =
        mapCommandEvent(sms, smsCommand, dataValues, orgUnit, username, dataElementCategoryService);
    event.setEnrollment(enrollmentUid);

    return TrackerObjects.builder().enrollments(enrollments).events(List.of(event)).build();
  }

  /** Maps command SMS of parser type {@code ParserType.EVENT_REGISTRATION_PARSER}. */
  static @Nonnull TrackerObjects mapCommand(
      @Nonnull IncomingSms sms,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> dataValues,
      @Nonnull String orgUnit,
      @Nonnull String username,
      @Nonnull CategoryService dataElementCategoryService) {
    return TrackerObjects.builder()
        .events(
            List.of(
                mapCommandEvent(
                    sms, smsCommand, dataValues, orgUnit, username, dataElementCategoryService)))
        .build();
  }

  /** Maps command SMS of parser type {@code ParserType.TRACKED_ENTITY_REGISTRATION_PARSER}. */
  static @Nonnull TrackerObjects mapTrackedEntityRegistrationParserCommand(
      @Nonnull IncomingSms sms,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> attributeValues,
      @Nonnull OrganisationUnit orgUnit) {
    UID trackedEntity = UID.generate();
    Date now = new Date();
    Date occurredDate = Objects.requireNonNullElse(SmsUtils.lookForDate(sms.getText()), now);

    // ignore any sent attributes that are not present in the SMS command
    Map<String, SMSCode> codes =
        smsCommand.getCodes().stream()
            .collect(Collectors.toMap(SMSCode::getCode, Function.identity()));
    List<Attribute> attributes =
        attributeValues.entrySet().stream()
            .filter(av -> codes.containsKey(av.getKey()))
            .map(
                av ->
                    Attribute.builder()
                        .attribute(
                            MetadataIdentifier.ofUid(
                                codes.get(av.getKey()).getTrackedEntityAttribute()))
                        .value(av.getValue())
                        .build())
            .toList();

    return TrackerObjects.builder()
        .trackedEntities(
            List.of(
                TrackedEntity.builder()
                    .trackedEntity(trackedEntity)
                    .orgUnit(metadataUid(orgUnit))
                    .trackedEntityType(metadataUid(smsCommand.getProgram().getTrackedEntityType()))
                    .attributes(attributes)
                    .build()))
        .enrollments(
            List.of(
                Enrollment.builder()
                    .enrollment(UID.generate())
                    .trackedEntity(trackedEntity)
                    .orgUnit(metadataUid(orgUnit))
                    .program(metadataUid(smsCommand.getProgram()))
                    .enrolledAt(now.toInstant())
                    .occurredAt(toInstant(occurredDate))
                    .status(EnrollmentStatus.ACTIVE)
                    .build()))
        .build();
  }

  static @Nonnull Event mapCommandEvent(
      @Nonnull IncomingSms sms,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> dataValues,
      @Nonnull String orgUnit,
      @Nonnull String username,
      @Nonnull CategoryService dataElementCategoryService) {
    return TrackerEvent.builder()
        .event(UID.generate())
        .orgUnit(MetadataIdentifier.ofUid(orgUnit))
        .program(metadataUid(smsCommand.getProgram()))
        .programStage(metadataUid(smsCommand.getProgramStage()))
        .occurredAt(sms.getSentDate().toInstant())
        .scheduledAt(sms.getSentDate().toInstant())
        .attributeOptionCombo(
            metadataUid(dataElementCategoryService.getDefaultCategoryOptionCombo()))
        .storedBy(username)
        .dataValues(map(smsCommand.getCodes(), dataValues, username))
        .build();
  }

  @Nonnull
  private static Set<DataValue> map(
      @Nonnull Set<SMSCode> codes,
      @Nonnull Map<String, String> dataValues,
      @Nonnull String username) {
    Set<DataValue> result = new HashSet<>();
    for (SMSCode code : codes) {
      String value = dataValues.get(code.getCode());
      // Filter empty values out -> this is "adding/saving/creating",
      // therefore, empty values are ignored
      if (StringUtils.isEmpty(value)) {
        continue;
      }

      result.add(
          DataValue.builder()
              .dataElement(MetadataIdentifier.ofUid(code.getDataElement().getUid()))
              .value(value)
              .storedBy(username)
              .build());
    }

    return result;
  }
}
