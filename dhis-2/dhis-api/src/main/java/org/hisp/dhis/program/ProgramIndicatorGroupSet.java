package org.hisp.dhis.program;

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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */

public class ProgramIndicatorGroupSet
    extends BaseIdentifiableObject
{
    private String description;

    private Boolean compulsory = false;

    private List<ProgramIndicatorGroup> members = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramIndicatorGroupSet()
    {

    }

    public ProgramIndicatorGroupSet( String name )
    {
        this.name = name;
        this.compulsory = false;
    }

    public ProgramIndicatorGroupSet( String name, Boolean compulsory )
    {
        this( name );
        this.compulsory = compulsory;
    }

    public ProgramIndicatorGroupSet( String name, String description, Boolean compulsory )
    {
        this( name, compulsory );
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public Collection<ProgramIndicator> getProgramProgramIndicators()
    {
        List<ProgramIndicator> ProgramIndicators = new ArrayList<>();

        for ( ProgramIndicatorGroup group : members )
        {
            ProgramIndicators.addAll( group.getMembers() );
        }

        return ProgramIndicators;
    }

    public ProgramIndicatorGroup getGroup( ProgramIndicator ProgramIndicator )
    {
        for ( ProgramIndicatorGroup group : members )
        {
            if ( group.getMembers().contains( ProgramIndicator ) )
            {
                return group;
            }
        }

        return null;
    }

    public Boolean isMemberOfProgramIndicatorGroups( ProgramIndicator ProgramIndicator )
    {
        for ( ProgramIndicatorGroup group : members )
        {
            if ( group.getMembers().contains( ProgramIndicator ) )
            {
                return true;
            }
        }

        return false;
    }

    public Boolean hasProgramIndicatorGroups()
    {
        return members != null && members.size() > 0;
    }

    public List<ProgramIndicatorGroup> getSortedGroups()
    {
        List<ProgramIndicatorGroup> sortedGroups = new ArrayList<>( members );

        Collections.sort( sortedGroups );

        return sortedGroups;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void removeAllProgramIndicatorGroups()
    {
        members.clear();
    }

    public void addProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup )
    {
        members.add( ProgramIndicatorGroup );
        ProgramIndicatorGroup.setGroupSet( this );
    }

    public void removeProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup )
    {
        members.remove( ProgramIndicatorGroup );
        ProgramIndicatorGroup.setGroupSet( null );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

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

    @JsonProperty( "programIndicatorGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programIndicatorGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicatorGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramIndicatorGroup> getMembers()
    {
        return members;
    }

    public void setMembers( List<ProgramIndicatorGroup> members )
    {
        this.members = members;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ProgramIndicatorGroupSet ProgramIndicatorGroupSet = (ProgramIndicatorGroupSet) other;

            if ( mergeMode.isReplace() )
            {
                compulsory = ProgramIndicatorGroupSet.isCompulsory();
                description = ProgramIndicatorGroupSet.getDescription();
            }
            else if ( mergeMode.isMerge() )
            {
                compulsory = ProgramIndicatorGroupSet.isCompulsory() == null ? compulsory : ProgramIndicatorGroupSet.isCompulsory();
                description = ProgramIndicatorGroupSet.getDescription() == null ? description : ProgramIndicatorGroupSet.getDescription();
            }

            removeAllProgramIndicatorGroups();

            for ( ProgramIndicatorGroup ProgramIndicatorGroup : ProgramIndicatorGroupSet.getMembers() )
            {
                addProgramIndicatorGroup( ProgramIndicatorGroup );
            }
        }
    }
}

