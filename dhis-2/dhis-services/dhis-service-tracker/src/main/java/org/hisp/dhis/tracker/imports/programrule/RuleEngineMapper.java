/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import kotlinx.datetime.LocalDateTime;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEnrollmentStatus;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleEventStatus;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/** RuleEngineMapper maps tracker objects from DB and payload to rule engine model objects. */
@Service
public class RuleEngineMapper {
  public @Nonnull RuleEnrollment toRuleEnrollment(
      @Nonnull org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      @Nonnull List<RuleAttributeValue> attributeValues,
      @Nonnull TrackerPreheat preheat) {
    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(enrollment.getOrgUnit());
    Program program = preheat.getProgram(enrollment.getProgram());

    return new RuleEnrollment(
        enrollment.getUid(),
        program.getName(),
        LocalDateTime.Formats.INSTANCE
            .getISO()
            .parse(DateUtils.toIso8601NoTz(DateUtils.fromInstant(enrollment.getOccurredAt())))
            .getDate(),
        LocalDateTime.Formats.INSTANCE
            .getISO()
            .parse(DateUtils.toIso8601NoTz(DateUtils.fromInstant(enrollment.getEnrolledAt())))
            .getDate(),
        RuleEnrollmentStatus.valueOf(enrollment.getStatus().toString()),
        organisationUnit.getUid(),
        organisationUnit.getCode(),
        attributeValues);
  }

  public @Nonnull RuleEnrollment toRuleEnrollment(
      @Nonnull Enrollment enrollment, @Nonnull List<RuleAttributeValue> attributeValues) {
    String orgUnit = "";
    String orgUnitCode = "";

    if (enrollment.getOrganisationUnit() != null) {
      orgUnit = enrollment.getOrganisationUnit().getUid();
      orgUnitCode = enrollment.getOrganisationUnit().getCode();
    }

    return new RuleEnrollment(
        enrollment.getUid(),
        enrollment.getProgram().getName(),
        LocalDateTime.Formats.INSTANCE
            .getISO()
            .parse(DateUtils.toIso8601NoTz(enrollment.getOccurredDate()))
            .getDate(),
        LocalDateTime.Formats.INSTANCE
            .getISO()
            .parse(DateUtils.toIso8601NoTz(enrollment.getEnrollmentDate()))
            .getDate(),
        RuleEnrollmentStatus.valueOf(enrollment.getStatus().toString()),
        orgUnit,
        orgUnitCode,
        attributeValues);
  }

  public @Nonnull List<RuleEvent> toRuleEvents(
      @Nonnull List<org.hisp.dhis.tracker.imports.domain.Event> events,
      @Nonnull TrackerPreheat preheat) {
    return events.stream().map(e -> toRuleEvent(e, preheat)).toList();
  }

  public @Nonnull List<RuleEvent> toRuleEvents(@Nonnull List<Event> events) {
    return events.stream().map(this::toRuleEvent).toList();
  }

  public @Nonnull List<RuleAttributeValue> toRuleAttributes(
      @Nonnull List<Attribute> attributes, @Nonnull TrackerPreheat preheat) {
    return attributes.stream()
        .map(
            a -> {
              TrackedEntityAttribute trackedEntityAttribute =
                  preheat.getTrackedEntityAttribute(a.getAttribute());
              return new RuleAttributeValue(
                  trackedEntityAttribute.getUid(),
                  getValue(a.getValue(), trackedEntityAttribute.getValueType()));
            })
        .toList();
  }

  private String getValue(String value, ValueType valueType) {
    if (value != null) {
      return value;
    }
    if (valueType.isBoolean()) {
      return "false";
    }

    if (valueType.isNumeric()) {
      return "0";
    }

    return "";
  }

  private RuleEvent toRuleEvent(
      org.hisp.dhis.tracker.imports.domain.Event eventToEvaluate, TrackerPreheat preheat) {
    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(eventToEvaluate.getOrgUnit());
    ProgramStage programStage = preheat.getProgramStage(eventToEvaluate.getProgramStage());
    Event event = preheat.getEvent(eventToEvaluate.getUid());
    Instant createdDate =
        event == null
            ? Clock.System.INSTANCE.now()
            : Instant.Companion.fromEpochMilliseconds(event.getCreated().getTime());

    return new RuleEvent(
        eventToEvaluate.getUid(),
        programStage.getUid(),
        programStage.getName(),
        RuleEventStatus.valueOf(eventToEvaluate.getStatus().toString()),
        eventToEvaluate.getOccurredAt() != null
            ? Instant.Companion.fromEpochMilliseconds(
                eventToEvaluate.getOccurredAt().toEpochMilli())
            : Instant.Companion.fromEpochMilliseconds(
                eventToEvaluate.getScheduledAt().toEpochMilli()),
        createdDate,
        eventToEvaluate.getScheduledAt() == null
            ? null
            : LocalDateTime.Formats.INSTANCE
                .getISO()
                .parse(
                    DateUtils.toIso8601NoTz(
                        DateUtils.fromInstant(eventToEvaluate.getScheduledAt())))
                .getDate(),
        eventToEvaluate.getCompletedAt() == null
            ? null
            : LocalDateTime.Formats.INSTANCE
                .getISO()
                .parse(
                    DateUtils.toIso8601NoTz(
                        DateUtils.fromInstant(eventToEvaluate.getCompletedAt())))
                .getDate(),
        organisationUnit.getUid(),
        organisationUnit.getCode(),
        eventToEvaluate.getDataValues().stream()
            .filter(Objects::nonNull)
            .filter(dv -> dv.getValue() != null)
            .map(
                dv ->
                    new RuleDataValue(
                        preheat.getDataElement(dv.getDataElement()).getUid(), dv.getValue()))
            .toList());
  }

  private RuleEvent toRuleEvent(Event eventToEvaluate) {
    OrganisationUnit organisationUnit = eventToEvaluate.getOrganisationUnit();
    String orgUnit = organisationUnit == null ? "" : organisationUnit.getUid();
    String orgUnitCode = organisationUnit == null ? "" : organisationUnit.getCode();

    return new RuleEvent(
        eventToEvaluate.getUid(),
        eventToEvaluate.getProgramStage().getUid(),
        eventToEvaluate.getProgramStage().getName(),
        RuleEventStatus.valueOf(eventToEvaluate.getStatus().toString()),
        eventToEvaluate.getOccurredDate() != null
            ? Instant.Companion.fromEpochMilliseconds(eventToEvaluate.getOccurredDate().getTime())
            : Instant.Companion.fromEpochMilliseconds(eventToEvaluate.getScheduledDate().getTime()),
        Instant.Companion.fromEpochMilliseconds(eventToEvaluate.getCreated().getTime()),
        eventToEvaluate.getScheduledDate() == null
            ? null
            : LocalDateTime.Formats.INSTANCE
                .getISO()
                .parse(DateUtils.toIso8601NoTz(eventToEvaluate.getScheduledDate()))
                .getDate(),
        eventToEvaluate.getCompletedDate() == null
            ? null
            : LocalDateTime.Formats.INSTANCE
                .getISO()
                .parse(DateUtils.toIso8601NoTz(eventToEvaluate.getCompletedDate()))
                .getDate(),
        orgUnit,
        orgUnitCode,
        eventToEvaluate.getEventDataValues().stream()
            .filter(Objects::nonNull)
            .filter(dv -> dv.getValue() != null)
            .map(dv -> new RuleDataValue(dv.getDataElement(), dv.getValue()))
            .toList());
  }
}
