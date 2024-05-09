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
package org.hisp.dhis.dxf2.deprecated.tracker.relationship;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.deprecated.tracker.EnrollmentParams;
import org.hisp.dhis.dxf2.deprecated.tracker.EventParams;
import org.hisp.dhis.dxf2.deprecated.tracker.RelationshipParams;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractRelationshipService implements RelationshipService {
  protected DbmsManager dbmsManager;

  protected SchemaService schemaService;

  protected QueryService queryService;

  protected TrackerAccessManager trackerAccessManager;

  protected org.hisp.dhis.relationship.RelationshipService relationshipService;

  protected TrackedEntityInstanceService trackedEntityInstanceService;

  protected EnrollmentService enrollmentService;

  protected EventService eventService;

  protected TrackedEntityService teiDaoService;

  protected UserService userService;

  protected ObjectMapper jsonMapper;

  protected ObjectMapper xmlMapper;

  @Override
  @Transactional(readOnly = true)
  public Optional<Relationship> findRelationship(
      org.hisp.dhis.relationship.Relationship dao, RelationshipParams params, UserDetails user) {
    List<String> errors = trackerAccessManager.canRead(user, dao);

    if (!errors.isEmpty()) {
      // Dont include relationship
      return Optional.empty();
    }

    Relationship relationship = new Relationship();

    relationship.setRelationship(dao.getUid());
    relationship.setRelationshipType(dao.getRelationshipType().getUid());
    relationship.setRelationshipName(dao.getRelationshipType().getName());

    relationship.setFrom(
        includeRelationshipItem(
            dao.getFrom(), !params.isIncludeFrom(), dao.getRelationshipType().getFromConstraint()));
    relationship.setTo(
        includeRelationshipItem(
            dao.getTo(), !params.isIncludeTo(), dao.getRelationshipType().getToConstraint()));

    relationship.setBidirectional(dao.getRelationshipType().isBidirectional());

    relationship.setCreated(DateUtils.toIso8601NoTz(dao.getCreated()));
    relationship.setLastUpdated(DateUtils.toIso8601NoTz(dao.getLastUpdated()));

    return Optional.of(relationship);
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.RelationshipItem
      includeRelationshipItem(
          RelationshipItem dao, boolean uidOnly, RelationshipConstraint constraint) {
    org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.RelationshipItem relationshipItem =
        new org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.RelationshipItem();

    if (dao.getTrackedEntity() != null) {
      TrackedEntityInstance tei = new TrackedEntityInstance();
      String uid = dao.getTrackedEntity().getUid();

      if (uidOnly) {
        tei.clear();
        tei.setTrackedEntityInstance(uid);
      } else {
        tei =
            trackedEntityInstanceService.getTrackedEntityInstance(
                dao.getTrackedEntity(), TrackedEntityInstanceParams.TRUE);

        if (constraint.getTrackerDataView() != null) {
          tei.setAttributes(
              tei.getAttributes().stream()
                  .filter(
                      a ->
                          constraint
                              .getTrackerDataView()
                              .getAttributes()
                              .contains(a.getAttribute()))
                  .toList());
        }
      }

      relationshipItem.setTrackedEntityInstance(tei);
    } else if (dao.getEnrollment() != null) {
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment =
          new org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment();
      String uid = dao.getEnrollment().getUid();

      if (uidOnly) {

        enrollment.clear();
        enrollment.setEnrollment(uid);
      } else {
        enrollment = enrollmentService.getEnrollment(dao.getEnrollment(), EnrollmentParams.TRUE);

        if (constraint.getTrackerDataView() != null) {
          enrollment.setAttributes(
              enrollment.getAttributes().stream()
                  .filter(
                      a ->
                          constraint
                              .getTrackerDataView()
                              .getAttributes()
                              .contains(a.getAttribute()))
                  .toList());
        }
      }

      relationshipItem.setEnrollment(enrollment);
    } else if (dao.getEvent() != null) {
      org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
          new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
      String uid = dao.getEvent().getUid();

      if (uidOnly) {
        event.clear();
        event.setEvent(uid);
      } else {
        event = eventService.getEvent(dao.getEvent(), EventParams.FALSE);

        if (constraint.getTrackerDataView() != null) {
          event.setDataValues(
              event.getDataValues().stream()
                  .filter(
                      d ->
                          constraint
                              .getTrackerDataView()
                              .getDataElements()
                              .contains(d.getDataElement()))
                  .collect(Collectors.toSet()));
        }
      }

      relationshipItem.setEvent(event);
    }

    return relationshipItem;
  }
}
