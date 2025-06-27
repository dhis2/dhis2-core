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
package org.hisp.dhis.tracker.imports.programrule;

import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.needsToValidateDataValues;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.programrule.engine.ValidationEffect;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.AssignDataValueExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.RuleEngineErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ScheduleEventExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.SetMandatoryFieldExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowErrorOnCompleteExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowWarningExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowWarningOnCompleteExecutor;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.tracker.imports.programrule.RuleActionEventMapper")
@RequiredArgsConstructor
class RuleActionEventMapper {
  private final SystemSettingsProvider settingsProvider;
  private final AclService aclService;

  public Map<Event, List<RuleActionExecutor<Event>>> mapRuleEffects(
      Map<UID, List<ValidationEffect>> eventValidationEffects, TrackerBundle bundle) {
    return eventValidationEffects.keySet().stream()
        .filter(e -> bundle.findEventByUid(e).isPresent())
        .collect(
            Collectors.toMap(
                e -> bundle.findEventByUid(e).get(),
                e ->
                    mapRuleEffects(
                        bundle.findEventByUid(e).get(), eventValidationEffects.get(e), bundle)));
  }

  private List<RuleActionExecutor<Event>> mapRuleEffects(
      Event event, List<ValidationEffect> ruleValidationEffects, TrackerBundle bundle) {
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());

    return ruleValidationEffects.stream()
        .filter(executor -> needsToValidateDataValues(event, programStage))
        .map(effect -> buildEventRuleActionExecutor(effect, event.getDataValues()))
        .filter(
            executor -> isDataElementPartOfProgramStage(executor.getDataElementUid(), programStage))
        .toList();
  }

  private RuleActionExecutor<Event> buildEventRuleActionExecutor(
      ValidationEffect validationEffect, Set<DataValue> dataValues) {
    return switch (validationEffect.type()) {
      case ASSIGN ->
          new AssignDataValueExecutor(
              settingsProvider,
              validationEffect.rule(),
              validationEffect.data(),
              validationEffect.field(),
              dataValues);
      case SET_MANDATORY_FIELD ->
          new SetMandatoryFieldExecutor(validationEffect.rule(), validationEffect.field());
      case SHOW_ERROR -> new ShowErrorExecutor(validationEffect);
      case SHOW_WARNING -> new ShowWarningExecutor(validationEffect);
      case SHOW_ERROR_ON_COMPLETE -> new ShowErrorOnCompleteExecutor(validationEffect);
      case SHOW_WARNING_ON_COMPLETE -> new ShowWarningOnCompleteExecutor(validationEffect);
      case SCHEDULE_EVENT ->
          new ScheduleEventExecutor(
              validationEffect.rule(),
              validationEffect.field(),
              validationEffect.data(),
              aclService);
      case RAISE_ERROR ->
          new RuleEngineErrorExecutor(validationEffect.rule(), validationEffect.data());
    };
  }

  private boolean isDataElementPartOfProgramStage(UID dataElementUid, ProgramStage programStage) {
    if (dataElementUid == null) {
      return true;
    }

    return programStage.getDataElements().stream()
        .map(IdentifiableObject::getUid)
        .anyMatch(de -> de.equals(dataElementUid.getValue()));
  }
}
