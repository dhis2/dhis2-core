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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.rules.api.RuleContextRequirements;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.rules.models.RuleVariableAttribute;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.tracker.imports.programrule.engine.ProgramRuleEntityMapperService;
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

  private final ProgramRuleEntityMapperService mapperService;

  private final TrackerEventService trackerEventService;

  private final ConstantService constantService;

  private final org.hisp.dhis.programrule.ProgramRuleService programRuleMetadataService;

  private final ProgramRuleVariableService programRuleVariableService;

  private final RuleActionEnrollmentMapper ruleActionEnrollmentMapper;

  private final RuleActionEventMapper ruleActionEventMapper;

  /**
   * Calculates rule effects for all enrollments, tracker events, and single events in the payload.
   *
   * <p>Payload enrollments and tracker events whose enrollment is not in the payload are grouped by
   * program so that the rule engine is called at most once per distinct program. Three evaluation
   * paths are chosen based on the rule set's declared requirements:
   *
   * <ul>
   *   <li>{@code !needsEnrollment} — evaluated as single events; no enrollment context or saved
   *       events are loaded from the database.
   *   <li>{@code needsEnrollment && !needsAllEvents} — evaluated with enrollment context; only
   *       payload events are passed, so no saved events are fetched from the database.
   *   <li>{@code needsEnrollment && needsAllEvents} — evaluated with full context; saved events are
   *       fetched from the database and combined with payload events.
   * </ul>
   *
   * <p>Single events are evaluated separately per program.
   */
  @Override
  @Transactional(readOnly = true)
  public void calculateRuleEffects(TrackerBundle bundle, TrackerPreheat preheat) {
    // Deduplicate UIDs before hitting preheat: many enrollments/events typically share the same
    // program, so look up each distinct program UID and enrollment UID at most once.
    Set<Program> allPrograms = getAllProgramsFromPayload(bundle, preheat);

    Map<Program, ProgramRuleContext> contextByProgram = getRulesForPrograms(allPrograms);
    if (contextByProgram.isEmpty()) {
      return;
    }

    Map<String, String> constantMap =
        constantService.getConstantMap().entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> Double.toString(v.getValue().getValue())));

    RuleEngineEffects ruleEffects =
        RuleEngineEffects.merge(
            calculateEnrollmentRuleEffects(bundle, preheat, constantMap, contextByProgram),
            calculateSingleEventRuleEffects(bundle, preheat, constantMap, contextByProgram));

    bundle.setEnrollmentNotifications(ruleEffects.getEnrollmentNotifications());
    bundle.setTrackerEventNotifications(ruleEffects.getEventNotifications());
    bundle.setEnrollmentRuleActionExecutors(
        ruleActionEnrollmentMapper.mapRuleEffects(
            ruleEffects.getEnrollmentValidationEffects(), bundle));
    bundle.setEventRuleActionExecutors(
        ruleActionEventMapper.mapRuleEffects(ruleEffects.getEventValidationEffects(), bundle));
  }

  private static Set<Program> getAllProgramsFromPayload(
      TrackerBundle bundle, TrackerPreheat preheat) {
    Set<MetadataIdentifier> programIdentifiers = new HashSet<>();
    bundle.getEnrollments().forEach(e -> programIdentifiers.add(e.getProgram()));
    bundle.getSingleEvents().forEach(e -> programIdentifiers.add(e.getProgram()));

    Set<UID> trackerEventEnrollmentUids =
        bundle.getTrackerEvents().stream()
            .map(TrackerEvent::getEnrollment)
            .collect(Collectors.toSet());

    trackerEventEnrollmentUids.stream()
        .map(preheat::getEnrollment)
        .filter(Objects::nonNull)
        .forEach(e -> programIdentifiers.add(MetadataIdentifier.ofUid(e.getProgram())));

    Set<Program> allPrograms = new HashSet<>(programIdentifiers.size());
    for (MetadataIdentifier identifier : programIdentifiers) {
      allPrograms.add(preheat.getProgram(identifier));
    }

    return allPrograms;
  }

  private RuleEngineEffects calculateEnrollmentRuleEffects(
      TrackerBundle bundle,
      TrackerPreheat preheat,
      Map<String, String> constantMap,
      Map<Program, ProgramRuleContext> contextByProgram) {
    Set<UID> payloadEnrollmentUids =
        bundle.getEnrollments().stream()
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUID)
            .collect(Collectors.toSet());
    Map<Program, List<org.hisp.dhis.tracker.imports.domain.Enrollment>>
        payloadEnrollmentsByProgram =
            bundle.getEnrollments().stream()
                .filter(e -> contextByProgram.containsKey(preheat.getProgram(e.getProgram())))
                .collect(Collectors.groupingBy(e -> preheat.getProgram(e.getProgram())));
    Map<Program, List<Enrollment>> savedEnrollmentsByProgram =
        bundle.getTrackerEvents().stream()
            .filter(event -> !payloadEnrollmentUids.contains(event.getEnrollment()))
            .map(event -> preheat.getEnrollment(event.getEnrollment()))
            .filter(Objects::nonNull)
            .distinct()
            .filter(e -> contextByProgram.containsKey(e.getProgram()))
            .collect(Collectors.groupingBy(Enrollment::getProgram));

    if (payloadEnrollmentsByProgram.isEmpty() && savedEnrollmentsByProgram.isEmpty()) {
      return RuleEngineEffects.empty();
    }

    // Saved events are only needed when the rule set references variables that depend on all events
    // (e.g. newest-event, previous-event, event_count). Skipping this DB query for other programs
    // is the main performance saving of the !needsAllEvents path.
    Set<UID> enrollmentUidsNeedingAllEvents =
        Stream.concat(
                payloadEnrollmentsByProgram.entrySet().stream()
                    .filter(e -> contextByProgram.get(e.getKey()).needsAllEvents())
                    .flatMap(e -> e.getValue().stream())
                    .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getUID),
                savedEnrollmentsByProgram.entrySet().stream()
                    .filter(e -> contextByProgram.get(e.getKey()).needsAllEvents())
                    .flatMap(e -> e.getValue().stream())
                    .map(Enrollment::getUID))
            .collect(Collectors.toSet());
    Map<UID, List<RuleEvent>> savedEventsByEnrollment =
        fetchSavedRuleEventsByEnrollment(enrollmentUidsNeedingAllEvents, bundle);
    Map<UID, List<TrackerEvent>> payloadEventsByEnrollment =
        bundle.getTrackerEvents().stream()
            .collect(Collectors.groupingBy(TrackerEvent::getEnrollment));

    return contextByProgram.entrySet().stream()
        .filter(
            entry ->
                payloadEnrollmentsByProgram.containsKey(entry.getKey())
                    || savedEnrollmentsByProgram.containsKey(entry.getKey()))
        .map(
            entry -> {
              Program program = entry.getKey();
              ProgramRuleContext ctx = entry.getValue();
              Map<RuleEnrollment, List<RuleEvent>> enrollmentsWithEvents =
                  buildEnrollmentsWithEvents(
                      program,
                      ctx.needsTeAttributes(),
                      payloadEnrollmentsByProgram,
                      savedEnrollmentsByProgram,
                      savedEventsByEnrollment,
                      payloadEventsByEnrollment,
                      bundle,
                      preheat);

              return programRuleEngine.evaluateEnrollmentsAndTrackerEvents(
                  enrollmentsWithEvents,
                  bundle.getUser(),
                  constantMap,
                  ctx.rules(),
                  ctx.variables());
            })
        .reduce(RuleEngineEffects::merge)
        .orElse(RuleEngineEffects.empty());
  }

  /**
   * Rules and variables for a single program, mapped to rule-engine types once so that neither the
   * analyzer nor the evaluation calls need to re-convert them.
   *
   * <p>{@code needsEnrollment} is {@code true} when the rules reference enrollment context
   * (enrollment-specific environment variables or TEI attributes), so the engine must be invoked
   * with a full {@link RuleEnrollment} rather than as single events.
   *
   * <p>{@code needsAllEvents} is {@code true} when the rules reference variables that require
   * knowledge of all events (newest-event or previous-event variables, or {@code V{event_count}}).
   */
  private record ProgramRuleContext(
      List<Rule> rules,
      List<RuleVariable> variables,
      boolean needsTeAttributes,
      boolean needsAllEvents) {}

  // Fetches rules and variables for each program; maps them to rule-engine types once;
  // skips programs with no applicable rules.
  private Map<Program, ProgramRuleContext> getRulesForPrograms(Set<Program> programs) {
    Map<Program, ProgramRuleContext> contextByProgram = new HashMap<>();
    for (Program program : programs) {
      List<ProgramRule> programRules =
          programRuleMetadataService.getProgramRulesByActionTypes(program, SERVER_SUPPORTED_TYPES);
      if (!programRules.isEmpty()) {
        List<Rule> rules = mapperService.toRules(programRules);
        List<RuleVariable> variables =
            mapperService.toRuleVariables(
                programRuleVariableService.getProgramRuleVariable(program));
        RuleContextRequirements requirements =
            programRuleEngine.analyzeContextRequirements(rules, variables);
        boolean needsTeAttributes =
            variables.stream().anyMatch(v -> v instanceof RuleVariableAttribute);
        contextByProgram.put(
            program,
            new ProgramRuleContext(
                rules, variables, needsTeAttributes, requirements.getNeedsAllEvents()));
      }
    }
    return contextByProgram;
  }

  // Builds the map of rule enrollments to rule events for a single program,
  // combining payload and saved enrollments with their respective events.
  // Attribute loading is skipped when the program has no TEI_ATTRIBUTE variables.
  private Map<RuleEnrollment, List<RuleEvent>> buildEnrollmentsWithEvents(
      Program program,
      boolean needsTeAttributes,
      Map<Program, List<org.hisp.dhis.tracker.imports.domain.Enrollment>> payloadByProgram,
      Map<Program, List<Enrollment>> savedByProgram,
      Map<UID, List<RuleEvent>> savedEventsByEnrollment,
      Map<UID, List<TrackerEvent>> payloadEventsByEnrollment,
      TrackerBundle bundle,
      TrackerPreheat preheat) {
    Map<RuleEnrollment, List<RuleEvent>> enrollmentsWithEvents = new HashMap<>();

    for (org.hisp.dhis.tracker.imports.domain.Enrollment e :
        payloadByProgram.getOrDefault(program, List.of())) {
      List<RuleAttributeValue> attributes =
          needsTeAttributes
              ? getAttributes(e.getEnrollment(), e.getTrackedEntity(), bundle, preheat)
              : List.of();
      enrollmentsWithEvents.put(
          RuleEngineMapper.mapPayloadEnrollment(preheat, e, attributes),
          buildRuleEvents(
              savedEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              payloadEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              preheat));
    }

    for (Enrollment e : savedByProgram.getOrDefault(program, List.of())) {
      List<RuleAttributeValue> attributes =
          needsTeAttributes
              ? getAttributes(e.getUID(), e.getTrackedEntity().getUID(), bundle, preheat)
              : List.of();
      enrollmentsWithEvents.put(
          RuleEngineMapper.mapSavedEnrollment(e, attributes),
          buildRuleEvents(
              savedEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              payloadEventsByEnrollment.getOrDefault(e.getUID(), List.of()),
              preheat));
    }

    return enrollmentsWithEvents;
  }

  private RuleEngineEffects calculateSingleEventRuleEffects(
      TrackerBundle bundle,
      TrackerPreheat preheat,
      Map<String, String> constantMap,
      Map<Program, ProgramRuleContext> contextByProgram) {
    return bundle.getSingleEvents().stream()
        .collect(Collectors.groupingBy(event -> preheat.getProgram(event.getProgram())))
        .entrySet()
        .stream()
        .flatMap(
            entry -> {
              ProgramRuleContext ctx = contextByProgram.get(entry.getKey());
              if (ctx == null) {
                return Stream.empty();
              }
              List<RuleEvent> events =
                  RuleEngineMapper.mapPayloadSingleEvents(preheat, entry.getValue());
              return Stream.of(
                  programRuleEngine.evaluateSingleEvents(
                      events, bundle.getUser(), constantMap, ctx.rules(), ctx.variables()));
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
