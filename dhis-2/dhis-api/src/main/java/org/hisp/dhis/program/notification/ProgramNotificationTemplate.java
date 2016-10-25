package org.hisp.dhis.program.notification;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.user.UserGroup;

import java.util.Set;

/**
 * @author Halvdan Hoem Grelland
 */
@JacksonXmlRootElement( namespace = DxfNamespaces.DXF_2_0 )
public class ProgramNotificationTemplate
    extends BaseIdentifiableObject
{
    private String subjectTemplate;

    private String messageTemplate;

    private NotificationTrigger notificationTrigger;

    private NotificationRecipient notificationRecipient;

    private Set<DeliveryChannel> deliveryChannels = Sets.newHashSet();

    // -------------------------------------------------------------------------
    // Conditionally relevant properties
    // -------------------------------------------------------------------------

    private Integer relativeScheduledDays = null;

    private UserGroup recipientUserGroup = null;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramNotificationTemplate()
    {
    }

    public ProgramNotificationTemplate( String name, String subjectTemplate, String messageTemplate,
        NotificationTrigger notificationTrigger, NotificationRecipient notificationRecipient,
        Set<DeliveryChannel> deliveryChannels, Integer relativeScheduledDays, UserGroup recipientUserGroup )
    {
        this.name = name;
        this.subjectTemplate = subjectTemplate;
        this.messageTemplate = messageTemplate;
        this.notificationTrigger = notificationTrigger;
        this.notificationRecipient = notificationRecipient;
        this.deliveryChannels = deliveryChannels;
        this.relativeScheduledDays = relativeScheduledDays;
        this.recipientUserGroup = recipientUserGroup;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSubjectTemplate()
    {
        return subjectTemplate;
    }

    public void setSubjectTemplate( String subjectTemplate )
    {
        this.subjectTemplate = subjectTemplate;
    }

    @PropertyRange( min = 1, max = 10000 )
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getMessageTemplate()
    {
        return messageTemplate;
    }

    public void setMessageTemplate( String messageTemplate )
    {
        this.messageTemplate = messageTemplate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public NotificationTrigger getNotificationTrigger()
    {
        return notificationTrigger;
    }

    public void setNotificationTrigger( NotificationTrigger notificationTrigger )
    {
        this.notificationTrigger = notificationTrigger;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public NotificationRecipient getNotificationRecipient()
    {
        return notificationRecipient;
    }

    public void setNotificationRecipient( NotificationRecipient notificationRecipient )
    {
        this.notificationRecipient = notificationRecipient;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<DeliveryChannel> getDeliveryChannels()
    {
        return deliveryChannels;
    }

    public void setDeliveryChannels( Set<DeliveryChannel> deliveryChannels )
    {
        this.deliveryChannels = deliveryChannels;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.INTEGER )
    @PropertyRange( min = Integer.MIN_VALUE, max = Integer.MAX_VALUE )
    public Integer getRelativeScheduledDays()
    {
        return relativeScheduledDays;
    }

    public void setRelativeScheduledDays( Integer relativeScheduledDays )
    {
        this.relativeScheduledDays = relativeScheduledDays;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserGroup getRecipientUserGroup()
    {
        return recipientUserGroup;
    }

    public void setRecipientUserGroup( UserGroup recipientUserGroup )
    {
        this.recipientUserGroup = recipientUserGroup;
    }

    // -------------------------------------------------------------------------
    // IdObject overrides
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ProgramNotificationTemplate pnt = (ProgramNotificationTemplate) other;

            if ( mergeMode.isReplace() )
            {
                subjectTemplate = pnt.getSubjectTemplate();
                messageTemplate = pnt.getMessageTemplate();
                notificationTrigger = pnt.getNotificationTrigger();
                notificationRecipient = pnt.getNotificationRecipient();
                relativeScheduledDays = pnt.getRelativeScheduledDays();
                recipientUserGroup = pnt.getRecipientUserGroup();
            }
            else if ( mergeMode.isMerge() )
            {
                subjectTemplate = pnt.getSubjectTemplate() == null ? subjectTemplate : pnt.getSubjectTemplate();
                messageTemplate = pnt.getMessageTemplate() == null ? messageTemplate : pnt.getMessageTemplate();
                notificationTrigger = pnt.getNotificationTrigger() == null ? notificationTrigger : pnt.getNotificationTrigger();
                notificationRecipient = pnt.getNotificationRecipient() == null ? notificationRecipient : pnt.getNotificationRecipient();
                relativeScheduledDays = pnt.getRelativeScheduledDays() == null ? relativeScheduledDays : pnt.getRelativeScheduledDays();
                recipientUserGroup = pnt.getRecipientUserGroup() == null ? recipientUserGroup : pnt.getRecipientUserGroup();
            }

            deliveryChannels.clear();
            deliveryChannels.addAll( pnt.getDeliveryChannels() );
        }
    }
}
