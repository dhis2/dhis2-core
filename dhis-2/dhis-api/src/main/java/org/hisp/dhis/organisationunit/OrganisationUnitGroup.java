package org.hisp.dhis.organisationunit;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.Coordinate.CoordinateObject;
import org.hisp.dhis.common.Coordinate.CoordinateUtils;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "organisationUnitGroup", namespace = DxfNamespaces.DXF_2_0 )
public class OrganisationUnitGroup
    extends BaseDimensionalItemObject
    implements MetadataObject, CoordinateObject
{
    private String symbol;
    
    private String color;

    private Set<OrganisationUnit> members = new HashSet<>();

    private Set<OrganisationUnitGroupSet> groupSets = new HashSet<>();

    private FeatureType featureType = FeatureType.NONE;

    private String coordinates;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public OrganisationUnitGroup()
    {
    }

    public OrganisationUnitGroup( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        members.add( organisationUnit );
        return organisationUnit.getGroups().add( this );
    }

    public boolean removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        members.remove( organisationUnit );
        return organisationUnit.getGroups().remove( this );
    }

    public void removeAllOrganisationUnits()
    {
        for ( OrganisationUnit organisationUnit : members )
        {
            organisationUnit.getGroups().remove( this );
        }

        members.clear();
    }

    public void updateOrganisationUnits( Set<OrganisationUnit> updates )
    {
        for ( OrganisationUnit unit : new HashSet<>( members ) )
        {
            if ( !updates.contains( unit ) )
            {
                removeOrganisationUnit( unit );
            }
        }

        for ( OrganisationUnit unit : updates )
        {
            addOrganisationUnit( unit );
        }
    }

    public boolean hasSymbol()
    {
        return symbol != null && !symbol.trim().isEmpty();
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.ORGANISATION_UNIT_GROUP;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol( String symbol )
    {
        this.symbol = symbol;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getColor()
    {
        return color;
    }

    public void setColor( String color )
    {
        this.color = color;
    }

    @JsonProperty( "organisationUnits" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getMembers()
    {
        return members;
    }

    public void setMembers( Set<OrganisationUnit> members )
    {
        this.members = members;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "groupSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "groupSet", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnitGroupSet> getGroupSets()
    {
        return groupSets;
    }

    public void setGroupSets( Set<OrganisationUnitGroupSet> groupSets )
    {
        this.groupSets = groupSets;
    }

    public boolean hasDescendantsWithCoordinates()
    {
        return CoordinateUtils.hasDescendantsWithCoordinates( members );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FeatureType getFeatureType()
    {
        return featureType;
    }

    public void setFeatureType( FeatureType featureType )
    {
        this.featureType = featureType;
    }

    @Override
    public boolean hasFeatureType()
    {
        return getFeatureType() != null;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.GEOLOCATION )
    public String getCoordinates()
    {
        return coordinates;
    }

    public void setCoordinates( String coordinates )
    {
        this.coordinates = coordinates;
    }

    @Override
    public boolean hasCoordinates()
    {
        return getCoordinates() != null;
    }
}