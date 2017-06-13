package org.hisp.dhis.minmax;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.io.Serializable;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "minMaxDataElement", namespace = DxfNamespaces.DXF_2_0 )
public class MinMaxDataElement
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 1557460368163701333L;

    private int id;

    private OrganisationUnit source;

    private DataElement dataElement;
    
    private DataElementCategoryOptionCombo optionCombo;

    private int min;

    private int max;

    private boolean generated;

    // -------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------- 
    
    public MinMaxDataElement()
    {
    }

    public MinMaxDataElement( OrganisationUnit source, DataElement dataElement, DataElementCategoryOptionCombo optionCombo, int min, int max,
        boolean generated )
    {
        this.source = source;
        this.dataElement = dataElement;
        this.optionCombo = optionCombo;
        this.min = min;
        this.max = max;
        this.generated = generated;
    }

    // -------------------------------------------------------------------------
    // Equals and hashCode
    // ------------------------------------------------------------------------- 

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + max;        
        result = prime * result + min;
        
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
        
        final MinMaxDataElement other = (MinMaxDataElement) obj;
        
        if ( max != other.max )
        {
            return false;
        }
        
        if ( min != other.min )
        {
            return false;
        }
        
        return true;
    }
    
    // -------------------------------------------------------------------------
    // Setters and getters
    // ------------------------------------------------------------------------- 

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getSource()
    {
        return source;
    }

    public void setSource( OrganisationUnit source )
    {
        this.source = source;
    }

    @JsonProperty
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryOptionCombo getOptionCombo()
    {
        return optionCombo;
    }

    public void setOptionCombo( DataElementCategoryOptionCombo optionCombo )
    {
        this.optionCombo = optionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getMin()
    {
        return min;
    }

    public void setMin( int min )
    {
        this.min = min;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getMax()
    {
        return max;
    }

    public void setMax( int max )
    {
        this.max = max;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isGenerated()
    {
        return generated;
    }

    public void setGenerated( boolean generated )
    {
        this.generated = generated;
    }

    public void mergeWith( MinMaxDataElement other )
    {
       source = other.getSource() == null ? source : other.getSource();
       dataElement = other.getDataElement() == null ? dataElement : other.getDataElement();
       optionCombo = other.getOptionCombo() == null ? optionCombo : other.getOptionCombo();
       min = other.getMin();
       max = other.getMax();
       generated = other.isGenerated();
    }
}
