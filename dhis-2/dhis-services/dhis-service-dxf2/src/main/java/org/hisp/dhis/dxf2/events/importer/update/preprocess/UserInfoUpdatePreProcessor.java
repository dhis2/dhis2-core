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
package org.hisp.dhis.dxf2.events.importer.update.preprocess;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.AbstractUserInfoPreProcessor;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.springframework.stereotype.Component;

@Component
public class UserInfoUpdatePreProcessor extends AbstractUserInfoPreProcessor {

  @Override
  protected void updateDataValueUserInfo(
      ProgramStageInstance existingPsi, EventDataValue dataValue, UserInfoSnapshot userInfo) {
    if (existingPsi != null) {
      Optional<EventDataValue> existingPsiEventDataValue =
          findEventDataValues(existingPsi, dataValue);
      if (existingPsiEventDataValue.isPresent()) {
        EventDataValue eventDataValue = existingPsiEventDataValue.get();
        dataValue.setCreatedByUserInfo(eventDataValue.getCreatedByUserInfo());
      } else {
        dataValue.setCreatedByUserInfo(userInfo);
      }
    }
    dataValue.setLastUpdatedByUserInfo(userInfo);
  }

  private Optional<EventDataValue> findEventDataValues(
      ProgramStageInstance existingPsi, EventDataValue dataValue) {
    return Optional.ofNullable(existingPsi)
        .map(ProgramStageInstance::getEventDataValues)
        .orElse(Collections.emptySet())
        .stream()
        .filter(Objects::nonNull)
        .filter(
            eventDataValue -> eventDataValue.getDataElement().equals(dataValue.getDataElement()))
        .findFirst();
  }

  @Override
  protected void updateEventUserInfo(Event event, UserInfoSnapshot eventUserInfo) {
    event.setLastUpdatedByUserInfo(eventUserInfo);
  }
}
