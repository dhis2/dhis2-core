/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryOptionGroupSetDimension;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.sharing.AccessObject;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultCascadeSharingService
    implements CascadeSharingService
{
    private final IdentifiableObjectManager manager;

    private final SchemaService schemaService;

    private final AclService aclService;

    @Override
    @Transactional
    public CascadeSharingReport cascadeSharing( Dashboard dashboard, CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( dashboard.getItems() ) )
        {
            return parameters.getReport();
        }

        Set<IdentifiableObject> canMergeObjects = new HashSet<>();

        dashboard.getItems().forEach( dashboardItem -> {

            Set<IdentifiableObject> itemCanMergeObjects = new HashSet<>();

            switch ( dashboardItem.getType() )
            {
            case MAP:
                handleMapObject( dashboard.getSharing(), dashboardItem.getMap(), itemCanMergeObjects, parameters );
                break;
            case VISUALIZATION:
                handleVisualization( dashboard.getSharing(), dashboardItem.getVisualization(), itemCanMergeObjects,
                    parameters );
                break;
            case EVENT_REPORT:
                handleEventReport( dashboard.getSharing(), dashboardItem.getEventReport(), itemCanMergeObjects,
                    parameters );
                break;
            case EVENT_CHART:
                handleEventChart( dashboard.getSharing(), dashboardItem.getEventChart(), itemCanMergeObjects,
                    parameters );
                break;
            case EVENT_VISUALIZATION:
                handleEventVisualization( dashboard.getSharing(), dashboardItem.getEventVisualization(),
                    itemCanMergeObjects,
                    parameters );
                break;
            default:
                break;
            }

            if ( !CollectionUtils.isEmpty( itemCanMergeObjects ) )
            {
                canMergeObjects.addAll( itemCanMergeObjects );

                if ( !parameters.isAtomic() || !parameters.getReport().hasErrors() )
                {
                    parameters.getReport().incUpdatedDashboardItem();
                }
            }
        } );

        if ( parameters.isAtomic() && parameters.getReport().hasErrors() )
        {
            return parameters.getReport();
        }

        if ( parameters.isDryRun() )
        {
            canMergeObjects
                .forEach( object -> parameters.getReport().addUpdatedObject( getTypeReportKey( object ), object ) );
            return parameters.getReport();
        }

        // All checks done, proceed to update sharing for all objects.
        canMergeObjects.forEach( object -> mergeSharing( dashboard.getSharing(), object, parameters ) );

        return parameters.getReport();
    }

    /**
     * Check if can merge sharing from given dashboard to given Map
     *
     * @param map {@link org.hisp.dhis.mapping.Map}
     * @param canMergeObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleMapObject( final Sharing sourceSharing, org.hisp.dhis.mapping.Map map,
        Set<IdentifiableObject> canMergeObjects, CascadeSharingParameters parameters )
    {
        if ( canUserUpdate( map, parameters ) && shouldUpdateSharing( sourceSharing, map ) )
        {
            canMergeObjects.add( map );
        }
    }

    /**
     * Check if can merge sharing from given dashboard to given visualization
     *
     * @param sourceSharing {@link Sharing}
     * @param visualization {@link Visualization}
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleVisualization( final Sharing sourceSharing, Visualization visualization,
        Set<IdentifiableObject> canMergeObjects, CascadeSharingParameters parameters )
    {
        if ( visualization == null )
        {
            return;
        }

        if ( canUserUpdate( visualization, parameters ) && shouldUpdateSharing( sourceSharing, visualization ) )
        {
            canMergeObjects.add( visualization );
        }

        handleBaseAnalyticObject( sourceSharing, visualization, canMergeObjects, parameters );
    }

    /**
     * Check if can merge sharing from given dashboard to given eventReport
     *
     * @param sourceSharing {@link Sharing}
     * @param eventReport {@link EventReport}
     * @param updateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleEventReport( final Sharing sourceSharing, EventReport eventReport,
        Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( eventReport == null )
        {
            return;
        }

        if ( handleIdentifiableObject( sourceSharing, EventReport.class, eventReport, updateObjects, parameters ) )
        {
            updateObjects.add( eventReport );
        }

        handleIdentifiableObject( sourceSharing, TrackedEntityAttribute.class, eventReport.getAttributeValueDimension(),
            updateObjects,
            parameters );

        handleIdentifiableObject( sourceSharing, DataElement.class, eventReport.getDataElementValueDimension(),
            updateObjects,
            parameters );

        handleBaseAnalyticObject( sourceSharing, eventReport, updateObjects, parameters );
    }

    /**
     * Check if can merge sharing from given dashboard to given eventChart
     *
     * @param sourceSharing {@link Sharing}
     * @param eventChart {@link EventChart}
     * @param updateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleEventChart( final Sharing sourceSharing, EventChart eventChart,
        Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( eventChart == null )
        {
            return;
        }

        if ( handleIdentifiableObject( sourceSharing, EventChart.class, eventChart, updateObjects, parameters ) )
        {
            updateObjects.add( eventChart );
        }

        handleIdentifiableObject( sourceSharing, TrackedEntityAttribute.class, eventChart.getAttributeValueDimension(),
            updateObjects,
            parameters );

        handleIdentifiableObject( sourceSharing, DataElement.class, eventChart.getDataElementValueDimension(),
            updateObjects,
            parameters );

        handleBaseAnalyticObject( sourceSharing, eventChart, updateObjects, parameters );
    }

    /**
     * Handles the sharing cascade for the given Dashboard and
     * EventVisualization
     *
     * @param sourceSharing {@link Sharing}
     * @param eventVisualization {@link EventVisualization}
     * @param updateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleEventVisualization( final Sharing sourceSharing, EventVisualization eventVisualization,
        Set<IdentifiableObject> updateObjects, CascadeSharingParameters parameters )
    {
        if ( eventVisualization == null )
        {
            return;
        }

        if ( handleIdentifiableObject( sourceSharing, EventVisualization.class, eventVisualization, updateObjects,
            parameters ) )
        {
            updateObjects.add( eventVisualization );
        }

        handleIdentifiableObject( sourceSharing, TrackedEntityAttribute.class,
            eventVisualization.getAttributeValueDimension(), updateObjects, parameters );

        handleIdentifiableObject( sourceSharing, DataElement.class, eventVisualization.getDataElementValueDimension(),
            updateObjects, parameters );

        handleBaseAnalyticObject( sourceSharing, eventVisualization, updateObjects, parameters );
    }

    /**
     * Check if can merge sharing from given source to all given
     * analyticalObject's dimensional objects
     *
     * @param sourceSharing {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleBaseAnalyticObject( final Sharing sourceSharing, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        handleIdentifiableObjects( sourceSharing, DataElement.class, analyticalObject.getDataElements(),
            listUpdateObjects, parameters );

        handleIdentifiableObjects( sourceSharing, Indicator.class, analyticalObject.getIndicators(),
            listUpdateObjects, parameters );

        handleCategoryDimension( sourceSharing, analyticalObject, listUpdateObjects, parameters );

        handleDataElementDimensions( sourceSharing, analyticalObject, listUpdateObjects, parameters );

        handleDataElementGroupSetDimensions( sourceSharing, analyticalObject, listUpdateObjects, parameters );

        handleCategoryOptionGroupSetDimensions( sourceSharing, analyticalObject, listUpdateObjects, parameters );

        handleTrackedEntityAttributeDimension( sourceSharing, analyticalObject, listUpdateObjects, parameters );
    }

    /**
     * Check if can merge sharing from given source to given analyticalObject's
     * {@link TrackedEntityAttributeDimension} and all related objects that has
     * Sharing enabled.
     *
     * @param sourceSharing {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleTrackedEntityAttributeDimension( final Sharing sourceSharing,
        BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<TrackedEntityAttributeDimension> attributeDimensions = analyticalObject
            .getAttributeDimensions();

        if ( CollectionUtils.isEmpty( attributeDimensions ) )
        {
            return;
        }

        attributeDimensions.forEach( attributeDimension -> {
            handleIdentifiableObject( sourceSharing, TrackedEntityAttribute.class, attributeDimension.getAttribute(),
                listUpdateObjects, parameters );
            handleIdentifiableObject( sourceSharing, LegendSet.class, attributeDimension.getLegendSet(),
                listUpdateObjects,
                parameters );
        } );
    }

    /**
     * Check if can merge sharing from given source to given analyticalObject's
     * {@link CategoryOptionGroupSetDimension} and all related objects that has
     * Sharing enabled.
     *
     * @param sourceSharing {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleCategoryOptionGroupSetDimensions( final Sharing sourceSharing,
        BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<CategoryOptionGroupSetDimension> catOptionGroupSetDimensions = analyticalObject
            .getCategoryOptionGroupSetDimensions();

        if ( CollectionUtils.isEmpty( catOptionGroupSetDimensions ) )
        {
            return;
        }

        catOptionGroupSetDimensions.forEach( categoryOptionGroupSetDimension -> {
            CategoryOptionGroupSet catOptionGroupSet = categoryOptionGroupSetDimension
                .getDimension();

            handleIdentifiableObject( sourceSharing, CategoryOptionGroupSet.class, catOptionGroupSet, listUpdateObjects,
                parameters );

            List<CategoryOptionGroup> catOptionGroups = catOptionGroupSet.getMembers();

            if ( CollectionUtils.isEmpty( catOptionGroups ) )
            {
                return;
            }

            catOptionGroups.forEach( catOptionGroup -> {
                handleIdentifiableObject( sourceSharing, CategoryOptionGroup.class, catOptionGroup, listUpdateObjects,
                    parameters );
                handleIdentifiableObjects( sourceSharing, CategoryOption.class, catOptionGroup.getMembers(),
                    listUpdateObjects,
                    parameters );
            } );

        } );
    }

    /**
     * Check if can merge sharing from given source to given analyticalObject's
     * {@link TrackedEntityDataElementDimension} and all related objects that
     * has Sharing enabled.
     *
     * @param sourceSharing {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleDataElementDimensions( final Sharing sourceSharing, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<TrackedEntityDataElementDimension> deDimensions = analyticalObject
            .getDataElementDimensions();

        if ( CollectionUtils.isEmpty( deDimensions ) )
        {
            return;
        }

        deDimensions.forEach( deDimension -> {
            handleIdentifiableObject( sourceSharing, DataElement.class, deDimension.getDataElement(), listUpdateObjects,
                parameters );
            handleIdentifiableObject( sourceSharing, LegendSet.class, deDimension.getLegendSet(), listUpdateObjects,
                parameters );
            handleIdentifiableObject( sourceSharing, ProgramStage.class, deDimension.getProgramStage(),
                listUpdateObjects,
                parameters );
        } );
    }

    /**
     * Check if can merge sharing from given source to given analyticalObject's
     * {@link CategoryDimension} and all related objects that has Sharing
     * enabled.
     *
     * @param sourceSharing {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleCategoryDimension( final Sharing sourceSharing, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<CategoryDimension> catDimensions = analyticalObject.getCategoryDimensions();

        if ( CollectionUtils.isEmpty( catDimensions ) )
        {
            return;
        }

        catDimensions.forEach( catDimension -> {
            Category category = catDimension.getDimension();

            handleIdentifiableObject( sourceSharing, Category.class, category, listUpdateObjects, parameters );

            List<CategoryOption> catOptions = catDimension.getItems();

            if ( CollectionUtils.isEmpty( catOptions ) )
            {
                return;
            }

            catOptions.forEach( catOption -> handleIdentifiableObject( sourceSharing, CategoryOption.class, catOption,
                listUpdateObjects, parameters ) );
        } );
    }

    /**
     * Check if can merge sharing from given source to given analyticalObject's
     * {@link DataElementGroupSetDimension} and all related objects that has
     * Sharing enabled.
     *
     * @param sourceSharing {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleDataElementGroupSetDimensions( final Sharing sourceSharing,
        BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<DataElementGroupSetDimension> deGroupSetDimensions = analyticalObject
            .getDataElementGroupSetDimensions();

        if ( CollectionUtils.isEmpty( deGroupSetDimensions ) )
        {
            return;
        }

        deGroupSetDimensions.forEach( deGroupSetDimension -> {
            DataElementGroupSet deGroupSet = deGroupSetDimension.getDimension();

            handleIdentifiableObject( sourceSharing, DataElementGroupSet.class, deGroupSet, listUpdateObjects,
                parameters );

            List<DataElementGroup> deGroups = deGroupSetDimension.getItems();

            if ( CollectionUtils.isEmpty( deGroups ) )
            {
                return;
            }

            deGroups
                .forEach( deGroup -> handleIdentifiableObject( sourceSharing, DataElementGroup.class, deGroup,
                    listUpdateObjects,
                    parameters ) );
        } );
    }

    /**
     * Check if each of target objects sharing can be merged from sourceSharing.
     * <p>
     * If yes then target object will be added to listUpdateObjects
     *
     * @param sourceSharing {@link Sharing}
     * @param targetObjects list of {@link IdentifiableObject}
     * @param listUpdateObjects Set of objects need to be updated.
     * @param parameters {@link CascadeSharingParameters}
     */
    private <T extends IdentifiableObject> void handleIdentifiableObjects( final Sharing sourceSharing, Class<T> type,
        Collection<T> targetObjects, Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( targetObjects ) )
        {
            return;
        }

        targetObjects.forEach(
            object -> handleIdentifiableObject( sourceSharing, type, object, listUpdateObjects, parameters ) );
    }

    /**
     * Check if target object's sharing can be merged from sourceSharing
     *
     * @param sourceSharing {@link Sharing}
     * @param target {@link IdentifiableObject}
     * @param listUpdateObjects Set of objects need to be updated.
     * @return TRUE if target object should be updated, otherwise return FALSE.
     */
    private <T extends IdentifiableObject> boolean handleIdentifiableObject( Sharing sourceSharing, Class<T> type,
        T target, Collection<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        if ( target == null )
        {
            return false;
        }

        T persistedTarget = manager.get( type, target.getUid() );

        if ( persistedTarget == null )
        {
            parameters.getReport().getErrorReports()
                .add( new ErrorReport( type, ErrorCode.E5001, target.getUid(), type.getSimpleName() ) );
            return false;
        }

        if ( canUserUpdate( persistedTarget, parameters ) && shouldUpdateSharing( sourceSharing, target ) )
        {
            listUpdateObjects.add( persistedTarget );
            return true;
        }

        return false;
    }

    /**
     * Check if given target's sharing need to be updated.
     *
     * @param source {@link Sharing}
     * @param target object to check
     * @return TRUE if object need update sharing, otherwise FALSE.
     */
    private <T extends IdentifiableObject> boolean shouldUpdateSharing( final Sharing source, final T target )
    {
        if ( AccessStringHelper.canRead( target.getSharing().getPublicAccess() ) )
        {
            return false;
        }

        if ( MapUtils.isEmpty( source.getUserGroups() ) && MapUtils.isEmpty( source.getUsers() ) )
        {
            return false;
        }

        return shouldUpdateAccessObjects( source.getUsers(), target.getSharing().getUsers() )
            || shouldUpdateAccessObjects( source.getUserGroups(), target.getSharing().getUserGroups() );
    }

    /**
     * Check if given target {@code Map<String,AccessObject>} need to be updated
     * based on given source {@code Map<String,AccessObject>}
     *
     * @return TRUE if one of AccessObject in target Map need to be updated,
     *         otherwise FALSE.
     */
    private <T extends AccessObject> boolean shouldUpdateAccessObjects( final Map<String, T> source,
        final Map<String, T> target )
    {
        if ( MapUtils.isEmpty( source ) )
        {
            return false;
        }

        boolean shouldUpdate = false;

        for ( final T sourceAccess : source.values() )
        {
            if ( !AccessStringHelper.canRead( sourceAccess.getAccess() ) )
            {
                continue;
            }

            T targetAccess = target.get( sourceAccess.getId() );

            if ( targetAccess != null && AccessStringHelper.canRead( targetAccess.getAccess() ) )
            {
                continue;
            }

            shouldUpdate = true;
        }

        return shouldUpdate;
    }

    /**
     * Add UserAccesses and UserGroupAccesses from source to target object.
     *
     * @return TRUE if targetObject is updated, otherwise return FALSE
     */
    private <T extends IdentifiableObject> boolean mergeSharing( final Sharing source, T target,
        CascadeSharingParameters parameters )
    {
        if ( mergeAccessObject( UserAccess.class, source.getUsers(), target.getSharing().getUsers() )
            || mergeAccessObject( UserGroupAccess.class, source.getUserGroups(), target.getSharing().getUserGroups() ) )
        {
            parameters.getReport().addUpdatedObject( getTypeReportKey( target ), target );
            return true;
        }

        return false;
    }

    /**
     * Add {@link AccessObject} from source to target
     * <p>
     * After added, target accessObjects will only have READ permission
     */
    private <T extends AccessObject> boolean mergeAccessObject( Class<T> type, final Map<String, T> source,
        Map<String, T> target )
    {
        if ( MapUtils.isEmpty( source ) )
        {
            return false;
        }

        boolean updated = false;

        for ( T sourceAccess : source.values() )
        {
            if ( !AccessStringHelper.canRead( sourceAccess.getAccess() ) )
            {
                continue;
            }

            T targetAccess = target.get( sourceAccess.getId() );

            if ( targetAccess != null && AccessStringHelper.canRead( targetAccess.getAccess() ) )
            {
                continue;
            }

            if ( targetAccess == null )
            {
                targetAccess = sourceAccess.copy();
            }

            targetAccess.setAccess( AccessStringHelper.READ );
            target.put( targetAccess.getId(), targetAccess );

            updated = true;
        }

        return updated;
    }

    /**
     * Get the plurals name of the object. Will be used to display list updated
     * objects in the {@link CascadeSharingReport}
     *
     * @return plurals name of the object schema.
     */
    private String getTypeReportKey( Object object )
    {
        return schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) ).getPlural();
    }

    /**
     * Check if currentUser can update given object, if not add new ErrorReport
     * to {@link CascadeSharingParameters}.
     *
     * @param object object for validating
     * @param parameters {@link CascadeSharingParameters}
     * @return TRUE if current user can update given object, otherwise FALSE.
     */
    private <T extends IdentifiableObject> boolean canUserUpdate( T object, CascadeSharingParameters parameters )
    {
        if ( !aclService.canUpdate( parameters.getUser(), object ) )
        {
            parameters.getReport().getErrorReports().add( new ErrorReport( HibernateProxyUtils.getRealClass( object ),
                ErrorCode.E3001, parameters.getUser().getUsername(), object.getUid() ) );
            return false;
        }

        return true;
    }
}
