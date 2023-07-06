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
package org.hisp.dhis.tracker.programrule.implementers;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.programrule.*;
import org.hisp.dhis.tracker.report.TrackerErrorCode;

/**
 * This implementer check if there are errors or warnings the {@link TrackerBundle} @Author Enrico
 * Colasante
 */
public abstract class ErrorWarningImplementer<T extends RuleActionMessage>
    extends AbstractRuleActionImplementer<T> implements RuleActionImplementer {
  public abstract boolean isOnComplete();

  public abstract IssueType getIssueType();

  @Override
  public String getField(RuleActionMessage ruleAction) {
    return ruleAction.field();
  }

  @Override
  public String getContent(RuleActionMessage ruleAction) {
    return ruleAction.content();
  }

  @Override
  List<ProgramRuleIssue> applyToEnrollments(
      Enrollment enrollment,
      List<EnrollmentActionRule> enrollmentActionRules,
      TrackerBundle bundle) {
    if (needsToRun(enrollment)) {
      return parseErrors(enrollmentActionRules);
    }
    return Collections.emptyList();
  }

  @Override
  public List<ProgramRuleIssue> applyToEvents(
      Event event, List<EventActionRule> actionRules, TrackerBundle bundle) {
    if (needsToRun(event)) {
      return parseErrors(actionRules);
    }
    return Collections.emptyList();
  }

  private <U extends ActionRule> List<ProgramRuleIssue> parseErrors(List<U> effects) {
    return effects.stream()
        .map(
            actionRule -> {
              String field = actionRule.getField();
              String content = actionRule.getContent();
              String data = actionRule.getData();

              StringBuilder stringBuilder = new StringBuilder(content);
              if (!StringUtils.isEmpty(data)) {
                stringBuilder.append(" ").append(data);
              }
              if (!StringUtils.isEmpty(field)) {
                stringBuilder.append(" (").append(field).append(")");
              }

              return Pair.of(actionRule.getRuleUid(), stringBuilder.toString());
            })
        .map(
            message ->
                new ProgramRuleIssue(
                    message.getKey(),
                    TrackerErrorCode.E1300,
                    Lists.newArrayList(message.getValue()),
                    getIssueType()))
        .collect(Collectors.toList());
  }

  private boolean needsToRun(Event event) {
    if (isOnComplete()) {
      return Objects.equals(EventStatus.COMPLETED, event.getStatus());
    } else {
      return true;
    }
  }

  private boolean needsToRun(Enrollment enrollment) {
    if (isOnComplete()) {
      return Objects.equals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
    } else {
      return true;
    }
  }
}
