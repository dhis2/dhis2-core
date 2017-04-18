package org.hisp.dhis.legend;

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
public class DefaultLegendSetService
    implements LegendSetService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<LegendSet> legendSetStore;

    public void setLegendSetStore( GenericIdentifiableObjectStore<LegendSet> legendSetStore )
    {
        this.legendSetStore = legendSetStore;
    }

    // -------------------------------------------------------------------------
    // LegendSet
    // -------------------------------------------------------------------------

    @Override
    public int addLegendSet( LegendSet legend )
    {
        legendSetStore.save( legend );

        return legend.getId();
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
