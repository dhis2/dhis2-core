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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static java.util.Map.entry;

import java.util.Map;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.webapi.controller.tracker.export.DataValueMapper;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.MetadataMapper;
import org.hisp.dhis.webapi.controller.tracker.export.NoteMapper;
import org.hisp.dhis.webapi.controller.tracker.export.UserMapper;
import org.hisp.dhis.webapi.controller.tracker.export.relationship.RelationshipMapper;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.UIDMapper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    uses = {
      DataValueMapper.class,
      InstantMapper.class,
      UIDMapper.class,
      NoteMapper.class,
      RelationshipMapper.class,
      UserMapper.class,
      MetadataMapper.class
    })
public interface EventMapper {

  /** Events can be ordered by given fields which correspond to fields on {@link TrackerEvent}. */
  Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("assignedUser", "assignedUser"),
          entry("assignedUserDisplayName", "assignedUser.displayName"),
          entry("attributeOptionCombo", "attributeOptionCombo.uid"),
          entry("completedAt", "completedDate"),
          entry("completedBy", "completedBy"),
          entry("createdAt", "created"),
          entry("createdAtClient", "createdAtClient"),
          entry("createdBy", "createdBy"),
          entry("deleted", "deleted"),
          entry("enrolledAt", "enrollment.enrollmentDate"),
          entry("enrollment", "enrollment.uid"),
          entry("enrollmentStatus", "enrollment.status"),
          entry("event", "uid"),
          entry("followUp", "enrollment.followUp"),
          entry("occurredAt", "occurredDate"),
          entry("orgUnit", "organisationUnit.uid"),
          entry("program", "enrollment.program.uid"),
          entry("programStage", "programStage.uid"),
          entry("scheduledAt", "scheduledDate"),
          entry("status", "status"),
          entry("storedBy", "storedBy"),
          entry("trackedEntity", "enrollment.trackedEntity.uid"),
          entry("updatedAt", "lastUpdated"),
          entry("updatedAtClient", "lastUpdatedAtClient"),
          entry("updatedBy", "lastUpdatedBy"));

  default org.hisp.dhis.webapi.controller.tracker.view.Event map(TrackerEvent event) {
    // events as part of enrollments and relationships are always exported using idScheme=UID
    TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder().build();
    return map(idSchemeParams, new MappingErrors(idSchemeParams), event);
  }

  @Mapping(target = "event", source = "uid")
  @Mapping(target = "program", source = "enrollment.program")
  @Mapping(target = "programStage", source = "programStage")
  @Mapping(target = "enrollment", source = "enrollment.uid")
  @Mapping(target = "trackedEntity", source = "enrollment.trackedEntity.uid")
  @Mapping(target = "orgUnit", source = "organisationUnit")
  @Mapping(target = "occurredAt", source = "occurredDate")
  @Mapping(target = "scheduledAt", source = "scheduledDate")
  @Mapping(target = "followUp", source = "enrollment.followup")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "createdAtClient", source = "createdAtClient")
  @Mapping(target = "updatedAt", source = "lastUpdated")
  @Mapping(target = "updatedAtClient", source = "lastUpdatedAtClient")
  @Mapping(target = "attributeOptionCombo", source = "attributeOptionCombo")
  @Mapping(target = "attributeCategoryOptions", source = "attributeOptionCombo.categoryOptions")
  @Mapping(target = "completedAt", source = "completedDate")
  @Mapping(target = "createdBy", source = "createdByUserInfo")
  @Mapping(target = "updatedBy", source = "lastUpdatedByUserInfo")
  @Mapping(target = "dataValues", source = "eventDataValues")
  @Mapping(target = "relationships", source = "relationshipItems")
  @Mapping(target = "notes", source = "notes")
  org.hisp.dhis.webapi.controller.tracker.view.Event map(
      @Context TrackerIdSchemeParams idSchemeParams,
      @Context MappingErrors errors,
      TrackerEvent event);

  @Mapping(target = "event", source = "uid")
  @Mapping(target = "program", source = "enrollment.program")
  @Mapping(target = "programStage", source = "programStage")
  @Mapping(target = "enrollment", source = "enrollment.uid")
  @Mapping(target = "orgUnit", source = "organisationUnit")
  @Mapping(target = "occurredAt", source = "occurredDate")
  @Mapping(target = "followUp", source = "enrollment.followup")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "createdAtClient", source = "createdAtClient")
  @Mapping(target = "updatedAt", source = "lastUpdated")
  @Mapping(target = "updatedAtClient", source = "lastUpdatedAtClient")
  @Mapping(target = "attributeOptionCombo", source = "attributeOptionCombo")
  @Mapping(target = "attributeCategoryOptions", source = "attributeOptionCombo.categoryOptions")
  @Mapping(target = "completedAt", source = "completedDate")
  @Mapping(target = "createdBy", source = "createdByUserInfo")
  @Mapping(target = "updatedBy", source = "lastUpdatedByUserInfo")
  @Mapping(target = "dataValues", source = "eventDataValues")
  @Mapping(target = "relationships", source = "relationshipItems")
  @Mapping(target = "notes", source = "notes")
  org.hisp.dhis.webapi.controller.tracker.view.Event map(
      @Context TrackerIdSchemeParams idSchemeParams,
      @Context MappingErrors errors,
      SingleEvent event);
}
