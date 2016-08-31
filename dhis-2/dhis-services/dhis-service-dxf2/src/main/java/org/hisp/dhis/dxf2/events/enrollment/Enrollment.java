package org.hisp.dhis.dxf2.events.enrollment;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "enrollment", namespace = DxfNamespaces.DXF_2_0 )
public class Enrollment
{
    private String enrollment;

    private Date created;

    private Date lastUpdated;

    private String trackedEntity;

    private String trackedEntityInstance;

    private String program;

    private EnrollmentStatus status;

    private String orgUnit;

    private Date enrollmentDate;

    private Date incidentDate;

    private List<Attribute> attributes = new ArrayList<>();

    private List<Note> notes = new ArrayList<>();

    private Boolean followup;

    public Enrollment()
    {
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getEnrollment()
    {
        return enrollment;
    }

    public void setEnrollment( String enrollment )
    {
        this.enrollment = enrollment;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( String trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance( String trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getProgram()
    {
        return program;
    }

    public void setProgram( String program )
    {
        this.program = program;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EnrollmentStatus getStatus()
    {
        return status;
    }

    public void setStatus( EnrollmentStatus status )
    {
        this.status = status;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( String orgUnit )
    {
        this.orgUnit = orgUnit;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEnrollmentDate()
    {
        return enrollmentDate;
    }

    public void setEnrollmentDate( Date enrollmentDate )
    {
        this.enrollmentDate = enrollmentDate;
    }

    @JsonProperty( required = true )
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
    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( List<Attribute> attributes )
    {
        this.attributes = attributes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<Note> getNotes()
    {
        return notes;
    }

    public void setNotes( List<Note> notes )
    {
        this.notes = notes;
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

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        Enrollment that = (Enrollment) o;

        if ( attributes != null ? !attributes.equals( that.attributes ) : that.attributes != null ) return false;
        if ( enrollmentDate != null ? !enrollmentDate.equals( that.enrollmentDate ) : that.enrollmentDate != null ) return false;
        if ( incidentDate != null ? !incidentDate.equals( that.incidentDate ) : that.incidentDate != null ) return false;
        if ( enrollment != null ? !enrollment.equals( that.enrollment ) : that.enrollment != null ) return false;
        if ( trackedEntityInstance != null ? !trackedEntityInstance.equals( that.trackedEntityInstance ) : that.trackedEntityInstance != null )
            return false;
        if ( program != null ? !program.equals( that.program ) : that.program != null ) return false;
        if ( status != that.status ) return false;
        if ( notes != null ? !notes.equals( that.notes ) : that.notes != null ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = enrollment != null ? enrollment.hashCode() : 0;
        result = 31 * result + (trackedEntityInstance != null ? trackedEntityInstance.hashCode() : 0);
        result = 31 * result + (program != null ? program.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (enrollmentDate != null ? enrollmentDate.hashCode() : 0);
        result = 31 * result + (incidentDate != null ? incidentDate.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Enrollment{" +
            "enrollment='" + enrollment + '\'' +
            ", trackedEntityInstance='" + trackedEntityInstance + '\'' +
            ", program='" + program + '\'' +
            ", status=" + status +
            ", dateOfEnrollment=" + enrollmentDate +
            ", incidentDate=" + incidentDate +
            ", attributes=" + attributes +
            ", notes=" + notes +
            '}';
    }
}
