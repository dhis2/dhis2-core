/*
 * Copyright (c) 2004-2024, University of Oslo
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

import org.hisp.dhis.webapi.controller.tracker.view.EventChangeLog;
import org.hisp.dhis.webapi.controller.tracker.view.EventChangeLog.DataValueChange;
import org.hisp.dhis.webapi.controller.tracker.view.EventChangeLog.FieldChange;
import org.hisp.dhis.webapi.controller.tracker.view.UIDMapper;
import org.hisp.dhis.webapi.controller.tracker.view.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(uses = {UIDMapper.class})
public interface EventChangeLogMapper {

  @Mapping(target = "createdBy", source = "eventChangeLog")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "type", source = "changeLogType")
  @Mapping(
      target = "change.dataValue",
      source = "eventChangeLog",
      qualifiedByName = "mapIfDataValueChangeExists")
  @Mapping(
      target = "change.eventField",
      source = "eventChangeLog",
      qualifiedByName = "mapIfEventFieldChangeExists")
  EventChangeLog map(org.hisp.dhis.tracker.export.event.EventChangeLog eventChangeLog);

  @Mapping(target = "uid", source = "createdBy.uid")
  @Mapping(target = "username", source = "createdBy.username")
  @Mapping(target = "firstName", source = "createdBy.firstName")
  @Mapping(target = "surname", source = "createdBy.surname")
  User mapUser(org.hisp.dhis.tracker.export.event.EventChangeLog eventChangeLog);

  @Mapping(target = "dataElement", source = "dataElement.uid")
  @Mapping(target = "previousValue", source = "previousValue")
  @Mapping(target = "currentValue", source = "currentValue")
  DataValueChange mapDataValueChange(
      org.hisp.dhis.tracker.export.event.EventChangeLog eventChangeLog);

  @Named("mapIfDataValueChangeExists")
  default DataValueChange mapIfDataValueChangeExists(
      org.hisp.dhis.tracker.export.event.EventChangeLog eventChangeLog) {
    if (eventChangeLog.dataElement() == null) {
      return null;
    }
    return mapDataValueChange(eventChangeLog);
  }

  @Mapping(target = "field", source = "eventField")
  @Mapping(target = "previousValue", source = "previousValue")
  @Mapping(target = "currentValue", source = "currentValue")
  FieldChange mapEventFieldChange(org.hisp.dhis.tracker.export.event.EventChangeLog eventChangeLog);

  @Named("mapIfEventFieldChangeExists")
  default FieldChange mapIfEventFieldExists(
      org.hisp.dhis.tracker.export.event.EventChangeLog eventChangeLog) {
    if (eventChangeLog.eventField() == null) {
      return null;
    }
    return mapEventFieldChange(eventChangeLog);
  }
}
