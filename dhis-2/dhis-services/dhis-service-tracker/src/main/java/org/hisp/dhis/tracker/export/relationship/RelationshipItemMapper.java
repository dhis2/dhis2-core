/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.relationship;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.preheat.mappers.ProgramStageMapper;
import org.hisp.dhis.tracker.imports.preheat.mappers.RelationshipTypeMapper;
import org.hisp.dhis.tracker.imports.preheat.mappers.TrackedEntityTypeMapper;
import org.hisp.dhis.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(
    uses = {ProgramStageMapper.class, RelationshipTypeMapper.class, TrackedEntityTypeMapper.class})
public interface RelationshipItemMapper {
  default RelationshipItem map(
      @Context RelationshipFields fields, RelationshipItem relationshipItem) {
    if (relationshipItem == null) {
      return null;
    }

    RelationshipItem result = new RelationshipItem();
    result.setRelationship(map(fields, relationshipItem.getRelationship()));
    // only one of these fields will be non null as it represents the entity in which the
    // relationship originates. we might be able to optimize these mappings but
    // that could result in more programmer errors as in unexpected NPEs
    result.setTrackedEntity(
        map(
            RelationshipItemFields.all().getTrackedEntityFields(),
            relationshipItem.getTrackedEntity()));
    result.setEnrollment(
        map(RelationshipItemFields.all().getEnrollmentFields(), relationshipItem.getEnrollment()));
    result.setEvent(map(relationshipItem.getEvent()));
    return result;
  }

  default Relationship map(@Context RelationshipFields fields, Relationship relationship) {
    if (relationship == null) {
      return null;
    }

    Relationship result = new Relationship();
    result.setUid(relationship.getUid());
    result.setRelationshipType(
        RelationshipTypeMapper.INSTANCE.map(relationship.getRelationshipType()));
    result.setCreated(relationship.getCreated());
    result.setCreatedAtClient(relationship.getCreatedAtClient());
    result.setLastUpdated(relationship.getLastUpdated());
    result.setFrom(
        mapRelationshipItemWithoutRelationship(fields.getFromFields(), relationship.getFrom()));
    result.setTo(
        mapRelationshipItemWithoutRelationship(fields.getToFields(), relationship.getTo()));
    return result;
  }

  // we need to ignore relationship to break the cycle between relationship and relationshipItem
  @Named("mapRelationshipItemWithoutRelationship")
  default RelationshipItem mapRelationshipItemWithoutRelationship(
      @Context RelationshipItemFields fields, RelationshipItem relationshipItem) {
    if (relationshipItem == null) {
      return null;
    }

    RelationshipItem result = new RelationshipItem();
    if (fields.isIncludesTrackedEntity()) {
      result.setTrackedEntity(
          map(fields.getTrackedEntityFields(), relationshipItem.getTrackedEntity()));
    }
    if (fields.isIncludesEnrollment()) {
      result.setEnrollment(map(fields.getEnrollmentFields(), relationshipItem.getEnrollment()));
    }
    if (fields.isIncludesEvent()) {
      result.setEvent(map(relationshipItem.getEvent()));
    }
    return result;
  }

  // these are needed to make mapstruct map these collections using the entity @Mappers
  Set<Enrollment> mapEnrollments(
      @Context RelationshipItemFields.EnrollmentFields fields, Set<Enrollment> enrollments);

  // these are needed to make mapstruct map these collections using the entity @Mappers
  Set<Event> mapEvents(Set<Event> events);

  default TrackedEntity map(
      @Context RelationshipItemFields.TrackedEntityFields fields, TrackedEntity trackedEntity) {
    if (trackedEntity == null) {
      return null;
    }

    TrackedEntity result = new TrackedEntity();
    result.setUid(trackedEntity.getUid());
    result.setTrackedEntityType(
        TrackedEntityTypeMapper.INSTANCE.map(trackedEntity.getTrackedEntityType()));
    result.setCreated(trackedEntity.getCreated());
    result.setCreatedAtClient(trackedEntity.getCreatedAtClient());
    result.setLastUpdated(trackedEntity.getLastUpdated());
    result.setLastUpdatedAtClient(trackedEntity.getLastUpdatedAtClient());
    result.setOrganisationUnit(map(trackedEntity.getOrganisationUnit()));
    result.setCreatedByUserInfo(trackedEntity.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(trackedEntity.getLastUpdatedByUserInfo());
    result.setInactive(trackedEntity.isInactive());
    result.setDeleted(trackedEntity.isDeleted());
    result.setPotentialDuplicate(trackedEntity.isPotentialDuplicate());
    if (fields.isIncludesAttributes()) {
      result.setTrackedEntityAttributeValues(
          mapTrackedEntityAttributeValues(trackedEntity.getTrackedEntityAttributeValues()));
    }
    if (fields.isIncludesProgramOwners()) {
      result.setProgramOwners(mapTrackedEntityProgramOwners(trackedEntity.getProgramOwners()));
    }
    if (fields.isIncludesEnrollments()) {
      result.setEnrollments(
          mapEnrollments(fields.getEnrollmentFields(), trackedEntity.getEnrollments()));
    }
    return result;
  }

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "organisationUnit")
  @Mapping(target = "program", qualifiedByName = "mapProgramUidOnly")
  @Mapping(target = "trackedEntity", qualifiedByName = "mapTrackedEntityUidOnly")
  TrackedEntityProgramOwner map(TrackedEntityProgramOwner programOwner);

  // these are needed to make mapstruct map these collections using the entity @Mappers
  Set<TrackedEntityProgramOwner> mapTrackedEntityProgramOwners(
      Set<TrackedEntityProgramOwner> programOwners);

