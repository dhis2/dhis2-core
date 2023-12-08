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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleActionError;
import org.hisp.dhis.rules.models.RuleActionMessage;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.ValidationRuleAction;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.AssignAttributeExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.RuleEngineErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.SetMandatoryFieldExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowErrorOnCompleteExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowWarningExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowWarningOnCompleteExecutor;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.tracker.imports.programrule.RuleActionEnrollmentMapper")
@RequiredArgsConstructor
class RuleActionEnrollmentMapper {
  private final SystemSettingManager systemSettingManager;

  public Map<Enrollment, List<RuleActionExecutor<Enrollment>>> mapRuleEffects(
      List<RuleEffects> ruleEffects, TrackerBundle bundle) {
    return ruleEffects.stream()
        .filter(RuleEffects::isEnrollment)
        .filter(e -> bundle.findEnrollmentByUid(e.getTrackerObjectUid()).isPresent())
        .collect(
            Collectors.toMap(
                e -> bundle.findEnrollmentByUid(e.getTrackerObjectUid()).get(),
                e ->
                    mapRuleEffects(
                        bundle.findEnrollmentByUid(e.getTrackerObjectUid()).get(), e, bundle)));
  }

  private List<RuleActionExecutor<Enrollment>> mapRuleEffects(
      Enrollment enrollment, RuleEffects ruleEffects, TrackerBundle bundle) {
    List<Attribute> payloadTeiAttributes =
        bundle
            .findTrackedEntityByUid(enrollment.getTrackedEntity())
            .map(TrackedEntity::getAttributes)
            .orElse(Collections.emptyList());
    List<Attribute> attributes = ListUtils.union(enrollment.getAttributes(), payloadTeiAttributes);

    return ruleEffects.getRuleEffects().stream()
        .map(
            effect ->
                buildEnrollmentRuleActionExecutor(
                    effect.getRuleId(), effect.data(), effect.ruleAction(), attributes))
        .filter(Objects::nonNull)
        .toList();
  }

  private RuleActionExecutor<Enrollment> buildEnrollmentRuleActionExecutor(
      String ruleId, String data, RuleAction ruleAction, List<Attribute> attributes) {
    if (ruleAction instanceof RuleActionAssign action) {
      return new AssignAttributeExecutor(
          systemSettingManager, ruleId, data, action.getField(), attributes);
    }
    if (ruleAction instanceof RuleActionSetMandatoryField action) {
      return new SetMandatoryFieldExecutor(ruleId, action.getField());
    }
    if (ruleAction instanceof RuleActionMessage action
        && action.getType() == RuleActionMessage.Type.SHOW_ERROR) {
      return new ShowErrorExecutor(
          new ValidationRuleAction(ruleId, data, action.getField(), action.getContent()));
    }
    if (ruleAction instanceof RuleActionMessage action
        && action.getType() == RuleActionMessage.Type.SHOW_WARNING) {
      return new ShowWarningExecutor(
          new ValidationRuleAction(ruleId, data, action.getField(), action.getContent()));
    }
    if (ruleAction instanceof RuleActionMessage action
        && action.getType() == RuleActionMessage.Type.ERROR_ON_COMPILATION) {
      return new ShowErrorOnCompleteExecutor(
          new ValidationRuleAction(ruleId, data, action.getField(), action.getContent()));
    }
    if (ruleAction instanceof RuleActionMessage action
        && action.getType() == RuleActionMessage.Type.WARNING_ON_COMPILATION) {
      return new ShowWarningOnCompleteExecutor(
          new ValidationRuleAction(ruleId, data, action.getField(), action.getContent()));
    }
    if (ruleAction instanceof RuleActionError) {
      return new RuleEngineErrorExecutor(ruleId, data);
    }
    return null;
  }
}
