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
package org.hisp.dhis.tracker.imports.preheat.mappers;

import org.hisp.dhis.program.TrackerEvent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = {
      DebugMapper.class,
      ProgramStageMapper.class,
      OrganisationUnitMapper.class,
      EnrollmentMapper.class
    })
public interface TrackerEventMapper extends PreheatMapper<TrackerEvent> {
  TrackerEventMapper INSTANCE = Mappers.getMapper(TrackerEventMapper.class);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id")
  @Mapping(target = "uid")
  @Mapping(target = "code")
  @Mapping(target = "user")
  @Mapping(target = "enrollment")
  @Mapping(target = "programStage")
  @Mapping(target = "status")
  @Mapping(target = "organisationUnit")
  @Mapping(target = "created")
  @Mapping(target = "eventDataValues")
  @Mapping(target = "notes")
  @Mapping(target = "scheduledDate")
  @Mapping(target = "occurredDate")
  @Mapping(target = "completedDate")
  @Mapping(target = "completedBy")
  @Mapping(target = "deleted")
  @Mapping(target = "createdByUserInfo")
  @Mapping(target = "lastUpdatedByUserInfo")
  @Mapping(target = "geometry")
  TrackerEvent map(TrackerEvent event);
}
