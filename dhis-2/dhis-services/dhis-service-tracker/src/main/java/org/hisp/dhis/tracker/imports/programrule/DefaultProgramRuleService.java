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
package org.hisp.dhis.tracker.imports.programrule;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventFields;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.tracker.imports.programrule.engine.RuleEngineEffects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Enrico Colasante
 */
@Service
@RequiredArgsConstructor
class DefaultProgramRuleService implements ProgramRuleService {
  private final ProgramRuleEngine programRuleEngine;

  private final TrackerEventService trackerEventService;

  private final RuleActionEnrollmentMapper ruleActionEnrollmentMapper;

  private final RuleActionEventMapper ruleActionEventMapper;

  /**
   * This is calculating the rule effects for all the enrollments and events present in the payload.
   * First, this method is iterating over the enrollments present in the payload and related events
   * (also the ones not present in the payload) and it is calculating rule effects for those. The,
   * this method is iterating over events present in the payload and related enrollment (also if it
   * is not present in the payload) and it is calculating rule effects for those. {@link
   * #calculateTrackerEventRuleEffects(TrackerBundle, TrackerPreheat)} method makes sure that rule
   * effects are calculated only once for every event. This ensures that there will be no duplicate
   * effects. Finally, this method is iterating over all program events present in the payload, and
   * it is calculating rule effects for those.
   */
  @Override
  @Transactional(readOnly = true)
  public void calculateRuleEffects(TrackerBundle bundle, TrackerPreheat preheat) {
    RuleEngineEffects ruleEffects =
        RuleEngineEffects.merge(
            RuleEngineEffects.merge(
                calculateEnrollmentRuleEffects(bundle, preheat),
                calculateTrackerEventRuleEffects(bundle, preheat)),
            calculateSingleEventRuleEffects(bundle, preheat));

    bundle.setEnrollmentNotifications(ruleEffects.getEnrollmentNotifications());
    bundle.setEventNotifications(ruleEffects.getEventNotifications());
    bundle.setEnrollmentRuleActionExecutors(
        ruleActionEnrollmentMapper.mapRuleEffects(
            ruleEffects.getEnrollmentValidationEffects(), bundle));
    bundle.setEventRuleActionExecutors(
        ruleActionEventMapper.mapRuleEffects(ruleEffects.getEventValidationEffects(), bundle));
  }

  private RuleEngineEffects calculateEnrollmentRuleEffects(
      TrackerBundle bundle, TrackerPreheat preheat) {
    return bundle.getEnrollments().stream()
        .map(
            e -> {
              List<RuleAttributeValue> attributes =
                  getAttributes(e.getEnrollment(), e.getTrackedEntity(), bundle, preheat);
              RuleEnrollment enrollment =
                  RuleEngineMapper.mapPayloadEnrollment(preheat, e, attributes);

              return programRuleEngine.evaluateEnrollmentAndTrackerEvents(
                  enrollment,
                  getEventsFromEnrollment(e.getUid(), bundle, preheat),
                  preheat.getProgram(e.getProgram()),
                  bundle.getUser());
            })
        .reduce(RuleEngineEffects::merge)
        .orElse(RuleEngineEffects.empty());
  }

  private RuleEngineEffects calculateTrackerEventRuleEffects(
      TrackerBundle bundle, TrackerPreheat preheat) {
    Set<Enrollment> enrollments =
        bundle.getTrackerEvents().stream()
            .filter(event -> bundle.findEnrollmentByUid(event.getEnrollment()).isEmpty())
            .map(event -> preheat.getEnrollment(event.getEnrollment()))
            .collect(Collectors.toSet());

    return enrollments.stream()
        .map(
            e -> {
              List<RuleAttributeValue> attributes =
                  getAttributes(UID.of(e), UID.of(e.getTrackedEntity()), bundle, preheat);
              RuleEnrollment enrollment = RuleEngineMapper.mapSavedEnrollment(e, attributes);
              return programRuleEngine.evaluateEnrollmentAndTrackerEvents(
                  enrollment,
                  getEventsFromEnrollment(UID.of(e), bundle, preheat),
                  e.getProgram(),
                  bundle.getUser());
            })
        .reduce(RuleEngineEffects::merge)
        .orElse(RuleEngineEffects.empty());
  }

