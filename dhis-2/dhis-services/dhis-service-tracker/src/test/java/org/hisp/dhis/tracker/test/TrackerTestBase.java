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
package org.hisp.dhis.tracker.test;

import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;

@Slf4j
public abstract class TrackerTestBase extends TestBase {
  protected static final String BASE_TE_UID = "teibcdefgh";

  public static Enrollment createEnrollment(
      Program program, TrackedEntity te, OrganisationUnit organisationUnit) {
    Enrollment enrollment = new Enrollment(new Date(), new Date(), te, program);
    enrollment.setAutoFields();
    enrollment.setTrackedEntity(te);
    enrollment.setOrganisationUnit(organisationUnit);
    return enrollment;
  }

  public static SingleEvent createSingleEvent(
      ProgramStage programStage, OrganisationUnit organisationUnit) {
    SingleEvent event = new SingleEvent();
    event.setAutoFields();
    event.setOccurredDate(new Date());
    event.setProgramStage(programStage);
    event.setOrganisationUnit(organisationUnit);
    if (categoryService != null) {
      event.setAttributeOptionCombo(categoryService.getDefaultCategoryOptionCombo());
    }
    return event;
  }

  public static TrackerEvent createEvent(
      ProgramStage programStage, Enrollment enrollment, OrganisationUnit organisationUnit) {
    TrackerEvent event = new TrackerEvent();
    event.setAutoFields();
    event.setProgramStage(programStage);
    event.setEnrollment(enrollment);
    event.setOrganisationUnit(organisationUnit);
    if (categoryService != null) {
      event.setAttributeOptionCombo(categoryService.getDefaultCategoryOptionCombo());
    }
    return event;
  }

  public static TrackerEvent createEvent(
      Enrollment enrollment,
      ProgramStage programStage,
      OrganisationUnit organisationUnit,
      Set<EventDataValue> dataValues) {
    TrackerEvent event = createEvent(programStage, enrollment, organisationUnit);
    event.setOccurredDate(new Date());
    event.setStatus(EventStatus.ACTIVE);
    event.setEventDataValues(dataValues);
    return event;
  }

  public static Relationship createTeToTeRelationship(
      TrackedEntity from, TrackedEntity to, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    RelationshipItem riFrom = new RelationshipItem();
    RelationshipItem riTo = new RelationshipItem();

    riFrom.setTrackedEntity(from);
    riFrom.setRelationship(relationship);
    riTo.setTrackedEntity(to);
    riTo.setRelationship(relationship);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(riFrom);
    relationship.setTo(riTo);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    relationship.setAutoFields();

    return relationship;
  }

  public static Relationship createTeToEnrollmentRelationship(
      TrackedEntity from, Enrollment to, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    RelationshipItem riFrom = new RelationshipItem();
    RelationshipItem riTo = new RelationshipItem();

    riFrom.setTrackedEntity(from);
    riTo.setEnrollment(to);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(riFrom);
    relationship.setTo(riTo);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    relationship.setAutoFields();

    return relationship;
  }

  public static Relationship createTeToEventRelationship(
      TrackedEntity from, TrackerEvent to, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    RelationshipItem riFrom = new RelationshipItem();
    RelationshipItem riTo = new RelationshipItem();

    riFrom.setTrackedEntity(from);
    riTo.setTrackerEvent(to);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(riFrom);
    relationship.setTo(riTo);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    relationship.setAutoFields();

    return relationship;
  }

  public static TrackedEntity createTrackedEntity(
      OrganisationUnit organisationUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setAutoFields();
    trackedEntity.setOrganisationUnit(organisationUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  public static TrackedEntity createTrackedEntity(
      char uniqueChar, OrganisationUnit organisationUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setAutoFields();
    trackedEntity.setOrganisationUnit(organisationUnit);
    trackedEntity.setUid(BASE_TE_UID + uniqueChar);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  public static TrackedEntity createTrackedEntity(
      char uniqueChar,
      OrganisationUnit organisationUnit,
      TrackedEntityAttribute attribute,
      TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setAutoFields();
    trackedEntity.setOrganisationUnit(organisationUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setAttribute(attribute);
    attributeValue.setTrackedEntity(trackedEntity);
    attributeValue.setValue("Attribute" + uniqueChar);
    trackedEntity.getTrackedEntityAttributeValues().add(attributeValue);

    return trackedEntity;
  }

  public static TrackedEntityAttributeValue createTrackedEntityAttributeValue(
      char uniqueChar, TrackedEntity trackedEntity, TrackedEntityAttribute attribute) {
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setTrackedEntity(trackedEntity);
    attributeValue.setAttribute(attribute);
    attributeValue.setValue("Attribute" + uniqueChar);

    return attributeValue;
  }
}
