package org.hisp.dhis.validation.notification;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.notification.NotificationTemplate;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.ValidationRule;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Halvdan Hoem Grelland
 */
public class ValidationNotificationTemplate
    extends BaseIdentifiableObject
    implements NotificationTemplate, MetadataObject
{
    private static final Set<DeliveryChannel> ALL_DELIVERY_CHANNELS = Sets.newHashSet( DeliveryChannel.values() );

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    private String subjectTemplate;

    private String messageTemplate;

    private Set<ValidationRule> validationRules = new HashSet<>();

    private Boolean notifyUsersInHierarchyOnly;

    private Set<UserGroup> recipientUserGroups = new HashSet<>();

    private SendStrategy sendStrategy = SendStrategy.COLLECTIVE_SUMMARY;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ValidationNotificationTemplate()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addValidationRule( ValidationRule validationRule )
    {
        this.validationRules.add( validationRule );
        validationRule.getNotificationTemplates().add( this );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Override
    public String getSubjectTemplate()
    {
        return subjectTemplate;
    }

    public void setSubjectTemplate( String subjectTemplate )
    {
        this.subjectTemplate = subjectTemplate;
    }

    @PropertyRange( min = 1, max = 1000 )
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Override
    public String getMessageTemplate()
    {
        return messageTemplate;
    }

    @Override
    public Set<DeliveryChannel> getDeliveryChannels()
    {
        return ALL_DELIVERY_CHANNELS;
    }

    public void setMessageTemplate( String messageTemplate )
    {
        this.messageTemplate = messageTemplate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<ValidationRule> getValidationRules()
    {
        return validationRules;
    }

    public void setValidationRules( Set<ValidationRule> validationRules )
    {
        this.validationRules = validationRules;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getNotifyUsersInHierarchyOnly()
    {
        return notifyUsersInHierarchyOnly;
    }

    public void setNotifyUsersInHierarchyOnly( Boolean notifyUsersInHierarchyOnly )
    {
        this.notifyUsersInHierarchyOnly = notifyUsersInHierarchyOnly;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserGroup> getRecipientUserGroups()
    {
        return recipientUserGroups;
    }

    public void setRecipientUserGroups( Set<UserGroup> recipientUserGroups )
    {
        this.recipientUserGroups = recipientUserGroups;
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
