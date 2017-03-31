package org.hisp.dhis.common;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;

import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "dataDimensionItem", namespace = DxfNamespaces.DXF_2_0 )
public class DataDimensionItem
{
    public static final Set<Class<? extends IdentifiableObject>> DATA_DIMENSION_CLASSES = ImmutableSet.<Class<? extends IdentifiableObject>>builder().
        add( Indicator.class ).add( DataElement.class ).add( DataElementOperand.class ).add( ReportingRate.class ).
        add( ProgramIndicator.class ).add( ProgramDataElementDimensionItem.class ).add( ProgramTrackedEntityAttributeDimensionItem.class ).build();

    public static final Map<DataDimensionItemType, Class<? extends NameableObject>> DATA_DIMENSION_TYPE_CLASS_MAP = ImmutableMap.<DataDimensionItemType, Class<? extends NameableObject>>builder().
        put( DataDimensionItemType.INDICATOR, Indicator.class ).put( DataDimensionItemType.DATA_ELEMENT, DataElement.class ).
        put( DataDimensionItemType.DATA_ELEMENT_OPERAND, DataElementOperand.class ).put( DataDimensionItemType.REPORTING_RATE, ReportingRate.class ).
        put( DataDimensionItemType.PROGRAM_INDICATOR, ProgramIndicator.class ).put( DataDimensionItemType.PROGRAM_DATA_ELEMENT, ProgramDataElementDimensionItem.class ).
        put( DataDimensionItemType.PROGRAM_ATTRIBUTE, ProgramTrackedEntityAttributeDimensionItem.class ).build();

    private int id;

    // -------------------------------------------------------------------------
    // Data dimension objects
    // -------------------------------------------------------------------------

    private Indicator indicator;

    private DataElement dataElement;

    private DataElementOperand dataElementOperand;

    private ReportingRate reportingRate;

    private ProgramIndicator programIndicator;

    private ProgramDataElementDimensionItem programDataElement;

    private ProgramTrackedEntityAttributeDimensionItem programAttribute;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DataDimensionItem()
    {
    }

    public static DataDimensionItem create( DimensionalItemObject object )
    {
        DataDimensionItem dimension = new DataDimensionItem();

        if ( Indicator.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setIndicator( (Indicator) object );
        }
        else if ( DataElement.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setDataElement( (DataElement) object );
        }
        else if ( DataElementOperand.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setDataElementOperand( (DataElementOperand) object );
        }
        else if ( ReportingRate.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setReportingRate( (ReportingRate) object );
        }
        else if ( ProgramIndicator.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setProgramIndicator( (ProgramIndicator) object );
        }
        else if ( ProgramDataElementDimensionItem.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setProgramDataElement( (ProgramDataElementDimensionItem) object );
        }
        else if ( ProgramTrackedEntityAttributeDimensionItem.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setProgramAttribute( (ProgramTrackedEntityAttributeDimensionItem) object );
        }
        else
        {
            throw new IllegalArgumentException( "Not a valid data dimension: " + object.getClass().getSimpleName() + ", " + object );
        }

        return dimension;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public DimensionalItemObject getDimensionalItemObject()
    {
        if ( indicator != null )
        {
            return indicator;
        }
        else if ( dataElement != null )
        {
            return dataElement;
        }
        else if ( dataElementOperand != null )
        {
            return dataElementOperand;
        }
        else if ( reportingRate != null )
        {
            return reportingRate;
        }
        else if ( programIndicator != null )
        {
            return programIndicator;
        }
        else if ( programDataElement != null )
        {
            return programDataElement;
        }
        else if ( programAttribute != null )
        {
            return programAttribute;
        }

        return null;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataDimensionItemType getDataDimensionItemType()
    {
        if ( indicator != null )
        {
            return DataDimensionItemType.INDICATOR;
        }
        else if ( dataElement != null )
        {
            return DataDimensionItemType.DATA_ELEMENT;
        }
        else if ( dataElementOperand != null )
        {
            return DataDimensionItemType.DATA_ELEMENT_OPERAND;
        }
        else if ( reportingRate != null )
        {
            return DataDimensionItemType.REPORTING_RATE;
        }
        else if ( programIndicator != null )
        {
            return DataDimensionItemType.PROGRAM_INDICATOR;
        }
        else if ( programDataElement != null )
        {
            return DataDimensionItemType.PROGRAM_DATA_ELEMENT;
        }
        else if ( programAttribute != null )
        {
            return DataDimensionItemType.PROGRAM_ATTRIBUTE;
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Equals and hashCode
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getDimensionalItemObject() == null) ? 0 : getDimensionalItemObject().hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        DataDimensionItem other = (DataDimensionItem) obj;

        DimensionalItemObject object = getDimensionalItemObject();

        if ( object == null )
        {
            if ( other.getDimensionalItemObject() != null )
            {
                return false;
            }
        }
        else if ( !object.equals( other.getDimensionalItemObject() ) )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    @JsonIgnore
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Indicator getIndicator()
    {
        return indicator;
    }

    public void setIndicator( Indicator indicator )
    {
        this.indicator = indicator;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementOperand getDataElementOperand()
    {
        return dataElementOperand;
    }

    public void setDataElementOperand( DataElementOperand dataElementOperand )
    {
        this.dataElementOperand = dataElementOperand;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ReportingRate getReportingRate()
    {
        return reportingRate;
    }

    public void setReportingRate( ReportingRate reportingRate )
    {
        this.reportingRate = reportingRate;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    public void setProgramIndicator( ProgramIndicator programIndicator )
    {
        this.programIndicator = programIndicator;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramDataElementDimensionItem getProgramDataElement()
    {
        return programDataElement;
    }

    public void setProgramDataElement( ProgramDataElementDimensionItem programDataElement )
    {
        this.programDataElement = programDataElement;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramTrackedEntityAttributeDimensionItem getProgramAttribute()
    {
        return programAttribute;
    }

    public void setProgramAttribute( ProgramTrackedEntityAttributeDimensionItem programAttribute )
    {
        this.programAttribute = programAttribute;
    }
}
