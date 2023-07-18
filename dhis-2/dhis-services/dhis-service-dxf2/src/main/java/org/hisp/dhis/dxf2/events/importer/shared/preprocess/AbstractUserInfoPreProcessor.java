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
package org.hisp.dhis.dxf2.events.importer.shared.preprocess;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.EventImporterUserService;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.ServiceDelegator;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.user.User;

public abstract class AbstractUserInfoPreProcessor implements Processor {

  @Override
  public void process(Event event, WorkContext workContext) {
    User user =
        findUserFromImportOptions(workContext.getImportOptions())
            .orElseGet(() -> getUser(workContext));

    if (user != null) {
      UserInfoSnapshot userInfo = UserInfoSnapshot.from(user);
      updateEventUserInfo(event, userInfo);

      Set<String> updatableDataValues =
          Optional.ofNullable(event)
              .map(Event::getDataValues)
              .orElse(Collections.emptySet())
              .stream()
              .map(DataValue::getDataElement)
              .collect(Collectors.toSet());

      Set<EventDataValue> eventDataValuesToUpdate =
          getWorkContextDataValueMapEntry(workContext, event.getUid()).stream()
              .filter(
                  eventDataValue -> updatableDataValues.contains(eventDataValue.getDataElement()))
              .collect(Collectors.toSet());

      updateDataValuesUserInfo(
          getExistingPsi(workContext, event.getUid()), eventDataValuesToUpdate, userInfo);
    }
  }

  private ProgramStageInstance getExistingPsi(WorkContext workContext, String uid) {
    return Optional.ofNullable(workContext)
        .map(WorkContext::getProgramStageInstanceMap)
        .orElse(Collections.emptyMap())
        .get(uid);
  }

  protected Set<EventDataValue> getWorkContextDataValueMapEntry(
      WorkContext workContext, String uid) {
    return Optional.ofNullable(workContext)
        .map(WorkContext::getEventDataValueMap)
        .orElse(Collections.emptyMap())
        .get(uid);
  }

  protected void updateDataValuesUserInfo(
      ProgramStageInstance existingPsi,
      Set<EventDataValue> eventDataValueMap,
      UserInfoSnapshot userInfo) {
    Optional.ofNullable(eventDataValueMap)
        .orElse(Collections.emptySet())
        .forEach(dataValue -> updateDataValueUserInfo(existingPsi, dataValue, userInfo));
  }

  protected abstract void updateDataValueUserInfo(
      ProgramStageInstance existingPsi, EventDataValue dataValue, UserInfoSnapshot userInfo);

  protected abstract void updateEventUserInfo(Event event, UserInfoSnapshot eventUserInfo);

  private User getUser(WorkContext workContext) {
    return Optional.ofNullable(workContext)
        .map(WorkContext::getServiceDelegator)
        .map(ServiceDelegator::getEventImporterUserService)
        .map(EventImporterUserService::getCurrentUser)
        .orElse(null);
  }

  private Optional<User> findUserFromImportOptions(ImportOptions importOptions) {
    return Optional.ofNullable(importOptions).map(ImportOptions::getUser);
  }
}
