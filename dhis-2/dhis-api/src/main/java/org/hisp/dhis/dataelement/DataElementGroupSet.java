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
import com.google.common.collect.Lists;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * DataElementGroupSet is a set of DataElementGroups. It is by default
 * exclusive, in the sense that a DataElement can only be a member of one or
 * zero of the DataElementGroups in a DataElementGroupSet.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "dataElementGroupSet", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementGroupSet
    extends BaseDimensionalObject
{
    private Boolean compulsory = false;

    private List<DataElementGroup> members = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementGroupSet()
    {

    }

    public DataElementGroupSet( String name )
    {
        this.name = name;
        this.compulsory = false;
    }

    public DataElementGroupSet( String name, Boolean compulsory )
    {
        this( name );
        this.compulsory = compulsory;
    }

    public DataElementGroupSet( String name, String description, Boolean compulsory )
    {
        this( name, compulsory );
        this.description = description;
    }

    public DataElementGroupSet( String name, String description, boolean compulsory, boolean dataDimension )
    {
        this( name, description, compulsory );
        this.dataDimension = dataDimension;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElementGroup( DataElementGroup dataElementGroup )
    {
        members.add( dataElementGroup );
        dataElementGroup.setGroupSet( this );
    }

    public void removeDataElementGroup( DataElementGroup dataElementGroup )
    {
        members.remove( dataElementGroup );
        dataElementGroup.setGroupSet( null );
    }

    public void removeAllDataElementGroups()
    {
        for ( DataElementGroup dataElementGroup : members )
        {
            if ( dataElementGroup.getGroupSet() != null && dataElementGroup.getGroupSet().equals( this ) )
            {
                dataElementGroup.setGroupSet( null );
            }
        }

        members.clear();
    }

    public Collection<DataElement> getDataElements()
    {
        List<DataElement> dataElements = new ArrayList<>();

        for ( DataElementGroup group : members )
        {
            dataElements.addAll( group.getMembers() );
        }

        return dataElements;
    }

    public DataElementGroup getGroup( DataElement dataElement )
    {
        for ( DataElementGroup group : members )
        {
            if ( group.getMembers().contains( dataElement ) )
            {
                return group;
            }
        }

        return null;
    }

    public Boolean isMemberOfDataElementGroups( DataElement dataElement )
    {
        for ( DataElementGroup group : members )
        {
            if ( group.getMembers().contains( dataElement ) )
            {
                return true;
            }
        }

        return false;
    }

    public Boolean hasDataElementGroups()
    {
        return members != null && members.size() > 0;
    }

    public List<DataElementGroup> getSortedGroups()
    {
        List<DataElementGroup> sortedGroups = new ArrayList<>( members );

        Collections.sort( sortedGroups );

        return sortedGroups;
    }

    @Override
    public String getShortName()
    {
        if ( getName() == null || getName().length() <= 50 )
        {
            return getName();
        }
        else
        {
            return getName().substring( 0, 49 );
        }
    }

    // -------------------------------------------------------------------------
    // Dimensional object
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonSerialize( contentAs = BaseDimensionalItemObject.class )
    @JacksonXmlElementWrapper( localName = "items", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "item", namespace = DxfNamespaces.DXF_2_0 )
    public List<DimensionalItemObject> getItems()
    {
        return Lists.newArrayList( members );
    }

    @Override
    public DimensionType getDimensionType()
    {
        return DimensionType.DATA_ELEMENT_GROUP_SET;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isCompulsory()
    {
        if ( compulsory == null )
        {
            return false;
        }

        return compulsory;
    }

    public void setCompulsory( Boolean compulsory )
    {
        this.compulsory = compulsory;
    }

    @JsonProperty( "dataElementGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataElementGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementGroup> getMembers()
    {
        return members;
    }

    public void setMembers( List<DataElementGroup> members )
    {
        this.members = members;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mode )
    {
        super.mergeWith( other, mode );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementGroupSet dataElementGroupSet = (DataElementGroupSet) other;

            if ( mode.isReplace() )
            {
                description = dataElementGroupSet.getDescription();
                compulsory = dataElementGroupSet.isCompulsory();
            }
            else if ( mode.isMerge() )
            {
                description = dataElementGroupSet.getDescription() == null ? description : dataElementGroupSet.getDescription();
                compulsory = dataElementGroupSet.isCompulsory() == null ? compulsory : dataElementGroupSet.isCompulsory();
            }

            removeAllDataElementGroups();

            for ( DataElementGroup dataElementGroup : dataElementGroupSet.getMembers() )
            {
                addDataElementGroup( dataElementGroup );
            }
        }
    }
}
