package org.hisp.dhis.dxf2.events.enrollment;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.vividsolutions.jts.geom.Geometry;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "enrollment", namespace = DxfNamespaces.DXF_2_0 )
public class Enrollment
{
    private Long enrollmentId;

    private String enrollment;

    private String created;

    private String lastUpdated;

    private String createdAtClient;

    private String lastUpdatedAtClient;

    private String trackedEntityType;

    private String trackedEntityInstance;

    private String program;

    private EnrollmentStatus status;

    private String orgUnit;

    private String orgUnitName;

    private Date enrollmentDate;

    private Date incidentDate;

    private List<Event> events = new ArrayList<>();

    private Set<Relationship> relationships = new HashSet<>();

    private List<Attribute> attributes = new ArrayList<>();

    private List<Note> notes = new ArrayList<>();

    private Boolean followup;

    private String completedBy;

    private Date completedDate;

    private Coordinate coordinate;

    private Boolean deleted = false;

    private String storedBy;
    
    private Geometry geometry;

    public Enrollment()
    {
    }

    public void clear()
    {
        this.setDeleted( null );
        this.setNotes( null );
        this.setRelationships( null );
        this.setAttributes( null );
        this.setEvents( null );
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
    public String getCreated()
    {
        return created;
    }

    public void setCreated( String created )
    {
        this.created = created;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getCreatedAtClient()
    {
        return createdAtClient;
    }

    public void setCreatedAtClient( String createdAtClient )
    {
        this.createdAtClient = createdAtClient;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getLastUpdatedAtClient()
    {
        return lastUpdatedAtClient;
    }

    public void setLastUpdatedAtClient( String lastUpdatedAtClient )
    {
        this.lastUpdatedAtClient = lastUpdatedAtClient;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityType()
    {
        return trackedEntityType;
    }

    public void setTrackedEntityType( String trackedEntityType )
    {
        this.trackedEntityType = trackedEntityType;
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

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOrgUnitName()
    {
        return orgUnitName;
    }

    public void setOrgUnitName( String orgUnitName )
    {
        this.orgUnitName = orgUnitName;
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
    @JacksonXmlElementWrapper( localName = "events", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<Event> getEvents()
    {
        return events;
    }

    public void setEvents( List<Event> events )
    {
        this.events = events;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "attributes", namespace = DxfNamespaces.DXF_2_0 )
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
    @JacksonXmlElementWrapper( localName = "notes", namespace = DxfNamespaces.DXF_2_0 )
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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
    public Date getCompletedDate()
    {
        return completedDate;
    }

    public void setCompletedDate( Date completedDate )
    {
        this.completedDate = completedDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Coordinate getCoordinate()
    {
        return coordinate;
    }

    public void setCoordinate( Coordinate coordinate )
    {
        this.coordinate = coordinate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted( Boolean deleted )
    {
        this.deleted = deleted;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Set<Relationship> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( Set<Relationship> relationships )
    {
        this.relationships = relationships;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Geometry getGeometry()
    {
        return geometry;
    }

    public void setGeometry( Geometry geometry )
    {
        this.geometry = geometry;
    }

    @Override public String toString()
    {
        return "Enrollment{" +
            "enrollment='" + enrollment + '\'' +
            ", trackedEntityType='" + trackedEntityType + '\'' +
            ", trackedEntityInstance='" + trackedEntityInstance + '\'' +
            ", program='" + program + '\'' +
            ", status=" + status +
            ", orgUnit='" + orgUnit + '\'' +
            ", enrollmentDate=" + enrollmentDate +
            ", incidentDate=" + incidentDate +
            ", events=" + events +
            ", relationships=" + relationships +
            ", attributes=" + attributes +
            ", notes=" + notes +
            ", deleted=" + deleted +
            '}';
    }

    @JsonIgnore
    public Long getId() {
        return enrollmentId;
    }

    public void setId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }
}
