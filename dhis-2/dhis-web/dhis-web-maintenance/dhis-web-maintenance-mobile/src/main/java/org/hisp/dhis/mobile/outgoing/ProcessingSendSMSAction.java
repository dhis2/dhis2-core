package org.hisp.dhis.mobile.outgoing;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.sms.task.SendSmsTask;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 */
public class ProcessingSendSMSAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SelectionTreeManager selectionTreeManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private GatewayAdministrationService gatewayAdminService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private Notifier notifier;

    @Autowired
    private SendSmsTask sendSmsTask;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String smsSubject;

    public void setSmsSubject( String smsSubject )
    {
        this.smsSubject = smsSubject;
    }

    private String text;

    public void setText( String text )
    {
        this.text = text;
    }

    private String sendTarget;

    public void setSendTarget( String sendTarget )
    {
        this.sendTarget = sendTarget;
    }

    private Integer userGroup;

    public void setUserGroup( Integer userGroup )
    {
        this.userGroup = userGroup;
    }

    private Set<String> recipients = new HashSet<>();

    public void setRecipients( Set<String> recipients )
    {
        this.recipients = recipients;
    }

    private String message = "success";

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    public String execute()
        throws Exception
    {
        SmsGatewayConfig defaultgateway = gatewayAdminService.getDefaultGateway();

        if ( defaultgateway == null )
        {
            message = i18n.getString( "please_select_a_gateway_type_to_send_sms" );

            return ERROR;
        }

        if ( text == null || text.trim().length() == 0 )
        {
            message = i18n.getString( "no_message" );

            return ERROR;
        }

        User currentUser = currentUserService.getCurrentUser();

        List<User> recipientsList = new ArrayList<>();

        // Set<User> recipientsList = new HashSet<User>();

        if ( "phone".equals( sendTarget ) )
        {
            ObjectMapper mapper = new ObjectMapper().setVisibility( PropertyAccessor.FIELD,
                JsonAutoDetect.Visibility.ANY );
            mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
            recipients = mapper.readValue( recipients.iterator().next(), Set.class );

            for ( String each : recipients )
            {
                User user = new User();
                user.setPhoneNumber( each );
                recipientsList.add( user );
            }
            // message = messageSender.sendMessage( smsSubject, smsMessage,
            // currentUser, true, recipients, gatewayId );
        }
        else if ( "userGroup".equals( sendTarget ) )
        {
            UserGroup group = userGroupService.getUserGroup( userGroup );

            if ( group == null )
            {
                message = i18n.getString( "selected_user_group_is_unavailable" );

                return ERROR;
            }

            if ( group.getMembers() == null || group.getMembers().isEmpty() )
            {
                message = i18n.getString( "selected_user_group_has_no_member" );

                return ERROR;
            }

            recipientsList = new ArrayList<>( group.getMembers() );
        }
        else if ( "user".equals( sendTarget ) )
        {
            Collection<OrganisationUnit> units = selectionTreeManager.getReloadedSelectedOrganisationUnits();

            if ( units != null && !units.isEmpty() )
            {
                for ( OrganisationUnit unit : units )
                {
                    recipientsList.addAll( unit.getUsers() );
                }

                if ( recipientsList.isEmpty() )
                {
                    message = i18n.getString( "there_is_no_user_assigned_to_selected_units" );

                    return ERROR;
                }

                // message = messageSender.sendMessage( smsSubject, smsMessage,
                // currentUser, false, users, gatewayId );
            }
        }
        else if ( "unit".equals( sendTarget ) )
        {
            for ( OrganisationUnit unit : selectionTreeManager.getSelectedOrganisationUnits() )
            {
                if ( unit.getPhoneNumber() != null && !unit.getPhoneNumber().isEmpty() )
                {
                    User user = new User();
                    user.setPhoneNumber( unit.getPhoneNumber() );
                    recipientsList.add( user );
                }
            }

            if ( recipientsList.isEmpty() )
            {
                message = i18n.getString( "selected_units_have_no_phone_number" );

                return ERROR;
            }
        }
        
        TaskId taskId = new TaskId( TaskCategory.SENDING_SMS, currentUser );
        notifier.clear( taskId );
        
        sendSmsTask.setTaskId( taskId );
        sendSmsTask.setCurrentUser( currentUser );
        sendSmsTask.setRecipientsList( recipientsList );
        sendSmsTask.setSmsSubject( smsSubject );
        sendSmsTask.setText( text );
        
        scheduler.executeTask( sendSmsTask );

        if ( message != null && !message.equals( "success" ) )
        {
            message = i18n.getString( message );
            return ERROR;
        }
        
        if ( message == null ) {
            message = "An inter error occurs, please contact your administration";
            return ERROR;
        }

        return SUCCESS;
    }
}
