package org.hisp.dhis.email;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.outboundmessage.OutboundMessageResponse;

import java.util.Set;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public interface EmailService
{
    /**
     * Indicates whether email is configured.
     * 
     * @return true if email is configured.
     */
    boolean emailConfigured();

    /**
     * Sends an email to the recipient user from the sender.
     *
     * @param email the email to send.
     */
    OutboundMessageResponse sendEmail( Email email );

    /**
     * Sends an email to the list of recipient users from the sender.
     *
     * @param subject the subject.
     * @param message the message.
     * @param recipients the recipients.
     * @return the {@link OutboundMessageResponse}.
     */
    OutboundMessageResponse sendEmail( String subject, String message, Set<String> recipients );

    /**
     * Sends an automatically generated email message to the current user.
     * Useful for testing the SMTP configuration of the system.
     * 
     * @return the {@link OutboundMessageResponse}.
     */
    OutboundMessageResponse sendTestEmail();
    
    /**
     * Sends an email using the system notification email as recipient. Requires
     * that a valid system notification email address has been specified. Only
     * the subject and text properties of the given email are read.
     * 
     * @param email the email to send.
     * @return the {@link OutboundMessageResponse}.
     */
    OutboundMessageResponse sendSystemEmail( Email email );
}
