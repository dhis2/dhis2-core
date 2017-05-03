package org.hisp.dhis.organisationunit;

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

import org.hisp.dhis.commons.filter.Filter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Torgeir Lorange Ostby
 */
@Transactional
public class DefaultOrganisationUnitGroupService
    implements OrganisationUnitGroupService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitGroupStore organisationUnitGroupStore;

    public void setOrganisationUnitGroupStore( OrganisationUnitGroupStore organisationUnitGroupStore )
    {
        this.organisationUnitGroupStore = organisationUnitGroupStore;
    }

    private OrganisationUnitGroupSetStore organisationUnitGroupSetStore;

    public void setOrganisationUnitGroupSetStore( OrganisationUnitGroupSetStore organisationUnitGroupSetStore )
    {
        this.organisationUnitGroupSetStore = organisationUnitGroupSetStore;
    }

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // OrganisationUnitGroup
    // -------------------------------------------------------------------------

    @Override
    public int addOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        organisationUnitGroupStore.save( organisationUnitGroup );

        return organisationUnitGroup.getId();
    }

    @Override
    public void updateOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        organisationUnitGroupStore.update( organisationUnitGroup );
    }

    @Override
    public void deleteOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        organisationUnitGroupStore.delete( organisationUnitGroup );
    }

    @Override
    public OrganisationUnitGroup getOrganisationUnitGroup( int id )
    {
        return organisationUnitGroupStore.get( id );
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsByUid( Collection<String> uids )
    {
        return organisationUnitGroupStore.getByUid( uids );
    }

    @Override
    public OrganisationUnitGroup getOrganisationUnitGroup( String uid )
    {
        return organisationUnitGroupStore.getByUid( uid );
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupByName( String name )
    {
        return new ArrayList<>( organisationUnitGroupStore.getAllEqName( name ) );
    }

    @Override
    public OrganisationUnitGroup getOrganisationUnitGroupByCode( String code )
    {
        return organisationUnitGroupStore.getByCode( code );
    }

    @Override
    public OrganisationUnitGroup getOrganisationUnitGroupByShortName( String shortName )
    {
        List<OrganisationUnitGroup> organisationUnitGroups = new ArrayList<>(
            organisationUnitGroupStore.getAllEqShortName( shortName ) );

        if ( organisationUnitGroups.isEmpty() )
        {
            return null;
        }

        return organisationUnitGroups.get( 0 );
    }

    @Override
    public List<OrganisationUnitGroup> getAllOrganisationUnitGroups()
    {
        return organisationUnitGroupStore.getAll();
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsWithGroupSets()
    {
        return organisationUnitGroupStore.getOrganisationUnitGroupsWithGroupSets();
    }

    @Override
    public int getOrganisationUnitGroupCount()
    {
        return organisationUnitGroupStore.getCount();
    }

    @Override
    public int getOrganisationUnitGroupCountByName( String name )
    {
        return organisationUnitGroupStore.getCountLikeName( name );
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsBetween( int first, int max )
    {
        return organisationUnitGroupStore.getAllOrderedName( first, max );
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsBetweenByName( String name, int first, int max )
    {
        return organisationUnitGroupStore.getAllLikeName( name, first, max );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int addOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        organisationUnitGroupSetStore.save( organisationUnitGroupSet );

        return organisationUnitGroupSet.getId();
    }

    @Override
    public void updateOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        organisationUnitGroupSetStore.update( organisationUnitGroupSet );
    }

    @Override
    public void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        organisationUnitGroupSetStore.delete( organisationUnitGroupSet );
    }

    @Override
    public OrganisationUnitGroupSet getOrganisationUnitGroupSet( int id )
    {
        return organisationUnitGroupSetStore.get( id );
    }

    @Override
    public OrganisationUnitGroupSet getOrganisationUnitGroupSet( String uid )
    {
        return organisationUnitGroupSetStore.getByUid( uid );
    }

    @Override
    public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetsByUid( Collection<String> uids )
    {
        return  organisationUnitGroupSetStore.getByUid( uids );
    }

    @Override
    public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetByName( String name )
    {
        return new ArrayList<>( organisationUnitGroupSetStore.getAllEqName( name ) );
    }

    @Override
    public List<OrganisationUnitGroupSet> getAllOrganisationUnitGroupSets()
    {
        return organisationUnitGroupSetStore.getAll();
    }

    @Override
    public List<OrganisationUnitGroupSet> getCompulsoryOrganisationUnitGroupSets()
    {
        List<OrganisationUnitGroupSet> groupSets = new ArrayList<>();

        for ( OrganisationUnitGroupSet groupSet : getAllOrganisationUnitGroupSets() )
        {
            if ( groupSet.isCompulsory() )
            {
                groupSets.add( groupSet );
            }
        }

        return groupSets;
    }

    @Override
    public List<OrganisationUnitGroupSet> getCompulsoryOrganisationUnitGroupSetsWithMembers()
    {
        return FilterUtils.filter( getAllOrganisationUnitGroupSets(), new Filter<OrganisationUnitGroupSet>()
        {
            @Override
            public boolean retain( OrganisationUnitGroupSet object )
            {
                return object.isCompulsory() && object.hasOrganisationUnitGroups();
            }
        } );
    }

    public OrganisationUnitGroup getOrganisationUnitGroup( OrganisationUnitGroupSet groupSet, OrganisationUnit unit )
    {
        for ( OrganisationUnitGroup group : groupSet.getOrganisationUnitGroups() )
        {
            if ( group.getMembers().contains( unit ) )
            {
                return group;
            }
        }

        return null;
    }

    @Override
    public List<OrganisationUnitGroupSet> getCompulsoryOrganisationUnitGroupSetsNotAssignedTo(
        OrganisationUnit organisationUnit )
    {
        List<OrganisationUnitGroupSet> groupSets = new ArrayList<>();

        for ( OrganisationUnitGroupSet groupSet : getCompulsoryOrganisationUnitGroupSets() )
        {
            if ( !groupSet.isMemberOfOrganisationUnitGroups( organisationUnit ) && groupSet.hasOrganisationUnitGroups() )
            {
                groupSets.add( groupSet );
            }
        }

        return groupSets;
    }

    @Override
    public int getOrganisationUnitGroupSetCount()
    {
        return organisationUnitGroupSetStore.getCount();
    }

    @Override
    public int getOrganisationUnitGroupSetCountByName( String name )
    {
        return organisationUnitGroupSetStore.getCountLikeName( name );
    }

    @Override
    public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetsBetween( int first, int max )
    {
        return organisationUnitGroupSetStore.getAllOrderedName( first, max );
    }

    @Override
    public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetsBetweenByName( String name, int first,
        int max )
    {
        return organisationUnitGroupSetStore.getAllLikeName( name, first, max ) ;
    }

    @Override
    public void mergeWithCurrentUserOrganisationUnits( OrganisationUnitGroup organisationUnitGroup, Collection<OrganisationUnit> mergeOrganisationUnits )
    {
        Set<OrganisationUnit> organisationUnits = new HashSet<>( organisationUnitGroup.getMembers() );

        Set<OrganisationUnit> userOrganisationUnits = new HashSet<>();

        for ( OrganisationUnit organisationUnit : currentUserService.getCurrentUser().getOrganisationUnits() )
        {
            userOrganisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( organisationUnit.getUid() ) );
        }

        organisationUnits.removeAll( userOrganisationUnits );
        organisationUnits.addAll( mergeOrganisationUnits );

        organisationUnitGroup.updateOrganisationUnits( organisationUnits );

        updateOrganisationUnitGroup( organisationUnitGroup );
    }
}
