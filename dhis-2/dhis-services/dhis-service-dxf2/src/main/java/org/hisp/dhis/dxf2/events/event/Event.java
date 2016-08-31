package org.hisp.dhis.dxf2.events.event;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseLinkableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.event.EventStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "event", namespace = DxfNamespaces.DXF_2_0 )
public class Event
    extends BaseLinkableObject
{
    private String event;

    private EventStatus status = EventStatus.ACTIVE;

    private String program;

    private String programStage;

    private String enrollment;

    private EnrollmentStatus enrollmentStatus;

    private String orgUnit;

    private String orgUnitName;

    private String trackedEntityInstance;

    private String eventDate;

    private String dueDate;

    private String storedBy;

    private Coordinate coordinate;

    private List<DataValue> dataValues = new ArrayList<>();

    private List<Note> notes = new ArrayList<>();

    private Boolean followup;

    private String created;

    private String lastUpdated;
    
    private String attributeCategoryOptions;

    public Event()
    {
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getEvent()
    {
        return event;
    }

    public void setEvent( String event )
    {
        this.event = event;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public EnrollmentStatus getEnrollmentStatus()
    {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus( EnrollmentStatus programStatus )
    {
        this.enrollmentStatus = programStatus;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public EventStatus getStatus()
    {
        return status;
    }

    public void setStatus( EventStatus status )
    {
        this.status = status;
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
    @JacksonXmlProperty( isAttribute = true )
    public String getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( String programStage )
    {
        this.programStage = programStage;
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

    @JsonProperty
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
    public String getEventDate()
    {
        return eventDate;
    }

    public void setEventDate( String eventDate )
    {
        this.eventDate = eventDate;
    }

    @JsonProperty( required = false )
    @JacksonXmlProperty( isAttribute = true )
    public String getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( String dueDate )
    {
        this.dueDate = dueDate;
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
    @JacksonXmlElementWrapper( localName = "dataValues", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataValue", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataValue> getDataValues()
    {
        return dataValues;
    }

    public void setDataValues( List<DataValue> dataValues )
    {
        this.dataValues = dataValues;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "notes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "note", namespace = DxfNamespaces.DXF_2_0 )
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
    public String getCreated()
    {
        return created;
    }

    public void setCreated( String created )
    {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAttributeCategoryOptions()
    {
        return attributeCategoryOptions;
    }

    public void setAttributeCategoryOptions( String attributeCategoryOptions )
    {
        this.attributeCategoryOptions = attributeCategoryOptions;
    } 

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        Event event1 = (Event) o;

        if ( event != null ? !event.equals( event1.event ) : event1.event != null ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return event != null ? event.hashCode() : 0;
    }

    @Override
    public String toString()
    {
        return "Event{" +
            "event='" + event + '\'' +
            ", status=" + status +
            ", program='" + program + '\'' +
            ", programStage='" + programStage + '\'' +
            ", orgUnit='" + orgUnit + '\'' +
            ", trackedEntityInstance='" + trackedEntityInstance + '\'' +
            ", eventDate='" + eventDate + '\'' +
            ", dueDate='" + dueDate + '\'' +
            ", storedBy='" + storedBy + '\'' +
            ", coordinate=" + coordinate +
            ", dataValues=" + dataValues +
            ", attributeCategoryOptions=" + attributeCategoryOptions +
            '}';
    }       
}
