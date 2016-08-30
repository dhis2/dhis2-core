package org.hisp.dhis.organisationunit;

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
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.adapter.JacksonOrganisationUnitGroupSymbolSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "organisationUnitGroupSet", namespace = DxfNamespaces.DXF_2_0 )
public class OrganisationUnitGroupSet
    extends BaseDimensionalObject
{
    private boolean compulsory;

    private Set<OrganisationUnitGroup> organisationUnitGroups = new HashSet<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public OrganisationUnitGroupSet()
    {
    }

    public OrganisationUnitGroupSet( String name, String description, boolean compulsory )
    {
        this.name = name;
        this.description = description;
        this.compulsory = compulsory;
    }

    public OrganisationUnitGroupSet( String name, String description, boolean compulsory, boolean dataDimension )
    {
        this( name, description, compulsory );
        this.dataDimension = dataDimension;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        organisationUnitGroups.add( organisationUnitGroup );
        organisationUnitGroup.setGroupSet( this );
    }

    public void removeOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        organisationUnitGroups.remove( organisationUnitGroup );
        organisationUnitGroup.setGroupSet( null );
    }

    public void removeAllOrganisationUnitGroups()
    {
        for ( OrganisationUnitGroup organisationUnitGroup : organisationUnitGroups )
        {
            organisationUnitGroup.setGroupSet( null );
        }

        organisationUnitGroups.clear();
    }

    public Collection<OrganisationUnit> getOrganisationUnits()
    {
        List<OrganisationUnit> units = new ArrayList<>();

        for ( OrganisationUnitGroup group : organisationUnitGroups )
        {
            units.addAll( group.getMembers() );
        }

        return units;
    }

    public boolean isMemberOfOrganisationUnitGroups( OrganisationUnit organisationUnit )
    {
        for ( OrganisationUnitGroup group : organisationUnitGroups )
        {
            if ( group.getMembers().contains( organisationUnit ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasOrganisationUnitGroups()
    {
        return organisationUnitGroups != null && organisationUnitGroups.size() > 0;
    }

    public OrganisationUnitGroup getGroup( OrganisationUnit unit )
    {
        for ( OrganisationUnitGroup group : organisationUnitGroups )
        {
            if ( group.getMembers().contains( unit ) )
            {
                return group;
            }
        }

        return null;
    }

    public List<OrganisationUnitGroup> getSortedGroups()
    {
        List<OrganisationUnitGroup> sortedGroups = new ArrayList<>( organisationUnitGroups );

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
        return new ArrayList<>( organisationUnitGroups );
    }

    @Override
    public DimensionType getDimensionType()
    {
        return DimensionType.ORGANISATION_UNIT_GROUP_SET;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public boolean isAutoGenerated()
    {
        return name != null && (name.equals( "Type" ) || name.equals( "Ownership" ));
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCompulsory()
    {
        return compulsory;
    }

    public void setCompulsory( boolean compulsory )
    {
        this.compulsory = compulsory;
    }

    @JsonProperty( "organisationUnitGroups" )
    // @JsonSerialize(contentAs = BaseIdentifiableObject.class)
    @JsonSerialize( contentUsing = JacksonOrganisationUnitGroupSymbolSerializer.class )
    @JacksonXmlElementWrapper( localName = "organisationUnitGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnitGroup", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnitGroup> getOrganisationUnitGroups()
    {
        return organisationUnitGroups;
    }

    public void setOrganisationUnitGroups( Set<OrganisationUnitGroup> organisationUnitGroups )
    {
        this.organisationUnitGroups = organisationUnitGroups;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            OrganisationUnitGroupSet organisationUnitGroupSet = (OrganisationUnitGroupSet) other;

            compulsory = organisationUnitGroupSet.isCompulsory();

            if ( mergeMode.isReplace() )
            {
                description = organisationUnitGroupSet.getDescription();
            }
            else if ( mergeMode.isMerge() )
            {
                description = organisationUnitGroupSet.getDescription() == null ? description : organisationUnitGroupSet.getDescription();
            }

            removeAllOrganisationUnitGroups();

            for ( OrganisationUnitGroup organisationUnitGroup : organisationUnitGroupSet.getOrganisationUnitGroups() )
            {
                addOrganisationUnitGroup( organisationUnitGroup );
            }
        }
    }
}
