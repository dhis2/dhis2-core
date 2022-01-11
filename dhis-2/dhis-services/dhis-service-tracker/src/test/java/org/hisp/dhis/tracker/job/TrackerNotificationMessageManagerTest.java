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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.render.RenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectFactory;

/**
 * @author Zubair Asghar
 */
@ExtendWith( MockitoExtension.class )
class TrackerNotificationMessageManagerTest
{

    @Mock
    private ObjectFactory<TrackerNotificationThread> objectFactory;

    @Mock
    private MessageManager messageManager;

    @Mock
    private RenderService renderService;

    @Mock
    private TextMessage textMessage;

    @Mock
    private AsyncTaskExecutor taskExecutor;

    @Mock
    private TrackerNotificationThread trackerNotificationThread;

    @InjectMocks
    private TrackerNotificationMessageManager trackerNotificationMessageManager;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<TrackerSideEffectDataBundle> bundleArgumentCaptor;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Test
    void test_add_job()
    {
        doNothing().when( messageManager ).sendQueue( anyString(), any( TrackerSideEffectDataBundle.class ) );

        TrackerSideEffectDataBundle dataBundle = TrackerSideEffectDataBundle.builder().build();

        trackerNotificationMessageManager.addJob( dataBundle );

        Mockito.verify( messageManager ).sendQueue( topicCaptor.capture(), bundleArgumentCaptor.capture() );

        assertEquals( Topics.TRACKER_IMPORT_NOTIFICATION_TOPIC_NAME, topicCaptor.getValue() );
        assertEquals( dataBundle, bundleArgumentCaptor.getValue() );
    }

    @Test
    void test_message_consumer()
        throws JMSException,
        IOException
    {
        TrackerSideEffectDataBundle bundle = TrackerSideEffectDataBundle.builder().accessedBy( "test-user" ).build();

        when( textMessage.getText() ).thenReturn( "text" );
        when( objectFactory.getObject() ).thenReturn( trackerNotificationThread );
        doNothing().when( taskExecutor ).executeTask( any( Runnable.class ) );

        when( renderService.fromJson( anyString(), eq( TrackerSideEffectDataBundle.class ) ) ).thenReturn( null );
        trackerNotificationMessageManager.consume( textMessage );

        verify( taskExecutor, times( 0 ) ).executeTask( any( Runnable.class ) );

        doReturn( bundle ).when( renderService ).fromJson( anyString(), eq( TrackerSideEffectDataBundle.class ) );
        trackerNotificationMessageManager.consume( textMessage );

        Mockito.verify( taskExecutor ).executeTask( runnableCaptor.capture() );

        assertTrue( runnableCaptor.getValue() instanceof TrackerNotificationThread );
    }
}
