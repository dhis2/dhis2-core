package org.hisp.dhis.program;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "programInstance", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramInstance
    extends BaseIdentifiableObject
{
    private ProgramStatus status = ProgramStatus.ACTIVE;

    private OrganisationUnit organisationUnit;

    private Date incidentDate;

    private Date enrollmentDate;

    private Date endDate;

    private TrackedEntityInstance entityInstance;

    private Program program;

    private Set<ProgramStageInstance> programStageInstances = new HashSet<>();

    @Deprecated
    private List<OutboundSms> outboundSms = new ArrayList<>();

    private List<MessageConversation> messageConversations = new ArrayList<>();

    private Boolean followup = false;

    private List<TrackedEntityComment> comments = new ArrayList<>();

    private String completedBy;
    
    private Double longitude;

    private Double latitude;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramInstance()
    {
    }

    public ProgramInstance( Date enrollmentDate, Date incidentDate, TrackedEntityInstance entityInstance, Program program )
    {
        this.enrollmentDate = enrollmentDate;
        this.incidentDate = incidentDate;
        this.entityInstance = entityInstance;
        this.program = program;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Updated the bi-directional associations between this program instance and
     * the given entity instance and program.
     *
     * @param entityInstance the entity instance to enroll.
     * @param program        the program to enroll the entity instance to.
     */
    public void enrollTrackedEntityInstance( TrackedEntityInstance entityInstance, Program program )
    {
        Assert.notNull( entityInstance );
        Assert.notNull( program );

        setEntityInstance( entityInstance );
        entityInstance.getProgramInstances().add( this );

        setProgram( program );
    }

    public ProgramStageInstance getProgramStageInstanceByStage( int stage )
    {
        int count = 1;

        for ( ProgramStageInstance programInstanceStage : programStageInstances )
        {
            if ( count == stage )
            {
                return programInstanceStage;
            }

            count++;
        }

        return null;
    }

    public ProgramStageInstance getActiveProgramStageInstance()
    {
        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( programStageInstance.getProgramStage().getOpenAfterEnrollment()
                && !programStageInstance.isCompleted()
                && (programStageInstance.getStatus() != null && programStageInstance.getStatus() != EventStatus.SKIPPED) )
            {
                return programStageInstance;
            }
        }

        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( !programStageInstance.isCompleted()
                && (programStageInstance.getStatus() != null && programStageInstance.getStatus() != EventStatus.SKIPPED) )
            {
                return programStageInstance;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // equals and hashCode
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();

        result = prime * result + ((incidentDate == null) ? 0 : incidentDate.hashCode());
        result = prime * result + ((enrollmentDate == null) ? 0 : enrollmentDate.hashCode());
        result = prime * result + ((entityInstance == null) ? 0 : entityInstance.hashCode());
        result = prime * result + ((program == null) ? 0 : program.hashCode());

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( object.getClass() ) )
        {
            return false;
        }

        final ProgramInstance other = (ProgramInstance) object;

        if ( incidentDate == null )
        {
            if ( other.incidentDate != null )
            {
                return false;
            }
        }
        else if ( !incidentDate.equals( other.incidentDate ) )
        {
            return false;
        }

        if ( enrollmentDate == null )
        {
            if ( other.enrollmentDate != null )
            {
                return false;
            }
        }
        else if ( !enrollmentDate.equals( other.enrollmentDate ) )
        {
            return false;
        }

        if ( entityInstance == null )
        {
            if ( other.entityInstance != null )
            {
                return false;
            }
        }
        else if ( !entityInstance.equals( other.entityInstance ) )
        {
            return false;
        }

        if ( program == null )
        {
            if ( other.program != null )
            {
                return false;
            }
        }
        else if ( !program.equals( other.program ) )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getIncidentDate()
    {
        return incidentDate;
    }

    public void setIncidentDate( Date incidentDate )
    {
        this.incidentDate = incidentDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEnrollmentDate()
    {
        return enrollmentDate;
    }

    public void setEnrollmentDate( Date enrollmentDate )
    {
        this.enrollmentDate = enrollmentDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStatus getStatus()
    {
        return status;
    }

    public void setStatus( ProgramStatus status )
    {
        this.status = status;
    }

    @JsonProperty( "trackedEntityInstance" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityInstance getEntityInstance()
    {
        return entityInstance;
    }

    public void setEntityInstance( TrackedEntityInstance entityInstance )
    {
        this.entityInstance = entityInstance;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programStageInstances", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStageInstance", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramStageInstance> getProgramStageInstances()
    {
        return programStageInstances;
    }

    public void setProgramStageInstances( Set<ProgramStageInstance> programStageInstances )
    {
        this.programStageInstances = programStageInstances;
    }

    public List<OutboundSms> getOutboundSms()
    {
        return outboundSms;
    }

    public void setOutboundSms( List<OutboundSms> outboundSms )
    {
        this.outboundSms = outboundSms;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getFollowup()
    {
        return followup;
    }

    public void setFollowup( Boolean followup )
    {
        this.followup = followup;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "messageConversations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "messageConversation", namespace = DxfNamespaces.DXF_2_0 )
    public List<MessageConversation> getMessageConversations()
    {
        return messageConversations;
    }

    public void setMessageConversations( List<MessageConversation> messageConversations )
    {
        this.messageConversations = messageConversations;
    }

    @JsonProperty( "trackedEntityComments" )
    @JacksonXmlElementWrapper( localName = "trackedEntityComments", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntityComment", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityComment> getComments()
    {
        return comments;
    }

    public void setComments( List<TrackedEntityComment> comments )
    {
        this.comments = comments;
    }

    public String getCompletedBy()
    {
        return completedBy;
    }

    public void setCompletedBy( String completedBy )
    {
        this.completedBy = completedBy;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getLongitude()
    {
        return longitude;
    }
       
    public void setLongitude( Double longitude )
    {
        this.longitude = longitude;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getLatitude()
    {
        return latitude;
    }
    
    public void setLatitude( Double latitude )
    {
        this.latitude = latitude;
    }
}
