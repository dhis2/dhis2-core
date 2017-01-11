package org.hisp.dhis.dxf2.events.event.csv;

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

import java.util.Objects;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JsonPropertyOrder( {
    "event",
    "status",
    "program",
    "programStage",
    "enrollment",
    "orgUnit",
    "eventDate",
    "dueDate",
    "latitude",
    "longitude",
    "dataElement",
    "value",
    "storedBy",
    "providedElsewhere"
} )
public class CsvEventDataValue
{
    private String event;

    private String status;

    private String program;

    private String programStage;

    private String orgUnit;

    private String enrollment;

    private String eventDate;

    private String dueDate;

    private Double latitude;

    private Double longitude;

    private String dataElement;

    private String value;

    private String storedBy;

    private Boolean providedElsewhere;

    public CsvEventDataValue()
    {
    }

    public CsvEventDataValue( CsvEventDataValue dataValue )
    {
        Assert.notNull( dataValue, "A non-null CsvOutputEventDataValue must be given as a parameter." );

        this.event = dataValue.getEvent();
        this.status = dataValue.getStatus();
        this.program = dataValue.getProgram();
        this.programStage = dataValue.getProgramStage();
        this.enrollment = dataValue.getEnrollment();
        this.orgUnit = dataValue.getOrgUnit();
        this.eventDate = dataValue.getEventDate();
        this.dueDate = dataValue.getDueDate();
        this.latitude = dataValue.getLatitude();
        this.longitude = dataValue.getLongitude();
        this.dataElement = dataValue.getDataElement();
        this.value = dataValue.getValue();
        this.storedBy = dataValue.getStoredBy();
        this.providedElsewhere = dataValue.getProvidedElsewhere();
    }

    @JsonProperty
    public String getEvent()
    {
        return event;
    }

    public void setEvent( String event )
    {
        this.event = event;
    }

    @JsonProperty
    public String getStatus()
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    @JsonProperty
    public String getProgram()
    {
        return program;
    }

    public void setProgram( String program )
    {
        this.program = program;
    }

    @JsonProperty
    public String getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( String programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    public String getEnrollment()
    {
        return enrollment;
    }

    public void setEnrollment( String enrollment )
    {
        this.enrollment = enrollment;
    }

    @JsonProperty
    public String getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( String orgUnit )
    {
        this.orgUnit = orgUnit;
    }

    @JsonProperty
    public String getEventDate()
    {
        return eventDate;
    }

    public void setEventDate( String eventDate )
    {
        this.eventDate = eventDate;
    }

    @JsonProperty
    public String getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( String dueDate )
    {
        this.dueDate = dueDate;
    }

    @JsonProperty
    public Double getLatitude()
    {
        return latitude;
    }

    public void setLatitude( Double latitude )
    {
        this.latitude = latitude;
    }

    @JsonProperty
    public Double getLongitude()
    {
        return longitude;
    }

    public void setLongitude( Double longitude )
    {
        this.longitude = longitude;
    }

    @JsonProperty
    public String getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( String dataElement )
    {
        this.dataElement = dataElement;
    }

    @JsonProperty
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    @JsonProperty
    public Boolean getProvidedElsewhere()
    {
        return providedElsewhere;
    }

    public void setProvidedElsewhere( Boolean providedElsewhere )
    {
        this.providedElsewhere = providedElsewhere;
    }

    @JsonProperty
    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( event, status, program, programStage, orgUnit, enrollment, eventDate, dueDate, latitude, longitude,
            dataElement, value, storedBy, providedElsewhere );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        final CsvEventDataValue other = (CsvEventDataValue) obj;

        return Objects.equals( this.event, other.event ) && Objects.equals( this.status, other.status ) && Objects.equals( this.program,
            other.program ) && Objects.equals( this.programStage, other.programStage ) && Objects.equals( this.orgUnit,
            other.orgUnit ) && Objects.equals( this.enrollment, other.enrollment ) && Objects.equals( this.eventDate,
            other.eventDate ) && Objects.equals( this.dueDate, other.dueDate ) && Objects.equals( this.latitude,
            other.latitude ) && Objects.equals( this.longitude, other.longitude ) && Objects.equals( this.dataElement,
            other.dataElement ) && Objects.equals( this.value, other.value ) && Objects.equals( this.storedBy,
            other.storedBy ) && Objects.equals( this.providedElsewhere, other.providedElsewhere );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "event", event )
            .add( "status", status )
            .add( "program", program )
            .add( "programStage", programStage )
            .add( "enrollment", enrollment )
            .add( "orgUnit", orgUnit )
            .add( "eventDate", eventDate )
            .add( "dueDate", dueDate )
            .add( "latitude", latitude )
            .add( "longitude", longitude )
            .add( "dataElement", dataElement )
            .add( "value", value )
            .add( "storedBy", storedBy )
            .add( "providedElsewhere", providedElsewhere )
            .toString();
    }
}
