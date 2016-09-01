package org.hisp.dhis.message;

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

import java.util.Set;

import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.sms.MessageResponseStatus;
import org.hisp.dhis.sms.MessageResponseSummary;
import org.hisp.dhis.sms.outbound.MessageBatch;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface MessageSender
{
    /**
     * Sends a message. The given message will be sent to the given set of
     * users.
     * 
     * @param subject the message subject.
     * @param text the message text.
     * @param footer the message footer. Optionally included by the
     *        implementation.
     * @param users the users to send the message to.
     * @param forceSend force sending the message despite potential user
     *        settings.
     */
    MessageResponseStatus sendMessage( String subject, String text, String footer, User sender, Set<User> users, boolean forceSend );

    MessageResponseStatus sendMessage( String subject, String text, Set<String> recipient );

    MessageResponseStatus sendMessage( String subject, String text, String recipient );
   
    MessageResponseSummary sendMessageBatch( MessageBatch batch );

    boolean accept( Set<DeliveryChannel> channels );

    boolean isServiceReady();

    DeliveryChannel getDeliveryChannel();
}
