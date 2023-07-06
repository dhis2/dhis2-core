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
package org.hisp.dhis.tracker.programrule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerProgramRuleService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.RuleEngineConverterService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Enrico Colasante
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerProgramRuleService implements TrackerProgramRuleService {
  @NonNull
  @Qualifier("serviceTrackerRuleEngine")
  private final ProgramRuleEngine programRuleEngine;

  @NonNull
  private final RuleEngineConverterService<Enrollment, ProgramInstance>
      enrollmentTrackerConverterService;

  @NonNull
  private final RuleEngineConverterService<Event, ProgramStageInstance>
      eventTrackerConverterService;

  @NonNull
  private final TrackerConverterService<Attribute, TrackedEntityAttributeValue>
      attributeValueTrackerConverterService;

  /**
   * This method is calling rule engine for every enrollment and all the linked events, for all
   * events linked to an enrollment not present in the payload and for all the program events.
   *
   * @param bundle The bundle to build the context for rule engine
   * @return A list of rule effects for every enrollment and event present in the payload
   */
  @Override
  @Transactional(readOnly = true)
  public List<RuleEffects> calculateRuleEffects(TrackerBundle bundle) {
    Stream<RuleEffects> enrollmentStream =
        bundle.getEnrollments().stream()
            .flatMap(
                e -> {
                  ProgramInstance enrollment =
                      enrollmentTrackerConverterService.fromForRuleEngine(bundle.getPreheat(), e);

                  return programRuleEngine
                      .evaluateEnrollmentAndEvents(
                          enrollment,
                          getEventsFromEnrollment(enrollment.getUid(), bundle),
                          getAttributes(e, bundle))
                      .stream();
                });

    return Stream.concat(enrollmentStream, calculateEventRuleEffects(bundle).stream())
        .collect(Collectors.toList());
  }

  // Get all the attributes linked to enrollment from the payload and the DB,
  // using the one from payload
  // if they are present in both places
  private List<TrackedEntityAttributeValue> getAttributes(
      Enrollment enrollment, TrackerBundle bundle) {
    List<TrackedEntityAttributeValue> attributeValues =
        attributeValueTrackerConverterService.from(bundle.getPreheat(), enrollment.getAttributes());

    TrackedEntityInstance trackedEntity =
        bundle.getPreheat().getTrackedEntity(enrollment.getTrackedEntity());

    List<TrackedEntityAttributeValue> payloadAttributeValues =
        bundle
            .getTrackedEntity(enrollment.getTrackedEntity())
            .map(
                tei ->
                    attributeValueTrackerConverterService.from(
                        bundle.getPreheat(), tei.getAttributes()))
            .orElse(Lists.newArrayList());
    attributeValues.addAll(payloadAttributeValues);

    if (trackedEntity != null) {
      List<String> payloadAttributeValuesIds =
          payloadAttributeValues.stream()
              .map(av -> av.getAttribute().getUid())
              .collect(Collectors.toList());

      attributeValues.addAll(
          trackedEntity.getTrackedEntityAttributeValues().stream()
              .filter(av -> !payloadAttributeValuesIds.contains(av.getAttribute().getUid()))
              .collect(Collectors.toList()));
    }

    return attributeValues;
  }

  private List<RuleEffects> calculateEventRuleEffects(TrackerBundle bundle) {
    List<String> enrollmentUids =
        bundle.getEnrollments().stream()
            .map(Enrollment::getEnrollment)
            .collect(Collectors.toList());

    Map<String, List<Event>> eventsByEnrollment =
        bundle.getEvents().stream()
            .filter(event -> !enrollmentUids.contains(event.getEnrollment()))
            .filter(event -> StringUtils.isNotEmpty(event.getEnrollment()))
            .collect(Collectors.groupingBy(Event::getEnrollment));

    return eventsByEnrollment.entrySet().stream()
        .flatMap(
            entry -> {
              ProgramInstance enrollment = getEnrollment(bundle, entry.getKey());
              List<ProgramStageInstance> programStageInstances =
                  eventTrackerConverterService.fromForRuleEngine(
                      bundle.getPreheat(), entry.getValue());
              if (enrollment == null) {
                return programRuleEngine
                    .evaluateProgramEvents(
                        Sets.newHashSet(programStageInstances),
                        getProgramFromEvent(bundle.getPreheat(), entry.getValue().get(0)))
                    .stream();
              } else {
                List<TrackedEntityAttributeValue> attributeValues =
                    bundle.getEnrollments().stream()
                        .filter(e -> entry.getKey().equals(e.getEnrollment()))
                        .findAny()
                        .map(e -> getAttributes(e, bundle))
                        .orElse(Collections.EMPTY_LIST);
                return programRuleEngine
                    .evaluateEnrollmentAndEvents(
                        enrollment,
                        getEventsFromEnrollment(enrollment.getUid(), bundle),
                        attributeValues)
                    .stream();
              }
            })
        .collect(Collectors.toList());
  }

  private Program getProgramFromEvent(TrackerPreheat preheat, Event event) {
    return preheat.getProgram(event.getProgram());
  }

  private ProgramInstance getEnrollment(TrackerBundle bundle, String enrollmentUid) {
    return bundle.getPreheat().getEnrollment(enrollmentUid);
  }

  private Set<ProgramStageInstance> getEventsFromEnrollment(
      String enrollment, TrackerBundle bundle) {
    List<String> bundleEventUids =
        bundle.getEvents().stream().map(Event::getUid).collect(Collectors.toList());

    // Get all programStageInstances from preheat that are linked to
    // enrollment
    // and are not present in the payload
    Stream<ProgramStageInstance> programStageInstances =
        bundle.getPreheat().getEvents().values().stream()
            .filter(e -> e.getProgramInstance().getUid().equals(enrollment))
            .filter(e -> !bundleEventUids.contains(e.getUid()));

    // All events in the payload that are linked to enrollment
    Stream<ProgramStageInstance> bundleEvents =
        bundle.getEvents().stream()
            .filter(e -> e.getEnrollment().equals(enrollment))
            .map(
                event ->
                    eventTrackerConverterService.fromForRuleEngine(bundle.getPreheat(), event));

    return Stream.concat(programStageInstances, bundleEvents).collect(Collectors.toSet());
  }
}
