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
package org.hisp.dhis.tracker.job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.engine.RuleActionImplementer;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.converter.TrackerSideEffectConverterService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Producer and consumer for handling program rule actions.
 *
 * @author Zubair Asghar
 */
@Component
public class TrackerRuleEngineMessageManager
{
    private final List<RuleActionImplementer> ruleActionImplementers;

    private final TrackerSideEffectConverterService trackerSideEffectConverterService;

    private final Notifier notifier;

    private final AsyncTaskExecutor taskExecutor;

    private final RenderService renderService;

    public TrackerRuleEngineMessageManager(
        AsyncTaskExecutor taskExecutor,
        RenderService renderService,
        @Qualifier( "org.hisp.dhis.programrule.engine.RuleActionSendMessageImplementer" ) RuleActionImplementer sendMessageRuleActionImplementer,
        @Qualifier( "org.hisp.dhis.programrule.engine.RuleActionScheduleMessageImplementer" ) RuleActionImplementer scheduleMessageRuleActionImplementer,
        TrackerSideEffectConverterService trackerSideEffectConverterService,
        Notifier notifier )
    {
        this.taskExecutor = taskExecutor;
        this.renderService = renderService;
        this.ruleActionImplementers = List.of(
            scheduleMessageRuleActionImplementer, sendMessageRuleActionImplementer );
        this.trackerSideEffectConverterService = trackerSideEffectConverterService;
        this.notifier = notifier;

    }

    @JmsListener( destination = Topics.TRACKER_IMPORT_RULE_ENGINE_TOPIC_NAME, containerFactory = "jmsQueueListenerContainerFactory" )
    public void consume( TextMessage message )
        throws JMSException,
        IOException
    {
        TrackerSideEffectDataBundle bundle = renderService.fromJson( message.getText(),
            TrackerSideEffectDataBundle.class );

        if ( bundle == null )
        {
            return;
        }

        JobConfiguration jobConfiguration = new JobConfiguration( "", JobType.TRACKER_IMPORT_RULE_ENGINE_JOB,
            bundle.getAccessedBy(), true );

        bundle.setJobConfiguration( jobConfiguration );

        taskExecutor.executeTask( () -> sendRuleEngineNotifications( bundle ) );
    }

    public void sendRuleEngineNotifications( TrackerSideEffectDataBundle bundle )
    {
        Map<String, List<RuleEffect>> enrollmentRuleEffects = trackerSideEffectConverterService
            .toRuleEffects( bundle.getEnrollmentRuleEffects() );
        Map<String, List<RuleEffect>> eventRuleEffects = trackerSideEffectConverterService
            .toRuleEffects( bundle.getEventRuleEffects() );

        for ( RuleActionImplementer ruleActionImplementer : ruleActionImplementers )
        {
            for ( Map.Entry<String, List<RuleEffect>> entry : enrollmentRuleEffects.entrySet() )
            {
                ProgramInstance pi = bundle.getProgramInstance();
                pi.setProgram( bundle.getProgram() );

                entry.getValue()
                    .stream()
                    .filter( effect -> ruleActionImplementer.accept( effect.ruleAction() ) )
                    .forEach( effect -> ruleActionImplementer.implement( effect, pi ) );
            }

            for ( Map.Entry<String, List<RuleEffect>> entry : eventRuleEffects.entrySet() )
            {
                ProgramStageInstance psi = bundle.getProgramStageInstance();
                psi.getProgramStage().setProgram( bundle.getProgram() );

                entry.getValue()
                    .stream()
                    .filter( effect -> ruleActionImplementer.accept( effect.ruleAction() ) )
                    .forEach( effect -> ruleActionImplementer.implement( effect, psi ) );
            }
        }

        notifier.notify( bundle.getJobConfiguration(), "Tracker Rule-engine side effects completed" );
    }
}
