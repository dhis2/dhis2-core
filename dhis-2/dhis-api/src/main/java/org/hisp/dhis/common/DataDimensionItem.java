package org.hisp.dhis.common;

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

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
* @author Lars Helge Overland
*/
@JacksonXmlRootElement( localName = "dataDimensionItem", namespace = DxfNamespaces.DXF_2_0 )
public class DataDimensionItem
{    
    public static final Set<Class<? extends IdentifiableObject>> DATA_DIMENSION_CLASSES = ImmutableSet.<Class<? extends IdentifiableObject>>builder().
        add( Indicator.class ).add( DataElement.class ).add( DataElementOperand.class ).
        add( DataSet.class ).add( ProgramIndicator.class ).add( TrackedEntityAttribute.class ).build();
    
    public static final Map<DataDimensionItemType, Class<? extends NameableObject>> DATA_DIMENSION_TYPE_CLASS_MAP = ImmutableMap.<DataDimensionItemType, Class<? extends NameableObject>>builder().
        put( DataDimensionItemType.INDICATOR, Indicator.class ).put( DataDimensionItemType.AGGREGATE_DATA_ELEMENT, DataElement.class ).
        put( DataDimensionItemType.DATA_ELEMENT_OPERAND, DataElementOperand.class ).put( DataDimensionItemType.DATA_SET, DataSet.class ).
        put( DataDimensionItemType.PROGRAM_INDICATOR, ProgramIndicator.class ).put( DataDimensionItemType.PROGRAM_ATTRIBUTE, TrackedEntityAttribute.class ).
        put( DataDimensionItemType.PROGRAM_DATA_ELEMENT, DataElement.class ).build();
    
    public static final Map<DataDimensionItemType, DataElementDomain> DATA_DIMENSION_TYPE_DOMAIN_MAP = ImmutableMap.<DataDimensionItemType, DataElementDomain>builder().
        put( DataDimensionItemType.AGGREGATE_DATA_ELEMENT, DataElementDomain.AGGREGATE ).put( DataDimensionItemType.PROGRAM_DATA_ELEMENT, DataElementDomain.TRACKER ).build();
    
    private int id;
    
    // -------------------------------------------------------------------------
    // Data dimension objects
    // -------------------------------------------------------------------------

    private Indicator indicator;
    
    private DataElement dataElement;
    
    private DataElementOperand dataElementOperand;
    
    private DataSet dataSet;
    
    private ProgramIndicator programIndicator;
    
    private TrackedEntityAttribute trackedEntityAttribute;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DataDimensionItem()
    {
    }

    public static DataDimensionItem create( NameableObject object )
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
        else if ( DataSet.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setDataSet( (DataSet) object );
        }
        else if ( ProgramIndicator.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setProgramIndicator( (ProgramIndicator) object );
        }
        else if ( TrackedEntityAttribute.class.isAssignableFrom( object.getClass() ) )
        {
            dimension.setTrackedEntityAttribute( (TrackedEntityAttribute) object );
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

    public NameableObject getNameableObject()
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
        else if ( dataSet != null )
        {
            return dataSet;
        }
        else if ( programIndicator != null )
        {
            return programIndicator;
        }
        else if ( trackedEntityAttribute != null )
        {
            return trackedEntityAttribute;
        }
        
        return null;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataDimensionItemType getDataDimensionItemType()
    {
        if ( indicator != null )
        {
            return DataDimensionItemType.INDICATOR;
        }
        else if ( dataElement != null )
        {
            if ( DataElementDomain.TRACKER.equals( dataElement.getDomainType() ) )
            {
                return DataDimensionItemType.PROGRAM_DATA_ELEMENT;
            }
            else
            {
                return DataDimensionItemType.AGGREGATE_DATA_ELEMENT;
            }
        }
        else if ( dataElementOperand != null )
        {
            return DataDimensionItemType.DATA_ELEMENT_OPERAND;
        }
        else if ( dataSet != null )
        {
            return DataDimensionItemType.DATA_SET;
        }
        else if ( programIndicator != null )
        {
            return DataDimensionItemType.PROGRAM_INDICATOR;
        }
        else if ( trackedEntityAttribute != null )
        {
            return DataDimensionItemType.PROGRAM_ATTRIBUTE;
        }
        
        return null;
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
    }

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttribute getTrackedEntityAttribute()
    {
        return trackedEntityAttribute;
    }

    public void setTrackedEntityAttribute( TrackedEntityAttribute trackedEntityAttribute )
    {
        this.trackedEntityAttribute = trackedEntityAttribute;
    }
}
