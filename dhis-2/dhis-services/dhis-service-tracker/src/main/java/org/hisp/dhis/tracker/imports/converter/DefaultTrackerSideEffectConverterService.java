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
package org.hisp.dhis.tracker.imports.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleActionScheduleMessage;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerAssignValueSideEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerRuleEngineSideEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerScheduleMessageSideEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerSendMessageSideEffect;
import org.springframework.stereotype.Service;

/**
 * @author Zubair Asghar
 */
@Service
public class DefaultTrackerSideEffectConverterService implements TrackerSideEffectConverterService {
  private final Map<Class<?>, Function<RuleEffect, TrackerRuleEngineSideEffect>> sideEffectMap =
      Map.of(
          RuleActionSendMessage.class, this::toTrackerSendMessageSideEffect,
          RuleActionScheduleMessage.class, this::toTrackerScheduleMessageSideEffect,
          RuleActionAssign.class, this::toTrackerAssignSideEffect);

  private final Map<
          Class<? extends TrackerRuleEngineSideEffect>,
          Function<TrackerRuleEngineSideEffect, RuleEffect>>
      ruleEffectMap =
          Map.of(
              TrackerAssignValueSideEffect.class, this::toAssignRuleEffect,
              TrackerSendMessageSideEffect.class, this::toSendMessageRuleEffect,
              TrackerScheduleMessageSideEffect.class, this::toScheduleMessageRuleEffect);

  @Override
  public Map<String, List<TrackerRuleEngineSideEffect>> toTrackerSideEffects(
      Map<String, List<RuleEffect>> ruleEffects) {
    Map<String, List<TrackerRuleEngineSideEffect>> trackerSideEffects = new HashMap<>();

    for (Map.Entry<String, List<RuleEffect>> entry : ruleEffects.entrySet()) {
      if (entry.getValue() != null && !entry.getValue().isEmpty()) {
        List<RuleEffect> ruleEffectList = entry.getValue();

        trackerSideEffects.put(entry.getKey(), toTrackerSideEffectList(ruleEffectList));
      }
    }

    return trackerSideEffects;
  }

  @Override
  public Map<String, List<RuleEffect>> toRuleEffects(
      Map<String, List<TrackerRuleEngineSideEffect>> trackerSideEffects) {
    Map<String, List<RuleEffect>> ruleEffects = new HashMap<>();

    for (Map.Entry<String, List<TrackerRuleEngineSideEffect>> entry :
        trackerSideEffects.entrySet()) {
      if (entry.getValue() != null && !entry.getValue().isEmpty()) {
        List<TrackerRuleEngineSideEffect> trackerSideEffectsList = entry.getValue();

        ruleEffects.put(entry.getKey(), toRuleEffectList(trackerSideEffectsList));
      }
    }

    return ruleEffects;
  }

  private List<TrackerRuleEngineSideEffect> toTrackerSideEffectList(List<RuleEffect> ruleEffects) {
    List<TrackerRuleEngineSideEffect> trackerSideEffects = new ArrayList<>();

    for (RuleEffect ruleEffect : ruleEffects) {
      if (ruleEffect != null) {
        RuleAction action = ruleEffect.ruleAction();

        if (sideEffectMap.containsKey(action.getClass().getSuperclass())) {
          trackerSideEffects.add(
              sideEffectMap.get(action.getClass().getSuperclass()).apply(ruleEffect));
        }
      }
    }

    return trackerSideEffects;
  }

  private List<RuleEffect> toRuleEffectList(List<TrackerRuleEngineSideEffect> trackerSideEffects) {
    List<RuleEffect> ruleEffects = new ArrayList<>();

    for (TrackerRuleEngineSideEffect trackerSideEffect : trackerSideEffects) {
      if (trackerSideEffect != null) {
        ruleEffects.add(ruleEffectMap.get(trackerSideEffect.getClass()).apply(trackerSideEffect));
      }
    }

    return ruleEffects;
  }

  private TrackerRuleEngineSideEffect toTrackerSendMessageSideEffect(RuleEffect ruleEffect) {
    RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleEffect.ruleAction();

    return TrackerSendMessageSideEffect.builder()
        .notification(ruleActionSendMessage.notification())
        .data(ruleActionSendMessage.data())
        .build();
  }

  private TrackerRuleEngineSideEffect toTrackerScheduleMessageSideEffect(RuleEffect ruleEffect) {
    RuleActionScheduleMessage ruleActionScheduleMessage =
        (RuleActionScheduleMessage) ruleEffect.ruleAction();

    return TrackerScheduleMessageSideEffect.builder()
        .notification(ruleActionScheduleMessage.notification())
        .data(ruleActionScheduleMessage.data())
        .build();
  }

  private TrackerRuleEngineSideEffect toTrackerAssignSideEffect(RuleEffect ruleEffect) {
    RuleActionAssign ruleActionAssign = (RuleActionAssign) ruleEffect.ruleAction();

    return TrackerAssignValueSideEffect.builder()
        .content(ruleActionAssign.content())
        .field(ruleActionAssign.field())
        .data(ruleEffect.data())
        .build();
  }

  private RuleEffect toAssignRuleEffect(TrackerRuleEngineSideEffect trackerSideEffect) {
    TrackerAssignValueSideEffect assignValueSideEffect =
        (TrackerAssignValueSideEffect) trackerSideEffect;

    return RuleEffect.create(
        "",
        RuleActionAssign.create(
            assignValueSideEffect.getContent(),
            assignValueSideEffect.getData(),
            assignValueSideEffect.getField()),
        assignValueSideEffect.getData());
  }

  private RuleEffect toSendMessageRuleEffect(TrackerRuleEngineSideEffect trackerSideEffect) {
    TrackerSendMessageSideEffect sendMessageSideEffect =
        (TrackerSendMessageSideEffect) trackerSideEffect;

    return RuleEffect.create(
        "",
        RuleActionSendMessage.create(
            sendMessageSideEffect.getNotification(), sendMessageSideEffect.getData()),
        sendMessageSideEffect.getData());
  }

  private RuleEffect toScheduleMessageRuleEffect(TrackerRuleEngineSideEffect trackerSideEffect) {
    TrackerScheduleMessageSideEffect scheduleMessageSideEffect =
        (TrackerScheduleMessageSideEffect) trackerSideEffect;

    return RuleEffect.create(
        "",
        RuleActionScheduleMessage.create(
            scheduleMessageSideEffect.getNotification(), scheduleMessageSideEffect.getData()),
        scheduleMessageSideEffect.getData());
  }
}
