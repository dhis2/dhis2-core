package org.hisp.dhis.audit.legacy;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.audit.AuditConsumer;
import org.hisp.dhis.audit.AuditService;
import org.hisp.dhis.render.RenderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.TextMessage;
import java.io.IOException;

/**
 * Tracker audits consumer.
 *
 * @author Morten Olav Hansen <morten@dhis2.org>
 */
@Component
public class TrackerAuditConsumer implements AuditConsumer
{
    private static final Log log = LogFactory.getLog( TrackerAuditConsumer.class );

    private final AuditService auditService;
    private final RenderService renderService;

    public TrackerAuditConsumer(
        AuditService auditService, RenderService renderService )
    {
        this.auditService = auditService;
        this.renderService = renderService;
    }

    @JmsListener( destination = Topics.TRACKER_TOPIC_NAME )
    public void consume( TextMessage message )
    {
        try
        {
            String payload = message.getText();

            Audit auditMessage = renderService.fromJson( payload, Audit.class );
            auditMessage.setData( renderService.toJsonAsString( auditMessage.getData() ) );

            auditService.addAudit( auditMessage.toAudit() );
        }
        catch ( IOException e )
        {
            log.error(
                "An error occurred de-serializing the message payload. The message can not be de-serialized to an Audit object.",
                e );
        }
        catch ( Exception e )
        {
            log.error( "An error occurred persisting an Audit message of type 'TRACKER'", e );
        }
    }
}
