package org.hisp.dhis.light.messaging.action;

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

import com.opensymphony.xwork2.Action;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

public class SendMessagesAction
    implements Action
{
    private static final Log log = LogFactory.getLog( SendMessagesAction.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private User user;

//    private Integer userId;
//
//    public void setUserId( Integer userId )
//    {
//        this.userId = userId;
//    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    private String subject;

    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    private String text;

    public void setText( String text )
    {
        this.text = text;
    }

    private String recipientCheckBox;
    public String getRecipientCheckBox()
    {
        return recipientCheckBox;
    }

    public void setRecipientCheckBox( String recipientCheckBox )
    {
        this.recipientCheckBox = recipientCheckBox;
    }

    private Set<User> recipient;
    public Set<User> getRecipient()
    {
        return recipient;
    }

    public void setRecipients( Set<User> recipient )
    {
        this.recipient = recipient;
    }    
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        log.info( "SendMessagesAction.execute() called" );        
        
        updateRecipients(recipientCheckBox);
        
//        user = userService.getUser( userId );
        String metaData = MessageService.META_USER_AGENT
            + ServletActionContext.getRequest().getHeader( ContextUtils.HEADER_USER_AGENT );

//        Set<User> users = new HashSet<User>();
//        users.add( user );

//        messageService.sendMessage( subject, text, metaData, users );
        messageService.sendPrivateMessage( subject, text, metaData, recipient );

        log.debug( "SendMessagesAction.execute() exit: " + SUCCESS);
                
        return SUCCESS;
    }
    
    /**
     * 
     * @param recipientCheckBox
     */
    private void updateRecipients(String recipientCheckBox)
    {
        recipient = new HashSet<>();
        
        if ( recipientCheckBox != null )
        {
            String rcbArray[] = recipientCheckBox.split( "," );

            for ( int i = 0; i < rcbArray.length; i++ )
            {
                rcbArray[i] = rcbArray[i].trim();
                User u = userService.getUser( Integer.parseInt( rcbArray[i] ) );
                recipient.add( u );
            }
        }
    }    
}
