package org.hisp.dhis.indicator;

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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Lars Helge Overland
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
        indicatorStore.save( indicator );

        return indicator.getId();
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

    // -------------------------------------------------------------------------
    // IndicatorType
    // -------------------------------------------------------------------------

    @Override
    public int addIndicatorType( IndicatorType indicatorType )
    {
        indicatorTypeStore.save( indicatorType );

        return indicatorType.getId();
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

    // -------------------------------------------------------------------------
    // IndicatorGroup
    // -------------------------------------------------------------------------

    @Override
    public int addIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        indicatorGroupStore.save( indicatorGroup );

        return indicatorGroup.getId();
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
    public IndicatorGroup getIndicatorGroup( String uid )
    {
        return indicatorGroupStore.getByUid( uid );
    }

    @Override
    public List<IndicatorGroup> getAllIndicatorGroups()
    {
        return indicatorGroupStore.getAll();
    }

    // -------------------------------------------------------------------------
    // IndicatorGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int addIndicatorGroupSet( IndicatorGroupSet groupSet )
    {
        indicatorGroupSetStore.save( groupSet );

        return groupSet.getId();
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
    public IndicatorGroupSet getIndicatorGroupSet( String uid )
    {
        return indicatorGroupSetStore.getByUid( uid );
    }

    @Override
    public List<IndicatorGroupSet> getAllIndicatorGroupSets()
    {
        return indicatorGroupSetStore.getAll();
    }
}
