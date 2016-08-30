package org.hisp.dhis.dataelement;

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
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementGroup
    extends BaseDimensionalItemObject
{
    private Set<DataElement> members = new HashSet<>();

    private DataElementGroupSet groupSet;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementGroup()
    {
    }

    public DataElementGroup( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElement( DataElement dataElement )
    {
        members.add( dataElement );
        dataElement.getGroups().add( this );
    }

    public void removeDataElement( DataElement dataElement )
    {
        members.remove( dataElement );
        dataElement.getGroups().remove( this );
    }

    public void removeAllDataElements()
    {
        for ( DataElement dataElement : members )
        {
            dataElement.getGroups().remove( this );
        }

        members.clear();
    }

    public void updateDataElements( Set<DataElement> updates )
    {
        for ( DataElement dataElement : new HashSet<>( members ) )
        {
            if ( !updates.contains( dataElement ) )
            {
                removeDataElement( dataElement );
            }
        }

        for ( DataElement dataElement : updates )
        {
            addDataElement( dataElement );
        }
    }

    /**
     * Returns the value type of the data elements in this group. Uses an arbitrary
     * member to determine the value type.
     */
    public ValueType getValueType()
    {
        return members != null && !members.isEmpty() ? members.iterator().next().getValueType() : null;
    }

    /**
     * Returns the aggregation type of the data elements in this group. Uses
     * an arbitrary member to determine the aggregation operator.
     */
    public AggregationType getAggregationType()
    {
        return members != null && !members.isEmpty() ? members.iterator().next().getAggregationType() : null;
    }

    /**
     * Returns the period type of the data elements in this group. Uses an
     * arbitrary member to determine the period type.
     */
    public PeriodType getPeriodType()
    {
        return members != null && !members.isEmpty() ? members.iterator().next().getPeriodType() : null;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.DATA_ELEMENT_GROUP;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty( "dataElements" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElement", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElement> getMembers()
    {
        return members;
    }

    public void setMembers( Set<DataElement> members )
    {
        this.members = members;
    }

    @JsonProperty( "dataElementGroupSet" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.REFERENCE, required = Property.Required.FALSE )
    public DataElementGroupSet getGroupSet()
    {
        return groupSet;
    }

    public void setGroupSet( DataElementGroupSet groupSet )
    {
        this.groupSet = groupSet;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementGroup dataElementGroup = (DataElementGroup) other;

            if ( mergeMode.isReplace() )
            {
                groupSet = dataElementGroup.getGroupSet();
            }
            else if ( mergeMode.isMerge() )
            {
                groupSet = dataElementGroup.getGroupSet() == null ? groupSet : dataElementGroup.getGroupSet();
            }

            removeAllDataElements();

            for ( DataElement dataElement : dataElementGroup.getMembers() )
            {
                addDataElement( dataElement );
            }
        }
    }
}
