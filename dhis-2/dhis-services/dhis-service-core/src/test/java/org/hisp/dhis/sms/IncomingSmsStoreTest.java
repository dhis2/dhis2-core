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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsStore;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.sms.outbound.OutboundSmsStore;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
class IncomingSmsStoreTest extends DhisSpringTest
{

    @Autowired
    private IncomingSmsStore incomingSmsStore;

    @Autowired
    private OutboundSmsStore outboundSmsStore;

    @Autowired
    private UserService _userService;

    private User user;

    @Override
    public void setUpTest()
    {
        this.userService = _userService;
        user = makeUser( "A" );
        userService.addUser( user );
    }

    @Test
    void testGetIncomingSms()
    {
        IncomingSms sms = new IncomingSms();
        sms.setText( "testMessage" );
        sms.setOriginator( "474000000" );
        sms.setGatewayId( "testGateway" );
        sms.setCreatedBy( user );
        sms.setSentDate( new Date() );
        sms.setReceivedDate( new Date() );
        incomingSmsStore.save( sms );
        List<IncomingSms> incomingSmsList = incomingSmsStore.getSmsByStatus( SmsMessageStatus.INCOMING, "474" );
        assertEquals( 1, incomingSmsList.size() );
        assertEquals( "testMessage", incomingSmsList.get( 0 ).getText() );
        assertEquals( 1, incomingSmsStore.getSmsByStatus( SmsMessageStatus.INCOMING, "474", 0, 10, false ).size() );
        assertEquals( 1, incomingSmsStore.getSmsByOriginator( "474000000" ).size() );
    }

    @Test
    void testOutboundSms()
    {
        OutboundSms outboundSms = new OutboundSms();
        outboundSms.setDate( new Date() );
        outboundSms.setMessage( "testMessage" );
        outboundSms.setSender( "testSender" );
        outboundSms.setStatus( OutboundSmsStatus.OUTBOUND );
        outboundSms.setSubject( "testSubject" );
        outboundSms.setRecipients( Sets.newHashSet( "testRecipient" ) );
        outboundSmsStore.saveOutboundSms( outboundSms );
        assertEquals( 1, outboundSmsStore.get( OutboundSmsStatus.OUTBOUND ).size() );
        assertEquals( 1, outboundSmsStore.get( OutboundSmsStatus.OUTBOUND, 0, 10, false ).size() );
    }
}
