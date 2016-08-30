package org.hisp.dhis.indicator;

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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "indicatorGroup", namespace = DxfNamespaces.DXF_2_0 )
public class IndicatorGroup
    extends BaseIdentifiableObject
{
    private Set<Indicator> members = new HashSet<>();

    private IndicatorGroupSet groupSet;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public IndicatorGroup()
    {
    }

    public IndicatorGroup( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addIndicator( Indicator indicator )
    {
        members.add( indicator );
        indicator.getGroups().add( this );
    }

    public void removeIndicator( Indicator indicator )
    {
        members.remove( indicator );
        indicator.getGroups().remove( this );
    }

    public void updateIndicators( Set<Indicator> updates )
    {
        for ( Indicator indicator : new HashSet<>( members ) )
        {
            if ( !updates.contains( indicator ) )
            {
                removeIndicator( indicator );
            }
        }

        for ( Indicator indicator : updates )
        {
            addIndicator( indicator );
        }
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void removeAllIndicators()
    {
        members.clear();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------


    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @JsonProperty( "indicators" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "indicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicator", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Indicator> getMembers()
    {
        return members;
    }

    public void setMembers( Set<Indicator> members )
    {
        this.members = members;
    }

    @JsonProperty( "indicatorGroupSet" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "indicatorGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.REFERENCE, required = Property.Required.FALSE )
    public IndicatorGroupSet getGroupSet()
    {
        return groupSet;
    }

    public void setGroupSet( IndicatorGroupSet groupSet )
    {
        this.groupSet = groupSet;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            IndicatorGroup indicatorGroup = (IndicatorGroup) other;

            if ( mergeMode.isReplace() )
            {
                groupSet = indicatorGroup.getGroupSet();
            }
            else if ( mergeMode.isMerge() )
            {
                groupSet = indicatorGroup.getGroupSet() == null ? groupSet : indicatorGroup.getGroupSet();
            }

            removeAllIndicators();

            for ( Indicator indicator : indicatorGroup.getMembers() )
            {
                addIndicator( indicator );
            }
        }
    }
}
