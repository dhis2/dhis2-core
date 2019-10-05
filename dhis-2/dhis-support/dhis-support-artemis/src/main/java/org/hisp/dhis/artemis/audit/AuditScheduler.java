package org.hisp.dhis.artemis;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.QueuedAudit;
import org.hisp.dhis.audit.AuditScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * @author Luciano Fiandesio
 */
@Component
public class AuditScheduler
{
    private static final Log log = LogFactory.getLog( AuditScheduler.class );
    private final long delay = 20000; // 20 seconds

    private final MessageManager messageManager;

    private final Map<AuditScope, String> auditScopeDestinationMap;

    private final BlockingQueue<QueuedAudit> delayed = new DelayQueue<>();

    public AuditScheduler( MessageManager messageManager, Map<AuditScope, String> auditScopeDestinationMap )
    {
        this.messageManager = messageManager;
        this.auditScopeDestinationMap = auditScopeDestinationMap;
    }

    public void addAuditItem(final Audit workItem )
    {
        System.out.println("adding item.....");
        final QueuedAudit postponed = new QueuedAudit( workItem, delay );
        if ( !delayed.contains( postponed ) )
        {
            System.out.println("in the queue!");
            delayed.offer( postponed );
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void process()
    {
        final Collection<QueuedAudit> expired = new ArrayList<>();
        delayed.drainTo( expired );

        for ( final QueuedAudit postponed : expired )
        {
            String topic = getTopicName( postponed.getAuditItem() );
            if ( !Strings.isNullOrEmpty( topic ) )
            {
                log.debug( "sending auditing message to topic: [" + topic + "] with content: "
                    + postponed.getAuditItem().toString() );

                this.messageManager.send( topic, postponed.getAuditItem() );
            }
            else
            {
                log.error( String.format( "Unable to map AuditScope [%s] to a topic name. Sending aborted",
                    postponed.getAuditItem().getAuditScope() ) );
            }
            
        }
    }
    
    private String getTopicName( Audit audit )
    {
        return auditScopeDestinationMap.get( audit.getAuditScope() );
    }
}
