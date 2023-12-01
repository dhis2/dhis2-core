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
package org.hisp.dhis.tracker.imports.programrule;

import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.needsToValidateDataValues;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleActionError;
import org.hisp.dhis.rules.models.RuleActionErrorOnCompletion;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleActionShowError;
import org.hisp.dhis.rules.models.RuleActionShowWarning;
import org.hisp.dhis.rules.models.RuleActionWarningOnCompletion;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.ValidationRuleAction;
import org.hisp.dhis.tracker.imports.programrule.executor.event.AssignDataValueExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.RuleEngineErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.SetMandatoryFieldExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowErrorOnCompleteExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowWarningExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.ShowWarningOnCompleteExecutor;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.tracker.imports.programrule.RuleActionEventMapper")
@RequiredArgsConstructor
class RuleActionEventMapper {
  private final SystemSettingManager systemSettingManager;

  public Map<Event, List<RuleActionExecutor<Event>>> mapRuleEffects(
      List<RuleEffects> ruleEffects, TrackerBundle bundle) {
    return ruleEffects.stream()
        .filter(RuleEffects::isEvent)
        .filter(e -> bundle.findEventByUid(e.getTrackerObjectUid()).isPresent())
        .collect(
            Collectors.toMap(
                e -> bundle.findEventByUid(e.getTrackerObjectUid()).get(),
                e ->
                    mapRuleEffects(
                        bundle.findEventByUid(e.getTrackerObjectUid()).get(), e, bundle)));
  }

  private List<RuleActionExecutor<Event>> mapRuleEffects(
      Event event, RuleEffects ruleEffects, TrackerBundle bundle) {
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());

    return ruleEffects.getRuleEffects().stream()
        .map(
            effect ->
                buildEventRuleActionExecutor(
                    effect.ruleId(), effect.data(), effect.ruleAction(), event.getDataValues()))
        .filter(Objects::nonNull)
        .filter(
            executor -> isDataElementPartOfProgramStage(executor.getDataElementUid(), programStage))
        .filter(executor -> needsToValidateDataValues(event, programStage))
        .collect(Collectors.toList());
  }

  private RuleActionExecutor<Event> buildEventRuleActionExecutor(
      String ruleId, String data, RuleAction ruleAction, Set<DataValue> dataValues) {
    if (ruleAction instanceof RuleActionAssign) {
      RuleActionAssign action = (RuleActionAssign) ruleAction;
      return new AssignDataValueExecutor(
          systemSettingManager, ruleId, data, action.field(), dataValues);
    }
    if (ruleAction instanceof RuleActionSetMandatoryField) {
      RuleActionSetMandatoryField action = (RuleActionSetMandatoryField) ruleAction;
      return new SetMandatoryFieldExecutor(ruleId, action.field());
    }
    if (ruleAction instanceof RuleActionShowError) {
      RuleActionShowError action = (RuleActionShowError) ruleAction;
      return new ShowErrorExecutor(
          new ValidationRuleAction(ruleId, data, action.field(), action.content()));
    }
    if (ruleAction instanceof RuleActionShowWarning) {
      RuleActionShowWarning action = (RuleActionShowWarning) ruleAction;
      return new ShowWarningExecutor(
          new ValidationRuleAction(ruleId, data, action.field(), action.content()));
    }
    if (ruleAction instanceof RuleActionErrorOnCompletion) {
      RuleActionErrorOnCompletion action = (RuleActionErrorOnCompletion) ruleAction;
      return new ShowErrorOnCompleteExecutor(
          new ValidationRuleAction(ruleId, data, action.field(), action.content()));
    }
    if (ruleAction instanceof RuleActionWarningOnCompletion) {
      RuleActionWarningOnCompletion action = (RuleActionWarningOnCompletion) ruleAction;
      return new ShowWarningOnCompleteExecutor(
          new ValidationRuleAction(ruleId, data, action.field(), action.content()));
    }
    if (ruleAction instanceof RuleActionError) {
      return new RuleEngineErrorExecutor(ruleId, data);
    }
    return null;
  }

  private boolean isDataElementPartOfProgramStage(
      String dataElementUid, ProgramStage programStage) {
    if (StringUtils.isEmpty(dataElementUid)) {
      return true;
    }

    return programStage.getDataElements().stream()
        .map(IdentifiableObject::getUid)
        .anyMatch(de -> de.equals(dataElementUid));
  }
}
