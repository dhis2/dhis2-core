package org.hisp.dhis.outboundmessage;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageSender;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultOutboundMessageBatchService
    implements OutboundMessageBatchService
{
    private static final Log log = LogFactory.getLog( DefaultOutboundMessageBatchService.class );

    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------

    private Map<DeliveryChannel, MessageSender> messageSenders;

    public void setMessageSenders( Map<DeliveryChannel, MessageSender> messageSenders )
    {
        this.messageSenders = messageSenders;
    }

    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------

    public DefaultOutboundMessageBatchService()
    {
    }

    // ---------------------------------------------------------------------
    // OutboundMessageService implementation
    // ---------------------------------------------------------------------

    @Override
    public List<OutboundMessageResponseSummary> sendBatches( List<OutboundMessageBatch> batches )
    {
        // Partition by channel (sender) first to avoid sender config checks
        return batches.stream()
            .collect( Collectors.groupingBy( OutboundMessageBatch::getDeliveryChannel ) )
            .entrySet().stream()
            .flatMap( entry -> entry.getValue().stream().map( this::send ) )
            .collect( Collectors.toList() );
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private OutboundMessageResponseSummary send( OutboundMessageBatch batch )
    {
        DeliveryChannel channel = batch.getDeliveryChannel();
        MessageSender sender = messageSenders.get( channel );

        if ( sender == null )
        {
            String errorMessage = String.format( "No server/gateway found for delivery channel %s", channel );
            log.error( errorMessage );

            return new OutboundMessageResponseSummary(
                errorMessage,
                channel,
                OutboundMessageBatchStatus.FAILED
            );
        }
        else if ( !sender.isConfigured() )
        {
            String errorMessage = String.format( "Server/gateway for delivery channel %s is not configured", channel );
            log.error( errorMessage );

            return new OutboundMessageResponseSummary(
                errorMessage,
                channel,
                OutboundMessageBatchStatus.FAILED
            );
        }

        log.info( "Invoking message sender: " + sender.getClass().getSimpleName() );

        return sender.sendMessageBatch( batch );
    }
}
