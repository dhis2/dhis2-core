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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.export.AttributeMapper;
import org.hisp.dhis.webapi.controller.tracker.export.DataValueMapper;
import org.hisp.dhis.webapi.controller.tracker.export.NoteMapper;
import org.hisp.dhis.webapi.controller.tracker.export.ProgramOwnerMapper;
import org.hisp.dhis.webapi.controller.tracker.export.UserMapper;
import org.hisp.dhis.webapi.controller.tracker.export.event.CategoryOptionMapper;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.RelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.view.UIDMapper;
import org.hisp.dhis.webapi.controller.tracker.view.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    uses = {
      AttributeMapper.class,
      CategoryOptionMapper.class,
      DataValueMapper.class,
      InstantMapper.class,
      UIDMapper.class,
      NoteMapper.class,
      ProgramOwnerMapper.class,
      UserMapper.class,
    })
interface RelationshipItemMapper {
  RelationshipItem map(org.hisp.dhis.relationship.RelationshipItem relationshipItem);

  @Mapping(target = "trackedEntity", source = "uid")
  @Mapping(target = "trackedEntityType", source = "trackedEntityType.uid")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "createdAtClient", source = "createdAtClient")
  @Mapping(target = "updatedAt", source = "lastUpdated")
  @Mapping(target = "updatedAtClient", source = "lastUpdatedAtClient")
  @Mapping(target = "orgUnit", source = "organisationUnit.uid")
  @Mapping(target = "createdBy", source = "createdByUserInfo")
  @Mapping(target = "updatedBy", source = "lastUpdatedByUserInfo")
  @Mapping(target = "attributes", source = "trackedEntityAttributeValues")
  RelationshipItem.TrackedEntity map(TrackedEntity trackedEntity);

  @Mapping(target = "enrollment", source = "uid")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "createdAtClient", source = "createdAtClient")
  @Mapping(target = "updatedAt", source = "lastUpdated")
  @Mapping(target = "updatedAtClient", source = "lastUpdatedAtClient")
  @Mapping(target = "trackedEntity", source = "trackedEntity.uid")
  @Mapping(target = "program", source = "program.uid")
  @Mapping(target = "orgUnit", source = "organisationUnit.uid")
  @Mapping(target = "enrolledAt", source = "enrollmentDate")
  @Mapping(target = "occurredAt", source = "occurredDate")
  @Mapping(target = "followUp", source = "followup")
  @Mapping(target = "completedAt", source = "completedDate")
  @Mapping(target = "createdBy", source = "createdByUserInfo")
  @Mapping(target = "updatedBy", source = "lastUpdatedByUserInfo")
  @Mapping(target = "attributes", source = "trackedEntity.trackedEntityAttributeValues")
  @Mapping(target = "notes", source = "notes")
  RelationshipItem.Enrollment map(Enrollment enrollment);

  @Mapping(target = "event", source = "uid")
  @Mapping(target = "program", source = "enrollment.program.uid")
  @Mapping(target = "programStage", source = "programStage.uid")
  @Mapping(target = "enrollment", source = "enrollment.uid")
  @Mapping(target = "orgUnit", source = "organisationUnit.uid")
  @Mapping(target = "occurredAt", source = "occurredDate")
  @Mapping(target = "scheduledAt", source = "scheduledDate")
  @Mapping(target = "followUp", source = "enrollment.followup")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "createdAtClient", source = "createdAtClient")
  @Mapping(target = "updatedAt", source = "lastUpdated")
  @Mapping(target = "updatedAtClient", source = "lastUpdatedAtClient")
  @Mapping(target = "attributeOptionCombo", source = "attributeOptionCombo.uid")
  @Mapping(target = "attributeCategoryOptions", source = "attributeOptionCombo.categoryOptions")
  @Mapping(target = "completedAt", source = "completedDate")
  @Mapping(target = "createdBy", source = "createdByUserInfo")
  @Mapping(target = "updatedBy", source = "lastUpdatedByUserInfo")
  @Mapping(target = "dataValues", source = "eventDataValues")
  @Mapping(target = "notes", source = "notes")
  RelationshipItem.Event map(TrackerEvent event);

  @Mapping(target = "displayName", source = "name")
  User map(org.hisp.dhis.user.User user);
}
