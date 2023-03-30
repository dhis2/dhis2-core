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
package org.hisp.dhis.tracker.event;

import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service( "org.hisp.dhis.tracker.event.EventService" )
@RequiredArgsConstructor
public class DefaultEventService implements EventService
{
    private final CurrentUserService currentUserService;

    private final EventStore eventStore;

    private final TrackerAccessManager trackerAccessManager;

    private final DataElementService dataElementService;

    @Transactional( readOnly = true )
    @Override
    public Events getEvents( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        validate( params, user );

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        Events events = new Events();
        List<ProgramStageInstance> eventList = new ArrayList<>();

        if ( params.isSkipPaging() )
        {
            events.setEvents( eventStore.getEvents( params, emptyMap() ) );
            return events;
        }

        Pager pager;
        eventList.addAll( eventStore.getEvents( params, emptyMap() ) );

        if ( params.isTotalPages() )
        {
            int count = eventStore.getEventCount( params );
            pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
        }
        else
        {
            pager = handleLastPageFlag( params, eventList );
        }

        events.setPager( pager );
        events.setEvents( eventList );

        return events;
    }

    /**
     * This method will apply the logic related to the parameter
     * 'totalPages=false'. This works in conjunction with the method:
     * {@link EventStore#getEvents(EventSearchParams,Map<String,Set<String>>)}
     *
     * This is needed because we need to query (pageSize + 1) at DB level. The
     * resulting query will allow us to evaluate if we are in the last page or
     * not. And this is what his method does, returning the respective Pager
     * object.
     *
     * @param params the request params
     * @param eventList the reference to the list of Event
     * @return the populated SlimPager instance
     */
    private Pager handleLastPageFlag( EventSearchParams params, List<ProgramStageInstance> eventList )
    {
        Integer originalPage = defaultIfNull( params.getPage(), FIRST_PAGE );
        Integer originalPageSize = defaultIfNull( params.getPageSize(), DEFAULT_PAGE_SIZE );
        boolean isLastPage = false;

        if ( isNotEmpty( eventList ) )
        {
            isLastPage = eventList.size() <= originalPageSize;
            if ( !isLastPage )
            {
                // Get the same number of elements of the pageSize, forcing
                // the removal of the last additional element added at querying
                // time.
                eventList.retainAll( eventList.subList( 0, originalPageSize ) );
            }
        }

        return new SlimPager( originalPage, originalPageSize, isLastPage );
    }

    @Transactional( readOnly = true )
    @Override
    public ProgramStageInstance getEvent( ProgramStageInstance programStageInstance, EventParams eventParams )
    {
        return getEvent( programStageInstance, false, false, eventParams );
    }

    @Transactional( readOnly = true )
    @Override
    public ProgramStageInstance getEvent( ProgramStageInstance programStageInstance, boolean isSynchronizationQuery,
        boolean skipOwnershipCheck, EventParams eventParams )
    {
        if ( programStageInstance == null )
        {
            return null;
        }

        ProgramStageInstance event = new ProgramStageInstance();
        event.setUid( programStageInstance.getUid() );

        event.setStatus( programStageInstance.getStatus() );
        event.setExecutionDate( programStageInstance.getExecutionDate() );
        event.setDueDate( programStageInstance.getDueDate() );
        event.setStoredBy( programStageInstance.getStoredBy() );
        event.setCompletedBy( programStageInstance.getCompletedBy() );
        event.setCompletedDate( programStageInstance.getCompletedDate() );
        event.setCreated( programStageInstance.getCreated() );
        event.setCreatedByUserInfo( programStageInstance.getCreatedByUserInfo() );
        event.setLastUpdatedByUserInfo( programStageInstance.getLastUpdatedByUserInfo() );
        event.setCreatedAtClient( programStageInstance.getCreatedAtClient() );
        event.setLastUpdated( programStageInstance.getLastUpdated() );
        event.setLastUpdatedAtClient( programStageInstance.getLastUpdatedAtClient() );
        event.setGeometry( programStageInstance.getGeometry() );
        event.setDeleted( programStageInstance.isDeleted() );
        event.setAssignedUser( programStageInstance.getAssignedUser() );

        User user = currentUserService.getCurrentUser();
        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        event.setProgramInstance( programStageInstance.getProgramInstance() );
        event.setProgramStage( programStageInstance.getProgramStage() );

        List<String> errors = trackerAccessManager.canRead( user, programStageInstance, skipOwnershipCheck );

        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }

        event.setOrganisationUnit( ou );
        event.setProgramStage( programStageInstance.getProgramStage() );

        event.setAttributeOptionCombo( programStageInstance.getAttributeOptionCombo() );

