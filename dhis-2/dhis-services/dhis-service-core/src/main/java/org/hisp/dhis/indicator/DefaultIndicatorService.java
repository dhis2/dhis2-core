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

import static org.hisp.dhis.i18n.I18nUtils.getCountByName;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsBetween;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsBetweenByName;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsByName;
import static org.hisp.dhis.i18n.I18nUtils.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.commons.filter.Filter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.i18n.I18nService;
import org.springframework.transaction.annotation.Transactional;

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

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
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
        return i18n( i18nService, indicatorStore.get( id ) );
    }

    @Override
    public Indicator getIndicator( String uid )
    {
        return i18n( i18nService, indicatorStore.getByUid( uid ) );
    }

    @Override
    public List<Indicator> getAllIndicators()
    {
        return i18n( i18nService, indicatorStore.getAll() );
    }

    @Override
    public List<Indicator> getIndicatorsByUid( Collection<String> uids )
    {
        return i18n( i18nService, indicatorStore.getByUid( uids ) );
    }

    @Override
    public List<Indicator> getIndicatorByName( String name )
    {
        return new ArrayList<>( i18n( i18nService, indicatorStore.getAllEqName( name ) ) );
    }

    @Override
    public List<Indicator> getIndicatorByShortName( String shortName )
    {
        return new ArrayList<>( i18n( i18nService, indicatorStore.getAllEqShortName( shortName ) ) );
    }

    @Override
    public Indicator getIndicatorByCode( String code )
    {
        return i18n( i18nService, indicatorStore.getByCode( code ) );
    }

    @Override
    public List<Indicator> getIndicatorsWithGroupSets()
    {
        return i18n( i18nService, indicatorStore.getIndicatorsWithGroupSets() );
    }

    @Override
    public List<Indicator> getIndicatorsWithoutGroups()
    {
        return i18n( i18nService, indicatorStore.getIndicatorsWithoutGroups() );
    }

    @Override
    public List<Indicator> getIndicatorsWithDataSets()
    {
        return i18n( i18nService, indicatorStore.getIndicatorsWithDataSets() );
    }

    @Override
    public List<Indicator> getIndicatorsLikeName( String name )
    {
        return getObjectsByName( i18nService, indicatorStore, name );
    }

    @Override
    public int getIndicatorCount()
    {
        return indicatorStore.getCount();
    }

    @Override
    public int getIndicatorCountByName( String name )
    {
        return getCountByName( i18nService, indicatorStore, name );
    }

    @Override
    public List<Indicator> getIndicatorsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, indicatorStore, first, max );
    }

    @Override
    public List<Indicator> getIndicatorsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, indicatorStore, name, first, max );
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
        return i18n( i18nService, indicatorTypeStore.get( id ) );
    }

    @Override
    public IndicatorType getIndicatorType( String uid )
    {
        return i18n( i18nService, indicatorTypeStore.getByUid( uid ) );
    }

    @Override
    public List<IndicatorType> getAllIndicatorTypes()
    {
        return i18n( i18nService, indicatorTypeStore.getAll() );
    }

    @Override
    public IndicatorType getIndicatorTypeByName( String name )
    {
        return i18n( i18nService, indicatorTypeStore.getByName( name ) );
    }

    @Override
    public int getIndicatorTypeCount()
    {
        return indicatorTypeStore.getCount();
    }

    @Override
    public int getIndicatorTypeCountByName( String name )
    {
        return getCountByName( i18nService, indicatorTypeStore, name );
    }

    @Override
    public List<IndicatorType> getIndicatorTypesBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, indicatorTypeStore, first, max );
    }

    @Override
    public List<IndicatorType> getIndicatorTypesBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, indicatorTypeStore, name, first, max );
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
        return i18n( i18nService, indicatorGroupStore.get( id ) );
    }

    @Override
    public IndicatorGroup getIndicatorGroup( int id, boolean i18nIndicators )
    {
        IndicatorGroup group = getIndicatorGroup( id );

        if ( i18nIndicators )
        {
            i18n( i18nService, group.getMembers() );
        }

        return group;
    }

    @Override
    public IndicatorGroup getIndicatorGroup( String uid )
    {
        return i18n( i18nService, indicatorGroupStore.getByUid( uid ) );
    }

    @Override
    public List<IndicatorGroup> getAllIndicatorGroups()
    {
        return i18n( i18nService, indicatorGroupStore.getAll() );
    }

    @Override
    public List<IndicatorGroup> getIndicatorGroupByName( String name )
    {
        return new ArrayList<>( i18n( i18nService, indicatorGroupStore.getAllEqName( name ) ) );
    }

    @Override
    public int getIndicatorGroupCount()
    {
        return indicatorGroupStore.getCount();
    }

    @Override
    public int getIndicatorGroupCountByName( String name )
    {
        return getCountByName( i18nService, indicatorGroupStore, name );
    }

    @Override
    public List<IndicatorGroup> getIndicatorGroupsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, indicatorGroupStore, first, max );
    }

    @Override
    public List<IndicatorGroup> getIndicatorGroupsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, indicatorGroupStore, name, first, max );
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
        return i18n( i18nService, indicatorGroupSetStore.get( id ) );
    }

    @Override
    public IndicatorGroupSet getIndicatorGroupSet( int id, boolean i18nGroups )
    {
        IndicatorGroupSet groupSet = getIndicatorGroupSet( id );

        if ( i18nGroups )
        {
            i18n( i18nService, groupSet.getMembers() );
        }

        return groupSet;
    }

    @Override
    public IndicatorGroupSet getIndicatorGroupSet( String uid )
    {
        return i18n( i18nService, indicatorGroupSetStore.getByUid( uid ) );
    }

    @Override
    public List<IndicatorGroupSet> getIndicatorGroupSetByName( String name )
    {
        return new ArrayList<>( i18n( i18nService, indicatorGroupSetStore.getAllEqName( name ) ) );
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
        return i18n( i18nService, indicatorGroupSetStore.getAll() );
    }

    @Override
    public int getIndicatorGroupSetCount()
    {
        return indicatorGroupSetStore.getCount();
    }

    @Override
    public int getIndicatorGroupSetCountByName( String name )
    {
        return getCountByName( i18nService, indicatorGroupSetStore, name );
    }

    @Override
    public List<IndicatorGroupSet> getIndicatorGroupSetsBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, indicatorGroupSetStore, first, max );
    }

    @Override
    public List<IndicatorGroupSet> getIndicatorGroupSetsBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, indicatorGroupSetStore, name, first, max );
    }
}
