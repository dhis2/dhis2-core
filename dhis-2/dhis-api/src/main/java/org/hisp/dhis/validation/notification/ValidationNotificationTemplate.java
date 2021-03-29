/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.validation.notification;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.notification.NotificationTemplate;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.ValidationRule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Sets;

/**
 * @author Halvdan Hoem Grelland
 */

@Data
@EqualsAndHashCode( callSuper = true )
@JacksonXmlRootElement( namespace = DxfNamespaces.DXF_2_0 )
public class ValidationNotificationTemplate
    extends BaseIdentifiableObject
    implements NotificationTemplate, MetadataObject
{
    private static final Set<DeliveryChannel> ALL_DELIVERY_CHANNELS = Sets.newHashSet( DeliveryChannel.values() );

    public ValidationNotificationTemplate()
    {
    }

    public ValidationNotificationTemplate( String subjectTemplate, String messageTemplate,
        Set<ValidationRule> validationRules, Boolean notifyUsersInHierarchyOnly,
        Boolean notifyParentOrganisationUnitOnly, Set<UserGroup> recipientUserGroups, SendStrategy sendStrategy )
    {
        this.subjectTemplate = subjectTemplate;
        this.messageTemplate = messageTemplate;
        this.validationRules = validationRules;
        this.notifyUsersInHierarchyOnly = notifyUsersInHierarchyOnly;
        this.notifyParentOrganisationUnitOnly = notifyParentOrganisationUnitOnly;
        this.recipientUserGroups = recipientUserGroups;
        this.sendStrategy = sendStrategy;
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private String subjectTemplate;

    @PropertyRange( min = 1, max = 1000 )
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private String messageTemplate;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Set<ValidationRule> validationRules = new HashSet<>();

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Boolean notifyUsersInHierarchyOnly;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Boolean notifyParentOrganisationUnitOnly;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Set<UserGroup> recipientUserGroups = new HashSet<>();

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private SendStrategy sendStrategy = SendStrategy.COLLECTIVE_SUMMARY;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addValidationRule( ValidationRule validationRule )
    {
        this.validationRules.add( validationRule );
        validationRule.getNotificationTemplates().add( this );
    }

    @Override
    public Set<DeliveryChannel> getDeliveryChannels()
    {
        return ALL_DELIVERY_CHANNELS;
    }
}
