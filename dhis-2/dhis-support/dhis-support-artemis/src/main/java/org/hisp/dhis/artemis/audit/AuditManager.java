package org.hisp.dhis.artemis.audit;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import com.google.common.primitives.Primitives;
import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.JmsTopic;
import org.hisp.dhis.render.RenderService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Destination;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class AuditManager
{
    private final JmsTemplate jmsTemplate;
    private final RenderService renderService;

    public AuditManager(
        JmsTemplate jmsTemplate,
        RenderService renderService
    )
    {
        this.jmsTemplate = jmsTemplate;
        this.renderService = renderService;
    }

    public void send( Destination destination, Audit audit )
    {
        send( destination, audit, null );
    }

    public void send( Destination destination, Audit audit, Object data )
    {
        String payload = null;

        if ( data != null )
        {
            Class<?> klass = data.getClass();

            if ( klass.isPrimitive() || Primitives.isWrapperType( klass ) )
            {
                payload = String.valueOf( data );
            }
            else
            {
                payload = renderService.toJsonAsString( data );
            }
        }

        audit.setData( payload );

        jmsTemplate.send( destination, session -> session.createTextMessage( renderService.toJsonAsString( audit ) ) );
    }

    public void sendTopic( String topic, Audit audit )
    {
        send( new JmsTopic( topic ), audit );
    }

    public void sendTopic( String topic, Audit audit, Object data )
    {
        send( new JmsTopic( topic ), audit, data );
    }

    public void sendQueue( String queue, Audit audit )
    {
        send( new JmsQueue( queue ), audit );
    }

    public void sendQueue( String queue, Audit audit, Object data )
    {
        send( new JmsQueue( queue ), audit, data );
    }
}
