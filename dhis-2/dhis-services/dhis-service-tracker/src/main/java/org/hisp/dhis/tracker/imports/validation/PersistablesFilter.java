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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;

/**
 * Determines whether entities can be persisted (created, updated, deleted) taking into account the
 * {@link TrackerImportStrategy} and the parent/child relationships between entities. For example
 * during {@link TrackerImportStrategy#CREATE} no valid child of an invalid parent can be created
 * (i.e. enrollment of trackedEntity or event of enrollment). During {@link
 * TrackerImportStrategy#UPDATE} a valid child of an invalid parent can be updated.
 *
 * <p>The {@link Result} returned from {@link #filter(TrackerBundle, Map, TrackerImportStrategy)}
 * can be trusted to only contain persistable entities. The {@link TrackerBundle} is not mutated!
 *
 * <p>Errors are only added to {@link Result#errors} if they add information. For example a valid
 * entity with invalid parent cannot be created because of its parent. Since the valid children did
 * not already have an error added during validation on will be added here. For more information see
 * {@link #addErrorsForChildren(List, TrackerDto)}.
 *
 * <p>This filter relies on preprocessing and all {@link
 * org.hisp.dhis.tracker.imports.validation.Validator}s having run beforehand. The following are
 * some assumptions the filtering relies on:
 *
 * <ul>
 *   <li>This does not validate whether the {@link TrackerImportStrategy} matches the state of a
 *       given entity. In case a non existing entity is updated it is expected to already be flagged
 *       in the {@code invalidEntities}.
 *   <li>Existence is only checked in relation to a parent. During {@link
 *       TrackerImportStrategy#UPDATE} a valid enrollment can be updated if its parent the TE is
 *       invalid but exists.
 *   <li>An {@link Event} in an event program does not have an {@link Event#getEnrollment()}
 *       (parent) set in the payload. This expects one to be set during preprocessing. So events in
 *       program with or without registration do not get any special treatment.
 *   <li>It is not possible to have a valid parent with invalid children in {@link
 *       TrackerImportStrategy#DELETE} as an entity can be invalid only in 2 cases:
 *       <ul>
 *         <li>if it doesn't exist there are no children to check
 *         <li>if user has no access to it, then it will not have access to its children
 *       </ul>
 * </ul>
 */
class PersistablesFilter {
  private static final List<Function<TrackedEntity, TrackerDto>> TRACKED_ENTITY_PARENTS =
      Collections.emptyList();

  /**
   * Using {@link TrackerDto} as a tuple of UID and {@link TrackerType}. This makes working with the
   * different types (trackedEntity, enrollment, ...) transparent.
   */
  private static final List<Function<Enrollment, TrackerDto>> ENROLLMENT_PARENTS =
      List.of(en -> TrackedEntity.builder().trackedEntity(en.getTrackedEntity()).build());

  private static final List<Function<Event, TrackerDto>> EVENT_PARENTS =
      List.of(ev -> Enrollment.builder().enrollment(ev.getEnrollment()).build());

  private static final List<Function<Relationship, TrackerDto>> RELATIONSHIP_PARENTS =
      List.of(rel -> toTrackerDto(rel.getFrom()), rel -> toTrackerDto(rel.getTo()));

  /**
   * Collects non-deletable parent entities on DELETE and persistable entities otherwise. Checking
   * each "layer" depends on the knowledge (marked entities) we gain from the previous layers. For
   * example on DELETE event, enrollment, trackedEntity entities cannot be deleted if an invalid
   * relationship points to them.
   */
  private final EnumMap<TrackerType, Set<UID>> markedEntities =
      new EnumMap<>(
          Map.of(
              TRACKED_ENTITY, new HashSet<>(),
              ENROLLMENT, new HashSet<>(),
              EVENT, new HashSet<>(),
              RELATIONSHIP, new HashSet<>()));

  private final Result result = new Result();

  private final TrackerBundle bundle;

  private final TrackerPreheat preheat;

