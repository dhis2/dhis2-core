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
package org.hisp.dhis.tracker.imports.programrule;

import static org.hisp.dhis.programrule.ProgramRuleActionType.SERVER_SUPPORTED_TYPES;

import java.util.ArrayList;
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
import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.RuleEngineConverterService;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Enrico Colasante
 */
@Service
@RequiredArgsConstructor
class DefaultProgramRuleService implements ProgramRuleService {
  @Qualifier("serviceTrackerRuleEngine")
  private final ProgramRuleEngine programRuleEngine;

  private final EventService eventService;

  private final RuleEngineConverterService<
          org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment>
      enrollmentTrackerConverterService;

  private final RuleEngineConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event>
      eventTrackerConverterService;

  private final TrackerConverterService<Attribute, TrackedEntityAttributeValue>
      attributeValueTrackerConverterService;

  private final ConstantService constantService;

  private final org.hisp.dhis.programrule.ProgramRuleService programRuleMetadataService;

  private final ProgramRuleVariableService programRuleVariableService;

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
    // Deduplicate UIDs before hitting preheat: many enrollments/events typically share the same
    // program, so look up each distinct program identifier at most once.
    Set<Program> allPrograms = getAllProgramsFromPayload(bundle, preheat);

    Map<Program, ProgramRuleContext> contextByProgram = getRulesForPrograms(allPrograms);
    if (contextByProgram.isEmpty()) {
      return;
    }

