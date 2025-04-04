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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static java.util.Map.entry;

import java.util.Map;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.export.AttributeMapper;
import org.hisp.dhis.webapi.controller.tracker.export.NoteMapper;
import org.hisp.dhis.webapi.controller.tracker.export.UserMapper;
import org.hisp.dhis.webapi.controller.tracker.export.event.EventMapper;
import org.hisp.dhis.webapi.controller.tracker.export.relationship.RelationshipMapper;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.UIDMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    uses = {
      AttributeMapper.class,
      EventMapper.class,
      InstantMapper.class,
      UIDMapper.class,
      NoteMapper.class,
      RelationshipMapper.class,
      UserMapper.class
    })
public interface EnrollmentMapper {
  /**
   * Enrollments can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.program.Enrollment}.
   */
  Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("completedAt", "completedDate"),
          entry("createdAt", "created"),
          entry("createdAtClient", "createdAtClient"),
          entry("enrolledAt", "enrollmentDate"),
          entry("updatedAt", "lastUpdated"),
          entry("updatedAtClient", "lastUpdatedAtClient"));

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
  @Mapping(target = "relationships", source = "relationshipItems")
  @Mapping(target = "attributes", source = "trackedEntity.trackedEntityAttributeValues")
  @Mapping(target = "notes", source = "notes")
  org.hisp.dhis.webapi.controller.tracker.view.Enrollment map(Enrollment enrollment);
}
