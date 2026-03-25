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
import static org.hisp.dhis.programrule.ProgramRuleActionType.SERVER_SUPPORTED_TYPES;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.event.EventFields;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventService;
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

  private final EventService eventService;

  private final ConstantService constantService;

  private final org.hisp.dhis.programrule.ProgramRuleService programRuleMetadataService;

  private final RuleActionEnrollmentMapper ruleActionEnrollmentMapper;

  private final RuleActionEventMapper ruleActionEventMapper;

  /**
   * Calculates rule effects for all enrollments, tracker events, and single events in the payload.
   *
   * <p>All programs referenced by the bundle are collected in a single pass and their rules fetched
   * at once. The constant map is allocated only when at least one program has applicable rules.
   * Enrollments and tracker events whose enrollment is not in the payload are grouped by program so
   * that {@link ProgramRuleEngine#evaluateEnrollmentsAndTrackerEvents} is called at most once per
   * distinct program. Single events are evaluated separately per program.
   */
  @Override
  @Transactional(readOnly = true)
  public void calculateRuleEffects(TrackerBundle bundle, TrackerPreheat preheat) {
    // Collect all programs referenced by the bundle in one pass, then fetch rules for all of them
    // at once. This avoids per-program DB queries inside the sub-methods and lets us skip the
    // constant map allocation entirely when no program has applicable rules.
    Set<Program> allPrograms =
        Stream.concat(
                bundle.getEnrollments().stream().map(e -> preheat.getProgram(e.getProgram())),
                bundle.getEvents().stream().map(e -> preheat.getProgram(e.getProgram())))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Program, List<ProgramRule>> rulesByProgram = getRulesForPrograms(allPrograms);
    if (rulesByProgram.isEmpty()) {
      return;
    }

    Map<String, String> constantMap =
        constantService.getConstantMap().entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> Double.toString(v.getValue().getValue())));

    RuleEngineEffects ruleEffects =
        RuleEngineEffects.merge(
            calculateEnrollmentRuleEffects(bundle, preheat, constantMap, rulesByProgram),
            calculateProgramEventRuleEffects(bundle, preheat, constantMap, rulesByProgram));

    bundle.setEnrollmentNotifications(ruleEffects.getEnrollmentNotifications());
    bundle.setEventNotifications(ruleEffects.getEventNotifications());
    bundle.setEnrollmentRuleActionExecutors(
        ruleActionEnrollmentMapper.mapRuleEffects(
            ruleEffects.getEnrollmentValidationEffects(), bundle));
    bundle.setEventRuleActionExecutors(
        ruleActionEventMapper.mapRuleEffects(ruleEffects.getEventValidationEffects(), bundle));
  }

  private RuleEngineEffects calculateEnrollmentRuleEffects(
      TrackerBundle bundle,
      TrackerPreheat preheat,
      Map<String, String> constantMap,
      Map<Program, List<ProgramRule>> rulesByProgram) {
    Set<UID> payloadEnrollmentUids =
        bundle.getEnrollments().stream()
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUid)
            .collect(Collectors.toSet());
    Map<Program, List<org.hisp.dhis.tracker.imports.domain.Enrollment>>
        payloadEnrollmentsByProgram =
            bundle.getEnrollments().stream()
                .filter(e -> rulesByProgram.containsKey(preheat.getProgram(e.getProgram())))
                .collect(Collectors.groupingBy(e -> preheat.getProgram(e.getProgram())));
    Map<Program, List<Enrollment>> savedEnrollmentsByProgram =
        bundle.getEvents().stream()
            .filter(event -> preheat.getProgram(event.getProgram()).isRegistration())
            .filter(event -> !payloadEnrollmentUids.contains(event.getEnrollment()))
            .map(event -> preheat.getEnrollment(event.getEnrollment()))
            .filter(Objects::nonNull)
            .distinct()
            .filter(e -> rulesByProgram.containsKey(e.getProgram()))
            .collect(Collectors.groupingBy(Enrollment::getProgram));

    if (payloadEnrollmentsByProgram.isEmpty() && savedEnrollmentsByProgram.isEmpty()) {
      return RuleEngineEffects.empty();
    }

    Set<UID> enrollmentUids =
        Stream.concat(
                payloadEnrollmentsByProgram.values().stream()
                    .flatMap(List::stream)
                    .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUid),
                savedEnrollmentsByProgram.values().stream().flatMap(List::stream).map(UID::of))
            .collect(Collectors.toSet());
    Map<UID, List<RuleEvent>> savedEventsByEnrollment =
        fetchSavedRuleEventsByEnrollment(enrollmentUids, bundle);
    Map<UID, List<org.hisp.dhis.tracker.imports.domain.Event>> payloadEventsByEnrollment =
        bundle.getEvents().stream()
            .filter(event -> preheat.getProgram(event.getProgram()).isRegistration())
            .collect(Collectors.groupingBy(e -> e.getEnrollment()));

    return rulesByProgram.entrySet().stream()
        .filter(
            entry ->
                payloadEnrollmentsByProgram.containsKey(entry.getKey())
                    || savedEnrollmentsByProgram.containsKey(entry.getKey()))
        .map(
            entry -> {
              Program program = entry.getKey();
              Map<RuleEnrollment, List<RuleEvent>> enrollmentsWithEvents =
                  buildEnrollmentsWithEvents(
                      program,
                      payloadEnrollmentsByProgram,
                      savedEnrollmentsByProgram,
                      savedEventsByEnrollment,
                      payloadEventsByEnrollment,
                      bundle,
                      preheat);
              return programRuleEngine.evaluateEnrollmentsAndTrackerEvents(
                  enrollmentsWithEvents, program, bundle.getUser(), constantMap, entry.getValue());
            })
        .reduce(RuleEngineEffects::merge)
        .orElse(RuleEngineEffects.empty());
  }

  // Fetches rules for each program and returns only programs that have applicable rules.
  private Map<Program, List<ProgramRule>> getRulesForPrograms(Set<Program> programs) {
    Map<Program, List<ProgramRule>> rulesByProgram = new HashMap<>();
    for (Program program : programs) {
      List<ProgramRule> rules =
          programRuleMetadataService.getProgramRulesByActionTypes(program, SERVER_SUPPORTED_TYPES);
      if (!rules.isEmpty()) {
        rulesByProgram.put(program, rules);
      }
    }
    return rulesByProgram;
  }

  // Builds the map of rule enrollments to rule events for a single program,
  // combining payload and saved enrollments with their respective events.
  private Map<RuleEnrollment, List<RuleEvent>> buildEnrollmentsWithEvents(
      Program program,
      Map<Program, List<org.hisp.dhis.tracker.imports.domain.Enrollment>> payloadByProgram,
      Map<Program, List<Enrollment>> savedByProgram,
      Map<UID, List<RuleEvent>> savedEventsByEnrollment,
      Map<UID, List<org.hisp.dhis.tracker.imports.domain.Event>> payloadEventsByEnrollment,
      TrackerBundle bundle,
      TrackerPreheat preheat) {
    Map<RuleEnrollment, List<RuleEvent>> enrollmentsWithEvents = new HashMap<>();

    for (org.hisp.dhis.tracker.imports.domain.Enrollment e :
        payloadByProgram.getOrDefault(program, List.of())) {
      List<RuleAttributeValue> attributes =
          getAttributes(e.getEnrollment(), e.getTrackedEntity(), bundle, preheat);
      enrollmentsWithEvents.put(
          RuleEngineMapper.mapPayloadEnrollment(preheat, e, attributes),
          buildRuleEvents(
              savedEventsByEnrollment.getOrDefault(e.getUid(), List.of()),
              payloadEventsByEnrollment.getOrDefault(e.getUid(), List.of()),
              preheat));
    }

    for (Enrollment e : savedByProgram.getOrDefault(program, List.of())) {
      List<RuleAttributeValue> attributes =
          getAttributes(UID.of(e), UID.of(e.getTrackedEntity()), bundle, preheat);
      enrollmentsWithEvents.put(
          RuleEngineMapper.mapSavedEnrollment(e, attributes),
          buildRuleEvents(
              savedEventsByEnrollment.getOrDefault(UID.of(e), List.of()),
              payloadEventsByEnrollment.getOrDefault(UID.of(e), List.of()),
              preheat));
    }

    return enrollmentsWithEvents;
  }

  private RuleEngineEffects calculateProgramEventRuleEffects(
      TrackerBundle bundle,
      TrackerPreheat preheat,
      Map<String, String> constantMap,
      Map<Program, List<ProgramRule>> rulesByProgram) {
    return bundle.getEvents().stream()
        .filter(event -> preheat.getProgram(event.getProgram()).isWithoutRegistration())
        .collect(Collectors.groupingBy(event -> preheat.getProgram(event.getProgram())))
        .entrySet()
        .stream()
        .flatMap(
            entry -> {
              List<ProgramRule> rules = rulesByProgram.get(entry.getKey());
              if (rules == null) {
                return Stream.empty();
              }
              List<RuleEvent> events = RuleEngineMapper.mapPayloadEvents(preheat, entry.getValue());
              return Stream.of(
                  programRuleEngine.evaluateSingleEvents(
                      events, entry.getKey(), bundle.getUser(), constantMap, rules));
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
    if (trackedEntity == null) {
      return Stream.concat(
              payloadTrackedEntityAttributes.stream(), payloadProgramAttributes.stream())
          .toList();
    }

    // Build the payload UID set directly from the two component lists to avoid materialising
    // payloadAttributes into an intermediate List that is only streamed again below.
    Set<String> payloadAttributeUids = new HashSet<>();
    for (RuleAttributeValue a : payloadTrackedEntityAttributes)
      payloadAttributeUids.add(a.getTrackedEntityAttribute());
    for (RuleAttributeValue a : payloadProgramAttributes)
      payloadAttributeUids.add(a.getTrackedEntityAttribute());

    Stream<RuleAttributeValue> dbAttributesNotPresentInPayload =
        trackedEntity.getTrackedEntityAttributeValues().stream()
            .filter(av -> !payloadAttributeUids.contains(av.getAttribute().getUid()))
            .map(av -> new RuleAttributeValue(av.getAttribute().getUid(), av.getValue()));
    return Stream.concat(
            Stream.concat(
                payloadTrackedEntityAttributes.stream(), payloadProgramAttributes.stream()),
            dbAttributesNotPresentInPayload)
        .toList();
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
      return eventService
          .findEvents(
              EventOperationParams.builder()
                  .fields(EventFields.all())
                  .orgUnitMode(ACCESSIBLE)
                  .enrollments(enrollmentUids)
                  .build())
          .stream()
          .filter(e -> bundle.findEventByUid(UID.of(e)).isEmpty())
          .collect(
              Collectors.groupingBy(
                  e -> UID.of(e.getEnrollment()),
                  Collectors.collectingAndThen(
                      Collectors.toList(), RuleEngineMapper::mapSavedEvents)));
    } catch (BadRequestException | ForbiddenException e) {
      throw new RuntimeException(e);
    }
  }

  // Combine pre-fetched saved events with payload events for a single enrollment.
  private List<RuleEvent> buildRuleEvents(
      List<RuleEvent> savedRuleEvents,
      List<org.hisp.dhis.tracker.imports.domain.Event> payloadEvents,
      TrackerPreheat preheat) {
    List<RuleEvent> payloadRuleEvents = RuleEngineMapper.mapPayloadEvents(preheat, payloadEvents);
    return Stream.concat(savedRuleEvents.stream(), payloadRuleEvents.stream()).toList();
  }

  private List<Attribute> filterNullAttributes(List<Attribute> attributes) {
    return attributes.stream()
        .filter(Objects::nonNull)
        .filter(attr -> attr.getValue() != null)
        .toList();
  }
}
