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
package org.hisp.dhis.sms.listener;

import java.util.Date;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.RelationshipSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.sms.listener.RelationshipSMSListener")
@Transactional
public class RelationshipSMSListener extends CompressionSMSListener {
  private enum RelationshipDir {
    FROM,
    TO;
  }

  private final RelationshipService relationshipService;

  private final RelationshipTypeService relationshipTypeService;

  private final TrackedEntityService trackedEntityService;

  private final EnrollmentService enrollmentService;

  public RelationshipSMSListener(
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      UserService userService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      ProgramService programService,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      DataElementService dataElementService,
      EventService eventService,
      RelationshipService relationshipService,
      RelationshipTypeService relationshipTypeService,
      TrackedEntityService trackedEntityService,
      EnrollmentService enrollmentService,
      IdentifiableObjectManager identifiableObjectManager) {
    super(
        incomingSmsService,
        smsSender,
        userService,
        trackedEntityTypeService,
        trackedEntityAttributeService,
        programService,
        organisationUnitService,
        categoryService,
        dataElementService,
        eventService,
        identifiableObjectManager);

    this.relationshipService = relationshipService;
    this.relationshipTypeService = relationshipTypeService;
    this.trackedEntityService = trackedEntityService;
    this.enrollmentService = enrollmentService;
  }

  @Override
  protected SmsResponse postProcess(IncomingSms sms, SmsSubmission submission)
      throws SMSProcessingException {
    RelationshipSmsSubmission subm = (RelationshipSmsSubmission) submission;

    Uid fromid = subm.getFrom();
    Uid toid = subm.getTo();
    Uid typeid = subm.getRelationshipType();

    RelationshipType relType = relationshipTypeService.getRelationshipType(typeid.getUid());

    if (relType == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_RELTYPE.set(typeid));
    }

    RelationshipItem fromItem = createRelationshipItem(relType, RelationshipDir.FROM, fromid);
    RelationshipItem toItem = createRelationshipItem(relType, RelationshipDir.TO, toid);

    Relationship rel = new Relationship();

    // If we aren't given a Uid for the relationship, it will be
    // auto-generated
    if (subm.getRelationship() != null) {
      rel.setUid(subm.getRelationship().getUid());
    }

    rel.setRelationshipType(relType);
    rel.setFrom(fromItem);
    rel.setTo(toItem);
    rel.setCreated(new Date());
    rel.setLastUpdated(new Date());

    // TODO: Are there values we need to account for in relationships?

    relationshipService.addRelationship(rel);

    return SmsResponse.SUCCESS;
  }

  private RelationshipItem createRelationshipItem(
      RelationshipType relType, RelationshipDir dir, Uid objId) {
    RelationshipItem relItem = new RelationshipItem();
    RelationshipEntity fromEnt = relType.getFromConstraint().getRelationshipEntity();
    RelationshipEntity toEnt = relType.getFromConstraint().getRelationshipEntity();
    RelationshipEntity relEnt = dir == RelationshipDir.FROM ? fromEnt : toEnt;

    switch (relEnt) {
      case TRACKED_ENTITY_INSTANCE:
        TrackedEntity tei = trackedEntityService.getTrackedEntity(objId.getUid());
        if (tei == null) {
          throw new SMSProcessingException(SmsResponse.INVALID_TEI.set(objId));
        }
        relItem.setTrackedEntity(tei);
        break;

      case PROGRAM_INSTANCE:
        Enrollment progInst = enrollmentService.getEnrollment(objId.getUid());
        if (progInst == null) {
          throw new SMSProcessingException(SmsResponse.INVALID_ENROLL.set(objId));
        }
        relItem.setEnrollment(progInst);
        break;

      case PROGRAM_STAGE_INSTANCE:
        Event event = eventService.getEvent(objId.getUid());
        if (event == null) {
          throw new SMSProcessingException(SmsResponse.INVALID_EVENT.set(objId));
        }
        relItem.setEvent(event);
        break;
    }

    return relItem;
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.RELATIONSHIP);
  }
}
