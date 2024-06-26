/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.programrule.api;

import static org.hisp.dhis.programrule.engine.RuleActionKey.NOTIFICATION;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleEffects;

/**
 * This class holds the action results from rule-engine organized by effect type ({@link
 * ValidationEffect} and {@link NotificationEffect}) and by tracker entity ({@link Enrollment} and
 * {@link Event}
 */
@Getter
public class RuleEngineEffects {
  private final Map<String, List<ValidationEffect>> enrollmentValidationEffects;
  private final Map<String, List<ValidationEffect>> eventValidationEffects;
  private final Map<String, List<NotificationEffect>> enrollmentNotificationEffects;
  private final Map<String, List<NotificationEffect>> eventNotificationEffects;

  private RuleEngineEffects(
      Map<String, List<ValidationEffect>> enrollmentValidationEffects,
      Map<String, List<ValidationEffect>> eventValidationEffects,
      Map<String, List<NotificationEffect>> enrollmentNotificationEffects,
      Map<String, List<NotificationEffect>> eventNotificationEffects) {
    this.enrollmentValidationEffects = enrollmentValidationEffects;
    this.eventValidationEffects = eventValidationEffects;
    this.enrollmentNotificationEffects = enrollmentNotificationEffects;
    this.eventNotificationEffects = eventNotificationEffects;
  }

  public static RuleEngineEffects empty() {
    return new RuleEngineEffects(Map.of(), Map.of(), Map.of(), Map.of());
  }

  public static RuleEngineEffects fromRuleEffects(List<RuleEffects> effects) {
    return map(effects);
  }

  public static RuleEngineEffects merge(RuleEngineEffects effects, RuleEngineEffects effects2) {
    Map<String, List<ValidationEffect>> enrollmentValidationEffects =
        merge(effects.enrollmentValidationEffects, effects2.enrollmentValidationEffects);
    Map<String, List<ValidationEffect>> eventValidationEffects =
        merge(effects.eventValidationEffects, effects2.eventValidationEffects);
    Map<String, List<NotificationEffect>> enrollmentNotificationEffects =
        merge(effects.enrollmentNotificationEffects, effects2.enrollmentNotificationEffects);
    Map<String, List<NotificationEffect>> eventNotificationEffects =
        merge(effects.eventNotificationEffects, effects2.eventNotificationEffects);

    return new RuleEngineEffects(
        enrollmentValidationEffects,
        eventValidationEffects,
        enrollmentNotificationEffects,
        eventNotificationEffects);
  }

  private static <T> Map<String, List<T>> merge(
      Map<String, List<T>> effects, Map<String, List<T>> effects2) {
    return Stream.of(effects.entrySet(), effects2.entrySet())
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static RuleEngineEffects map(List<RuleEffects> ruleEffects) {
    Map<String, List<ValidationEffect>> enrollmentValidationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEnrollment)
            .collect(
                Collectors.toMap(
                    RuleEffects::getTrackerObjectUid,
                    e -> mapValidationEffect(e.getRuleEffects())));
    Map<String, List<ValidationEffect>> eventValidationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEvent)
            .collect(
                Collectors.toMap(
                    RuleEffects::getTrackerObjectUid,
                    e -> mapValidationEffect(e.getRuleEffects())));
    Map<String, List<NotificationEffect>> enrollmentNotificationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEnrollment)
            .collect(
                Collectors.toMap(
                    RuleEffects::getTrackerObjectUid,
                    e -> mapNotificationEffect(e.getRuleEffects())));
    Map<String, List<NotificationEffect>> eventNotificationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEvent)
            .collect(
                Collectors.toMap(
                    RuleEffects::getTrackerObjectUid,
                    e -> mapNotificationEffect(e.getRuleEffects())));
    return new RuleEngineEffects(
        enrollmentValidationEffects,
        eventValidationEffects,
        enrollmentNotificationEffects,
        eventNotificationEffects);
  }

  private static List<ValidationEffect> mapValidationEffect(List<RuleEffect> effects) {
    return effects.stream()
        .filter(e -> ValidationActionType.contains(e.getRuleAction().getType()))
        .map(
            e ->
                new ValidationEffect(
                    e.getRuleId(),
                    e.getData(),
                    e.getRuleAction().field(),
                    e.getRuleAction().content(),
                    ValidationActionType.valueOf(e.getRuleAction().getType())))
        .toList();
  }

  private static List<NotificationEffect> mapNotificationEffect(List<RuleEffect> effects) {
    return effects.stream()
        .filter(e -> NotificationActionType.contains(e.getRuleAction().getType()))
        .map(
            e ->
                new NotificationEffect(
                    e.getRuleId(),
                    e.getData(),
                    e.getRuleAction().getValues().get(NOTIFICATION),
                    NotificationActionType.valueOf(e.getRuleAction().getType())))
        .toList();
  }
}
