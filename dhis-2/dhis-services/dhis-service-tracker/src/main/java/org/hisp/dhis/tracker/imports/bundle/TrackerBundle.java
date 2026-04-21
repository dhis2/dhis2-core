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
package org.hisp.dhis.tracker.imports.bundle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.FlushMode;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@AllArgsConstructor
public class TrackerBundle {
  /** User making the import */
  private UserDetails user;

  /** Should import be imported or just validated. */
  @Builder.Default private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

  /** Sets import strategy (create, update, etc). */
  @Builder.Default private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE;

  /** Should text pattern validation be skipped or not, default is not. */
  @JsonProperty private boolean skipTextPatternValidation;

  /** Should side effects be skipped or not, default is not. */
  @JsonProperty private boolean skipSideEffects;

  /** Should rule engine call be skipped or not, default is to skip. */
  @JsonProperty private boolean skipRuleEngine;

  /** Should import be treated as an atomic import (all or nothing). */
  @Builder.Default private AtomicMode atomicMode = AtomicMode.ALL;

  /** Flush for every object or per type. */
  @Builder.Default private FlushMode flushMode = FlushMode.AUTO;

  /** Validation mode to use, defaults to fully validated objects. */
  @Builder.Default private ValidationMode validationMode = ValidationMode.FULL;

  /** Preheat bundle for all attached objects (or null if preheat not run yet). */
  private TrackerPreheat preheat;

  /** Tracked entities to import. */
  @Builder.Default private List<TrackedEntity> trackedEntities = new ArrayList<>();

  /** Enrollments to import. */
  @Builder.Default private List<Enrollment> enrollments = new ArrayList<>();

  /** Events to import. */
  @Builder.Default private List<Event> events = new ArrayList<>();

  /** Relationships to import. */
  @Builder.Default private List<Relationship> relationships = new ArrayList<>();

  // Lazily-built UID indexes for O(1) lookups. Invalidated when entity lists change.
  private Map<UID, TrackedEntity> trackedEntityByUid;
  private Map<UID, Enrollment> enrollmentByUid;
  private Map<UID, Event> eventByUid;
  private Map<UID, Relationship> relationshipByUid;

  /** Notifications for enrollments. */
  @Builder.Default private Map<UID, List<Notification>> enrollmentNotifications = new HashMap<>();

  /** Notifications for events. */
  @Builder.Default private Map<UID, List<Notification>> eventNotifications = new HashMap<>();

  /** Rule action executors for enrollments. */
  @Builder.Default
  private Map<Enrollment, List<RuleActionExecutor<Enrollment>>> enrollmentRuleActionExecutors =
      new HashMap<>();

  /** Rule action executors for events. */
  @Builder.Default
  private Map<Event, List<RuleActionExecutor<Event>>> eventRuleActionExecutors = new HashMap<>();

  @Builder.Default
  private Map<TrackerType, Map<UID, TrackerImportStrategy>> resolvedStrategyMap = initStrategyMap();

  private static Map<TrackerType, Map<UID, TrackerImportStrategy>> initStrategyMap() {
    Map<TrackerType, Map<UID, TrackerImportStrategy>> resolvedStrategyMap =
        new EnumMap<>(TrackerType.class);

    resolvedStrategyMap.put(TrackerType.RELATIONSHIP, new HashMap<>());
    resolvedStrategyMap.put(TrackerType.EVENT, new HashMap<>());
    resolvedStrategyMap.put(TrackerType.ENROLLMENT, new HashMap<>());
    resolvedStrategyMap.put(TrackerType.TRACKED_ENTITY, new HashMap<>());

    return resolvedStrategyMap;
  }

  @Builder.Default @JsonIgnore private Set<UID> updatedTrackedEntities = new HashSet<>();

  public void setTrackedEntities(List<TrackedEntity> trackedEntities) {
    this.trackedEntities = trackedEntities;
    this.trackedEntityByUid = null;
  }

  public void setEnrollments(List<Enrollment> enrollments) {
    this.enrollments = enrollments;
    this.enrollmentByUid = null;
  }

  public void setEvents(List<Event> events) {
    this.events = events;
    this.eventByUid = null;
  }

  public void setRelationships(List<Relationship> relationships) {
    this.relationships = relationships;
    this.relationshipByUid = null;
  }

  public Optional<TrackedEntity> findTrackedEntityByUid(@Nonnull UID uid) {
    return Optional.ofNullable(getTrackedEntityByUid().get(uid));
  }

  public Optional<Enrollment> findEnrollmentByUid(@Nonnull UID uid) {
    return Optional.ofNullable(getEnrollmentByUid().get(uid));
  }

  public Optional<Event> findEventByUid(@Nonnull UID uid) {
    return Optional.ofNullable(getEventByUid().get(uid));
  }

  public Optional<Relationship> findRelationshipByUid(@Nonnull UID uid) {
    return Optional.ofNullable(getRelationshipByUid().get(uid));
  }

  private Map<UID, TrackedEntity> getTrackedEntityByUid() {
    if (trackedEntityByUid == null) {
      trackedEntityByUid = indexByUid(trackedEntities);
    }
    return trackedEntityByUid;
  }

  private Map<UID, Enrollment> getEnrollmentByUid() {
    if (enrollmentByUid == null) {
      enrollmentByUid = indexByUid(enrollments);
    }
    return enrollmentByUid;
  }

  private Map<UID, Event> getEventByUid() {
    if (eventByUid == null) {
      eventByUid = indexByUid(events);
    }
    return eventByUid;
  }

  private Map<UID, Relationship> getRelationshipByUid() {
    if (relationshipByUid == null) {
      relationshipByUid = indexByUid(relationships);
    }
    return relationshipByUid;
  }

  private static <T extends TrackerDto> Map<UID, T> indexByUid(List<T> entities) {
    return entities.stream()
        .collect(Collectors.toMap(TrackerDto::getUid, Function.identity(), (a, b) -> a));
  }

  public Set<UID> getUpdatedTrackedEntities() {
    return Set.copyOf(this.updatedTrackedEntities);
  }

  public void addUpdatedTrackedEntities(Set<UID> updatedTrackedEntities) {
    this.updatedTrackedEntities.addAll(updatedTrackedEntities);
  }

  public Map<UID, List<Notification>> getEnrollmentNotifications() {
    return Map.copyOf(enrollmentNotifications);
  }

  public Map<UID, List<Notification>> getEventNotifications() {
    return Map.copyOf(eventNotifications);
  }

  public void setStrategy(TrackerDto dto, TrackerImportStrategy strategy) {
    this.getResolvedStrategyMap().get(dto.getTrackerType()).put(dto.getUid(), strategy);
  }

  public TrackerImportStrategy getStrategy(TrackerDto dto) {
    return getResolvedStrategyMap().get(dto.getTrackerType()).get(dto.getUid());
  }
}
