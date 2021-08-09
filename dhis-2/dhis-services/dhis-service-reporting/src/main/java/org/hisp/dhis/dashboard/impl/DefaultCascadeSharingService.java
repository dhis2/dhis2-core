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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryOptionGroupSetDimension;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.sharing.AbstractCascadeSharingService;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.visualization.Visualization;
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
                handleVisualization( dashboard, dashboardItem.getVisualization(), parameters );
                break;
            case EVENT_REPORT:
                handleEventReport( dashboard, dashboardItem.getEventReport(), parameters );
                break;
            case EVENT_CHART:
                handleEventChart( dashboard, dashboardItem.getEventChart(), parameters );
                break;
            default:
                break;
            }
        } );

        return parameters.getReport();
    }

    private void handleMapObject( Dashboard dashboard, Map map, CascadeSharingParameters parameters )
    {
        if ( mergeSharing( dashboard, map, parameters ) )
        {
            manager.update( map );
        }
    }

    private void handleVisualization( Dashboard dashboard, Visualization visualization,
        CascadeSharingParameters parameters )
    {
        Set<IdentifiableObject> listUpdateObjects = new HashSet<>();

        if ( !handleIdentifiableObject( dashboard, visualization, listUpdateObjects, parameters ) )
        {
            return;
        }

        if ( canUpdate( parameters ) )
        {
            manager.update( visualization );
        }

        handleBaseAnalyticObject( visualization, listUpdateObjects, parameters );

        if ( canUpdate( parameters ) )
        {
            manager.update( new ArrayList<>( listUpdateObjects ) );
        }
    }

    private void handleEventReport( Dashboard dashboard, EventReport eventReport, CascadeSharingParameters parameters )
    {
        Set<IdentifiableObject> listUpdateObjects = new HashSet<>();

        if ( !handleIdentifiableObject( dashboard, eventReport, listUpdateObjects, parameters ) )
        {
            return;
        }

        if ( canUpdate( parameters ) )
        {
            manager.update( eventReport );
        }

        handleBaseAnalyticObject( eventReport, listUpdateObjects, parameters );

        if ( canUpdate( parameters ) )
        {
            manager.update( new ArrayList<>( listUpdateObjects ) );
        }
    }

    private void handleEventChart( Dashboard dashboard, EventChart eventChart, CascadeSharingParameters parameters )
    {
        Set<IdentifiableObject> listUpdateObjects = new HashSet<>();

        if ( !handleIdentifiableObject( dashboard, eventChart, listUpdateObjects, parameters ) )
        {
            return;
        }

        if ( canUpdate( parameters ) )
        {
            manager.update( eventChart );
        }

        handleIdentifiableObject( dashboard, eventChart.getAttributeValueDimension(), listUpdateObjects, parameters );

        handleIdentifiableObject( dashboard, eventChart.getDataElementValueDimension(), listUpdateObjects, parameters );

        handleBaseAnalyticObject( eventChart, listUpdateObjects, parameters );

        if ( canUpdate( parameters ) )
        {
            manager.update( new ArrayList<>( listUpdateObjects ) );
        }
    }

    private void handleBaseAnalyticObject( BaseAnalyticalObject sourceObject, Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        handleIdentifiableObjects( sourceObject, sourceObject.getDataElements(), listUpdateObjects, parameters );

        handleIdentifiableObjects( sourceObject, sourceObject.getIndicators(), listUpdateObjects, parameters );

        handleCategoryDimension( sourceObject, listUpdateObjects, parameters );

        handleDataElementDimensions( sourceObject, listUpdateObjects, parameters );

        handleDataElementGroupSetDimensions( sourceObject, listUpdateObjects, parameters );

        handleCategoryOptionGroupSetDimensions( sourceObject, listUpdateObjects, parameters );

        handleTrackedEntityAttributeDimension( sourceObject, listUpdateObjects, parameters );
    }

    private void handleTrackedEntityAttributeDimension( BaseAnalyticalObject sourceObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<TrackedEntityAttributeDimension> attributeDimensions = sourceObject
            .getAttributeDimensions();

        if ( CollectionUtils.isEmpty( attributeDimensions ) )
        {
            return;
        }

        attributeDimensions.forEach( attributeDimension -> {
            handleIdentifiableObject( sourceObject, attributeDimension.getAttribute(), listUpdateObjects, parameters );
            handleIdentifiableObject( sourceObject, attributeDimension.getLegendSet(), listUpdateObjects, parameters );
        } );
    }

    private void handleCategoryOptionGroupSetDimensions( BaseAnalyticalObject sourceObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<CategoryOptionGroupSetDimension> catOptionGroupSetDimensions = sourceObject
            .getCategoryOptionGroupSetDimensions();

        if ( CollectionUtils.isEmpty( catOptionGroupSetDimensions ) )
        {
            return;
        }

        catOptionGroupSetDimensions.forEach( categoryOptionGroupSetDimension -> {
            CategoryOptionGroupSet catOptionGroupSet = categoryOptionGroupSetDimension
                .getDimension();

            handleIdentifiableObject( sourceObject, catOptionGroupSet, listUpdateObjects, parameters );

            List<CategoryOptionGroup> catOptionGroups = catOptionGroupSet.getMembers();

            if ( CollectionUtils.isEmpty( catOptionGroups ) )
            {
                return;
            }

            catOptionGroups.forEach( catOptionGroup -> {
                handleIdentifiableObject( sourceObject, catOptionGroup, listUpdateObjects, parameters );
                handleIdentifiableObjects( sourceObject, catOptionGroup.getMembers(), listUpdateObjects, parameters );
            } );

        } );
    }

    private void handleDataElementDimensions( BaseAnalyticalObject sourceObject,
        Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<TrackedEntityDataElementDimension> deDimensions = sourceObject
            .getDataElementDimensions();

        if ( CollectionUtils.isEmpty( deDimensions ) )
        {
            return;
        }

        deDimensions.forEach( deDimension -> {
            handleIdentifiableObject( sourceObject, deDimension.getDataElement(), listUpdateObjects, parameters );
            handleIdentifiableObject( sourceObject, deDimension.getLegendSet(), listUpdateObjects, parameters );
            handleIdentifiableObject( sourceObject, deDimension.getProgramStage(), listUpdateObjects, parameters );
        } );
    }

    private void handleCategoryDimension( BaseAnalyticalObject sourceObject, Set<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        List<CategoryDimension> catDimensions = sourceObject.getCategoryDimensions();

        if ( CollectionUtils.isEmpty( catDimensions ) )
        {
            return;
        }

        catDimensions.forEach( catDimension -> {
            Category category = catDimension.getDimension();

            handleIdentifiableObject( sourceObject, category, listUpdateObjects, parameters );

            List<CategoryOption> catOptions = catDimension.getItems();

            if ( CollectionUtils.isEmpty( catOptions ) )
            {
                return;
            }

            catOptions.forEach( catOption -> handleIdentifiableObject( sourceObject, catOption,
                listUpdateObjects, parameters ) );
        } );
    }

    private void handleDataElementGroupSetDimensions( BaseAnalyticalObject sourceObject,
        Set<IdentifiableObject> listUpdateObjects,
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

            handleIdentifiableObject( sourceObject, deGroupSet, listUpdateObjects, parameters );

            List<DataElementGroup> deGroups = deGroupSetDimension.getItems();

            if ( CollectionUtils.isEmpty( deGroups ) )
            {
                return;
            }

            deGroups
                .forEach( deGroup -> handleIdentifiableObject( sourceObject, deGroup, listUpdateObjects, parameters ) );
        } );
    }

    private void handleIdentifiableObjects( final BaseAnalyticalObject sourceObject,
        final Collection<? extends IdentifiableObject> targetObjects, Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( targetObjects ) )
        {
            return;
        }

        targetObjects.forEach( object -> {
            handleIdentifiableObject( sourceObject, object, listUpdateObjects, parameters );
        } );
    }

    private boolean handleIdentifiableObject( final BaseIdentifiableObject sourceObject,
        IdentifiableObject target, Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( target == null )
        {
            return false;
        }

        if ( mergeSharing( sourceObject, target, parameters ) )
        {
            listUpdateObjects.add( target );
            return true;
        }

        return false;
    }
}
