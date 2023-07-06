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
package org.hisp.dhis.webapi.controller.tracker.imports;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Note;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;

/**
 * Converts a {@see TrackerBundleParams} containing a nested Tracked Entity structure into a "flat"
 * structure
 *
 * <p>Assuming a structure like:
 *
 * <pre>
 *
 * TrackerBundleParams
 *   |
 *   __TEI
 *      |_ENROLLMENT 1
 *      |      |
 *      |      |_ EVENT 1
 *      |      |
 *      |      |_ EVENT 2
 *      |
 *      |_ENROLLMENT 2
 *            |
 *            |_ EVENT 3
 *            |_ EVENT 4
 * </pre>
 *
 * <p>This converter will transform the object into:
 *
 * <pre>
 *
 * TrackerBundleParams
 *  |
 *  |___TEI
 *  |___ENROLLMENT 1, ENROLLMENT 2
 *  |
 *  |___EVENT 1, EVENT 2, EVENT 3, EVENT 4
 *
 * </pre>
 *
 * <p>This converter also assigns UIDs to Tracked Entities, Enrollment and Events if the payload
 * does not contain UIDs
 *
 * @author Luciano Fiandesio
 */
class TrackerBundleParamsConverter extends StdConverter<TrackerBundleParams, TrackerBundleParams> {

  /**
   * Iterates over the collections of a dataBundle. If any objects in those collections have objects
   * nested within them, they are extracted. For each object we process, we make sure all references
   * are valid as well.
   *
   * @param dataBundle containing collections to check and update.
   * @return a dataBundle with a flattened data structure, and valid uid references.
   */
  @Override
  public TrackerBundleParams convert(TrackerBundleParams dataBundle) {
    Map<String, TrackedEntity> trackedEntityMap = new HashMap<>();
    Map<String, Enrollment> enrollmentHashMap = new HashMap<>();
    Map<String, Event> eventHashMap = new HashMap<>();
    Map<String, Relationship> relationshipHashMap = new HashMap<>();

    // Extract all enrollments and relationships, and set parent reference.
    for (TrackedEntity te : dataBundle.getTrackedEntities()) {
      updateTrackedEntityReferences(te);
      trackedEntityMap.put(te.getTrackedEntity(), te);

      extractEnrollments(te)
          .forEach(enrollment -> enrollmentHashMap.put(enrollment.getEnrollment(), enrollment));

      extractRelationships(te)
          .forEach(
              relationship ->
                  relationshipHashMap.put(relationship.getRelationship(), relationship));
    }

    // Set UID for all enrollments and notes
    dataBundle.getEnrollments().stream()
        .peek(enrollment -> updateEnrollmentReferences(enrollment, enrollment.getTrackedEntity()))
        .forEach(enrollment -> enrollmentHashMap.put(enrollment.getEnrollment(), enrollment));

    // Extract all events and relationships, and set parent references
    for (Enrollment enrollment : enrollmentHashMap.values()) {
      extractEvents(enrollment).forEach(event -> eventHashMap.put(event.getEvent(), event));

      extractRelationships(enrollment)
          .forEach(
              relationship ->
                  relationshipHashMap.put(relationship.getRelationship(), relationship));

      enrollment.setNotes(
          enrollment.getNotes().stream()
              .filter(note -> !StringUtils.isEmpty(note.getValue()))
              .peek(this::updateNoteReferences)
              .collect(Collectors.toList()));
    }

    // Set UID for all events and notes
    dataBundle.getEvents().stream()
        .peek(event -> updateEventReferences(event, event.getEnrollment()))
        .forEach(event -> eventHashMap.put(event.getEvent(), event));

    // Extract all relationships
    for (Event event : eventHashMap.values()) {
      extractRelationships(event)
          .forEach(
              relationship ->
                  relationshipHashMap.put(relationship.getRelationship(), relationship));

      event.setNotes(
          event.getNotes().stream()
              .filter(note -> !StringUtils.isEmpty(note.getValue()))
              .peek(this::updateNoteReferences)
              .collect(Collectors.toList()));
    }

    // Set UID for all relationships
    dataBundle.getRelationships().stream()
        .peek(this::updateRelationshipReferences)
        .forEach(
            relationship -> relationshipHashMap.put(relationship.getRelationship(), relationship));

    return TrackerBundleParams.builder()
        .trackedEntities(new ArrayList<>(trackedEntityMap.values()))
        .enrollments(new ArrayList<>(enrollmentHashMap.values()))
        .events(new ArrayList<>(eventHashMap.values()))
        .relationships(new ArrayList<>(relationshipHashMap.values()))
        .build();
  }

