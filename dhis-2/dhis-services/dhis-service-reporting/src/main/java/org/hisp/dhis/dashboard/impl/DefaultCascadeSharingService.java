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

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sharing.AbstractCascadeSharingService;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
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
                handleMapObject( dashboard, dashboardItem.getMap(), parameters );
                break;
            case VISUALIZATION:
                handleAnalyticalObject( dashboard, dashboardItem.getVisualization(), parameters );
                break;
            case EVENT_REPORT:
                handleAnalyticalObject( dashboard, dashboardItem.getEventReport(), parameters );
                break;
            case EVENT_CHART:
                handleAnalyticalObject( dashboard, dashboardItem.getEventChart(), parameters );
                break;
            default:
                break;
            }
        } );

        return parameters.getReport();
    }

    @Override
    @Transactional
    public CascadeSharingReport cascadeSharing( BaseAnalyticalObject sourceObject,
        CascadeSharingParameters parameters )
    {
        List<IdentifiableObject> listUpdateObjects = new ArrayList<>();

        handleIdentifiableObjects( sourceObject, sourceObject.getDataElements(), listUpdateObjects, parameters );

        handleIdentifiableObjects( sourceObject, sourceObject.getIndicators(), listUpdateObjects, parameters );

        handleCategoryDimension( sourceObject, listUpdateObjects, parameters );

        handleDataElementDimensions( sourceObject, listUpdateObjects, parameters );

        handleDataElementGroupSetDimensions( sourceObject, listUpdateObjects, parameters );

        if ( canUpdate( parameters ) )
        {
            manager.update( listUpdateObjects );
        }

        return parameters.getReport();
    }

    private void handleMapObject( Dashboard dashboard, Map map, CascadeSharingParameters parameters )
    {
        if ( mergeSharing( dashboard, map, parameters ) )
        {
            manager.update( map );
        }
    }

    private <T extends BaseAnalyticalObject> void handleAnalyticalObject( Dashboard dashboard, T analyticalObject,
        CascadeSharingParameters parameters )
    {
        if ( analyticalObject == null )
        {
            return;
        }

        if ( !mergeSharing( dashboard, analyticalObject, parameters ) )
        {
            return;
        }

        if ( canUpdate( parameters ) )
        {
            manager.update( analyticalObject );
        }

        cascadeSharing( analyticalObject, parameters );

        parameters.getReport().increaseCountDashboardItem();
    }

    private void handleDataElementDimensions( BaseAnalyticalObject sourceObject,
        List<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<TrackedEntityDataElementDimension> deDimensions = sourceObject
            .getDataElementDimensions();

        if ( CollectionUtils.isEmpty( deDimensions ) )
        {
            return;
        }

        deDimensions.forEach( deDimension -> {
            DataElement dataElement = deDimension.getDataElement();

            if ( dataElement != null && mergeSharing( sourceObject, dataElement, parameters ) )
            {
                listUpdateObjects.add( dataElement );
            }

            LegendSet legendSet = deDimension.getLegendSet();

            if ( legendSet != null && mergeSharing( sourceObject, legendSet, parameters ) )
            {
                listUpdateObjects.add( legendSet );
            }

            ProgramStage programStage = deDimension.getProgramStage();

            if ( programStage != null && mergeSharing( sourceObject, programStage, parameters ) )
            {
                listUpdateObjects.add( programStage );
            }

        } );
    }

    private void handleCategoryDimension( BaseAnalyticalObject sourceObject, List<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<CategoryDimension> catDimensions = sourceObject.getCategoryDimensions();

        if ( CollectionUtils.isEmpty( catDimensions ) )
        {
            return;
        }

        catDimensions.forEach( catDimension -> {
            Category category = catDimension.getDimension();

            if ( category != null && mergeSharing( sourceObject, category, parameters ) )
            {
                listUpdateObjects.add( category );
            }

            List<CategoryOption> catOptions = catDimension.getItems();

            if ( CollectionUtils.isEmpty( catOptions ) )
            {
                return;
            }

            catOptions.forEach( catOption -> {
                if ( mergeSharing( sourceObject, catOption, parameters ) )
                {
                    listUpdateObjects.add( catOption );
                }
            } );

        } );
    }

    private void handleDataElementGroupSetDimensions( BaseAnalyticalObject sourceObject,
        List<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<DataElementGroupSetDimension> deGroupSetDimensions = sourceObject
            .getDataElementGroupSetDimensions();

        if ( CollectionUtils.isEmpty( deGroupSetDimensions ) )
        {
            return;
        }

        deGroupSetDimensions.forEach( deGroupSetDimension -> {
            DataElementGroupSet deGroupSet = deGroupSetDimension.getDimension();

            if ( deGroupSet != null && mergeSharing( sourceObject, deGroupSet, parameters ) )
            {
                listUpdateObjects.add( deGroupSet );
            }

            List<DataElementGroup> deGroups = deGroupSetDimension.getItems();

            if ( CollectionUtils.isEmpty( deGroups ) )
            {
                return;
            }

            deGroups.forEach( deGroup -> {
                if ( mergeSharing( sourceObject, deGroup, parameters ) )
                {
                    listUpdateObjects.add( deGroup );
                }
            } );
        } );
    }

    private void handleIdentifiableObjects( final BaseAnalyticalObject sourceObject,
        final List<? extends IdentifiableObject> targetObjects, List<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( targetObjects ) )
        {
            return;
        }

        targetObjects.forEach( object -> {
            if ( mergeSharing( sourceObject, object, parameters ) )
            {
                listUpdateObjects.add( object );
            }
        } );
    }
}
