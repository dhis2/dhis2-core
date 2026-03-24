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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.tracker.imports.programrule.engine.RuleEngineEffects;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
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
   * Calculates rule effects for all enrollments, tracker events, and single events in the payload.
   *
   * <p>Payload enrollments and tracker events whose enrollment is not in the payload are grouped by
   * program so that {@link ProgramRuleEngine#evaluateEnrollmentsAndTrackerEvents} is called at most
   * once per distinct program. Single events are evaluated separately per program.
   */
  @Override
  @Transactional(readOnly = true)
  public void calculateRuleEffects(TrackerBundle bundle, TrackerPreheat preheat) {
    RuleEngineEffects ruleEffects =
        RuleEngineEffects.merge(
            calculateEnrollmentRuleEffects(bundle, preheat),
            calculateSingleEventRuleEffects(bundle, preheat));

    bundle.setEnrollmentNotifications(ruleEffects.getEnrollmentNotifications());
    bundle.setTrackerEventNotifications(ruleEffects.getEventNotifications());
    bundle.setEnrollmentRuleActionExecutors(
        ruleActionEnrollmentMapper.mapRuleEffects(
            ruleEffects.getEnrollmentValidationEffects(), bundle));
    bundle.setEventRuleActionExecutors(
        ruleActionEventMapper.mapRuleEffects(ruleEffects.getEventValidationEffects(), bundle));
  }

  // Evaluates rule effects for all tracker enrollments (payload and saved) grouped by program,
  // ensuring evaluateEnrollmentsAndTrackerEvents is called at most once per distinct program.
  // Saved enrollments are those referenced by payload tracker events whose enrollment is not
  // itself in the payload, avoiding duplicate evaluation.
  private RuleEngineEffects calculateEnrollmentRuleEffects(
      TrackerBundle bundle, TrackerPreheat preheat) {
    Set<UID> payloadEnrollmentUids =
        bundle.getEnrollments().stream()
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUID)
            .collect(Collectors.toSet());

    Set<Enrollment> savedEnrollments =
        bundle.getTrackerEvents().stream()
            .filter(event -> bundle.findEnrollmentByUid(event.getEnrollment()).isEmpty())
            .map(event -> preheat.getEnrollment(event.getEnrollment()))
            .collect(Collectors.toSet());

    Set<UID> allEnrollmentUids =
        Stream.concat(
                payloadEnrollmentUids.stream(), savedEnrollments.stream().map(Enrollment::getUID))
            .collect(Collectors.toSet());

    Map<UID, List<RuleEvent>> savedEventsByEnrollment =
        fetchSavedRuleEventsByEnrollment(allEnrollmentUids, bundle);

    Map<UID, List<TrackerEvent>> payloadEventsByEnrollment =
        bundle.getTrackerEvents().stream()
            .collect(Collectors.groupingBy(TrackerEvent::getEnrollment));

    Map<Program, Map<RuleEnrollment, List<RuleEvent>>> byProgram = new HashMap<>();

    for (org.hisp.dhis.tracker.imports.domain.Enrollment e : bundle.getEnrollments()) {
      List<RuleAttributeValue> attributes =
          getAttributes(e.getEnrollment(), e.getTrackedEntity(), bundle, preheat);
      RuleEnrollment enrollment = RuleEngineMapper.mapPayloadEnrollment(preheat, e, attributes);
      List<RuleEvent> events =
          buildRuleEvents(
              savedEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              payloadEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              preheat);
      byProgram
          .computeIfAbsent(preheat.getProgram(e.getProgram()), k -> new HashMap<>())
          .put(enrollment, events);
    }

    for (Enrollment e : savedEnrollments) {
      List<RuleAttributeValue> attributes =
          getAttributes(e.getUID(), e.getTrackedEntity().getUID(), bundle, preheat);
      RuleEnrollment enrollment = RuleEngineMapper.mapSavedEnrollment(e, attributes);
      List<RuleEvent> events =
          buildRuleEvents(
              savedEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              payloadEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              preheat);
      byProgram.computeIfAbsent(e.getProgram(), k -> new HashMap<>()).put(enrollment, events);
    }

    return byProgram.entrySet().stream()
        .map(
            entry ->
                programRuleEngine.evaluateEnrollmentsAndTrackerEvents(
                    entry.getValue(), entry.getKey(), bundle.getUser()))
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
              List<RuleEvent> events =
                  RuleEngineMapper.mapPayloadSingleEvents(preheat, entry.getValue());

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

    Set<String> payloadAttributeValuesIds =
        payloadAttributes.stream()
            .map(RuleAttributeValue::getTrackedEntityAttribute)
            .collect(Collectors.toSet());

    Stream<RuleAttributeValue> dbAttributesNotPresentInPayload =
        trackedEntity.getTrackedEntityAttributeValues().stream()
            .filter(av -> !payloadAttributeValuesIds.contains(av.getAttribute().getUid()))
            .map(av -> new RuleAttributeValue(av.getAttribute().getUid(), av.getValue()));
    return Stream.concat(payloadAttributes.stream(), dbAttributesNotPresentInPayload).toList();
  }

  // Fetch all saved events for the given enrollments in a single DB query and group by enrollment.
  // Payload events (already in the bundle) are excluded since they will be added in
  // buildRuleEvents.
  private Map<UID, List<RuleEvent>> fetchSavedRuleEventsByEnrollment(
      Set<UID> enrollmentUids, TrackerBundle bundle) {
    if (enrollmentUids.isEmpty()) {
      return Map.of();
    }
    try {
      return trackerEventService
          .findEvents(TrackerEventOperationParams.builderForEnrollments(enrollmentUids).build())
          .stream()
          .filter(e -> bundle.findTrackerEventByUid(e.getUID()).isEmpty())
          .collect(
              Collectors.groupingBy(
                  e -> e.getEnrollment().getUID(),
                  Collectors.collectingAndThen(
                      Collectors.toList(), RuleEngineMapper::mapSavedEvents)));
    } catch (BadRequestException | ForbiddenException e) {
      throw new RuntimeException(e);
    }
  }

  // Combine pre-fetched saved events with payload events for a single enrollment.
  private List<RuleEvent> buildRuleEvents(
      List<RuleEvent> savedRuleEvents, List<TrackerEvent> payloadEvents, TrackerPreheat preheat) {
    List<RuleEvent> payloadRuleEvents =
        RuleEngineMapper.mapPayloadTrackerEvents(preheat, payloadEvents);
    return Stream.concat(savedRuleEvents.stream(), payloadRuleEvents.stream()).toList();
  }

  private List<Attribute> filterNullAttributes(List<Attribute> attributes) {
    return attributes.stream()
        .filter(Objects::nonNull)
        .filter(attr -> attr.getValue() != null)
        .toList();
  }
}
