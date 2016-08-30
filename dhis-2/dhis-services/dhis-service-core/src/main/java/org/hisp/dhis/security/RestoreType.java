package org.hisp.dhis.security;

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

import java.util.Calendar;

/**
 * Type of user account restore operation.
 *
 * @author Jim Grace
 */

public enum RestoreType
{
    RECOVER_PASSWORD( Calendar.DAY_OF_MONTH, 2, "restore_message", "email_restore_subject", "restore.action" ),
    INVITE( Calendar.MONTH, 3, "invite_message", "email_invite_subject", "invite.action" );

    /**
     * Type of Calendar interval before the restore expires.
     */
    private final int expiryIntervalType;

    /**
     * Count of Calendar intervals before the restore expires.
     */
    private final int expiryIntervalCount;

    /**
     * Name of the email template for this restore action type.
     */
    private final String emailTemplate;

    /**
     * Subject line of the email for this restore action type.
     */
    private final String emailSubject;

    /**
     * Return web action to put in the email message.
     */
    private final String action;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private RestoreType( int expiryIntervalType, int expiryIntervalCount,
                 String emailTemplate, String emailSubject, String action )
    {
        this.expiryIntervalType = expiryIntervalType;
        this.expiryIntervalCount = expiryIntervalCount;
        this.emailTemplate = emailTemplate;
        this.emailSubject = emailSubject;
        this.action = action;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getExpiryIntervalType()
    {
        return expiryIntervalType;
    }

    public int getExpiryIntervalCount()
    {
        return expiryIntervalCount;
    }

    public String getEmailTemplate()
    {
        return emailTemplate;
    }

    public String getEmailSubject()
    {
        return emailSubject;
    }

    public String getAction()
    {
        return action;
    }
}
