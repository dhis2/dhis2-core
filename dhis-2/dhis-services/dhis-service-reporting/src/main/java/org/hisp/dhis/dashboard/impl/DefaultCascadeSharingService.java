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
package org.hisp.dhis.dashboard.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.sharing.AbstractCascadeSharingService;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCascadeSharingService
    extends AbstractCascadeSharingService implements CascadeSharingService
{
    private final IdentifiableObjectManager manager;

    public DefaultCascadeSharingService( @NonNull IdentifiableObjectManager manager )
    {
        this.manager = manager;
    }

    @Override
    @Transactional
    public CascadeSharingReport cascadeSharing( Dashboard dashboard, CascadeSharingParameters parameters )
    {
        dashboard.getItems().forEach( dashboardItem -> {
            switch ( dashboardItem.getType() )
            {
            case MAP:
                mergeSharing( dashboard, dashboardItem.getMap(), parameters );
                break;
            case VISUALIZATION:
                handleAnalyticalObject( dashboard, dashboardItem, dashboardItem.getVisualization(), parameters );
                break;
            case EVENT_REPORT:
                handleAnalyticalObject( dashboard, dashboardItem, dashboardItem.getEventReport(), parameters );
                break;
            case EVENT_CHART:
                handleAnalyticalObject( dashboard, dashboardItem, dashboardItem.getEventChart(), parameters );
                break;
            default:
                break;
            }
        } );

        if ( canUpdate( parameters ) )
        {
            manager.update( dashboard );
        }

        return parameters.getReport();
    }

    @Override
    @Transactional
    public CascadeSharingReport cascadeSharing( BaseAnalyticalObject baseAnalyticalObject,
        CascadeSharingParameters parameters )
    {
        CachingMap<String, BaseIdentifiableObject> mapObjects = new CachingMap<>();

        List<IdentifiableObject> listUpdateObjects = new ArrayList<>();

        baseAnalyticalObject.getColumns().forEach( column -> column.getItems().forEach( item -> {
            BaseIdentifiableObject dimensionObject = mapObjects.get( item.getDimensionItem(),
                () -> manager.get( item.getDimensionItem() ) );

            dimensionObject = mergeSharing( baseAnalyticalObject, dimensionObject, parameters );

            listUpdateObjects.add( dimensionObject );
        } ) );

        baseAnalyticalObject.getRows().forEach( row -> row.getItems().forEach( item -> {
            BaseIdentifiableObject dimensionObject = mapObjects.get( item.getDimensionItem(),
                () -> manager.get( item.getDimensionItem() ) );

            dimensionObject = mergeSharing( baseAnalyticalObject, dimensionObject, parameters );

            listUpdateObjects.add( dimensionObject );
        } ) );

        if ( canUpdate( parameters ) )
        {
            manager.update( listUpdateObjects );
        }

        return parameters.getReport();
    }

    private void handleAnalyticalObject( Dashboard dashboard, DashboardItem dashboardItem,
        BaseAnalyticalObject analyticalObject,
        CascadeSharingParameters parameters )
    {
        if ( analyticalObject == null )
        {
            return;
        }

        BaseAnalyticalObject mergedObject = mergeSharing( dashboard, analyticalObject, parameters );

        if ( canUpdate( parameters ) )
        {
            manager.update( mergedObject );
        }

        cascadeSharing( analyticalObject, parameters );

        parameters.getReport().increaseCountDashboardItem();
    }
}
