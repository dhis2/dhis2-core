package org.hisp.dhis.trackedentity;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.user.UserGroup;

import java.util.regex.Pattern;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "trackedEntityInstanceReminder", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityInstanceReminder
    extends BaseIdentifiableObject
{
    public static final String ATTRIBUTE = "attributeid";

    public static final Pattern ATTRIBUTE_PATTERN = Pattern.compile( "\\{(" + ATTRIBUTE + ")=(\\w+)\\}" );

    public static final String DUE_DATE_TO_COMPARE = "duedate";

    public static final String ENROLLEMENT_DATE_TO_COMPARE = "enrollmentdate";

    public static final String INCIDENT_DATE_TO_COMPARE = "incidentdate";

    public static final String TEMPLATE_MESSSAGE_PROGRAM_NAME = "{program-name}";
    public static final String TEMPLATE_MESSSAGE_PROGAM_STAGE_NAME = "{program-stage-name}";
    public static final String TEMPLATE_MESSSAGE_DUE_DATE = "{due-date}";
    public static final String TEMPLATE_MESSSAGE_ORGUNIT_NAME = "{orgunit-name}";
    public static final String TEMPLATE_MESSSAGE_DAYS_SINCE_DUE_DATE = "{days-since-due-date}";
    public static final String TEMPLATE_MESSSAGE_INCIDENT_DATE = "{incident-date}";
    public static final String TEMPLATE_MESSSAGE_ENROLLMENT_DATE = "{enrollement-date}";
    public static final String TEMPLATE_MESSSAGE_DAYS_SINCE_ENROLLMENT_DATE = "{days-since-enrollment-date}";
    public static final String TEMPLATE_MESSSAGE_DAYS_SINCE_INCIDENT_DATE = "{days-since-incident-date}";

    public static final int SEND_TO_TRACKED_ENTITY_INSTANCE = 1;
    public static final int SEND_TO_ATTRIBUTE_TYPE_USERS = 2;
    public static final int SEND_TO_REGISTERED_ORGUNIT = 3;
    public static final int SEND_TO_ALL_USERS_AT_REGISTERED_ORGUNIT = 4;
    public static final int SEND_TO_USER_GROUP = 5;

    public static final int SEND_WHEN_TO_ENROLLMENT = 1;
    public static final int SEND_WHEN_TO_C0MPLETED_EVENT = 2;
    public static final int SEND_WHEN_TO_COMPLETED_PROGRAM = 3;

    public static final int MESSAGE_TYPE_DIRECT_SMS = 1;
    public static final int MESSAGE_TYPE_DHIS_MESSAGE = 2;
    public static final int MESSAGE_TYPE_BOTH = 3;

    private Integer daysAllowedSendMessage;

    private String templateMessage;

    private String dateToCompare;

    private Integer sendTo;

    private Integer whenToSend;

    private Integer messageType;

    private UserGroup userGroup;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityInstanceReminder()
    {

    }

    public TrackedEntityInstanceReminder( String name, Integer daysAllowedSendMessage, String templateMessage )
    {
        this.name = name;
        this.daysAllowedSendMessage = daysAllowedSendMessage;
        this.templateMessage = templateMessage;
    }

    public TrackedEntityInstanceReminder( String name, Integer daysAllowedSendMessage, String templateMessage, String dateToCompare,
        Integer sendTo, Integer whenToSend, Integer messageType )
    {
        this.name = name;
        this.daysAllowedSendMessage = daysAllowedSendMessage;
        this.templateMessage = templateMessage;
        this.dateToCompare = dateToCompare;
        this.sendTo = sendTo;
        this.whenToSend = whenToSend;
        this.messageType = messageType;
    }

    // -------------------------------------------------------------------------
    // Getter && Setter
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getDaysAllowedSendMessage()
    {
        return daysAllowedSendMessage;
    }

    public void setDaysAllowedSendMessage( Integer daysAllowedSendMessage )
    {
        this.daysAllowedSendMessage = daysAllowedSendMessage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTemplateMessage()
    {
        return templateMessage;
    }

    public void setTemplateMessage( String templateMessage )
    {
        this.templateMessage = templateMessage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDateToCompare()
    {
        return dateToCompare;
    }

    public void setDateToCompare( String dateToCompare )
    {
        this.dateToCompare = dateToCompare;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSendTo()
    {
        return sendTo;
    }

    public void setSendTo( Integer sendTo )
    {
        this.sendTo = sendTo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getWhenToSend()
    {
        return whenToSend;
    }

    public void setWhenToSend( Integer whenToSend )
    {
        this.whenToSend = whenToSend;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserGroup getUserGroup()
    {
        return userGroup;
    }

    public void setUserGroup( UserGroup userGroup )
    {
        this.userGroup = userGroup;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getMessageType()
    {
        return messageType;
    }

    public void setMessageType( Integer messageType )
    {
        this.messageType = messageType;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            TrackedEntityInstanceReminder trackedEntityInstanceReminder = (TrackedEntityInstanceReminder) other;

            if ( mergeMode.isReplace() )
            {
                daysAllowedSendMessage = trackedEntityInstanceReminder.getDaysAllowedSendMessage();
                templateMessage = trackedEntityInstanceReminder.getTemplateMessage();
                dateToCompare = trackedEntityInstanceReminder.getDateToCompare();
                sendTo = trackedEntityInstanceReminder.getSendTo();
                whenToSend = trackedEntityInstanceReminder.getWhenToSend();
                messageType = trackedEntityInstanceReminder.getMessageType();
                userGroup = trackedEntityInstanceReminder.getUserGroup();
            }
            else if ( mergeMode.isMerge() )
            {
                daysAllowedSendMessage = trackedEntityInstanceReminder.getDaysAllowedSendMessage() == null ? daysAllowedSendMessage : trackedEntityInstanceReminder.getDaysAllowedSendMessage();
                templateMessage = trackedEntityInstanceReminder.getTemplateMessage() == null ? templateMessage : trackedEntityInstanceReminder.getTemplateMessage();
                dateToCompare = trackedEntityInstanceReminder.getDateToCompare() == null ? dateToCompare : trackedEntityInstanceReminder.getDateToCompare();
                sendTo = trackedEntityInstanceReminder.getSendTo() == null ? sendTo : trackedEntityInstanceReminder.getSendTo();
                whenToSend = trackedEntityInstanceReminder.getWhenToSend() == null ? whenToSend : trackedEntityInstanceReminder.getWhenToSend();
                messageType = trackedEntityInstanceReminder.getMessageType() == null ? messageType : trackedEntityInstanceReminder.getMessageType();
                userGroup = trackedEntityInstanceReminder.getUserGroup() == null ? userGroup : trackedEntityInstanceReminder.getUserGroup();
            }
        }
    }
}
