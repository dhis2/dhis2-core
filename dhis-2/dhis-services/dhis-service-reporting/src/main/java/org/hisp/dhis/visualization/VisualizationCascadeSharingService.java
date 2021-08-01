/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.visualization;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.sharing.AbstractCascadeSharingService;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisualizationCascadeSharingService
    extends AbstractCascadeSharingService
    implements CascadeSharingService<Visualization>
{
    private final IdentifiableObjectManager manager;

    public VisualizationCascadeSharingService( @NonNull IdentifiableObjectManager manager )
    {
        this.manager = manager;
    }

    @Override
    @Transactional
    public CascadeSharingReport cascadeSharing( Visualization visualization, CascadeSharingParameters parameters )
    {
        CachingMap<String, BaseIdentifiableObject> mapObjects = new CachingMap<>();

        List<IdentifiableObject> listUpdateObjects = new ArrayList<>();

        visualization.getGridColumns().forEach( columns -> columns.forEach( item -> {
            BaseIdentifiableObject dimensionObject = mapObjects.get( item.getDimensionItem(),
                () -> manager.get( item.getDimensionItem() ) );

            dimensionObject = mergeSharing( visualization, dimensionObject, parameters );

            listUpdateObjects.add( dimensionObject );
        } ) );

        visualization.getGridRows().forEach( columns -> columns.forEach( item -> {
            BaseIdentifiableObject dimensionObject = mapObjects.get( item.getDimensionItem(),
                () -> manager.get( item.getDimensionItem() ) );

            dimensionObject = mergeSharing( visualization, dimensionObject, parameters );

            listUpdateObjects.add( dimensionObject );
        } ) );

        if ( canUpdate( parameters ) )
        {
            manager.update( listUpdateObjects );
        }

        return parameters.getReport();
    }
}
