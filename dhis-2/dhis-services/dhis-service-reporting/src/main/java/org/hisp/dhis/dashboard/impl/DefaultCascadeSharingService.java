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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;

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
public class DefaultCascadeSharingService
    implements CascadeSharingService
{
    private final IdentifiableObjectManager manager;

    private final SchemaService schemaService;

    private final AclService aclService;

    public DefaultCascadeSharingService( @NonNull IdentifiableObjectManager manager,
        @NonNull SchemaService schemaService, @NonNull AclService aclService )
    {
        this.manager = manager;
        this.schemaService = schemaService;
        this.aclService = aclService;
    }

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
                handleMapObject( dashboardItem.getMap(), itemCanMergeObjects, parameters );
                break;
            case VISUALIZATION:
                handleVisualization( dashboard, dashboardItem.getVisualization(), itemCanMergeObjects, parameters );
                break;
            case EVENT_REPORT:
                handleEventReport( dashboard, dashboardItem.getEventReport(), itemCanMergeObjects, parameters );
                break;
            case EVENT_CHART:
                handleEventChart( dashboard, dashboardItem.getEventChart(), itemCanMergeObjects, parameters );
                break;
            default:
                break;
            }

            if ( !CollectionUtils.isEmpty( itemCanMergeObjects ) )
            {
                canMergeObjects.addAll( itemCanMergeObjects );

                parameters.getReport().incUpdatedDashboardItem();
            }
        } );

        if ( parameters.isDryRun() || (parameters.isAtomic() && parameters.getReport().hasErrors()) )
        {
            canMergeObjects
                .forEach( object -> parameters.getReport().addUpdatedObject( getTypeReportKey( object ), object ) );
            return parameters.getReport();
        }

        canMergeObjects.forEach( object -> {
            if ( mergeSharing( dashboard.getSharing(), object, parameters ) )
            {
                manager.update( object );
            }
        } );

        return parameters.getReport();
    }

    /**
     * Merge sharing from given dashboard to given visualization
     *
     * @param map {@link org.hisp.dhis.mapping.Map}
     * @param updateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleMapObject( org.hisp.dhis.mapping.Map map,
        Set<IdentifiableObject> updateObjects, CascadeSharingParameters parameters )
    {
        if ( validateUserAccess( map, parameters ) )
        {
            updateObjects.add( map );
        }
    }

    /**
     * Merge sharing from given dashboard to given visualization
     *
     * @param dashboard {@link Dashboard}
     * @param visualization {@link Visualization}
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleVisualization( Dashboard dashboard, Visualization visualization,
        Set<IdentifiableObject> canMergeObjects, CascadeSharingParameters parameters )
    {
        if ( visualization == null )
        {
            return;
        }

        if ( validateUserAccess( visualization, parameters ) )
        {
            canMergeObjects.add( visualization );
        }

        handleBaseAnalyticObject( dashboard.getSharing(), visualization, canMergeObjects, parameters );
    }

    /**
     * Merge sharing from given dashboard to given eventReport
     *
     * @param dashboard {@link Dashboard}
     * @param eventReport {@link EventReport}
     * @param updateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleEventReport( Dashboard dashboard, EventReport eventReport, Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( eventReport == null )
        {
            return;
        }

        if ( handleIdentifiableObject( EventReport.class, eventReport, updateObjects, parameters ) )
        {
            updateObjects.add( eventReport );
        }

        handleIdentifiableObject( TrackedEntityAttribute.class, eventReport.getAttributeValueDimension(), updateObjects,
            parameters );

        handleIdentifiableObject( DataElement.class, eventReport.getDataElementValueDimension(), updateObjects,
            parameters );

        handleBaseAnalyticObject( dashboard.getSharing(), eventReport, updateObjects, parameters );
    }

    /**
     * Merge sharing from given dashboard to given eventChart
     *
     * @param dashboard {@link Dashboard}
     * @param eventChart {@link EventChart}
     * @param updateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleEventChart( Dashboard dashboard, EventChart eventChart, Set<IdentifiableObject> updateObjects,
        CascadeSharingParameters parameters )
    {
        if ( eventChart == null )
        {
            return;
        }

        if ( handleIdentifiableObject( EventChart.class, eventChart, updateObjects, parameters ) )
        {
            updateObjects.add( eventChart );
        }

        handleIdentifiableObject( TrackedEntityAttribute.class, eventChart.getAttributeValueDimension(), updateObjects,
            parameters );

        handleIdentifiableObject( DataElement.class, eventChart.getDataElementValueDimension(), updateObjects,
            parameters );

        handleBaseAnalyticObject( dashboard.getSharing(), eventChart, updateObjects, parameters );
    }

    /**
     * Merge sharing from given source to all given analyticalObject's
     * dimensional objects
     *
     * @param source {@link Sharing}
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleBaseAnalyticObject( Sharing source, BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        handleIdentifiableObjects( DataElement.class, analyticalObject.getDataElements(), listUpdateObjects,
            parameters );

        handleIdentifiableObjects( Indicator.class, analyticalObject.getIndicators(), listUpdateObjects, parameters );

        handleCategoryDimension( analyticalObject, listUpdateObjects, parameters );

        handleDataElementDimensions( analyticalObject, listUpdateObjects, parameters );

        handleDataElementGroupSetDimensions( analyticalObject, listUpdateObjects, parameters );

        handleCategoryOptionGroupSetDimensions( analyticalObject, listUpdateObjects, parameters );

        handleTrackedEntityAttributeDimension( analyticalObject, listUpdateObjects, parameters );
    }

    /**
     * Merge sharing from given source to given analyticalObject's
     * {@link TrackedEntityAttributeDimension} and all related objects that has
     * Sharing enabled.
     *
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleTrackedEntityAttributeDimension( BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<TrackedEntityAttributeDimension> attributeDimensions = analyticalObject
            .getAttributeDimensions();

        if ( CollectionUtils.isEmpty( attributeDimensions ) )
        {
            return;
        }

        attributeDimensions.forEach( attributeDimension -> {
            handleIdentifiableObject( TrackedEntityAttribute.class, attributeDimension.getAttribute(),
                listUpdateObjects, parameters );
            handleIdentifiableObject( LegendSet.class, attributeDimension.getLegendSet(), listUpdateObjects,
                parameters );
        } );
    }

    /**
     * Merge sharing from given source to given analyticalObject's
     * {@link CategoryOptionGroupSetDimension} and all related objects that has
     * Sharing enabled.
     *
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleCategoryOptionGroupSetDimensions( BaseAnalyticalObject analyticalObject,
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

            handleIdentifiableObject( CategoryOptionGroupSet.class, catOptionGroupSet, listUpdateObjects, parameters );

            List<CategoryOptionGroup> catOptionGroups = catOptionGroupSet.getMembers();

            if ( CollectionUtils.isEmpty( catOptionGroups ) )
            {
                return;
            }

            catOptionGroups.forEach( catOptionGroup -> {
                handleIdentifiableObject( CategoryOptionGroup.class, catOptionGroup, listUpdateObjects, parameters );
                handleIdentifiableObjects( CategoryOption.class, catOptionGroup.getMembers(), listUpdateObjects,
                    parameters );
            } );

        } );
    }

    /**
     * Merge sharing from given source to given analyticalObject's
     * {@link TrackedEntityDataElementDimension} and all related objects that
     * has Sharing enabled.
     *
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleDataElementDimensions( BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<TrackedEntityDataElementDimension> deDimensions = analyticalObject
            .getDataElementDimensions();

        if ( CollectionUtils.isEmpty( deDimensions ) )
        {
            return;
        }

        deDimensions.forEach( deDimension -> {
            handleIdentifiableObject( DataElement.class, deDimension.getDataElement(), listUpdateObjects, parameters );
            handleIdentifiableObject( LegendSet.class, deDimension.getLegendSet(), listUpdateObjects, parameters );
            handleIdentifiableObject( ProgramStage.class, deDimension.getProgramStage(), listUpdateObjects,
                parameters );
        } );
    }

    /**
     * Merge sharing from given source to given analyticalObject's
     * {@link CategoryDimension} and all related objects that has Sharing
     * enabled.
     *
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleCategoryDimension( BaseAnalyticalObject analyticalObject,
        Set<IdentifiableObject> listUpdateObjects, CascadeSharingParameters parameters )
    {
        List<CategoryDimension> catDimensions = analyticalObject.getCategoryDimensions();

        if ( CollectionUtils.isEmpty( catDimensions ) )
        {
            return;
        }

        catDimensions.forEach( catDimension -> {
            Category category = catDimension.getDimension();

            handleIdentifiableObject( Category.class, category, listUpdateObjects, parameters );

            List<CategoryOption> catOptions = catDimension.getItems();

            if ( CollectionUtils.isEmpty( catOptions ) )
            {
                return;
            }

            catOptions.forEach( catOption -> handleIdentifiableObject( CategoryOption.class, catOption,
                listUpdateObjects, parameters ) );
        } );
    }

    /**
     * Merge sharing from given source to given analyticalObject's
     * {@link DataElementGroupSetDimension} and all related objects that has
     * Sharing enabled.
     *
     * @param analyticalObject any object extends {@link BaseAnalyticalObject}
     * @param listUpdateObjects Set of objects need to be updated
     * @param parameters {@link CascadeSharingParameters}
     */
    private void handleDataElementGroupSetDimensions( BaseAnalyticalObject analyticalObject,
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

            handleIdentifiableObject( DataElementGroupSet.class, deGroupSet, listUpdateObjects, parameters );

            List<DataElementGroup> deGroups = deGroupSetDimension.getItems();

            if ( CollectionUtils.isEmpty( deGroups ) )
            {
                return;
            }

            deGroups
                .forEach( deGroup -> handleIdentifiableObject( DataElementGroup.class, deGroup, listUpdateObjects,
                    parameters ) );
        } );
    }

    /**
     * Util method for merge sharing from given {@link Sharing} to give List of
     * {@link IdentifiableObject}
     *
     * @param targetObjects list of {@link IdentifiableObject}
     * @param listUpdateObjects Set of objects need to be updated.
     * @param parameters {@link CascadeSharingParameters}
     */
    private <T extends IdentifiableObject> void handleIdentifiableObjects( Class type, Collection<T> targetObjects,
        Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
    {
        if ( CollectionUtils.isEmpty( targetObjects ) )
        {
            return;
        }

        targetObjects.forEach( object -> handleIdentifiableObject( type, object, listUpdateObjects, parameters ) );
    }

    /**
     * Util method for merge sharing from given {@link Sharing} to given
     * {@link IdentifiableObject}
     *
     * @param target {@link IdentifiableObject}
     * @param listUpdateObjects Set of objects need to be updated.
     * @return TRUE if target object should be updated, otherwise return FALSE.
     */
    private <T extends IdentifiableObject> boolean handleIdentifiableObject( Class<T> type, T target,
        Collection<IdentifiableObject> listUpdateObjects,
        CascadeSharingParameters parameters )
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

        if ( validateUserAccess( persistedTarget, parameters ) )
        {
            listUpdateObjects.add( persistedTarget );
            return true;
        }

        return false;
    }

    /**
     * Add UserAccesses and UserGroupAccesses from source to target object.
     * <p>
     * If dryRun=true then target object will not be updated, but function still
     * return TRUE.
     *
     * @return TRUE if targetObject should be updated, otherwise return FALSE
     */
    private <T extends IdentifiableObject> boolean mergeSharing( final Sharing source, T target,
        CascadeSharingParameters parameters )
    {
        if ( AccessStringHelper.canRead( target.getSharing().getPublicAccess() ) )
        {
            return false;
        }

        Map<String, UserAccess> userAccesses = new HashMap<>( target.getSharing().getUsers() );
        Map<String, UserGroupAccess> userGroupAccesses = new HashMap<>( target.getSharing()
            .getUserGroups() );

        if ( mergeAccessObject( source.getUsers(), userAccesses )
            || mergeAccessObject( source.getUserGroups(), userGroupAccesses ) )
        {
            if ( !parameters.isDryRun() )
            {
                target.getSharing().setUsers( userAccesses );
                target.getSharing().setUserGroups( userGroupAccesses );
            }

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
    private <T extends AccessObject> boolean mergeAccessObject(
        final Map<String, T> source, Map<String, T> target )
    {
        if ( MapUtils.isEmpty( source ) )
        {
            return false;
        }

        boolean shouldUpdate = false;

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
                targetAccess = sourceAccess;
            }

            targetAccess.setAccess( AccessStringHelper.READ );
            target.put( targetAccess.getId(), targetAccess );

            shouldUpdate = true;
        }

        return shouldUpdate;
    }

    /**
     * If dryRun=TRUE then no objects will be updated.
     */
    private boolean canUpdate( CascadeSharingParameters parameters )
    {
        return !parameters.isDryRun() && parameters.getReport().getErrorReports().isEmpty();
    }

    /**
     * Get the plurals name of the object. Will be used in the
     * {@link CascadeSharingReport}
     *
     * @return plurals name of the object schema.
     */
    private String getTypeReportKey( Object object )
    {
        return schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) ).getPlural();
    }

    /**
     * Check if currentUser can given object, if not add new ErrorReport to
     * {@link CascadeSharingParameters}
     *
     * @param object object for validating
     * @param parameters {@link CascadeSharingParameters}
     * @return TRUE if current user can update given object, otherwise FALSE
     */
    private <T extends IdentifiableObject> boolean validateUserAccess( T object, CascadeSharingParameters parameters )
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