  /**
   * Takes a trackedEntity and extracts the relationships, if any, and updates the uid references of
   * the relationships
   *
   * @param trackedEntity the trackedEntity to extract relationships from
   * @return a list of relationships
   */
  private List<Relationship> extractRelationships(TrackedEntity trackedEntity) {
    List<Relationship> relationships =
        trackedEntity.getRelationships().stream()
            .peek(this::updateRelationshipReferences)
            .collect(Collectors.toList());

    trackedEntity.setRelationships(new ArrayList<>());

    return relationships;
  }

  /**
   * Takes an enrollment and extracts the relationships from, if any, and updates the uid references
   * of the relationships
   *
   * @param enrollment the enrollment to extract relationships from
   * @return a list of relationships
   */
  private List<Relationship> extractRelationships(Enrollment enrollment) {
    List<Relationship> relationships =
        enrollment.getRelationships().stream()
            .peek(this::updateRelationshipReferences)
            .collect(Collectors.toList());

    enrollment.setRelationships(new ArrayList<>());

    return relationships;
  }

  /**
   * Takes an event and extracts the relationships from, if any, and updates the uid references of
   * the relationships
   *
   * @param event the event to extract relationships from
   * @return a list of relationships
   */
  private List<Relationship> extractRelationships(Event event) {
    List<Relationship> relationships =
        event.getRelationships().stream()
            .peek(this::updateRelationshipReferences)
            .collect(Collectors.toList());

    event.setRelationships(new ArrayList<>());

    return relationships;
  }

  /**
   * Takes an enrollment and extracts the events from, if any, and updates the uid references of the
   * events
   *
   * @param enrollment the enrollment to extract events from
   * @return a list of events
   */
  private List<Event> extractEvents(Enrollment enrollment) {
    List<Event> events =
        enrollment.getEvents().stream()
            .peek(event -> updateEventReferences(event, enrollment.getEnrollment()))
            .collect(Collectors.toList());

    enrollment.setEvents(new ArrayList<>());

    return events;
  }

  /**
   * Takes a trackedEntity and extracts enrollments, if any, and updated the uid references of the
   * enrollments
   *
   * @param trackedEntity the trackedEntity to extract enrollments from
   * @return a list of enrollments
   */
  private List<Enrollment> extractEnrollments(TrackedEntity trackedEntity) {
    List<Enrollment> enrollments =
        trackedEntity.getEnrollments().stream()
            .peek(
                enrollment ->
                    updateEnrollmentReferences(enrollment, trackedEntity.getTrackedEntity()))
            .collect(Collectors.toList());

    trackedEntity.setEnrollments(new ArrayList<>());

    return enrollments;
  }

  /**
   * Updates a reference (uid). If the String supplied is null or empty, generates and returns a new
   * uid. Otherwise, return the uid.
   *
   * @param uid the uid to check and update
   * @return a valid uid
   */
  private String updateReference(String uid) {
    return StringUtils.isEmpty(uid) ? CodeGenerator.generateUid() : uid;
  }

  /**
   * Updates uid of references in a relationship
   *
   * @param relationship the relationship to update references for
   */
  private void updateRelationshipReferences(Relationship relationship) {
    relationship.setRelationship(updateReference(relationship.getRelationship()));
  }

  /**
   * Updates uid of references in an event
   *
   * @param event the event to check and update references for
   * @param enrollment the parent enrollment uid
   */
  private void updateEventReferences(Event event, String enrollment) {
    event.setEvent(updateReference(event.getEvent()));
    event.setEnrollment(StringUtils.isEmpty(enrollment) ? null : enrollment);
    event.setEnrollment(StringUtils.isEmpty(enrollment) ? null : enrollment);
  }

  /**
   * Updates uid of references in an enrollment
   *
   * @param enrollment the enrollment to check and update references for
   * @param trackedEntity the parent trackedEntity uid
   */
  private void updateEnrollmentReferences(Enrollment enrollment, String trackedEntity) {
    enrollment.setEnrollment(updateReference(enrollment.getEnrollment()));
    enrollment.setTrackedEntity(StringUtils.isEmpty(trackedEntity) ? null : trackedEntity);
  }

  /**
   * Updates uid of references in a trackedEntity
   *
   * @param trackedEntity the trackedEntity to check and update references for
   */
  private void updateTrackedEntityReferences(TrackedEntity trackedEntity) {
    trackedEntity.setTrackedEntity(updateReference(trackedEntity.getTrackedEntity()));
  }

  /**
   * Updates uid of references in a note
   *
   * @param note the note to check and update references for
   */
  private void updateNoteReferences(Note note) {
    note.setNote(updateReference(note.getNote()));
  }
}
