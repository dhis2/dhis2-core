package org.hisp.dhis.program;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Markus Bekken
 */
@JacksonXmlRootElement( localName = "analyticsPeriodBoundary", namespace = DxfNamespaces.DXF_2_0 )
public class AnalyticsPeriodBoundary extends BaseIdentifiableObject implements EmbeddedObject
{
    public static final String EVENT_DATE = "EVENT_DATE";
    public static final String ENROLLMENT_DATE = "ENROLLMENT_DATE";
    public static final String INCIDENT_DATE = "INCIDENT_DATE";
    
    public static final String DB_EVENT_DATE = "executiondate";
    public static final String DB_ENROLLMENT_DATE = "enrollmentdate";
    public static final String DB_INCIDENT_DATE = "incidentdate";
    
    
    private String boundaryTarget;
    
    private AnalyticsPeriodBoundaryType analyticsPeriodBoundaryType;
    
    private PeriodType offsetPeriodType;
    
    private Integer offsetPeriods;
    
    private ProgramIndicator programIndicator;
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    
    public AnalyticsPeriodBoundary()
    {
        
    }
    
    public AnalyticsPeriodBoundary( String boundaryTarget, AnalyticsPeriodBoundaryType analyticsPeriodBoundaryType, 
        PeriodType offsetPeriodType, Integer offsetPeriods )
    {
        this.boundaryTarget = boundaryTarget;
        this.analyticsPeriodBoundaryType = analyticsPeriodBoundaryType;
        this.offsetPeriodType = offsetPeriodType;
        this.offsetPeriods = offsetPeriods;
    }
    
    public AnalyticsPeriodBoundary( String boundaryTarget, AnalyticsPeriodBoundaryType analyticsPeriodBoundaryType )
    {
        this.boundaryTarget = boundaryTarget;
        this.analyticsPeriodBoundaryType = analyticsPeriodBoundaryType;
    }
    
    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
 
    private Date getBoundaryDate( Date reportingStartDate, Date reportingEndDate )
    {
        Date returnDate = null;
        
        if ( analyticsPeriodBoundaryType.isEndBoundary() )
        {
            returnDate = new Date( reportingEndDate.getTime() );
        }
        else
        {
            returnDate = new Date( reportingStartDate.getTime() );
        }
        
        if ( offsetPeriods != null && offsetPeriodType != null )
        {
           returnDate = this.offsetPeriodType.getDateWithOffset( returnDate, this.offsetPeriods );
        }
        
        return returnDate;
    }
    
    public Boolean isEventDateBoundary()
    {
        return boundaryTarget.equals( AnalyticsPeriodBoundary.EVENT_DATE );
    }
    
    public Boolean isEnrollmentDateBoundary()
    {
        return boundaryTarget.equals( AnalyticsPeriodBoundary.ENROLLMENT_DATE );
    }
    
    public Boolean isIncidentDateBoundary()
    {
        return boundaryTarget.equals( AnalyticsPeriodBoundary.INCIDENT_DATE );
    }
    
    public String getSqlCondition( Date reportingStartDate, Date reportingEndDate )
    {
        String column = isEventDateBoundary() ? DB_EVENT_DATE : isEnrollmentDateBoundary() ? DB_ENROLLMENT_DATE : isIncidentDateBoundary() ? DB_INCIDENT_DATE : null;
        Assert.isTrue( column != null, "Can not generate where condition for analyticsPeriodBoundary " + this.uid + " - unknown boundaryTarget" );
        
        final SimpleDateFormat format = new SimpleDateFormat();
        format.applyPattern( Period.DEFAULT_DATE_FORMAT );
        return column + " " + ( analyticsPeriodBoundaryType.isEndBoundary() ? "<=" : ">=" ) +
            " cast( '" + format.format( getBoundaryDate( reportingStartDate, reportingEndDate ) ) + "' as date )";
    }
    
    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------
  
    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash( this.boundaryTarget, this.analyticsPeriodBoundaryType, this.offsetPeriodType, this.offsetPeriods );
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
        if ( !super.equals( obj ) )
        {
            return false;
        }

        final AnalyticsPeriodBoundary other = (AnalyticsPeriodBoundary) obj;

        return Objects.equals( this.boundaryTarget, other.boundaryTarget )
            && Objects.equals( this.analyticsPeriodBoundaryType, other.analyticsPeriodBoundaryType )
            && Objects.equals( this.offsetPeriodType, other.offsetPeriodType )
            && Objects.equals( this.offsetPeriods, other.offsetPeriods );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getBoundaryTarget()
    {
        return boundaryTarget;
    }

    public void setBoundaryTarget( String boundaryTarget )
    {
        this.boundaryTarget = boundaryTarget;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AnalyticsPeriodBoundaryType getAnalyticsPeriodBoundaryType()
    {
        return analyticsPeriodBoundaryType;
    }

    public void setAnalyticsPeriodBoundaryType( AnalyticsPeriodBoundaryType analyticsPeriodBoundaryType )
    {
        this.analyticsPeriodBoundaryType = analyticsPeriodBoundaryType;
    }
    
    @JsonProperty
    @JsonSerialize( using = JacksonPeriodTypeSerializer.class )
    @JsonDeserialize( using = JacksonPeriodTypeDeserializer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.TEXT )
    public PeriodType getOffsetPeriodType()
    {
        return offsetPeriodType;
    }

    public void setOffsetPeriodType( PeriodType offsetPeriodType )
    {
        this.offsetPeriodType = offsetPeriodType;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getOffsetPeriods()
    {
        return offsetPeriods;
    }

    public void setOffsetPeriods( Integer offsetPeriods )
    {
        this.offsetPeriods = offsetPeriods;
    }
    
    @Property( owner = Property.Value.FALSE )
    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    public void setProgramIndicator( ProgramIndicator programIndicator )
    {
        this.programIndicator = programIndicator;
    }
}
