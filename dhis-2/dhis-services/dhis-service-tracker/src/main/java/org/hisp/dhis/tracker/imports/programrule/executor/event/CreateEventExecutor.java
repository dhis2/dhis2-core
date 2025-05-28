/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Zubair Asghar
 */
@RequiredArgsConstructor
public class CreateEventExecutor implements RuleActionExecutor<Event> {
  private final UID programRule;
  private final UID programStage;
  private final String scheduledAt;

  @Override
  public Optional<ProgramRuleIssue> executeRuleAction(TrackerBundle bundle, Event event) {
    if (!DateUtils.dateIsValid(scheduledAt)) {
      return Optional.of(ProgramRuleIssue.warning(programRule, ValidationCode.E1318, scheduledAt));
    }

    LocalDate localDate = LocalDate.parse(scheduledAt);
    Event scheduledEvent = new Event();
    scheduledEvent.setEvent(UID.generate());
    scheduledEvent.setEnrollment(event.getEnrollment());
    scheduledEvent.setProgramStage(MetadataIdentifier.ofUid(programStage.getValue()));
    scheduledEvent.setAttributeOptionCombo(event.getAttributeOptionCombo());
    scheduledEvent.setProgram(event.getProgram());
    scheduledEvent.setOrgUnit(event.getOrgUnit());
    scheduledEvent.setOccurredAt(null);
    scheduledEvent.setScheduledAt(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    scheduledEvent.setStatus(EventStatus.SCHEDULE);

    bundle.getEvents().add(scheduledEvent);
    bundle.setStrategy(scheduledEvent, TrackerImportStrategy.CREATE);

    return Optional.empty();
  }
}
