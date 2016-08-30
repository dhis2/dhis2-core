package org.hisp.dhis.legend;

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

import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultLegendService
    implements LegendService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<Legend> legendStore;

    public void setLegendStore( GenericIdentifiableObjectStore<Legend> legendStore )
    {
        this.legendStore = legendStore;
    }

    private GenericIdentifiableObjectStore<LegendSet> legendSetStore;

    public void setLegendSetStore( GenericIdentifiableObjectStore<LegendSet> legendSetStore )
    {
        this.legendSetStore = legendSetStore;
    }

    // -------------------------------------------------------------------------
    // Legend
    // -------------------------------------------------------------------------

    @Override
    public int addLegend( Legend legend )
    {
        return legendStore.save( legend );
    }

    @Override
    public void updateLegend( Legend legend )
    {
        legendStore.update( legend );        
    }

    @Override
    public Legend getLegend( int id )
    {
        return legendStore.get( id );
    }

    @Override
    public Legend getLegend( String uid )
    {
        return legendStore.getByUid( uid );
    }
    
    @Override
    public void deleteLegend( Legend legend )
    {
        legendStore.delete( legend );
    }

    @Override
    public List<Legend> getAllLegends()
    {
        return legendStore.getAll();
    }

    // -------------------------------------------------------------------------
    // LegendSet
    // -------------------------------------------------------------------------

    @Override
    public int addLegendSet( LegendSet legend )
    {
        return legendSetStore.save( legend );
    }

    @Override
    public void updateLegendSet( LegendSet legend )
    {
        legendSetStore.update( legend );
    }

    @Override
    public LegendSet getLegendSet( int id )
    {
        return legendSetStore.get( id );
    }

    @Override
    public LegendSet getLegendSet( String uid )
    {
        return legendSetStore.getByUid( uid );
    }
    
    @Override
    public void deleteLegendSet( LegendSet legendSet )
    {
        legendSetStore.delete( legendSet );
    }

    @Override
    public List<LegendSet> getAllLegendSets()
    {
        return legendSetStore.getAll();
    }
}