  private final Map<TrackerType, Set<UID>> invalidEntities;

  private final TrackerImportStrategy importStrategy;

  public static Result filter(
      TrackerBundle bundle,
      Map<TrackerType, Set<UID>> invalidEntities,
      TrackerImportStrategy importStrategy) {
    return new PersistablesFilter(bundle, invalidEntities, importStrategy).result;
  }

  private PersistablesFilter(
      TrackerBundle bundle,
      Map<TrackerType, Set<UID>> invalidEntities,
      TrackerImportStrategy importStrategy) {
    this.bundle = bundle;
    this.preheat = bundle.getPreheat();
    this.invalidEntities = invalidEntities;
    this.importStrategy = importStrategy;

    filter();
  }

  private void filter() {
    if (onDelete()) {
      // bottom-up
      collectDeletables(Relationship.class, bundle.getRelationships());
      collectDeletables(Event.class, bundle.getEvents());
      collectDeletables(Enrollment.class, bundle.getEnrollments());
      collectDeletables(TrackedEntity.class, bundle.getTrackedEntities());
    } else {

      // top-down
      collectPersistables(TrackedEntity.class, TRACKED_ENTITY_PARENTS, bundle.getTrackedEntities());
      collectPersistables(Enrollment.class, ENROLLMENT_PARENTS, bundle.getEnrollments());
      collectPersistables(Event.class, EVENT_PARENTS, bundle.getEvents());
      collectPersistables(Relationship.class, RELATIONSHIP_PARENTS, bundle.getRelationships());
    }
  }

  private <T extends TrackerDto> void collectDeletables(Class<T> type, List<T> entities) {
    for (T entity : entities) {
      if (isValid(entity)) {
        collectPersistable(type, entity);
      }
    }
  }

  private <T extends TrackerDto> void collectPersistables(
      Class<T> type, List<Function<T, TrackerDto>> parents, List<T> entities) {
    for (T entity : entities) {
      if (isValid(entity) && isCreateOrUpdatable(parents, entity)) {
        markAsPersistable(entity);
        collectPersistable(type, entity);
        continue;
      }

      addErrorsForChildren(parents, entity);
    }
  }

  private boolean isNotValid(TrackerDto entity) {
    return !isValid(entity);
  }

  private boolean isValid(TrackerDto entity) {
    return !isContained(this.invalidEntities, entity);
  }

  private boolean isContained(Map<TrackerType, Set<UID>> map, TrackerDto entity) {
    return map.get(entity.getTrackerType()).contains(entity.getUid());
  }

  /**
   * Collect given entity in the persistables result. This entity can be created, updated or deleted
   * (depending on context and {@link TrackerImportStrategy}).
   *
   * @param type tracker dto type to add persistable to
   * @param entity persistable entity
   * @param <T> type of tracker dto
   */
  private <T extends TrackerDto> void collectPersistable(Class<T> type, T entity) {
    this.result.put(type, entity);
  }

  private <T extends TrackerDto> boolean isCreateOrUpdatable(
      List<Function<T, TrackerDto>> parents, T entity) {
    return !hasInvalidParents(parents, entity);
  }

  private boolean onDelete() {
    return this.importStrategy == TrackerImportStrategy.DELETE;
  }

  private <T extends TrackerDto> void mark(T entity) {
    this.markedEntities.get(entity.getTrackerType()).add(entity.getUid());
  }

  private <T extends TrackerDto> boolean isMarked(T entity) {
    return isContained(this.markedEntities, entity);
  }

  /**
   * Mark as persistable for later children. For example a tracked entity (parent) referenced by an
   * enrollment (child).
   */
  private <T extends TrackerDto> void markAsPersistable(T entity) {
    mark(entity);
  }

  private <T extends TrackerDto> boolean hasInvalidParents(
      List<Function<T, TrackerDto>> parents, T entity) {
    return !parentConditions(parents).test(entity);
  }

