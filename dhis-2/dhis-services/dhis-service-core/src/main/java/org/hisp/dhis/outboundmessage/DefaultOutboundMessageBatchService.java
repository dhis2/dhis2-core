package org.hisp.dhis.outboundmessage;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultOutboundMessageBatchService
    implements OutboundMessageBatchService
{
    private static final Log log = LogFactory.getLog( DefaultOutboundMessageBatchService.class );

    @Autowired
    private Set<MessageSender> messageSenders;

    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------

    public DefaultOutboundMessageBatchService()
    {
    }

    @Override
    public OutboundMessageResponseSummary sendBatches( List<OutboundMessageBatch> batches )
    {
        Map<DeliveryChannel, List<OutboundMessageBatch>> batchesByChannel = batches.stream()
            .collect( Collectors.groupingBy( OutboundMessageBatch::getDeliveryChannel ) );


        return null;
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private OutboundMessageResponseSummary send( OutboundMessageBatch batch, DeliveryChannel channel )
    {
        Set<MessageSender> applicableSenders = getApplicableMessageSenders( ImmutableSet.of( channel ) );
        Set<MessageSender> notApplicableSenders = Sets.difference( messageSenders, applicableSenders );

        List<OutboundMessageResponseSummary> summaries = batch.getBatch().stream()
            .map( message ->  )


    }

    private Set<MessageSender> getApplicableMessageSenders( Set<DeliveryChannel> channels )
    {
        return messageSenders.stream()
            .filter( sender -> sender.accept( channels ) )
            .collect( Collectors.toSet() );
    }
}