  default Enrollment map(
      @Context RelationshipItemFields.EnrollmentFields fields, Enrollment enrollment) {
    if (enrollment == null) {
      return null;
    }

    Enrollment result = new Enrollment();
    result.setUid(enrollment.getUid());
    result.setCreated(enrollment.getCreated());
    result.setCreatedAtClient(enrollment.getCreatedAtClient());
    result.setLastUpdated(enrollment.getLastUpdated());
    result.setLastUpdatedAtClient(enrollment.getLastUpdatedAtClient());
    result.setProgram(map(enrollment.getProgram()));
    result.setOrganisationUnit(map(enrollment.getOrganisationUnit()));
    result.setEnrollmentDate(enrollment.getEnrollmentDate());
    result.setOccurredDate(enrollment.getOccurredDate());
    result.setFollowup(enrollment.getFollowup());
    result.setCompletedDate(enrollment.getCompletedDate());
    result.setCreatedByUserInfo(enrollment.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(enrollment.getLastUpdatedByUserInfo());
    result.setNotes(mapNotes(enrollment.getNotes()));
    result.setStatus(enrollment.getStatus());
    result.setDeleted(enrollment.isDeleted());
    result.setGeometry(enrollment.getGeometry());
    result.setStoredBy(enrollment.getStoredBy());
    if (fields.isIncludesTrackedEntity() || fields.isIncludesAttributes()) {
      result.setTrackedEntity(mapTrackedEntityForEnrollment(enrollment.getTrackedEntity()));
    }
    if (fields.isIncludesEvents()) {
      result.setEvents(mapEvents(enrollment.getEvents()));
    }

    return result;
  }

  // enrollment.trackedEntity is only exported as UID
  // attribute values are needed to map enrollment.attributes as they are owned by the TE
  @Named("mapTrackedEntityForEnrollment")
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "trackedEntityAttributeValues")
  TrackedEntity mapTrackedEntityForEnrollment(TrackedEntity trackedEntity);

  // these are needed to make mapstruct map these collections using the entity @Mappers
  Set<TrackedEntityAttributeValue> mapTrackedEntityAttributeValues(
      Set<TrackedEntityAttributeValue> attributeValue);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "attribute")
  @Mapping(target = "created")
  @Mapping(target = "lastUpdated")
  TrackedEntityAttributeValue map(TrackedEntityAttributeValue attributeValue);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "code")
  @Mapping(target = "name")
  @Mapping(target = "valueType")
  TrackedEntityAttribute map(TrackedEntityAttribute attribute);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "enrollment", qualifiedByName = "mapEnrollmentForEvent")
  @Mapping(target = "programStage")
  @Mapping(target = "organisationUnit")
  @Mapping(target = "occurredDate")
  @Mapping(target = "scheduledDate")
  @Mapping(target = "created")
  @Mapping(target = "createdAtClient")
  @Mapping(target = "lastUpdated")
  @Mapping(target = "lastUpdatedAtClient")
  @Mapping(target = "attributeOptionCombo")
  @Mapping(target = "completedDate")
  @Mapping(target = "createdByUserInfo")
  @Mapping(target = "lastUpdatedByUserInfo")
  @Mapping(target = "status")
  @Mapping(target = "eventDataValues")
  @Mapping(target = "notes")
  @Mapping(target = "storedBy")
  @Mapping(target = "deleted")
  @Mapping(target = "completedBy")
  @Mapping(target = "assignedUser")
  @Mapping(target = "geometry")
  Event map(Event event);

  // relationshipItem.event.enrollment is only exported as UID
  @Named("mapEnrollmentForEvent")
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "program")
  @Mapping(target = "followup")
  Enrollment mapEnrollmentForEvent(Enrollment enrollment);

  // these are needed to make mapstruct map these collections using the entity @Mappers
  Set<EventDataValue> mapEventDataValues(Set<EventDataValue> eventDataValues);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "dataElement")
  @Mapping(target = "value")
  @Mapping(target = "created")
  @Mapping(target = "lastUpdated")
  @Mapping(target = "createdByUserInfo")
  @Mapping(target = "lastUpdatedByUserInfo")
  EventDataValue map(EventDataValue dataValue);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "categoryOptions")
  CategoryOptionCombo map(CategoryOptionCombo categoryOptionCombo);

  // these are needed to make mapstruct map these collections using the entity @Mappers
  Set<CategoryOption> mapCategoryOptions(Set<CategoryOption> categoryOptions);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  CategoryOption map(CategoryOption categoryOption);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "code")
  @Mapping(target = "name")
  OrganisationUnit map(OrganisationUnit organisationUnit);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "code")
  @Mapping(target = "name")
  @Mapping(target = "trackedEntityType")
  @Mapping(target = "programType")
  Program map(Program program);

  // programOwner is only exported as UID
  @Named("mapTrackedEntityUidOnly")
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  TrackedEntity mapTrackedEntityUidOnly(TrackedEntity trackedEntity);

  @Named("mapProgramUidOnly")
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  Program mapProgramUidOnly(Program program);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "username")
  @Mapping(target = "firstName")
  @Mapping(target = "surname")
  User map(User user);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "username")
  @Mapping(target = "firstName")
  @Mapping(target = "surname")
  User map(UserInfoSnapshot user);

  // these are needed to make mapstruct map these collections using the entity @Mappers
  List<Note> mapNotes(List<Note> notes);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "uid")
  @Mapping(target = "created")
  @Mapping(target = "noteText")
  @Mapping(target = "lastUpdatedBy")
  @Mapping(target = "creator")
  Note map(Note note);
}
