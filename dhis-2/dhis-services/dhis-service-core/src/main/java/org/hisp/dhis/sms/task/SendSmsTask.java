package org.hisp.dhis.sms.task;

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

import java.util.List;
import java.util.HashSet;

import javax.annotation.Resource;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.sms.MessageResponseStatus;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

public class SendSmsTask
    implements Runnable
{
    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Autowired
    private Notifier notifier;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String smsSubject;

    private String text;

    private User currentUser;

    private List<User> recipientsList;

    private String message;

    private MessageResponseStatus status;

    private TaskId taskId;

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    @Override
    public void run()
    {
        notifier.notify( taskId, "Sending SMS" );

        status = smsSender.sendMessage( smsSubject, text, null, currentUser, new HashSet<>( recipientsList ), false );

        if ( status.isOk() )
        {
            notifier.notify( taskId, "All Messages Sent" );
        }
    }

    public MessageResponseStatus getStatus()
    {
        return status;
    }

    public void setStatus( MessageResponseStatus status )
    {
        this.status = status;
    }

    public String getSmsSubject()
    {
        return smsSubject;
    }

    public void setSmsSubject( String smsSubject )
    {
        this.smsSubject = smsSubject;
    }

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public User getCurrentUser()
    {
        return currentUser;
    }

    public void setCurrentUser( User currentUser )
    {
        this.currentUser = currentUser;
    }

    public List<User> getRecipientsList()
    {
        return recipientsList;
    }

    public void setRecipientsList( List<User> recipientsList )
    {
        this.recipientsList = recipientsList;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public I18n getI18n()
    {
        return i18n;
    }
}
