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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
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
      Map.Entry<String, List<EnrollmentActionRule>> enrollmentActionRules, TrackerBundle bundle) {
    List<String> filteredEnrollments =
        bundle.getEnrollments().stream()
            .filter(filterEnrollment())
            .map(Enrollment::getEnrollment)
            .collect(Collectors.toList());

    if (filteredEnrollments.contains(enrollmentActionRules.getKey())) {
      return parseErrors(enrollmentActionRules.getValue());
    }

    return Lists.newArrayList();
  }

  @Override
  public List<ProgramRuleIssue> applyToEvents(
      Map.Entry<String, List<EventActionRule>> actionRules, TrackerBundle bundle) {
    List<String> filteredEvents =
        bundle.getEvents().stream()
            .filter(filterEvent())
            .map(Event::getEvent)
            .collect(Collectors.toList());

    if (filteredEvents.contains(actionRules.getKey())) {
      return parseErrors(actionRules.getValue());
    }

    return Lists.newArrayList();
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

  private Predicate<Event> filterEvent() {
    if (isOnComplete()) {
      return e -> Objects.equals(EventStatus.COMPLETED, e.getStatus());
    } else {
      return e -> true;
    }
  }

  private Predicate<Enrollment> filterEnrollment() {
    if (isOnComplete()) {
      return e -> Objects.equals(EnrollmentStatus.COMPLETED, e.getStatus());
    } else {
      return e -> true;
    }
  }
}
