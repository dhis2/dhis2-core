package org.hisp.dhis.program.notification;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;

import java.util.Date;

/**
 * @Author Zubair Asghar.
 */
@JacksonXmlRootElement( namespace = DxfNamespaces.DXF_2_0 )
public class ProgramNotificationInstance extends BaseIdentifiableObject
{
    private ProgramNotificationTemplate programNotificationTemplate;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private Date sentAt;

    private Date scheduledAt;

    public ProgramNotificationInstance()
    {
    }

    public ProgramNotificationInstance( ProgramInstance programInstance, ProgramNotificationTemplate programNotificationTemplate,
        ProgramStageInstance programStageInstance, Date scheduledAt, Date sentAt )
    {
        this.programInstance = programInstance;
        this.programNotificationTemplate = programNotificationTemplate;
        this.programStageInstance = programStageInstance;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
    }

    public boolean hasProgramInstance()
    {
        return programInstance != null;
    }

    public boolean hasProgramStageInstance()
    {
        return programStageInstance != null;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public void setProgramInstance( ProgramInstance programInstance )
    {
        this.programInstance = programInstance;
    }

    @JsonProperty()
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramNotificationTemplate getProgramNotificationTemplate()
    {
        return programNotificationTemplate;
    }

    public void setProgramNotificationTemplate( ProgramNotificationTemplate programNotificationTemplate )
    {
        this.programNotificationTemplate = programNotificationTemplate;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStageInstance getProgramStageInstance()
    {
        return programStageInstance;
    }

    public void setProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        this.programStageInstance = programStageInstance;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getScheduledAt()
    {
        return scheduledAt;
    }

    public void setScheduledAt( Date scheduledAt )
    {
        this.scheduledAt = scheduledAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getSentAt()
    {
        return sentAt;
    }

    public void setSentAt( Date sentAt )
    {
        this.sentAt = sentAt;
    }
}
