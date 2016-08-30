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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.commons.filter.Filter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Transactional
public class DefaultIndicatorService
    implements IndicatorService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private IndicatorStore indicatorStore;

    public void setIndicatorStore( IndicatorStore indicatorStore )
    {
        this.indicatorStore = indicatorStore;
    }

    private GenericIdentifiableObjectStore<IndicatorType> indicatorTypeStore;

    public void setIndicatorTypeStore( GenericIdentifiableObjectStore<IndicatorType> indicatorTypeStore )
    {
        this.indicatorTypeStore = indicatorTypeStore;
    }

    private GenericIdentifiableObjectStore<IndicatorGroup> indicatorGroupStore;

    public void setIndicatorGroupStore( GenericIdentifiableObjectStore<IndicatorGroup> indicatorGroupStore )
    {
        this.indicatorGroupStore = indicatorGroupStore;
    }

    private GenericIdentifiableObjectStore<IndicatorGroupSet> indicatorGroupSetStore;

    public void setIndicatorGroupSetStore( GenericIdentifiableObjectStore<IndicatorGroupSet> indicatorGroupSetStore )
    {
        this.indicatorGroupSetStore = indicatorGroupSetStore;
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    @Override
    public int addIndicator( Indicator indicator )
    {
        return indicatorStore.save( indicator );
    }

    @Override
    public void updateIndicator( Indicator indicator )
    {
        indicatorStore.update( indicator );
    }

    @Override
    public void deleteIndicator( Indicator indicator )
    {
        indicatorStore.delete( indicator );
    }

    @Override
    public Indicator getIndicator( int id )
    {
        return indicatorStore.get( id );
    }

    @Override
    public Indicator getIndicator( String uid )
    {
        return indicatorStore.getByUid( uid );
    }

    @Override
    public List<Indicator> getAllIndicators()
    {
        return indicatorStore.getAll();
    }

    @Override
    public List<Indicator> getIndicatorsByUid( Collection<String> uids )
    {
        return indicatorStore.getByUid( uids );
    }

    @Override
    public List<Indicator> getIndicatorByName( String name )
    {
        return new ArrayList<>( indicatorStore.getAllEqName( name ) );
    }

    @Override
    public List<Indicator> getIndicatorByShortName( String shortName )
    {
        return new ArrayList<>( indicatorStore.getAllEqShortName( shortName ) );
    }

    @Override
    public Indicator getIndicatorByCode( String code )
    {
        return indicatorStore.getByCode( code );
    }

    @Override
    public List<Indicator> getIndicatorsWithGroupSets()
    {
        return indicatorStore.getIndicatorsWithGroupSets();
    }

    @Override
    public List<Indicator> getIndicatorsWithoutGroups()
    {
        return indicatorStore.getIndicatorsWithoutGroups();
    }

    @Override
    public List<Indicator> getIndicatorsWithDataSets()
    {
        return indicatorStore.getIndicatorsWithDataSets();
    }

    @Override
    public List<Indicator> getIndicatorsLikeName( String name )
    {
        return indicatorStore.getAllLikeName( name ) ;
    }

    @Override
    public int getIndicatorCount()
    {
        return indicatorStore.getCount();
    }

    @Override
    public int getIndicatorCountByName( String name )
    {
        return  indicatorStore.getCountLikeName( name );
    }

    @Override
    public List<Indicator> getIndicatorsBetween( int first, int max )
    {
        return indicatorStore.getAllOrderedName( first, max ) ;
    }

    @Override
    public List<Indicator> getIndicatorsBetweenByName( String name, int first, int max )
    {
        return indicatorStore.getAllLikeName( name, first, max ) ;
    }

    // -------------------------------------------------------------------------
    // IndicatorType
    // -------------------------------------------------------------------------

    @Override
    public int addIndicatorType( IndicatorType indicatorType )
    {
        return indicatorTypeStore.save( indicatorType );
    }

    @Override
    public void updateIndicatorType( IndicatorType indicatorType )
    {
        indicatorTypeStore.update( indicatorType );
    }

    @Override
    public void deleteIndicatorType( IndicatorType indicatorType )
    {
        indicatorTypeStore.delete( indicatorType );
    }

    @Override
    public IndicatorType getIndicatorType( int id )
    {
        return indicatorTypeStore.get( id );
    }

    @Override
    public IndicatorType getIndicatorType( String uid )
    {
        return indicatorTypeStore.getByUid( uid );
    }

    @Override
    public List<IndicatorType> getAllIndicatorTypes()
    {
        return indicatorTypeStore.getAll();
    }

    @Override
    public IndicatorType getIndicatorTypeByName( String name )
    {
        return indicatorTypeStore.getByName( name );
    }

    @Override
    public int getIndicatorTypeCount()
    {
        return indicatorTypeStore.getCount();
    }

    @Override
    public int getIndicatorTypeCountByName( String name )
    {
        return indicatorTypeStore.getCountLikeName( name ) ;
    }

    @Override
    public List<IndicatorType> getIndicatorTypesBetween( int first, int max )
    {
        return indicatorTypeStore.getAllOrderedName( first, max );
    }

    @Override
    public List<IndicatorType> getIndicatorTypesBetweenByName( String name, int first, int max )
    {
        return indicatorTypeStore.getAllLikeName( name, first, max ) ;
    }

    // -------------------------------------------------------------------------
    // IndicatorGroup
    // -------------------------------------------------------------------------

    @Override
    public int addIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        return indicatorGroupStore.save( indicatorGroup );
    }

    @Override
    public void updateIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        indicatorGroupStore.update( indicatorGroup );
    }

    @Override
    public void deleteIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        indicatorGroupStore.delete( indicatorGroup );
    }

    @Override
    public IndicatorGroup getIndicatorGroup( int id )
    {
        return indicatorGroupStore.get( id );
    }

    @Override
    public IndicatorGroup getIndicatorGroup( int id, boolean i18nIndicators )
    {
        IndicatorGroup group = getIndicatorGroup( id );

        if ( i18nIndicators )
        {
            group.getMembers();
        }

        return group;
    }

    @Override
    public IndicatorGroup getIndicatorGroup( String uid )
    {
        return indicatorGroupStore.getByUid( uid );
    }

    @Override
    public List<IndicatorGroup> getAllIndicatorGroups()
    {
        return indicatorGroupStore.getAll();
    }

    @Override
    public List<IndicatorGroup> getIndicatorGroupByName( String name )
    {
        return new ArrayList<>( indicatorGroupStore.getAllEqName( name ) );
    }

    @Override
    public int getIndicatorGroupCount()
    {
        return indicatorGroupStore.getCount();
    }

    @Override
    public int getIndicatorGroupCountByName( String name )
    {
        return indicatorGroupStore.getCountLikeName( name );
    }

    @Override
    public List<IndicatorGroup> getIndicatorGroupsBetween( int first, int max )
    {
        return  indicatorGroupStore.getAllOrderedName( first, max );
    }

    @Override
    public List<IndicatorGroup> getIndicatorGroupsBetweenByName( String name, int first, int max )
    {
        return indicatorGroupStore.getAllLikeName( name, first, max );
    }

    // -------------------------------------------------------------------------
    // IndicatorGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int addIndicatorGroupSet( IndicatorGroupSet groupSet )
    {
        return indicatorGroupSetStore.save( groupSet );
    }

    @Override
    public void updateIndicatorGroupSet( IndicatorGroupSet groupSet )
    {
        indicatorGroupSetStore.update( groupSet );
    }

    @Override
    public void deleteIndicatorGroupSet( IndicatorGroupSet groupSet )
    {
        indicatorGroupSetStore.delete( groupSet );
    }

    @Override
    public IndicatorGroupSet getIndicatorGroupSet( int id )
    {
        return indicatorGroupSetStore.get( id );
    }

    @Override
    public IndicatorGroupSet getIndicatorGroupSet( int id, boolean i18nGroups )
    {
        IndicatorGroupSet groupSet = getIndicatorGroupSet( id );

        if ( i18nGroups )
        {
            groupSet.getMembers();
        }

        return groupSet;
    }

    @Override
    public IndicatorGroupSet getIndicatorGroupSet( String uid )
    {
        return indicatorGroupSetStore.getByUid( uid );
    }

    @Override
    public List<IndicatorGroupSet> getIndicatorGroupSetByName( String name )
    {
        return new ArrayList<>( indicatorGroupSetStore.getAllEqName( name ) );
    }

    @Override
    public List<IndicatorGroupSet> getCompulsoryIndicatorGroupSetsWithMembers()
    {
        return FilterUtils.filter( getAllIndicatorGroupSets(), new Filter<IndicatorGroupSet>()
        {
            @Override
            public boolean retain( IndicatorGroupSet object )
            {
                return object.isCompulsory() && object.hasIndicatorGroups();
            }
        } );
    }

    @Override
    public List<IndicatorGroupSet> getAllIndicatorGroupSets()
    {
        return indicatorGroupSetStore.getAll();
    }

    @Override
    public int getIndicatorGroupSetCount()
    {
        return indicatorGroupSetStore.getCount();
    }

    @Override
    public int getIndicatorGroupSetCountByName( String name )
    {
        return  indicatorGroupSetStore.getCountLikeName( name );
    }

    @Override
    public List<IndicatorGroupSet> getIndicatorGroupSetsBetween( int first, int max )
    {
        return indicatorGroupSetStore.getAllOrderedName( first, max );
    }

    @Override
    public List<IndicatorGroupSet> getIndicatorGroupSetsBetweenByName( String name, int first, int max )
    {
        return indicatorGroupSetStore.getAllLikeName( name, first, max );
    }
}