        Collection<EventDataValue> dataValues;
        if ( !isSynchronizationQuery )
        {
            dataValues = programStageInstance.getEventDataValues();
        }
        else
        {
            Set<String> dataElementsToSync = programStageInstance.getProgramStage().getProgramStageDataElements()
                .stream().filter( psde -> !psde.getSkipSynchronization() ).map( psde -> psde.getDataElement().getUid() )
                .collect( Collectors.toSet() );

            dataValues = programStageInstance.getEventDataValues().stream()
                .filter( dv -> dataElementsToSync.contains( dv.getDataElement() ) ).collect( Collectors.toSet() );
        }

        for ( EventDataValue dataValue : dataValues )
        {
            if ( dataElementService.getDataElement( dataValue.getDataElement() ) != null ) // check permissions
            {
                EventDataValue value = new EventDataValue();
                value.setCreated( dataValue.getCreated() );
                value.setCreatedByUserInfo( dataValue.getCreatedByUserInfo() );
                value.setLastUpdated( dataValue.getLastUpdated() );
                value.setLastUpdatedByUserInfo( dataValue.getLastUpdatedByUserInfo() );
                value.setDataElement( dataValue.getDataElement() );
                value.setValue( dataValue.getValue() );
                value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
                value.setStoredBy( dataValue.getStoredBy() );

                event.getEventDataValues().add( value );
            }
            else
            {
                log.info( "Can not find a Data Element having UID [" + dataValue.getDataElement() + "]" );
            }
        }

        event.getComments().addAll( programStageInstance.getComments() );

        if ( eventParams.isIncludeRelationships() )
        {
            Set<RelationshipItem> relationshipItems = new HashSet<>();

            for ( RelationshipItem relationshipItem : programStageInstance.getRelationshipItems() )
            {
                org.hisp.dhis.relationship.Relationship daoRelationship = relationshipItem.getRelationship();
                if ( trackerAccessManager.canRead( user, daoRelationship ).isEmpty()
                    && (!daoRelationship.isDeleted()) )
                {
                    relationshipItems.add( relationshipItem );
                }
            }

            event.setRelationshipItems( relationshipItems );
        }

        return event;
    }

    @Override
    public void validate( EventSearchParams params, User user )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params.hasLastUpdatedDuration() && (params.hasLastUpdatedStartDate() || params.hasLastUpdatedEndDate()) )
        {
            violation = "Last updated from and/or to and last updated duration cannot be specified simultaneously";
        }

        if ( violation == null && params.hasLastUpdatedDuration()
            && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getLastUpdatedDuration();
        }

        if ( violation == null && params.getOrgUnit() != null
            && !trackerAccessManager.canAccess( user, params.getProgram(), params.getOrgUnit() ) )
        {
            violation = "User does not have access to orgUnit: " + params.getOrgUnit().getUid();
        }

        if ( violation == null && params.getOrgUnitSelectionMode() != null )
        {
            violation = getOuModeViolation( params, user );
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    private String getOuModeViolation( EventSearchParams params, User user )
    {
        OrganisationUnitSelectionMode selectedOuMode = params.getOrgUnitSelectionMode();

        String violation = null;

        switch ( selectedOuMode )
        {
        case ALL:
            violation = userCanSearchOuModeALL( user ) ? null
                : "Current user is not authorized to query across all organisation units";
            break;
        case ACCESSIBLE:
            violation = getAccessibleScopeValidation( user, params );
            break;
        case CAPTURE:
            violation = getCaptureScopeValidation( user );
            break;
        case CHILDREN:
        case SELECTED:
        case DESCENDANTS:
            violation = params.getOrgUnit() == null
                ? "Organisation unit is required for ouMode: " + params.getOrgUnitSelectionMode()
                : null;
            break;
        default:
            violation = "Invalid ouMode:  " + params.getOrgUnitSelectionMode();
            break;
        }

        return violation;
    }

    private String getCaptureScopeValidation( User user )
    {
        String violation = null;

        if ( user == null )
        {
            violation = "User is required for ouMode: " + OrganisationUnitSelectionMode.CAPTURE;
        }
        else if ( user.getOrganisationUnits().isEmpty() )
        {
            violation = "User needs to be assigned data capture orgunits";
        }

        return violation;
    }

    private String getAccessibleScopeValidation( User user, EventSearchParams params )
    {
        String violation = null;

        if ( user == null )
        {
            return "User is required for ouMode: " + OrganisationUnitSelectionMode.ACCESSIBLE;
        }

        if ( params.getProgram() == null || params.getProgram().isClosed() || params.getProgram().isProtected() )
        {
            violation = user.getOrganisationUnits().isEmpty() ? "User needs to be assigned data capture orgunits"
                : null;
        }
        else
        {
            violation = user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()
                ? "User needs to be assigned either TEI search, data view or data capture org units"
                : null;
        }

        return violation;
    }

    private boolean userCanSearchOuModeALL( User user )
    {
        if ( user == null )
        {
            return false;
        }

        return user.isSuper()
            || user.isAuthorized( Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name() );
    }
}
