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
package org.hisp.dhis.sms.outbound;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple {@link OutboundSmsService sms service} storing the sms in a store and
 * forwards the request to a {@link org.hisp.dhis.sms.config.SmsMessageSender
 * sms transport service} for sending.
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.sms.outbound.OutboundSmsService" )
@Transactional
public class DefaultOutboundSmsService
    implements OutboundSmsService
{
    private final OutboundSmsStore outboundSmsStore;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public List<OutboundSms> getAll()
    {
        return outboundSmsStore.getAll();
    }

    @Override
    public List<OutboundSms> get( OutboundSmsStatus status )
    {
        return outboundSmsStore.get( status );
    }

    @Override
    public long save( OutboundSms sms )
    {
        outboundSmsStore.saveOutboundSms( sms );
        return sms.getId();
    }

    @Override
    public void delete( long outboundSmsId )
    {
        OutboundSms sms = get( outboundSmsId );

        if ( sms != null )
        {
            outboundSmsStore.delete( sms );
        }
    }

    @Override
    public void delete( String uid )
    {
        OutboundSms sms = outboundSmsStore.getByUid( uid );

        if ( sms != null )
        {
            outboundSmsStore.delete( sms );
        }
    }

    @Override
    public OutboundSms get( long id )
    {
        return outboundSmsStore.get( id );
    }

    @Override
    public OutboundSms get( String uid )
    {
        return outboundSmsStore.getByUid( uid );
    }

    @Override
    public List<OutboundSms> get( OutboundSmsStatus status, Integer min, Integer max, boolean hasPagination )
    {
        return outboundSmsStore.get( status, min, max, hasPagination );
    }

    @Override
    public List<OutboundSms> getAll( Integer min, Integer max, boolean hasPagination )
    {
        return outboundSmsStore.getAllOutboundSms( min, max, hasPagination );
    }
}