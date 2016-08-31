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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class which encapsulates logic for the organisation unit hierarchy.
 * 
 * The key format for the organisation unit group variant is
 * "<parent org unit id>:<group id>".
 * 
 * @author Lars Helge Overland
 */
public class OrganisationUnitHierarchy
{
    /**
     * Contains mappings between parent and immediate children.
     */
    private Map<Integer, Set<Integer>> relationships = new HashMap<>();

    private Map<Integer, Set<Integer>> subTrees = new HashMap<>();
    
    // Key is on format "parent id:group id"
    
    private Map<String, Set<Integer>> groupSubTrees = new HashMap<>();
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public OrganisationUnitHierarchy( Map<Integer, Set<Integer>> relationships )
    {
        this.relationships = relationships;
    }
    
    public OrganisationUnitHierarchy( Collection<OrganisationUnitRelationship> relations )
    {
        for ( OrganisationUnitRelationship relation : relations )
        {
            if ( relation.getParentId() == relation.getChildId() )
            {
                continue; // Parent cannot be same as child
            }
            
            Set<Integer> children = relationships.get( relation.getParentId() );
            
            if ( children == null )
            {
                children = new HashSet<>();
                relationships.put( relation.getParentId(), children );
            }
            
            children.add( relation.getChildId() );
        }
    }

    // -------------------------------------------------------------------------
    // Prepare
    // -------------------------------------------------------------------------

    public OrganisationUnitHierarchy prepareChildren( OrganisationUnit parent )
    {
        subTrees.put( parent.getId(), getChildren( parent.getId() ) );
        
        return this;
    }

    public OrganisationUnitHierarchy prepareChildren( Collection<OrganisationUnit> parents )
    {
        for ( OrganisationUnit unit : parents )
        {
            prepareChildren( unit );
        }
        
        return this;
    }

    // -------------------------------------------------------------------------
    // Prepare for group
    // -------------------------------------------------------------------------

    public OrganisationUnitHierarchy prepareChildren( OrganisationUnit parent, OrganisationUnitGroup group )
    {
        if ( group == null )
        {
            return prepareChildren( parent );
        }
        
        groupSubTrees.put( getKey( parent.getId(), group ), getChildren( parent.getId(), group ) );
        
        return this;
    }

    public OrganisationUnitHierarchy prepareChildren( Collection<OrganisationUnit> parents, Collection<OrganisationUnitGroup> groups )
    {
        if ( groups == null )
        {
            return prepareChildren( parents );
        }
        
        for ( OrganisationUnit unit : parents )
        {
            for ( OrganisationUnitGroup group : groups )
            {
                prepareChildren( unit, group );
            }
        }
        
        return this;
    }
    
    // -------------------------------------------------------------------------
    // Get children
    // -------------------------------------------------------------------------

    public Set<Integer> getChildren( int parentId )
    {
        Set<Integer> preparedChildren = subTrees.get( parentId );
        
        if ( preparedChildren != null )
        {
            return new HashSet<>( preparedChildren );
        }
        
        List<Integer> children = new ArrayList<>();
        
        children.add( 0, parentId ); // Adds parent id to beginning of list

        int childCounter = 1;
        
        for ( int i = 0; i < childCounter; i++ )
        {
            Set<Integer> currentChildren = relationships.get( children.get( i ) );
            
            if ( currentChildren != null )
            {
                children.addAll( currentChildren );
            
                childCounter += currentChildren.size();
            }
        }
        
        return new HashSet<>( children );
    }

    public Set<Integer> getChildren( Collection<Integer> parentIds )
    {
        int capacity = parentIds.size() + 5;
        
        Set<Integer> children = new HashSet<>( Math.max( capacity, 16 ) );

        for ( Integer id : parentIds )
        {
            children.addAll( getChildren( id ) );
        }
        
        return children;
    }

    // -------------------------------------------------------------------------
    // Get children for group
    // -------------------------------------------------------------------------

    public Set<Integer> getChildren( int parentId, OrganisationUnitGroup group )
    {
        if ( group == null )
        {
            return getChildren( parentId );
        }
        
        Set<Integer> children = groupSubTrees.get( getKey( parentId, group ) );
        
        if ( children != null )
        {
            return new HashSet<>( children );
        }
        
        children = getChildren( parentId );
        
        Set<Integer> groupMembers = new HashSet<>();
        
        for ( OrganisationUnit unit : group.getMembers() )
        {
            groupMembers.add( unit.getId() );
        }
        
        children.retainAll( groupMembers );
        
        return children;
    }
    
    public Set<Integer> getChildren( Collection<Integer> parentIds, Collection<OrganisationUnitGroup> groups )
    {
        if ( groups == null )
        {
            return getChildren( parentIds );
        }
        
        int capacity = ( parentIds.size() * groups.size() ) + 5;
        
        Set<Integer> children = new HashSet<>( Math.max( capacity, 16 ) );
        
        for ( Integer id : parentIds )
        {
            for ( OrganisationUnitGroup group : groups )
            {
                children.addAll( getChildren( id, group ) );
            }
        }
        
        return children;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getKey( int parentId, OrganisationUnitGroup group )
    {
        return parentId + ":" + group.getId();
    }
}

