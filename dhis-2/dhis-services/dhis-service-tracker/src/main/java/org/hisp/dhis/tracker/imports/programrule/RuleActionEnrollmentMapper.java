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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.programrule.engine.ValidationEffect;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.AssignAttributeExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.RuleEngineErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ScheduleEventExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.SetMandatoryFieldExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowErrorExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowErrorOnCompleteExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowWarningExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.ShowWarningOnCompleteExecutor;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.tracker.imports.programrule.RuleActionEnrollmentMapper")
@RequiredArgsConstructor
class RuleActionEnrollmentMapper {
  private final SystemSettingsProvider settingsProvider;

  public Map<Enrollment, List<RuleActionExecutor<Enrollment>>> mapRuleEffects(
      Map<UID, List<ValidationEffect>> enrollmentValidationEffects, TrackerBundle bundle) {
    return enrollmentValidationEffects.keySet().stream()
        .filter(e -> bundle.findEnrollmentByUid(e).isPresent())
        .collect(
            Collectors.toMap(
                e -> bundle.findEnrollmentByUid(e).get(),
                e ->
                    mapRuleEffects(
                        bundle.findEnrollmentByUid(e).get(),
                        enrollmentValidationEffects.get(e),
                        bundle)));
  }

  private List<RuleActionExecutor<Enrollment>> mapRuleEffects(
      Enrollment enrollment, List<ValidationEffect> ruleValidationEffects, TrackerBundle bundle) {
    List<Attribute> payloadTeiAttributes =
        bundle
            .findTrackedEntityByUid(enrollment.getTrackedEntity())
            .map(TrackedEntity::getAttributes)
            .orElse(Collections.emptyList());
    List<Attribute> attributes = ListUtils.union(enrollment.getAttributes(), payloadTeiAttributes);

    return ruleValidationEffects.stream()
        .map(effect -> buildEnrollmentRuleActionExecutor(effect, attributes))
        .toList();
  }

  private RuleActionExecutor<Enrollment> buildEnrollmentRuleActionExecutor(
      ValidationEffect validationEffect, List<Attribute> attributes) {
    return switch (validationEffect.type()) {
      case ASSIGN ->
          new AssignAttributeExecutor(
              settingsProvider,
              validationEffect.rule(),
              validationEffect.data(),
              validationEffect.field(),
              attributes);
      case SET_MANDATORY_FIELD ->
          new SetMandatoryFieldExecutor(validationEffect.rule(), validationEffect.field());
      case SHOW_ERROR -> new ShowErrorExecutor(validationEffect);
      case SHOW_WARNING -> new ShowWarningExecutor(validationEffect);
      case SHOW_ERROR_ON_COMPLETE -> new ShowErrorOnCompleteExecutor(validationEffect);
      case SHOW_WARNING_ON_COMPLETE -> new ShowWarningOnCompleteExecutor(validationEffect);
      case SCHEDULE_EVENT ->
          new ScheduleEventExecutor(
              validationEffect.rule(), validationEffect.field(), validationEffect.data());
      case RAISE_ERROR ->
          new RuleEngineErrorExecutor(validationEffect.rule(), validationEffect.data());
    };
  }
}
