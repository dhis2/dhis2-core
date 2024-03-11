package org.hisp.dhis.notification.logging;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.Date;

/**
 * Created by zubair@dhis2.org on 10.01.18.
 */

@JacksonXmlRootElement( localName = "externalNotificationLogEntry", namespace = DxfNamespaces.DXF_2_0 )
public class ExternalNotificationLogEntry
    extends BaseIdentifiableObject
{
    private Date lastSentAt;

    private int retries;

    private String key;

    private String notificationTemplateUid;

    private boolean allowMultiple;

    private NotificationTriggerEvent notificationTriggeredBy;

    public ExternalNotificationLogEntry()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastSentAt()
    {
        return lastSentAt;
    }

    public void setLastSentAt( Date lastSentAt )
    {
        this.lastSentAt = lastSentAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getRetries()
    {
        return retries;
    }

    public void setRetries( int retries )
    {
        this.retries = retries;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public NotificationTriggerEvent getNotificationTriggeredBy()
    {
        return notificationTriggeredBy;
    }

    public void setNotificationTriggeredBy( NotificationTriggerEvent notificationTriggeredBy )
    {
        this.notificationTriggeredBy = notificationTriggeredBy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isAllowMultiple()
    {
        return allowMultiple;
    }

    public void setAllowMultiple( boolean allowMultiple )
    {
        this.allowMultiple = allowMultiple;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNotificationTemplateUid()
    {
        return notificationTemplateUid;
    }

    public void setNotificationTemplateUid( String notificationTemplateUid )
    {
        this.notificationTemplateUid = notificationTemplateUid;
    }

    @Override
    public void setAutoFields()
    {
        super.setAutoFields();
    }
}