  private <T extends TrackerDto> Predicate<T> parentConditions(
      List<Function<T, TrackerDto>> parents) {
    final Predicate<TrackerDto> parentCondition =
        parent -> isMarked(parent) || this.preheat.exists(parent);

    return parents.stream()
        .map(
            p ->
                (Predicate<T>)
                    t ->
                        parentCondition.test(
                            p.apply(t))) // children of invalid parents can only be persisted under
        // certain conditions
        .reduce(Predicate::and)
        .orElse(t -> true); // predicate always returning true for entities without parents
  }

  /**
   * Add error for valid child entity with invalid parent as a reason. If a child is invalid that is
   * enough information for a user to know why it could not be persisted.
   */
  private <T extends TrackerDto> void addErrorsForChildren(
      List<Function<T, TrackerDto>> parents, T entity) {
    if (isNotValid(entity)) {
      return;
    }

    List<Error> errors =
        parents.stream()
            .map(p -> p.apply(entity))
            .filter(this::isNotValid) // remove valid parents
            .map(p -> error(ValidationCode.E5000, entity, p))
            .toList();
    this.result.errors.addAll(errors);
  }

  private static Error error(ValidationCode code, TrackerDto notPersistable, TrackerDto reason) {
    Object[] args = {
      notPersistable.getTrackerType().getName(),
      notPersistable.getUid(),
      reason.getTrackerType().getName(),
      reason.getUid()
    };
    String message = MessageFormat.format(code.getMessage(), args);
    return new Error(
        message, code, notPersistable.getTrackerType(), notPersistable.getUid(), List.of(args));
  }

  /**
   * Captures UID and {@link TrackerType} information in a {@link TrackerDto}. A valid {@link
   * RelationshipItem} only has one of its 3 identifier fields set. The RelationshipItem API does
   * not enforce it since it needs to capture invalid user input. Transform it to a shallow {@link
   * TrackerDto}, so working with it is transparent.
   *
   * @param item relationship item
   * @return a tracker dto only capturing the uid and type
   */
  private static TrackerDto toTrackerDto(RelationshipItem item) {
    if (item.getTrackedEntity() != null) {
      return TrackedEntity.builder().trackedEntity(item.getTrackedEntity()).build();
    } else if (item.getEnrollment() != null) {
      return Enrollment.builder().enrollment(item.getEnrollment()).build();
    } else if (item.getEvent() != null) {
      return TrackerEvent.builder().event(item.getEvent()).build();
    }
    // only reached if a new TrackerDto implementation is added
    throw new IllegalStateException("TrackerType for relationship item not yet supported.");
  }

  /**
   * Result of {@link #filter(TrackerBundle, Map, TrackerImportStrategy)} operation indicating all
   * entities that can be persisted. The meaning of persisted i.e. create, update, delete comes from
   * the context which includes the {@link TrackerImportStrategy} and whether the entity existed or
   * not.
   */
  @Getter
  public static class Result {
    private final List<TrackedEntity> trackedEntities = new ArrayList<>();

    private final List<Enrollment> enrollments = new ArrayList<>();

    private final List<Event> events = new ArrayList<>();

    private final List<Relationship> relationships = new ArrayList<>();

    private final List<Error> errors = new ArrayList<>();

    private <T extends TrackerDto> void put(Class<T> type, T instance) {
      get(Objects.requireNonNull(type)).add(instance);
    }

    @SuppressWarnings("unchecked")
    public <T extends TrackerDto> List<T> get(Class<T> type) {
      Objects.requireNonNull(type);
      if (type == TrackedEntity.class) {
        return (List<T>) trackedEntities;
      } else if (type == Enrollment.class) {
        return (List<T>) enrollments;
      } else if (type == Event.class) {
        return (List<T>) events;
      } else if (type == Relationship.class) {
        return (List<T>) relationships;
      }
      // only reached if a new TrackerDto implementation is added
      throw new IllegalStateException("TrackerType " + type.getName() + " not yet supported.");
    }
  }
}
