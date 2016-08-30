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

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.MoreObjects;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitQueryParams
{    
    /**
     * Query string to match like name and exactly on UID and code.
     */
    private String query;
    
    /**
     * The parent organisation units for which to include all children, inclusive.
     */
    private Set<OrganisationUnit> parents = new HashSet<>();
    
    /**
     * The groups for which members to include, inclusive.
     */
    private Set<OrganisationUnitGroup> groups = new HashSet<>();
    
    /**
     * The levels of organisation units to include.
     */
    private Set<Integer> levels = new HashSet<>();
    
    /**
     * The maximum number of organisation unit levels to include, relative to the
     * real root of the hierarchy.
     */
    private Integer maxLevels;
    
    /**
     * The first result to include.
     */
    private Integer first;
    
    /**
     * The max number of results to include.
     */
    private Integer max;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public OrganisationUnitQueryParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasQuery()
    {
        return query != null && !query.isEmpty();
    }
    
    public boolean hasParents()
    {
        return parents != null && !parents.isEmpty();
    }
        
    public boolean hasGroups()
    {
        return groups != null && !groups.isEmpty();
    }
    
    public boolean hasLevels()
    {
        return levels != null && !levels.isEmpty();
    }
        
    public void setLevel( Integer level )
    {
        if ( level != null )
        {
            levels.add( level );
        }
    }
        
    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this ).
            add( "query", query ).
            add( "parents", parents ).
            add( "groups", groups ).
            add( "levels", levels ).
            add( "maxLevels", maxLevels ).
            add( "first", first ).
            add( "max", max ).toString();
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getQuery()
    {
        return query;
    }

    public void setQuery( String query )
    {
        this.query = query;
    }

    public Set<OrganisationUnit> getParents()
    {
        return parents;
    }

    public void setParents( Set<OrganisationUnit> parents )
    {
        this.parents = parents;
    }

    public Set<OrganisationUnitGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<OrganisationUnitGroup> groups )
    {
        this.groups = groups;
    }

    public Set<Integer> getLevels()
    {
        return levels;
    }

    public void setLevels( Set<Integer> levels )
    {
        this.levels = levels;
    }

    public Integer getMaxLevels()
    {
        return maxLevels;
    }

    public void setMaxLevels( Integer maxLevels )
    {
        this.maxLevels = maxLevels;
    }

    public Integer getFirst()
    {
        return first;
    }

    public void setFirst( Integer first )
    {
        this.first = first;
    }

    public Integer getMax()
    {
        return max;
    }

    public void setMax( Integer max )
    {
        this.max = max;
    }
}
