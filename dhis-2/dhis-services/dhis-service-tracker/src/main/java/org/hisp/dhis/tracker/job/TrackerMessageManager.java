package org.hisp.dhis.tracker.job;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class TrackerMessageManager
{
    private final MessageManager messageManager;

    private final ObjectMapper objectMapper;

    private final SchedulingManager schedulingManager;

    private final ObjectFactory<TrackerImportThread> trackerImportThreadFactory;

    public TrackerMessageManager(
        MessageManager messageManager,
        ObjectMapper objectMapper,
        SchedulingManager schedulingManager,
        ObjectFactory<TrackerImportThread> trackerImportThreadFactory )
    {
        this.messageManager = messageManager;
        this.objectMapper = objectMapper;
        this.schedulingManager = schedulingManager;
        this.trackerImportThreadFactory = trackerImportThreadFactory;
    }

    public String addJob( TrackerImportParams params )
    {
        String jobId = CodeGenerator.generateUid();

        TrackerMessage trackerMessage = TrackerMessage.builder()
            .uid( jobId ).trackerImportParams( params )
            .build();

        messageManager.sendQueue( Topics.TRACKER_IMPORT_JOB_TOPIC_NAME, trackerMessage );

        return jobId;
    }

    @JmsListener( destination = Topics.TRACKER_IMPORT_JOB_TOPIC_NAME, containerFactory = "jmsQueueListenerContainerFactory" )
    public void consume( TextMessage message )
        throws JMSException, JsonProcessingException
    {
        String payload = message.getText();

        TrackerMessage trackerMessage = objectMapper.readValue( payload, TrackerMessage.class );
        TrackerImportParams trackerImportParams = trackerMessage.getTrackerImportParams();

        JobConfiguration jobConfiguration = new JobConfiguration(
            "",
            JobType.TRACKER_IMPORT_JOB,
            trackerImportParams.getUserId(),
            true
        );

        jobConfiguration.setUid( trackerMessage.getUid() );
        trackerImportParams.setJobConfiguration( jobConfiguration );

        TrackerImportThread trackerImportThread = trackerImportThreadFactory.getObject();
        trackerImportThread.setTrackerImportParams( trackerImportParams );

        schedulingManager.executeJob( trackerImportThread );
    }
}