    Map<String, String> constantMap =
        constantService.getConstantMap().entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> Double.toString(v.getValue().getValue())));

    List<RuleEffects> ruleEffects =
        ListUtils.union(
            calculateEnrollmentRuleEffects(bundle, preheat, constantMap, contextByProgram),
            calculateSingleEventRuleEffects(bundle, preheat, constantMap, contextByProgram));

    // This is needed for bundle side effects process
    bundle.setRuleEffects(ruleEffects);

    // These are needed for rule engine validation
    bundle.setEnrollmentRuleActionExecutors(
        ruleActionEnrollmentMapper.mapRuleEffects(ruleEffects, bundle));
    bundle.setEventRuleActionExecutors(ruleActionEventMapper.mapRuleEffects(ruleEffects, bundle));
  }

  private static Set<Program> getAllProgramsFromPayload(
      TrackerBundle bundle, TrackerPreheat preheat) {
    // Deduplicate program identifiers before hitting preheat: many enrollments/events typically
    // share the same program, so look up each distinct program identifier at most once.
    Set<MetadataIdentifier> programIdentifiers = new HashSet<>();
    bundle.getEnrollments().forEach(e -> programIdentifiers.add(e.getProgram()));
    bundle.getEvents().forEach(e -> programIdentifiers.add(e.getProgram()));

    Set<Program> allPrograms = new HashSet<>(programIdentifiers.size());
    for (MetadataIdentifier identifier : programIdentifiers) {
      Program p = preheat.getProgram(identifier);
      if (p != null) allPrograms.add(p);
    }
    return allPrograms;
  }

  /**
   * Rules and variables for a single program, co-fetched once per program so that the engine does
   * not need to re-query variables during context construction.
   */
  private record ProgramRuleContext(
      List<ProgramRule> rules, List<ProgramRuleVariable> variables, boolean needsTeAttributes) {}

  // Fetches rules and variables for each program; skips programs with no applicable rules.
  private Map<Program, ProgramRuleContext> getRulesForPrograms(Set<Program> programs) {
    Map<Program, ProgramRuleContext> contextByProgram = new HashMap<>();
    for (Program program : programs) {
      List<ProgramRule> rules =
          programRuleMetadataService.getProgramRulesByActionTypes(program, SERVER_SUPPORTED_TYPES);
      if (!rules.isEmpty()) {
        List<ProgramRuleVariable> variables =
            programRuleVariableService.getProgramRuleVariable(program);
        boolean needsTeAttributes =
            variables.stream()
                .anyMatch(v -> v.getSourceType() == ProgramRuleVariableSourceType.TEI_ATTRIBUTE);
        contextByProgram.put(program, new ProgramRuleContext(rules, variables, needsTeAttributes));
      }
    }
    return contextByProgram;
  }

  private List<RuleEffects> calculateEnrollmentRuleEffects(
      TrackerBundle bundle,
      TrackerPreheat preheat,
      Map<String, String> constantMap,
      Map<Program, ProgramRuleContext> contextByProgram) {
    Set<String> payloadEnrollmentUids =
        bundle.getEnrollments().stream()
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUid)
            .collect(Collectors.toSet());

    Map<Program, List<org.hisp.dhis.tracker.imports.domain.Enrollment>>
        payloadEnrollmentsByProgram =
            bundle.getEnrollments().stream()
                .filter(e -> contextByProgram.containsKey(preheat.getProgram(e.getProgram())))
                .collect(Collectors.groupingBy(e -> preheat.getProgram(e.getProgram())));

    Map<Program, List<Enrollment>> savedEnrollmentsByProgram =
        bundle.getEvents().stream()
            .filter(event -> preheat.getProgram(event.getProgram()).isRegistration())
            .filter(event -> !payloadEnrollmentUids.contains(event.getEnrollment()))
            .map(event -> preheat.getEnrollment(event.getEnrollment()))
            .filter(Objects::nonNull)
            .distinct()
            .filter(e -> contextByProgram.containsKey(e.getProgram()))
            .collect(Collectors.groupingBy(Enrollment::getProgram));

    if (payloadEnrollmentsByProgram.isEmpty() && savedEnrollmentsByProgram.isEmpty()) {
      return List.of();
    }

    Set<String> enrollmentUids =
        Stream.concat(
                payloadEnrollmentsByProgram.values().stream()
                    .flatMap(List::stream)
                    .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUid),
                savedEnrollmentsByProgram.values().stream()
                    .flatMap(List::stream)
                    .map(Enrollment::getUid))
            .collect(Collectors.toSet());

    Map<String, Set<Event>> savedEventsByEnrollment =
        fetchSavedEventsForEnrollments(enrollmentUids, bundle);

    return contextByProgram.entrySet().stream()
        .filter(
            entry ->
                payloadEnrollmentsByProgram.containsKey(entry.getKey())
                    || savedEnrollmentsByProgram.containsKey(entry.getKey()))
        .flatMap(
            entry -> {
              Program program = entry.getKey();
              ProgramRuleContext ctx = entry.getValue();
              List<ProgramRuleEngine.EnrollmentWithEvents> enrollmentsWithEvents =
                  buildEnrollmentsWithEvents(
                      program,
                      ctx.needsTeAttributes(),
                      payloadEnrollmentsByProgram,
                      savedEnrollmentsByProgram,
                      savedEventsByEnrollment,
                      bundle,
                      preheat);
              return programRuleEngine
                  .evaluateEnrollmentsAndTrackerEvents(
                      enrollmentsWithEvents,
                      UserDetails.fromUser(bundle.getUser()),
                      constantMap,
                      ctx.rules(),
                      ctx.variables())
                  .stream();
            })
        .toList();
  }

  // Builds the list of EnrollmentWithEvents for a single program,
  // combining payload and saved enrollments with their respective events.
  // Attribute loading is skipped when the program has no TEI_ATTRIBUTE variables.
  private List<ProgramRuleEngine.EnrollmentWithEvents> buildEnrollmentsWithEvents(
      Program program,
      boolean needsTeAttributes,
      Map<Program, List<org.hisp.dhis.tracker.imports.domain.Enrollment>> payloadByProgram,
      Map<Program, List<Enrollment>> savedByProgram,
      Map<String, Set<Event>> savedEventsByEnrollment,
      TrackerBundle bundle,
      TrackerPreheat preheat) {
    List<ProgramRuleEngine.EnrollmentWithEvents> result = new ArrayList<>();

    for (org.hisp.dhis.tracker.imports.domain.Enrollment e :
        payloadByProgram.getOrDefault(program, List.of())) {
      Enrollment enrollment = enrollmentTrackerConverterService.fromForRuleEngine(preheat, e);
      Set<Event> allEvents =
          new HashSet<>(savedEventsByEnrollment.getOrDefault(e.getUid(), Set.of()));
      bundle.getEvents().stream()
          .filter(ev -> ev.getEnrollment().equals(e.getUid()))
          .filter(ev -> preheat.getProgram(ev.getProgram()).isRegistration())
          .map(ev -> eventTrackerConverterService.fromForRuleEngine(preheat, ev))
          .forEach(allEvents::add);
      List<TrackedEntityAttributeValue> attributes =
          needsTeAttributes
              ? getAttributes(e.getEnrollment(), e.getTrackedEntity(), bundle, preheat)
              : Collections.emptyList();
      result.add(new ProgramRuleEngine.EnrollmentWithEvents(enrollment, allEvents, attributes));
    }

    for (Enrollment e : savedByProgram.getOrDefault(program, List.of())) {
      Set<Event> allEvents =
          new HashSet<>(savedEventsByEnrollment.getOrDefault(e.getUid(), Set.of()));
      bundle.getEvents().stream()
          .filter(ev -> ev.getEnrollment().equals(e.getUid()))
          .filter(ev -> preheat.getProgram(ev.getProgram()).isRegistration())
          .map(ev -> eventTrackerConverterService.fromForRuleEngine(preheat, ev))
          .forEach(allEvents::add);
      List<TrackedEntityAttributeValue> attributes =
          needsTeAttributes
              ? getAttributes(e.getUid(), e.getTrackedEntity().getUid(), bundle, preheat)
              : Collections.emptyList();
      result.add(new ProgramRuleEngine.EnrollmentWithEvents(e, allEvents, attributes));
    }

    return result;
  }

  private List<RuleEffects> calculateSingleEventRuleEffects(
      TrackerBundle bundle,
      TrackerPreheat preheat,
      Map<String, String> constantMap,
      Map<Program, ProgramRuleContext> contextByProgram) {
    return bundle.getEvents().stream()
        .filter(event -> preheat.getProgram(event.getProgram()).isWithoutRegistration())
        .filter(event -> contextByProgram.containsKey(preheat.getProgram(event.getProgram())))
        .collect(Collectors.groupingBy(event -> preheat.getProgram(event.getProgram())))
        .entrySet()
        .stream()
        .flatMap(
            entry -> {
              ProgramRuleContext ctx = contextByProgram.get(entry.getKey());
              List<Event> events =
                  eventTrackerConverterService.fromForRuleEngine(preheat, entry.getValue());
              return programRuleEngine
                  .evaluateProgramEvents(
                      new HashSet<>(events),
                      entry.getKey(),
                      UserDetails.fromUser(bundle.getUser()),
                      constantMap,
                      ctx.rules(),
                      ctx.variables())
                  .stream();
            })
        .toList();
  }

  // Fetch all saved events for the given enrollments in a single DB query and group by enrollment.
  // Payload events (already in the bundle) are excluded since they will be added in
  // buildEnrollmentsWithEvents.
  private Map<String, Set<Event>> fetchSavedEventsForEnrollments(
      Set<String> enrollmentUids, TrackerBundle bundle) {
    if (enrollmentUids.isEmpty()) {
      return Map.of();
    }
    try {
      return eventService
          .getEvents(EventOperationParams.builder().enrollments(enrollmentUids).build())
          .stream()
          .filter(e -> bundle.findEventByUid(e.getUid()).isEmpty())
          .collect(Collectors.groupingBy(e -> e.getEnrollment().getUid(), Collectors.toSet()));
    } catch (BadRequestException | ForbiddenException e) {
      throw new RuntimeException(e);
    }
  }

  // Get all the attributes linked to enrollment from the payload and the DB,
  // using the one from payload if they are present in both places
  private List<TrackedEntityAttributeValue> getAttributes(
      String enrollmentUid, String teUid, TrackerBundle bundle, TrackerPreheat preheat) {
    List<TrackedEntityAttributeValue> attributeValues =
        bundle
            .findEnrollmentByUid(enrollmentUid)
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getAttributes)
            .map(this::filterNullAttributes)
            .map(attributes -> attributeValueTrackerConverterService.from(preheat, attributes))
            .orElse(new ArrayList<>());

    List<TrackedEntityAttributeValue> payloadAttributeValues =
        bundle
            .findTrackedEntityByUid(teUid)
            .map(org.hisp.dhis.tracker.imports.domain.TrackedEntity::getAttributes)
            .map(this::filterNullAttributes)
            .map(attributes -> attributeValueTrackerConverterService.from(preheat, attributes))
            .orElse(Collections.emptyList());
    attributeValues.addAll(payloadAttributeValues);

    TrackedEntity trackedEntity = preheat.getTrackedEntity(teUid);

    if (trackedEntity != null) {
      List<String> payloadAttributeValuesIds =
          payloadAttributeValues.stream().map(av -> av.getAttribute().getUid()).toList();

      attributeValues.addAll(
          trackedEntity.getTrackedEntityAttributeValues().stream()
              .filter(av -> !payloadAttributeValuesIds.contains(av.getAttribute().getUid()))
              .toList());
    }

    return attributeValues;
  }

  private List<Attribute> filterNullAttributes(List<Attribute> attributes) {
    return attributes.stream()
        .filter(Objects::nonNull)
        .filter(attr -> attr.getValue() != null)
        .toList();
  }
}
