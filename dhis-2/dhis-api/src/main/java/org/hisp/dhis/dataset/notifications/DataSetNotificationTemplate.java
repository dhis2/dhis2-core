package org.hisp.dhis.dataset.notifications;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.notification.NotificationTemplate;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.user.UserGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by zubair on 26.06.17.
 */
@JacksonXmlRootElement( namespace = DxfNamespaces.DXF_2_0 )
public class DataSetNotificationTemplate
    extends BaseIdentifiableObject implements NotificationTemplate, MetadataObject
{
    private String messageTemplate;

    private String subjectTemplate;

    private Integer relativeScheduledDays = 0;

    private DataSetNotificationTrigger dataSetNotificationTrigger;

    private DataSetNotificationRecipient notificationRecipient;

    private Set<DeliveryChannel> deliveryChannels = new HashSet<>();

    private Set<DataSet> dataSets = new HashSet<>();

    private UserGroup recipientUserGroup;

    private SendStrategy sendStrategy = SendStrategy.SINGLE_NOTIFICATION;

    public DataSetNotificationTemplate()
    {
    }

    public DataSetNotificationTemplate( Set<DataSet> dataSets, Set<DeliveryChannel> deliveryChannels, String messageTemplate,
        DataSetNotificationRecipient notificationRecipient, DataSetNotificationTrigger dataSetNotificationTrigger, String subjectTemplate,
            UserGroup userGroup, Integer relativeScheduledDays, SendStrategy sendStrategy )
    {
        this.dataSets = dataSets;
        this.deliveryChannels = deliveryChannels;
        this.messageTemplate = messageTemplate;
        this.notificationRecipient = notificationRecipient;
        this.dataSetNotificationTrigger = dataSetNotificationTrigger;
        this.subjectTemplate = subjectTemplate;
        this.recipientUserGroup = userGroup;
        this.relativeScheduledDays = relativeScheduledDays;
        this.sendStrategy = sendStrategy;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty( "dataSets" )
    @JacksonXmlElementWrapper( localName = "dataSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataSet", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataSet> getDataSets()
    {
        return dataSets;
    }

    public void setDataSets( Set<DataSet> dataSets )
    {
        this.dataSets = dataSets;
    }

    @Override
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

    @Override
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

    @Override
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataSetNotificationRecipient getNotificationRecipient()
    {
        return notificationRecipient;
    }

    public void setNotificationRecipient( DataSetNotificationRecipient notificationRecipient )
    {
        this.notificationRecipient = notificationRecipient;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataSetNotificationTrigger getDataSetNotificationTrigger()
    {
        return dataSetNotificationTrigger;
    }

    public void setDataSetNotificationTrigger( DataSetNotificationTrigger dataSetNotificationTrigger )
    {
        this.dataSetNotificationTrigger = dataSetNotificationTrigger;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserGroup getRecipientUserGroup()
    {
        return recipientUserGroup;
    }

    public void setRecipientUserGroup( UserGroup userGroup )
    {
        this.recipientUserGroup = userGroup;
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
    public SendStrategy getSendStrategy()
    {
        return sendStrategy;
    }

    public void setSendStrategy( SendStrategy sendStrategy )
    {
        this.sendStrategy = sendStrategy;
    }
}
