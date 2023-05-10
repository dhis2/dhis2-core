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

import org.dhis2.ruleengine.RuleEffect;
import org.dhis2.ruleengine.models.RuleAction;
import org.dhis2.ruleengine.models.RuleAction.Assign;
import org.dhis2.ruleengine.models.RuleAction.ScheduleMessage;
import org.dhis2.ruleengine.models.RuleAction.SendMessage;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerAssignValueSideEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerRuleEngineSideEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerScheduleMessageSideEffect;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerSendMessageSideEffect;
import org.springframework.stereotype.Service;

/**
 * @author Zubair Asghar
 */

@Service
public class DefaultTrackerSideEffectConverterService implements TrackerSideEffectConverterService
{
    private final Map<Class<?>, Function<RuleEffect, TrackerRuleEngineSideEffect>> sideEffectMap = Map.of(
        SendMessage.class, this::toTrackerSendMessageSideEffect,
        ScheduleMessage.class, this::toTrackerScheduleMessageSideEffect,
        Assign.class, this::toTrackerAssignSideEffect );

    private final Map<Class<? extends TrackerRuleEngineSideEffect>, Function<TrackerRuleEngineSideEffect, RuleEffect>> ruleEffectMap = Map
        .of(
            TrackerAssignValueSideEffect.class, this::toAssignRuleEffect,
            TrackerSendMessageSideEffect.class, this::toSendMessageRuleEffect,
            TrackerScheduleMessageSideEffect.class, this::toScheduleMessageRuleEffect );

    @Override
    public Map<String, List<TrackerRuleEngineSideEffect>> toTrackerSideEffects(
        Map<String, List<RuleEffect>> ruleEffects )
    {
        Map<String, List<TrackerRuleEngineSideEffect>> trackerSideEffects = new HashMap<>();

        for ( Map.Entry<String, List<RuleEffect>> entry : ruleEffects.entrySet() )
        {
            if ( entry.getValue() != null && !entry.getValue().isEmpty() )
            {
                List<RuleEffect> ruleEffectList = entry.getValue();

                trackerSideEffects.put( entry.getKey(), toTrackerSideEffectList( ruleEffectList ) );
            }
        }

        return trackerSideEffects;
    }

    @Override
    public Map<String, List<RuleEffect>> toRuleEffects(
        Map<String, List<TrackerRuleEngineSideEffect>> trackerSideEffects )
    {
        Map<String, List<RuleEffect>> ruleEffects = new HashMap<>();

        for ( Map.Entry<String, List<TrackerRuleEngineSideEffect>> entry : trackerSideEffects.entrySet() )
        {
            if ( entry.getValue() != null && !entry.getValue().isEmpty() )
            {
                List<TrackerRuleEngineSideEffect> trackerSideEffectsList = entry.getValue();

                ruleEffects.put( entry.getKey(), toRuleEffectList( trackerSideEffectsList ) );
            }
        }

        return ruleEffects;
    }

    private List<TrackerRuleEngineSideEffect> toTrackerSideEffectList( List<RuleEffect> ruleEffects )
    {
        List<TrackerRuleEngineSideEffect> trackerSideEffects = new ArrayList<>();

        for ( RuleEffect ruleEffect : ruleEffects )
        {
            if ( ruleEffect != null )
            {
                RuleAction action = ruleEffect.getRuleAction();

                if ( sideEffectMap.containsKey( action.getClass().getSuperclass() ) )
                {
                    trackerSideEffects
                        .add( sideEffectMap.get( action.getClass().getSuperclass() ).apply( ruleEffect ) );
                }
            }
        }

        return trackerSideEffects;
    }

    private List<RuleEffect> toRuleEffectList( List<TrackerRuleEngineSideEffect> trackerSideEffects )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        for ( TrackerRuleEngineSideEffect trackerSideEffect : trackerSideEffects )
        {
            if ( trackerSideEffect != null )
            {
                ruleEffects.add( ruleEffectMap.get( trackerSideEffect.getClass() ).apply( trackerSideEffect ) );
            }
        }

        return ruleEffects;
    }

    private TrackerRuleEngineSideEffect toTrackerSendMessageSideEffect( RuleEffect ruleEffect )
    {
        SendMessage ruleActionSendMessage = (SendMessage) ruleEffect.getRuleAction();

        return TrackerSendMessageSideEffect.builder().notification( ruleActionSendMessage.getNotification() )
            .data( ruleActionSendMessage.getData() )
            .build();
    }

    private TrackerRuleEngineSideEffect toTrackerScheduleMessageSideEffect( RuleEffect ruleEffect )
    {
        ScheduleMessage ruleActionScheduleMessage = (ScheduleMessage) ruleEffect.getRuleAction();

        return TrackerScheduleMessageSideEffect.builder().notification( ruleActionScheduleMessage.getNotification() )
            .data( ruleActionScheduleMessage.getData() )
            .build();
    }

    private TrackerRuleEngineSideEffect toTrackerAssignSideEffect( RuleEffect ruleEffect )
    {
        Assign ruleActionAssign = (Assign) ruleEffect.getRuleAction();

        return TrackerAssignValueSideEffect
            .builder().content( ruleActionAssign.getContent() ).field( ruleActionAssign.getField() )
            .data( ruleEffect.getData() )
            .build();
    }

    private RuleEffect toAssignRuleEffect( TrackerRuleEngineSideEffect trackerSideEffect )
    {
        TrackerAssignValueSideEffect assignValueSideEffect = (TrackerAssignValueSideEffect) trackerSideEffect;

        return new RuleEffect( "", new Assign( assignValueSideEffect.getContent(),
            assignValueSideEffect.getData(), assignValueSideEffect.getField() ), assignValueSideEffect.getData() );
    }

    private RuleEffect toSendMessageRuleEffect( TrackerRuleEngineSideEffect trackerSideEffect )
    {
        TrackerSendMessageSideEffect sendMessageSideEffect = (TrackerSendMessageSideEffect) trackerSideEffect;

        return new RuleEffect( "", new SendMessage( sendMessageSideEffect.getNotification(),
            sendMessageSideEffect.getData() ), sendMessageSideEffect.getData() );
    }

    private RuleEffect toScheduleMessageRuleEffect( TrackerRuleEngineSideEffect trackerSideEffect )
    {
        TrackerScheduleMessageSideEffect scheduleMessageSideEffect = (TrackerScheduleMessageSideEffect) trackerSideEffect;

        return new RuleEffect( "", new ScheduleMessage( scheduleMessageSideEffect.getNotification(),
            scheduleMessageSideEffect.getData() ), scheduleMessageSideEffect.getData() );
    }
}
