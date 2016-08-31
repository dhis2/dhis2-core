package org.hisp.dhis.sms.incoming;

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

import java.util.Date;
import java.util.List;

import org.hisp.dhis.sms.MessageQueue;

public class DefaultIncomingSmsService
    implements IncomingSmsService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private IncomingSmsStore incomingSmsStore;

    public void setIncomingSmsStore( IncomingSmsStore incomingSmsStore )
    {
        this.incomingSmsStore = incomingSmsStore;
    }

    private MessageQueue incomingSmsQueue;

    public void setIncomingSmsQueue( MessageQueue incomingSmsQueue )
    {
        this.incomingSmsQueue = incomingSmsQueue;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public List<IncomingSms> listAllMessage()
    {
        return (List<IncomingSms>) incomingSmsStore.getAllSmses();
    }

    @Override
    public int save( IncomingSms incomingSms )
    {
        int smsId = incomingSmsStore.save( incomingSms );
        incomingSmsQueue.put( incomingSms );
        return smsId;
    }

    @Override
    public int save( String message, String originator, String gateway, Date receivedTime )
    {

        IncomingSms sms = new IncomingSms();
        sms.setText( message );
        sms.setOriginator( originator );
        sms.setGatewayId( gateway );

        if ( receivedTime != null )
        {
            sms.setSentDate( receivedTime );
        }
        else
        {
            sms.setSentDate( new Date() );
        }
        
        sms.setReceivedDate( new Date() );
        sms.setEncoding( SmsMessageEncoding.ENC7BIT );
        sms.setStatus( SmsMessageStatus.INCOMING );
        
        return save( sms );
    }

    @Override
    public void deleteById( Integer id )
    {
        IncomingSms incomingSms = incomingSmsStore.get( id );

        incomingSmsStore.delete( incomingSms );
    }

    @Override
    public IncomingSms findBy( Integer id )
    {
        return incomingSmsStore.get( id );
    }

    @Override
    public IncomingSms getNextUnprocessed()
    {
        return null;
    }

    @Override
    public void update( IncomingSms incomingSms )
    {
        incomingSmsStore.update( incomingSms );
    }

    @Override
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword )
    {
        return incomingSmsStore.getSmsByStatus( status, keyword );
    }

    @Override
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword, Integer min, Integer max )
    {
        return incomingSmsStore.getSmsByStatus( status, keyword, min, max );
    }

    @Override
    public List<IncomingSms> getAllUnparsedMessages()
    {
        return incomingSmsStore.getAllUnparsedSmses();
    }
}
