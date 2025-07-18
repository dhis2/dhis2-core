/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.engine;

import static org.hisp.dhis.programrule.ProgramRuleActionType.SCHEDULEMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SENDMESSAGE;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.NOTIFICATION;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.util.DateUtils;

/**
 * This class holds the action results from rule-engine organized by effect type ({@link
 * ValidationEffect} and {@link Notification}) and by tracker entity ({@link Enrollment} and {@link
 * TrackerEvent}
 */
@Getter
public class RuleEngineEffects {
  private final Map<UID, List<ValidationEffect>> enrollmentValidationEffects;
  private final Map<UID, List<ValidationEffect>> eventValidationEffects;
  private final Map<UID, List<Notification>> enrollmentNotifications;
  private final Map<UID, List<Notification>> eventNotifications;

  private RuleEngineEffects(
      Map<UID, List<ValidationEffect>> enrollmentValidationEffects,
      Map<UID, List<ValidationEffect>> eventValidationEffects,
      Map<UID, List<Notification>> enrollmentNotifications,
      Map<UID, List<Notification>> eventNotifications) {
    this.enrollmentValidationEffects = enrollmentValidationEffects;
    this.eventValidationEffects = eventValidationEffects;
    this.enrollmentNotifications = enrollmentNotifications;
    this.eventNotifications = eventNotifications;
  }

  public static RuleEngineEffects empty() {
    return new RuleEngineEffects(Map.of(), Map.of(), Map.of(), Map.of());
  }

  public static RuleEngineEffects of(List<RuleEffects> ruleEffects) {
    Map<UID, List<ValidationEffect>> enrollmentValidationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEnrollment)
            .collect(
                Collectors.toMap(
                    e -> UID.of(e.getTrackerObjectUid()),
                    e -> mapValidationEffect(e.getRuleEffects())));
    Map<UID, List<ValidationEffect>> eventValidationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEvent)
            .collect(
                Collectors.toMap(
                    e -> UID.of(e.getTrackerObjectUid()),
                    e -> mapValidationEffect(e.getRuleEffects())));
    Map<UID, List<Notification>> enrollmentNotificationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEnrollment)
            .collect(
                Collectors.toMap(
                    e -> UID.of(e.getTrackerObjectUid()),
                    e -> mapNotificationEffect(e.getRuleEffects())));
    Map<UID, List<Notification>> eventNotificationEffects =
        ruleEffects.stream()
            .filter(RuleEffects::isEvent)
            .collect(
                Collectors.toMap(
                    e -> UID.of(e.getTrackerObjectUid()),
                    e -> mapNotificationEffect(e.getRuleEffects())));
    return new RuleEngineEffects(
        enrollmentValidationEffects,
        eventValidationEffects,
        enrollmentNotificationEffects,
        eventNotificationEffects);
  }

  public static RuleEngineEffects merge(RuleEngineEffects effects, RuleEngineEffects effects2) {
    Map<UID, List<ValidationEffect>> enrollmentValidationEffects =
        merge(effects.enrollmentValidationEffects, effects2.enrollmentValidationEffects);
    Map<UID, List<ValidationEffect>> eventValidationEffects =
        merge(effects.eventValidationEffects, effects2.eventValidationEffects);
    Map<UID, List<Notification>> enrollmentNotificationEffects =
        merge(effects.enrollmentNotifications, effects2.enrollmentNotifications);
    Map<UID, List<Notification>> eventNotificationEffects =
        merge(effects.eventNotifications, effects2.eventNotifications);

    return new RuleEngineEffects(
        enrollmentValidationEffects,
        eventValidationEffects,
        enrollmentNotificationEffects,
        eventNotificationEffects);
  }

  private static <T> Map<UID, List<T>> merge(
      Map<UID, List<T>> effects, Map<UID, List<T>> effects2) {
    return Stream.of(effects.entrySet(), effects2.entrySet())
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static List<ValidationEffect> mapValidationEffect(List<RuleEffect> effects) {
    return effects.stream()
        .filter(e -> ValidationAction.contains(e.getRuleAction().getType()))
        .map(
            e ->
                new ValidationEffect(
                    ValidationAction.fromName(e.getRuleAction().getType()),
                    UID.of(e.getRuleId()),
                    e.getData(),
                    StringUtils.isEmpty(e.getRuleAction().field())
                        ? null
                        : UID.of(e.getRuleAction().field()),
                    e.getRuleAction().content()))
        .toList();
  }

  private static List<Notification> mapNotificationEffect(List<RuleEffect> effects) {
    return effects.stream()
        .filter(
            e ->
                SENDMESSAGE.name().equals(e.getRuleAction().getType())
                    || SCHEDULEMESSAGE.name().equals(e.getRuleAction().getType()))
        .filter(RuleEngineEffects::isValid)
        .map(
            e ->
                new Notification(
                    UID.of(e.getRuleAction().getValues().get(NOTIFICATION)),
                    DateUtils.parseDate(StringUtils.unwrap(e.getData(), '\''))))
        .toList();
  }

  private static boolean isValid(RuleEffect effect) {
    if (SCHEDULEMESSAGE.name().equals(effect.getRuleAction().getType())) {
      return DateUtils.dateIsValid(StringUtils.unwrap(effect.getData(), '\''));
    }
    return true;
  }
}
