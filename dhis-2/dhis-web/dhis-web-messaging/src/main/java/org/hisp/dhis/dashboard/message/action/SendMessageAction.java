package org.hisp.dhis.dashboard.message.action;

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

import java.util.HashSet;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class SendMessageAction
    implements Action
{
    private static final String PREFIX_USER = "u:";
    private static final String PREFIX_USERGROUP = "ug:";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private MessageService messageService;

    @Autowired
    private SelectionTreeManager selectionTreeManager;

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String recipients;

    public void setRecipients( String recipients )
    {
        this.recipients = recipients;
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
    
    private boolean ignoreTree;

    public void setIgnoreTree( boolean ignoreTree )
    {
        this.ignoreTree = ignoreTree;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        String metaData = MessageService.META_USER_AGENT +
            ServletActionContext.getRequest().getHeader( ContextUtils.HEADER_USER_AGENT );

        Set<User> users = new HashSet<>();

        if ( !ignoreTree )
        {
            for ( OrganisationUnit unit : selectionTreeManager.getReloadedSelectedOrganisationUnits() )
            {
                users.addAll( unit.getUsers() );
            }
        }

        String[] recipientsArray = recipients.split( "," );

        for ( String recipient : recipientsArray )
        {
            if ( recipient.startsWith( PREFIX_USER ) )
            {
                User user = userService.getUser( recipient.substring( 2 ) );

                if ( user != null )
                {
                    users.add( user );
                }
            }
            else if ( recipient.startsWith( PREFIX_USERGROUP ) )
            {
                UserGroup userGroup = userGroupService.getUserGroup( recipient.substring( 3 ) );

                if ( userGroup != null )
                {
                    users.addAll( userGroup.getMembers() );
                }
            }
        }
        
        messageService.sendMessage( subject, text, metaData, users );

        return SUCCESS;
    }
}
