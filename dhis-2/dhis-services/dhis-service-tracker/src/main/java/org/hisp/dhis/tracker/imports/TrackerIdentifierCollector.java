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
package org.hisp.dhis.tracker.imports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.springframework.stereotype.Component;

/**
 * This class "collects" identifiers from all input objects. This resulting map of all identifiers
 * will then be used to "preheat/cache" all the objects needed into memory to speed up the
 * validation process.
 *
 * <p>The metadata identifiers can be of different idSchemes as specified by the user on import.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see org.hisp.dhis.tracker.imports.preheat.DefaultTrackerPreheatService
 */
@Component
@RequiredArgsConstructor
public class TrackerIdentifierCollector {
  private final ProgramRuleService programRuleService;

  public Map<Class<?>, Set<String>> collect(TrackerObjects trackerObjects) {
    final Map<Class<?>, Set<String>> identifiers = new HashMap<>();
    collectTrackedEntities(identifiers, trackerObjects.getTrackedEntities());
    collectEnrollments(identifiers, trackerObjects.getEnrollments());
    collectEvents(identifiers, trackerObjects.getEvents());
    collectRelationships(identifiers, trackerObjects.getRelationships());
    collectProgramRulesFields(identifiers);
    return identifiers;
  }

  private void collectProgramRulesFields(Map<Class<?>, Set<String>> map) {
    // collecting program rule dataElement/attributes deliberately using
    // UIDs
    // Rule engine rules only know UIDs, so we need to be able to get
    // dataElements/attributes from rule actions
    // out of the preheat using UIDs
    programRuleService
        .getDataElementsPresentInProgramRules()
        .forEach(de -> addIdentifier(map, DataElement.class, de));

    programRuleService
        .getTrackedEntityAttributesPresentInProgramRules()
        .forEach(attribute -> addIdentifier(map, TrackedEntityAttribute.class, attribute));
  }

  private void collectTrackedEntities(
      Map<Class<?>, Set<String>> identifiers, List<TrackedEntity> trackedEntities) {
    trackedEntities.forEach(
        trackedEntity -> {
          addIdentifier(identifiers, TrackedEntity.class, trackedEntity.getTrackedEntity());
          addIdentifier(identifiers, TrackedEntityType.class, trackedEntity.getTrackedEntityType());
          addIdentifier(identifiers, OrganisationUnit.class, trackedEntity.getOrgUnit());

          trackedEntity
              .getAttributes()
              .forEach(
                  attribute ->
                      addIdentifier(
                          identifiers, TrackedEntityAttribute.class, attribute.getAttribute()));
        });
  }

  private void collectEnrollments(
      Map<Class<?>, Set<String>> identifiers, List<Enrollment> enrollments) {
    enrollments.forEach(
        enrollment -> {
          addIdentifier(identifiers, TrackedEntity.class, enrollment.getTrackedEntity());
          addIdentifier(identifiers, Enrollment.class, enrollment.getEnrollment());
          addIdentifier(identifiers, Program.class, enrollment.getProgram());
          addIdentifier(identifiers, OrganisationUnit.class, enrollment.getOrgUnit());

          collectNotes(identifiers, enrollment.getNotes());
          enrollment
              .getAttributes()
              .forEach(
                  attribute ->
                      addIdentifier(
                          identifiers, TrackedEntityAttribute.class, attribute.getAttribute()));
        });
  }

  private void collectNotes(Map<Class<?>, Set<String>> identifiers, List<Note> notes) {
    notes.forEach(
        note -> {
          if (StringUtils.isNotEmpty(note.getValue())) {
            addIdentifier(identifiers, org.hisp.dhis.note.Note.class, note.getNote());
          }
        });
  }

  private void collectEvents(Map<Class<?>, Set<String>> identifiers, List<Event> events) {
    events.forEach(
        event -> {
          addIdentifier(identifiers, Enrollment.class, event.getEnrollment());
          addIdentifier(identifiers, TrackerEvent.class, event.getEvent());
          addIdentifier(identifiers, SingleEvent.class, event.getEvent());
          addIdentifier(identifiers, Program.class, event.getProgram());
          addIdentifier(identifiers, ProgramStage.class, event.getProgramStage());
          addIdentifier(identifiers, OrganisationUnit.class, event.getOrgUnit());

          event
              .getAttributeCategoryOptions()
              .forEach(s -> addIdentifier(identifiers, CategoryOption.class, s));

          addIdentifier(identifiers, CategoryOptionCombo.class, event.getAttributeOptionCombo());

          event
              .getDataValues()
              .forEach(dv -> addIdentifier(identifiers, DataElement.class, dv.getDataElement()));

          collectNotes(identifiers, event.getNotes());
        });
  }

  private void collectRelationships(
      Map<Class<?>, Set<String>> identifiers, List<Relationship> relationships) {
    relationships.forEach(
        relationship -> {
          addIdentifier(identifiers, Relationship.class, relationship.getRelationship());
          addIdentifier(identifiers, RelationshipType.class, relationship.getRelationshipType());

          if (Objects.nonNull(relationship.getFrom())) {
            addIdentifier(
                identifiers, TrackedEntity.class, relationship.getFrom().getTrackedEntity());
            addIdentifier(identifiers, Enrollment.class, relationship.getFrom().getEnrollment());
            addIdentifier(identifiers, TrackerEvent.class, relationship.getFrom().getEvent());
            addIdentifier(identifiers, SingleEvent.class, relationship.getFrom().getEvent());
          }

          if (Objects.nonNull(relationship.getTo())) {
            addIdentifier(
                identifiers, TrackedEntity.class, relationship.getTo().getTrackedEntity());
            addIdentifier(identifiers, Enrollment.class, relationship.getTo().getEnrollment());
            addIdentifier(identifiers, TrackerEvent.class, relationship.getTo().getEvent());
            addIdentifier(identifiers, SingleEvent.class, relationship.getTo().getEvent());
          }
        });
  }

  private <T> void addIdentifier(
      @Nonnull Map<Class<?>, Set<String>> identifiers,
      @Nonnull Class<T> klass,
      MetadataIdentifier identifier) {
    addIdentifier(
        identifiers, klass, identifier == null ? null : identifier.getIdentifierOrAttributeValue());
  }

  private <T> void addIdentifier(
      @Nonnull Map<Class<?>, Set<String>> identifiers, @Nonnull Class<T> klass, String identifier) {
    if (StringUtils.isEmpty(identifier)) {
      return;
    }

    identifiers.computeIfAbsent(klass, k -> new HashSet<>()).add(identifier);
  }

  private <T> void addIdentifier(
      @Nonnull Map<Class<?>, Set<String>> identifiers, @Nonnull Class<T> klass, UID identifier) {
    if (identifier == null) {
      return;
    }

    identifiers.computeIfAbsent(klass, k -> new HashSet<>()).add(identifier.getValue());
  }
}
