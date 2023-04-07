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
import java.util.Map;
import java.util.function.Consumer;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Producer and consumer for handling tracker notifications.
 *
 * @author Zubair Asghar
 */
@Component
public class TrackerNotificationMessageManager
{
    private final AsyncTaskExecutor taskExecutor;

    private final RenderService renderService;

    private final Notifier notifier;

    private final IdentifiableObjectManager manager;

    private final Map<Class<? extends BaseIdentifiableObject>, Consumer<Long>> serviceMapper;

    public TrackerNotificationMessageManager( AsyncTaskExecutor taskExecutor, RenderService renderService,
        Notifier notifier, IdentifiableObjectManager manager, ProgramNotificationService programNotificationService )
    {
        this.taskExecutor = taskExecutor;
        this.renderService = renderService;
        this.notifier = notifier;
        this.manager = manager;
        this.serviceMapper = Map.of(
            ProgramInstance.class, programNotificationService::sendEnrollmentNotifications,
            ProgramStageInstance.class, programNotificationService::sendEventCompletionNotifications );
    }

    @JmsListener( destination = Topics.TRACKER_IMPORT_NOTIFICATION_TOPIC_NAME, containerFactory = "jmsQueueListenerContainerFactory" )
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

        JobConfiguration jobConfiguration = new JobConfiguration( "", JobType.TRACKER_IMPORT_NOTIFICATION_JOB,
            bundle.getAccessedBy(), true );

        bundle.setJobConfiguration( jobConfiguration );

        taskExecutor.executeTask( () -> sendNotifications( bundle ) );
    }

    public void sendNotifications( TrackerSideEffectDataBundle bundle )
    {
        if ( serviceMapper.containsKey( bundle.getKlass() ) )
        {
            BaseIdentifiableObject object = manager.get( bundle.getKlass(), bundle.getObject() );
            if ( object != null )
            {
                serviceMapper.get( bundle.getKlass() ).accept( object.getId() );
            }
        }

        notifier.notify( bundle.getJobConfiguration(), NotificationLevel.DEBUG,
            "Tracker notification side effects completed" );
    }
}
