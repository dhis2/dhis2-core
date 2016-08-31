package org.hisp.dhis.dxf2.events.report;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.hisp.dhis.common.BaseLinkableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */

@JacksonXmlRootElement( localName = "eventRow", namespace = DxfNamespaces.DXF_2_0 )
public class EventRow
    extends BaseLinkableObject
{
    private String trackedEntityInstance;
    
    private String trackedEntityInstanceOrgUnit;
    
    private String trackedEntityInstanceOrgUnitName;
    
    private String trackedEntityInstanceCreated;
    
    private boolean trackedEntityInstanceInactive;
    
    private String event;
    
    private String program;
    
    private String programStage;

    private String enrollment;
    
    private String orgUnit;
    
    private String orgUnitName;
    
    private String eventDate;
    
    private String dueDate;
    
    private Boolean followup;
    
    private List<Attribute> attributes = new ArrayList<>();    
    
    private List<DataValue> dataValues = new ArrayList<>();
    
    private List<Note> notes = new ArrayList<>();
    
    private String attributeCategoryOptions;

    public EventRow()
    {
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
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstanceOrgUnit()
    {
        return trackedEntityInstanceOrgUnit;
    }

    public void setTrackedEntityInstanceOrgUnit( String trackedEntityInstanceOrgUnit )
    {
        this.trackedEntityInstanceOrgUnit = trackedEntityInstanceOrgUnit;
    }   
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstanceOrgUnitName()
    {
        return trackedEntityInstanceOrgUnitName;
    }

    public void setTrackedEntityInstanceOrgUnitName( String trackedEntityInstanceOrgUnitName )
    {
        this.trackedEntityInstanceOrgUnitName = trackedEntityInstanceOrgUnitName;
    }
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstanceCreated()
    {
        return trackedEntityInstanceCreated;
    }

    public void setTrackedEntityInstanceCreated( String trackedEntityInstanceCreated )
    {
        this.trackedEntityInstanceCreated = trackedEntityInstanceCreated;
    }
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public boolean getTrackedEntityInstanceInactive()
    {
        return trackedEntityInstanceInactive;
    }

    public void setTrackedEntityInstanceInactive( boolean trackedEntityInstanceInactive )
    {
        this.trackedEntityInstanceInactive = trackedEntityInstanceInactive;
    }
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( List<Attribute> attributes )
    {
        this.attributes = attributes;
    }    
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public List<DataValue> getDataValues()
    {
        return dataValues;
    }

    public void setDataValues( List<DataValue> dataValues )
    {
        this.dataValues = dataValues;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getEvent()
    {
        return event;
    }

    public void setEvent( String event )
    {
        this.event = event;
    }  
    
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getProgram()
    {
        return program;
    }

    public void setProgram( String program )
    {
        this.program = program;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( String programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
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

    @JsonProperty
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
        if ( this == o )
        {
            return true;
        }
        
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        EventRow eventRow1 = (EventRow) o;

        if ( event != null ? !event.equals( eventRow1.event ) : eventRow1.event != null )
        {
            return false;
        }
        
        if ( attributes != null ? !attributes.equals( eventRow1.attributes ) : eventRow1.attributes != null )
        {
            return false;
        }
        
        if ( dataValues != null ? !dataValues.equals( eventRow1.dataValues ) : eventRow1.dataValues != null )
        {
            return false;
        }
        
        if ( eventDate != null ? !eventDate.equals( eventRow1.eventDate ) : eventRow1.eventDate != null )
        {
            return false;
        }
        
        if ( dueDate != null ? !dueDate.equals( eventRow1.dueDate ) : eventRow1.dueDate != null )
        {
            return false;
        }
        
        if ( orgUnitName != null ? !orgUnitName.equals( eventRow1.orgUnitName ) : eventRow1.orgUnitName != null )
        {
            return false;
        }
        
        if ( orgUnit != null ? !orgUnit.equals( eventRow1.orgUnit ) : eventRow1.orgUnit != null )
        {
            return false;
        }
        
        if ( trackedEntityInstance != null ? !trackedEntityInstance.equals( eventRow1.trackedEntityInstance )
            : eventRow1.trackedEntityInstance != null )
        {
            return false;
        }
        
        if ( program != null ? !program.equals( eventRow1.program ) : eventRow1.program != null )
        {
            return false;
        }
        
        if ( programStage != null ? !programStage.equals( eventRow1.programStage ) : eventRow1.programStage != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = event != null ? event.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (dataValues != null ? dataValues.hashCode() : 0);
        result = 31 * result + (program != null ? program.hashCode() : 0);
        result = 31 * result + (programStage != null ? programStage.hashCode() : 0);
        result = 31 * result + (orgUnitName != null ? orgUnitName.hashCode() : 0);
        result = 31 * result + (orgUnit != null ? orgUnit.hashCode() : 0);
        result = 31 * result + (trackedEntityInstance != null ? trackedEntityInstance.hashCode() : 0);
        result = 31 * result + (eventDate != null ? eventDate.hashCode() : 0);
        result = 31 * result + (dueDate != null ? dueDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Event{" + 
            "event='" + event + '\'' +
            ", attributes=" + attributes +
            ", dataValues=" + dataValues +
            ", program='" + program + '\'' + 
            ", programStage='" + programStage + '\'' + 
            ", eventOrgUnitName='" + orgUnitName + '\'' + 
            ", registrationOrgUnit='" + orgUnit + '\'' +
            ", trackedEntityInstance='" + trackedEntityInstance + '\'' + 
            ", eventDate='" + eventDate + '\'' + 
            ", dueDate='" + dueDate + '\'' +        
            ", attributeCategoryOptions=" + attributeCategoryOptions +
            '}';
    }
}