  private RuleEngineEffects calculateSingleEventRuleEffects(
      TrackerBundle bundle, TrackerPreheat preheat) {
    Map<Program, List<org.hisp.dhis.tracker.imports.domain.SingleEvent>> singleEvents =
        bundle.getSingleEvents().stream()
            .collect(Collectors.groupingBy(event -> preheat.getProgram(event.getProgram())));

    return singleEvents.entrySet().stream()
        .map(
            entry -> {
              List<RuleEvent> events = RuleEngineMapper.mapPayloadEvents(preheat, entry.getValue());

              return programRuleEngine.evaluateSingleEvents(
                  events, entry.getKey(), bundle.getUser());
            })
        .reduce(RuleEngineEffects::merge)
        .orElse(RuleEngineEffects.empty());
  }

  // Get all the attributes linked to enrollment from the payload and the DB,
  // using the one from payload if they are present in both places
  private List<RuleAttributeValue> getAttributes(
      UID enrollmentUid, UID teUid, TrackerBundle bundle, TrackerPreheat preheat) {
    List<RuleAttributeValue> payloadProgramAttributes =
        bundle
            .findEnrollmentByUid(enrollmentUid)
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getAttributes)
            .map(this::filterNullAttributes)
            .map(attributes -> RuleEngineMapper.mapAttributes(preheat, attributes))
            .orElse(Collections.emptyList());

    List<RuleAttributeValue> payloadTrackedEntityAttributes =
        bundle
            .findTrackedEntityByUid(teUid)
            .map(org.hisp.dhis.tracker.imports.domain.TrackedEntity::getAttributes)
            .map(this::filterNullAttributes)
            .map(attributes -> RuleEngineMapper.mapAttributes(preheat, attributes))
            .orElse(Collections.emptyList());

    TrackedEntity trackedEntity = preheat.getTrackedEntity(teUid);
    List<RuleAttributeValue> payloadAttributes =
        Stream.concat(payloadTrackedEntityAttributes.stream(), payloadProgramAttributes.stream())
            .toList();

    if (trackedEntity == null) {
      return payloadAttributes;
    }

    List<String> payloadAttributeValuesIds =
        payloadAttributes.stream().map(RuleAttributeValue::getTrackedEntityAttribute).toList();

    Stream<RuleAttributeValue> dbAttributesNotPresentInPayload =
        trackedEntity.getTrackedEntityAttributeValues().stream()
            .filter(av -> !payloadAttributeValuesIds.contains(av.getAttribute().getUid()))
            .map(av -> new RuleAttributeValue(av.getAttribute().getUid(), av.getValue()));
    return Stream.concat(payloadAttributes.stream(), dbAttributesNotPresentInPayload).toList();
  }

  // Get all the events linked to enrollment from the payload and the DB,
  // using the one from payload if they are present in both places
  @Nonnull
  private List<RuleEvent> getEventsFromEnrollment(
      UID enrollmentUid, TrackerBundle bundle, TrackerPreheat preheat) {
    Stream<Event> events;
    try {
      events =
          trackerEventService
              .findEvents(
                  TrackerEventOperationParams.builder()
                      .fields(TrackerEventFields.all())
                      .orgUnitMode(ACCESSIBLE)
                      .enrollments(Set.of(enrollmentUid))
                      .build())
              .stream()
              .filter(e -> bundle.findTrackerEventByUid(UID.of(e)).isEmpty());
    } catch (BadRequestException | ForbiddenException e) {
      throw new RuntimeException(e);
    }

    List<RuleEvent> ruleEvents =
        RuleEngineMapper.mapPayloadEvents(
            preheat,
            bundle.getTrackerEvents().stream()
                .filter(e -> e.getEnrollment().equals(enrollmentUid))
                .toList());

    return Stream.concat(
            RuleEngineMapper.mapSavedEvents(events.toList()).stream(), ruleEvents.stream())
        .toList();
  }

  private List<Attribute> filterNullAttributes(List<Attribute> attributes) {
    return attributes.stream()
        .filter(Objects::nonNull)
        .filter(attr -> attr.getValue() != null)
        .toList();
  }
}
