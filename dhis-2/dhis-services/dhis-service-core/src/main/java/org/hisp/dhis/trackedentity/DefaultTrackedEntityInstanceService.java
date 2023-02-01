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
package org.hisp.dhis.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.CREATED_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.DELETED;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.INACTIVE_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.LAST_UPDATED_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.META_DATA_NAMES_KEY;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.ORG_UNIT_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.ORG_UNIT_NAME;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.PAGER_META_KEY;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.POTENTIAL_DUPLICATE;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.TRACKED_ENTITY_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.TRACKED_ENTITY_INSTANCE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.events.event.EventContext;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew Gizaw
 */
@Slf4j
@Service( "org.hisp.dhis.trackedentity.TrackedEntityInstanceService" )
public class DefaultTrackedEntityInstanceService
    implements TrackedEntityInstanceService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final TrackedEntityInstanceStore trackedEntityInstanceStore;

    private final TrackedEntityAttributeValueService attributeValueService;

    private final TrackedEntityAttributeService attributeService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final OrganisationUnitService organisationUnitService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final TrackerOwnershipManager trackerOwnershipAccessManager;

    private final TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

    private final TrackedEntityAttributeValueAuditService attributeValueAuditService;

    // TODO: FIXME luciano using @Lazy here because we have circular
    // dependencies:
    // TrackedEntityInstanceService --> TrackerOwnershipManager -->
    // TrackedEntityProgramOwnerService --> TrackedEntityInstanceService
    public DefaultTrackedEntityInstanceService( TrackedEntityInstanceStore trackedEntityInstanceStore,
        TrackedEntityAttributeValueService attributeValueService,
        TrackedEntityAttributeService attributeService,
        TrackedEntityTypeService trackedEntityTypeService,
        OrganisationUnitService organisationUnitService,
        CurrentUserService currentUserService, AclService aclService,
        @Lazy TrackerOwnershipManager trackerOwnershipAccessManager,
        @Lazy TrackedEntityInstanceAuditService trackedEntityInstanceAuditService,
        @Lazy TrackedEntityAttributeValueAuditService attributeValueAuditService )
    {
        checkNotNull( trackedEntityInstanceStore );
        checkNotNull( attributeValueService );
        checkNotNull( attributeService );
        checkNotNull( trackedEntityTypeService );
        checkNotNull( organisationUnitService );
        checkNotNull( currentUserService );
        checkNotNull( aclService );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( trackedEntityInstanceAuditService );
        checkNotNull( attributeValueAuditService );

        this.trackedEntityInstanceStore = trackedEntityInstanceStore;
        this.attributeValueService = attributeValueService;
        this.attributeService = attributeService;
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.organisationUnitService = organisationUnitService;
        this.currentUserService = currentUserService;
        this.aclService = aclService;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.trackedEntityInstanceAuditService = trackedEntityInstanceAuditService;
        this.attributeValueAuditService = attributeValueAuditService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params,
        boolean skipAccessValidation, boolean skipSearchScopeValidation )
    {
        if ( params.isOrQuery() && !params.hasAttributes() && !params.hasProgram() )
        {
            Collection<TrackedEntityAttribute> attributes = attributeService
                .getTrackedEntityAttributesDisplayInListNoProgram();
            params.addAttributes( QueryItem.getQueryItems( attributes ) );
            params.addFiltersIfNotExist( QueryItem.getQueryItems( attributes ) );
        }

        decideAccess( params );
        // AccessValidation should be skipped only and only if it is internal
        // service that runs the task (for example sync job)
        if ( !skipAccessValidation )
        {
            validate( params );
        }

        if ( !skipSearchScopeValidation )
        {
            validateSearchScope( params, false );
        }

        User user = currentUserService.getCurrentUser();

        params.setUser( user );

        params.handleCurrentUserSelectionMode();

        List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceStore
            .getTrackedEntityInstances( params );

        trackedEntityInstances = trackedEntityInstances.stream()
            .filter( ( tei ) -> aclService.canDataRead( user, tei.getTrackedEntityType() ) )
            .collect( Collectors.toList() );

        // Avoiding NullPointerException
        String accessedBy = user != null ? user.getUsername() : currentUserService.getCurrentUsername();

        for ( TrackedEntityInstance tei : trackedEntityInstances )
        {
            addTrackedEntityInstanceAudit( tei, accessedBy, AuditType.SEARCH );
        }

        return trackedEntityInstances;
    }

    @Override
    @Transactional( readOnly = true )
    public List<Long> getTrackedEntityInstanceIds( TrackedEntityInstanceQueryParams params,
        boolean skipAccessValidation, boolean skipSearchScopeValidation )
    {
        if ( params.isOrQuery() && !params.hasAttributes() && !params.hasProgram() )
        {
            Collection<TrackedEntityAttribute> attributes = attributeService
                .getTrackedEntityAttributesDisplayInListNoProgram();
            params.addAttributes( QueryItem.getQueryItems( attributes ) );
            params.addFiltersIfNotExist( QueryItem.getQueryItems( attributes ) );
        }

        handleSortAttributes( params );

        decideAccess( params );

        // AccessValidation should be skipped only and only if it is internal
        // service that runs the task (for example sync job)
        if ( !skipAccessValidation )
        {
            validate( params );
        }

        if ( !skipSearchScopeValidation )
        {
            validateSearchScope( params, false );
        }

        User user = this.currentUserService.getCurrentUser();

        params.setUser( user );

        params.handleCurrentUserSelectionMode();

        return trackedEntityInstanceStore.getTrackedEntityInstanceIds( params );
    }

    /**
     * This method handles any dynamic sort order columns in the params. These
     * have to be added to the attribute list if neither are present in the
     * attribute list nor the filter list.
     *
     * For example, if attributes or filters don't have a specific
     * trackedentityattribute uid, but sorting has been requested for that tea
     * uid, then we need to add them to the attribute list.
     *
     * @param params The TEIQueryParams object
     */
    private void handleSortAttributes( TrackedEntityInstanceQueryParams params )
    {
        List<TrackedEntityAttribute> sortAttributes = params.getOrders().stream()
            .map( OrderParam::getField )
            .filter( this::isDynamicColumn )
            .map( attributeService::getTrackedEntityAttribute )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );

        params.addAttributesIfNotExist( QueryItem.getQueryItems( sortAttributes ).stream()
            .filter( sAtt -> !params.getFilters().contains( sAtt ) ).collect( Collectors.toList() ) );
    }

    public boolean isDynamicColumn( String propName )
    {
        return Arrays.stream( TrackedEntityInstanceQueryParams.OrderColumn.values() )
            .noneMatch( orderColumn -> orderColumn.getPropName().equals( propName ) );
    }

    @Override
    @Transactional( readOnly = true )
    public int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation )
    {
        decideAccess( params );

        if ( !skipAccessValidation )
        {
            validate( params );
        }

        if ( !skipSearchScopeValidation )
        {
            validateSearchScope( params, false );
        }

        params.setUser( currentUserService.getCurrentUser() );

        params.handleCurrentUserSelectionMode();

        // using countForGrid here to leverage the better performant rewritten
        // sql query
        return trackedEntityInstanceStore.getTrackedEntityInstanceCountForGrid( params );
    }

    // TODO lower index on attribute value?

    @Override
    @Transactional( readOnly = true )
    public Grid getTrackedEntityInstancesGrid( TrackedEntityInstanceQueryParams params )
    {
        decideAccess( params );
        validate( params );
        validateSearchScope( params, true );
        handleAttributes( params );

        // ---------------------------------------------------------------------
        // Conform parameters
        // ---------------------------------------------------------------------

        params.conform();
        params.handleCurrentUserSelectionMode();

        // ---------------------------------------------------------------------
        // Grid headers
        // ---------------------------------------------------------------------

        Grid grid = new ListGrid();

        grid.addHeader( new GridHeader( TRACKED_ENTITY_INSTANCE_ID, "Instance" ) );
        grid.addHeader( new GridHeader( CREATED_ID, "Created" ) );
        grid.addHeader( new GridHeader( LAST_UPDATED_ID, "Last updated" ) );
        grid.addHeader( new GridHeader( ORG_UNIT_ID, "Organisation unit" ) );
        grid.addHeader( new GridHeader( ORG_UNIT_NAME, "Organisation unit name" ) );
        grid.addHeader( new GridHeader( TRACKED_ENTITY_ID, "Tracked entity type" ) );
        grid.addHeader( new GridHeader( INACTIVE_ID, "Inactive" ) );
        grid.addHeader( new GridHeader( POTENTIAL_DUPLICATE, "Potential duplicate" ) );

        if ( params.isIncludeDeleted() )
        {
            grid.addHeader( new GridHeader( DELETED, "Deleted", ValueType.BOOLEAN, false, false ) );
        }

        for ( QueryItem item : params.getAttributes() )
        {
            grid.addHeader( new GridHeader( item.getItem().getUid(), item.getItem().getName() ) );
        }

        List<Map<String, String>> entities = trackedEntityInstanceStore.getTrackedEntityInstancesGrid( params );

        // ---------------------------------------------------------------------
        // Grid rows
        // ---------------------------------------------------------------------

        String accessedBy = currentUserService.getCurrentUsername();

        Map<String, TrackedEntityType> trackedEntityTypes = new HashMap<>();

        if ( params.hasTrackedEntityType() )
        {
            trackedEntityTypes.put( params.getTrackedEntityType().getUid(), params.getTrackedEntityType() );
        }

        if ( params.hasProgram() && params.getProgram().getTrackedEntityType() != null )
        {
            trackedEntityTypes.put( params.getProgram().getTrackedEntityType().getUid(),
                params.getProgram().getTrackedEntityType() );
        }

        Set<String> tes = new HashSet<>();

        for ( Map<String, String> entity : entities )
        {
            if ( params.getUser() != null && !params.getUser().isSuper() && params.hasProgram() &&
                (params.getProgram().getAccessLevel().equals( AccessLevel.PROTECTED ) ||
                    params.getProgram().getAccessLevel().equals( AccessLevel.CLOSED )) )
            {
                TrackedEntityInstance tei = trackedEntityInstanceStore
                    .getByUid( entity.get( TRACKED_ENTITY_INSTANCE_ID ) );

                if ( !trackerOwnershipAccessManager.hasAccess( params.getUser(), tei, params.getProgram() ) )
                {
                    continue;
                }
            }

            grid.addRow();
            grid.addValue( entity.get( TRACKED_ENTITY_INSTANCE_ID ) );
            grid.addValue( entity.get( CREATED_ID ) );
            grid.addValue( entity.get( LAST_UPDATED_ID ) );
            grid.addValue( entity.get( ORG_UNIT_ID ) );
            grid.addValue( entity.get( ORG_UNIT_NAME ) );
            grid.addValue( entity.get( TRACKED_ENTITY_ID ) );
            grid.addValue( entity.get( INACTIVE_ID ) );
            grid.addValue( entity.get( POTENTIAL_DUPLICATE ) );

            if ( params.isIncludeDeleted() )
            {
                grid.addValue( entity.get( DELETED ) );
            }

            tes.add( entity.get( TRACKED_ENTITY_ID ) );

            TrackedEntityType te = trackedEntityTypes.get( entity.get( TRACKED_ENTITY_ID ) );

            if ( te == null )
            {
                te = trackedEntityTypeService.getTrackedEntityType( entity.get( TRACKED_ENTITY_ID ) );
                trackedEntityTypes.put( entity.get( TRACKED_ENTITY_ID ), te );
            }

            if ( te != null && te.isAllowAuditLog() && accessedBy != null )
            {
                TrackedEntityInstanceAudit trackedEntityInstanceAudit = new TrackedEntityInstanceAudit(
                    entity.get( TRACKED_ENTITY_INSTANCE_ID ), accessedBy, AuditType.SEARCH );
                trackedEntityInstanceAuditService.addTrackedEntityInstanceAudit( trackedEntityInstanceAudit );
            }

            for ( QueryItem item : params.getAttributes() )
            {
                grid.addValue( entity.get( item.getItemId() ) );
            }
        }

        Map<String, Object> metaData = new HashMap<>();

        if ( params.isPaging() )
        {
            int count = 0;

            if ( params.isTotalPages() )
            {
                count = trackedEntityInstanceStore.getTrackedEntityInstanceCountForGrid( params );
            }

            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            metaData.put( PAGER_META_KEY, pager );
        }

        if ( !params.isSkipMeta() )
        {
            Map<String, String> names = new HashMap<>();

            for ( String te : tes )
            {
                TrackedEntityType entity = trackedEntityTypes.get( te );
                names.put( te, entity != null ? entity.getDisplayName() : null );
            }

            metaData.put( META_DATA_NAMES_KEY, names );
        }

        grid.setMetaData( metaData );

        return grid;
    }

    /**
     * Handles injection of attributes. The following combinations of parameters
     * will lead to attributes being injected.
     * <p>
     * - query: add display in list attributes - attributes - program: add
     * program attributes - query + attributes - query + program: add program
     * attributes - attributes + program - query + attributes + program
     */
    private void handleAttributes( TrackedEntityInstanceQueryParams params )
    {
        if ( params.isOrQuery() && !params.hasAttributes() && !params.hasProgram() )
        {
            Collection<TrackedEntityAttribute> attributes = attributeService
                .getTrackedEntityAttributesDisplayInListNoProgram();
            params.addAttributes( QueryItem.getQueryItems( attributes ) );
            params.addFiltersIfNotExist( QueryItem.getQueryItems( attributes ) );
        }
        else if ( params.hasProgram() )
        {
            params
                .addAttributesIfNotExist( QueryItem.getQueryItems( params.getProgram().getTrackedEntityAttributes() ) );
        }
        else if ( params.hasTrackedEntityType() )
        {
            params.addAttributesIfNotExist(
                QueryItem.getQueryItems( params.getTrackedEntityType().getTrackedEntityAttributes() ) );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public void decideAccess( TrackedEntityInstanceQueryParams params )
    {
        User user = params.isInternalSearch() ? null : params.getUser();

        if ( params.isOrganisationUnitMode( ALL ) &&
            !currentUserService
                .currentUserIsAuthorized( Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name() )
            &&
            !params.isInternalSearch() )
        {
            throw new IllegalQueryException( "Current user is not authorized to query across all organisation units" );
        }

        if ( params.hasProgram() )
        {
            if ( !aclService.canDataRead( user, params.getProgram() ) )
            {
                throw new IllegalQueryException( "Current user is not authorized to read data from selected program:  "
                    + params.getProgram().getUid() );
            }

            if ( params.getProgram().getTrackedEntityType() != null
                && !aclService.canDataRead( user, params.getProgram().getTrackedEntityType() ) )
            {
                throw new IllegalQueryException(
                    "Current user is not authorized to read data from selected program's tracked entity type:  "
                        + params.getProgram().getTrackedEntityType().getUid() );
            }

        }

        if ( params.hasTrackedEntityType() && !aclService.canDataRead( user, params.getTrackedEntityType() ) )
        {
            throw new IllegalQueryException(
                "Current user is not authorized to read data from selected tracked entity type:  "
                    + params.getTrackedEntityType().getUid() );
        }
        else
        {
            params.setTrackedEntityTypes( trackedEntityTypeService.getAllTrackedEntityType().stream()
                .filter( tet -> aclService.canDataRead( user, tet ) )
                .collect( Collectors.toList() ) );
        }
    }

    @Override
    public void validate( TrackedEntityInstanceQueryParams params )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        User user = params.getUser();

        if ( !params.hasTrackedEntityInstances() && !params.hasOrganisationUnits()
            && !(params.isOrganisationUnitMode( ALL ) || params.isOrganisationUnitMode( ACCESSIBLE )
                || params.isOrganisationUnitMode( CAPTURE )) )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( params.isOrganisationUnitMode( ACCESSIBLE )
            && (user == null || !user.hasDataViewOrganisationUnitWithFallback()) )
        {
            violation = "Current user must be associated with at least one organisation unit when selection mode is ACCESSIBLE";
        }

        if ( params.isOrganisationUnitMode( CAPTURE ) && (user == null || !user.hasOrganisationUnit()) )
        {
            violation = "Current user must be associated with at least one organisation unit with write access when selection mode is CAPTURE";
        }

        if ( params.hasProgram() && params.hasTrackedEntityType() )
        {
            violation = "Program and tracked entity cannot be specified simultaneously";
        }

        if ( !params.hasTrackedEntityInstances() && !params.hasProgram() && !params.hasTrackedEntityType() )
        {
            violation = "Either Program or Tracked entity type should be specified";
        }

        if ( params.hasProgramStatus() && !params.hasProgram() )
        {
            violation = "Program must be defined when program status is defined";
        }

        if ( params.hasFollowUp() && !params.hasProgram() )
        {
            violation = "Program must be defined when follow up status is defined";
        }

        if ( params.hasProgramEnrollmentStartDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program enrollment start date is specified";
        }

        if ( params.hasProgramEnrollmentEndDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program enrollment end date is specified";
        }

        if ( params.hasProgramIncidentStartDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program incident start date is specified";
        }

        if ( params.hasProgramIncidentEndDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program incident end date is specified";
        }

        if ( params.hasEventStatus() && (!params.hasEventStartDate() || !params.hasEventEndDate()) )
        {
            violation = "Event start and end date must be specified when event status is specified";
        }

        if ( params.getAssignedUserSelectionMode() != null && params.hasAssignedUsers()
            && !params.getAssignedUserSelectionMode().equals( AssignedUserSelectionMode.PROVIDED ) )
        {
            violation = "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED";
        }

        if ( params.isOrQuery() && params.hasFilters() )
        {
            violation = "Query cannot be specified together with filters";
        }

        if ( !params.getDuplicateAttributes().isEmpty() )
        {
            violation = "Attributes cannot be specified more than once: " + params.getDuplicateAttributes();
        }

        if ( !params.getDuplicateFilters().isEmpty() )
        {
            violation = "Filters cannot be specified more than once: " + params.getDuplicateFilters();
        }

        if ( params.hasLastUpdatedDuration() && (params.hasLastUpdatedStartDate() || params.hasLastUpdatedEndDate()) )
        {
            violation = "Last updated from and/or to and last updated duration cannot be specified simultaneously";
        }

        if ( params.hasLastUpdatedDuration() && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getLastUpdatedDuration();
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public void validateSearchScope( TrackedEntityInstanceQueryParams params, boolean isGridSearch )
        throws IllegalQueryException
    {
        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new IllegalQueryException( "User cannot be null" );
        }

        if ( !user.isSuper() && user.getOrganisationUnits().isEmpty() )
        {
            throw new IllegalQueryException( "User need to be associated with at least one organisation unit." );
        }

        if ( !params.hasProgram() && !params.hasTrackedEntityType() && params.hasAttributesOrFilters()
            && !params.hasOrganisationUnits() )
        {
            List<String> uniqeAttributeIds = attributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
                .map( TrackedEntityAttribute::getUid ).collect( Collectors.toList() );

            for ( String att : params.getAttributeAndFilterIds() )
            {
                if ( !uniqeAttributeIds.contains( att ) )
                {
                    throw new IllegalQueryException( "Either a program or tracked entity type must be specified" );
                }
            }
        }

        if ( !isLocalSearch( params, user ) )
        {
            int maxTeiLimit = 0; // no limit

            if ( params.hasQuery() )
            {
                throw new IllegalQueryException( "Query cannot be used during global search" );
            }

            if ( params.hasProgram() && params.hasTrackedEntityType() )
            {
                throw new IllegalQueryException( "Program and tracked entity cannot be specified simultaneously" );
            }

            if ( params.hasAttributesOrFilters() )
            {
                List<String> searchableAttributeIds = new ArrayList<>();

                if ( params.hasProgram() )
                {
                    searchableAttributeIds.addAll( params.getProgram().getSearchableAttributeIds() );
                }

                if ( params.hasTrackedEntityType() )
                {
                    searchableAttributeIds.addAll( params.getTrackedEntityType().getSearchableAttributeIds() );
                }

                if ( !params.hasProgram() && !params.hasTrackedEntityType() )
                {
                    searchableAttributeIds.addAll( attributeService.getAllSystemWideUniqueTrackedEntityAttributes()
                        .stream().map( TrackedEntityAttribute::getUid ).collect( Collectors.toList() ) );
                }

                List<String> violatingAttributes = new ArrayList<>();

                for ( String attributeId : params.getAttributeAndFilterIds() )
                {
                    if ( !searchableAttributeIds.contains( attributeId ) )
                    {
                        violatingAttributes.add( attributeId );
                    }
                }

                if ( !violatingAttributes.isEmpty() )
                {
                    throw new IllegalQueryException(
                        "Non-searchable attribute(s) can not be used during global search:  "
                            + violatingAttributes.toString() );
                }
            }

            if ( params.hasTrackedEntityType() )
            {
                maxTeiLimit = params.getTrackedEntityType().getMaxTeiCountToReturn();

                if ( !params.hasTrackedEntityInstances() && isTeTypeMinAttributesViolated( params ) )
                {
                    throw new IllegalQueryException(
                        "At least " + params.getTrackedEntityType().getMinAttributesRequiredToSearch()
                            + " attributes should be mentioned in the search criteria." );
                }
            }

            if ( params.hasProgram() )
            {
                maxTeiLimit = params.getProgram().getMaxTeiCountToReturn();

                if ( !params.hasTrackedEntityInstances() && isProgramMinAttributesViolated( params ) )
                {
                    throw new IllegalQueryException(
                        "At least " + params.getProgram().getMinAttributesRequiredToSearch()
                            + " attributes should be mentioned in the search criteria." );
                }
            }

            checkIfMaxTeiLimitIsReached( params, maxTeiLimit );
            params.setMaxTeiLimit( maxTeiLimit );
        }
    }

    private void checkIfMaxTeiLimitIsReached( TrackedEntityInstanceQueryParams params, int maxTeiLimit )
    {
        int instanceCount = trackedEntityInstanceStore.getTrackedEntityInstanceCountForGrid( params );
        if ( maxTeiLimit > 0 && instanceCount > maxTeiLimit )
        {
            throw new IllegalQueryException( "maxteicountreached" );
        }
    }

    private boolean isProgramMinAttributesViolated( TrackedEntityInstanceQueryParams params )
    {
        if ( params.hasUniqueFilter() )
        {
            return false;
        }

        return (!params.hasFilters() && !params.hasAttributes()
            && params.getProgram().getMinAttributesRequiredToSearch() > 0)
            || (params.hasFilters()
                && params.getFilters().size() < params.getProgram().getMinAttributesRequiredToSearch())
            || (params.hasAttributes()
                && params.getAttributes().size() < params.getProgram().getMinAttributesRequiredToSearch());
    }

    private boolean isTeTypeMinAttributesViolated( TrackedEntityInstanceQueryParams params )
    {
        if ( params.hasUniqueFilter() )
        {
            return false;
        }

        return (!params.hasFilters() && !params.hasAttributes()
            && params.getTrackedEntityType().getMinAttributesRequiredToSearch() > 0)
            || (params.hasFilters()
                && params.getFilters().size() < params.getTrackedEntityType().getMinAttributesRequiredToSearch())
            || (params.hasAttributes()
                && params.getAttributes().size() < params.getTrackedEntityType().getMinAttributesRequiredToSearch());
    }

    @Override
    @Transactional
    public long addTrackedEntityInstance( TrackedEntityInstance instance )
    {
        trackedEntityInstanceStore.save( instance );

        return instance.getId();
    }

    @Override
    @Transactional
    public long createTrackedEntityInstance( TrackedEntityInstance instance,
        Set<TrackedEntityAttributeValue> attributeValues )
    {
        long id = addTrackedEntityInstance( instance );

        for ( TrackedEntityAttributeValue pav : attributeValues )
        {
            attributeValueService.addTrackedEntityAttributeValue( pav );
            instance.getTrackedEntityAttributeValues().add( pav );
        }

        updateTrackedEntityInstance( instance ); // Update associations

        return id;
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityInstance> getTrackedEntityInstancesByUid( List<String> uids, User user )
    {
        if ( uids == null || uids.isEmpty() )
        {
            return Collections.emptyList();
        }
        return trackedEntityInstanceStore.getTrackedEntityInstancesByUid( uids, user );
    }

    @Override
    @Transactional( readOnly = true )
    public List<EventContext.TrackedEntityOuInfo> getTrackedEntityOuInfoByUid( List<String> uids, User user )
    {
        if ( uids == null || uids.isEmpty() )
        {
            return Collections.emptyList();
        }
        return trackedEntityInstanceStore.getTrackedEntityOuInfoByUid( uids, user );
    }

    @Override
    @Transactional
    public void updateTrackedEntityInstance( TrackedEntityInstance instance )
    {
        trackedEntityInstanceStore.update( instance );
    }

    @Override
    @Transactional
    public void updateTrackedEntityInstance( TrackedEntityInstance instance, User user )
    {
        trackedEntityInstanceStore.update( instance, user );
    }

    @Override
    @Transactional
    public void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs,
        Date lastSynchronized )
    {
        trackedEntityInstanceStore.updateTrackedEntityInstancesSyncTimestamp( trackedEntityInstanceUIDs,
            lastSynchronized );
    }

    @Override
    @Transactional
    public void updateTrackedEntityInstanceLastUpdated( Set<String> trackedEntityInstanceUIDs, Date lastUpdated )
    {
        trackedEntityInstanceStore.updateTrackedEntityInstancesLastUpdated( trackedEntityInstanceUIDs, lastUpdated );
    }

    @Override
    @Transactional
    public void deleteTrackedEntityInstance( TrackedEntityInstance instance )
    {
        attributeValueAuditService.deleteTrackedEntityAttributeValueAudits( instance );
        trackedEntityInstanceStore.delete( instance );
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityInstance getTrackedEntityInstance( long id )
    {
        TrackedEntityInstance tei = trackedEntityInstanceStore.get( id );

        addTrackedEntityInstanceAudit( tei, currentUserService.getCurrentUsername(), AuditType.READ );

        return tei;
    }

    @Override
    @Transactional
    public TrackedEntityInstance getTrackedEntityInstance( String uid )
    {
        TrackedEntityInstance tei = trackedEntityInstanceStore.getByUid( uid );
        addTrackedEntityInstanceAudit( tei, currentUserService.getCurrentUsername(), AuditType.READ );

        return tei;
    }

    @Override
    public TrackedEntityInstance getTrackedEntityInstance( String uid, User user )
    {
        TrackedEntityInstance tei = trackedEntityInstanceStore.getByUid( uid );
        addTrackedEntityInstanceAudit( tei, User.username( user ), AuditType.READ );

        return tei;
    }

    @Override
    @Transactional( readOnly = true )
    public boolean trackedEntityInstanceExists( String uid )
    {
        return trackedEntityInstanceStore.exists( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean trackedEntityInstanceExistsIncludingDeleted( String uid )
    {
        return trackedEntityInstanceStore.existsIncludingDeleted( uid );
    }

    @Override
    public List<String> getTrackedEntityInstancesUidsIncludingDeleted( List<String> uids )
    {
        return trackedEntityInstanceStore.getUidsIncludingDeleted( uids );
    }

    private boolean isLocalSearch( TrackedEntityInstanceQueryParams params, User user )
    {
        Set<OrganisationUnit> localOrgUnits = user.getOrganisationUnits();

        Set<OrganisationUnit> searchOrgUnits = new HashSet<>();

        if ( params.isOrganisationUnitMode( SELECTED ) )
        {
            searchOrgUnits = params.getOrganisationUnits();
        }
        else if ( params.isOrganisationUnitMode( CHILDREN ) || params.isOrganisationUnitMode( DESCENDANTS ) )
        {
            for ( OrganisationUnit ou : params.getOrganisationUnits() )
            {
                searchOrgUnits.addAll( ou.getChildren() );
            }
        }
        else if ( params.isOrganisationUnitMode( ALL ) )
        {
            searchOrgUnits.addAll( organisationUnitService.getRootOrganisationUnits() );
        }
        else
        {
            searchOrgUnits.addAll( user.getTeiSearchOrganisationUnitsWithFallback() );
        }

        for ( OrganisationUnit ou : searchOrgUnits )
        {
            if ( !organisationUnitService.isDescendant( ou, localOrgUnits ) )
            {
                return false;
            }
        }

        return true;
    }

    private void addTrackedEntityInstanceAudit( TrackedEntityInstance trackedEntityInstance, String user,
        AuditType auditType )
    {
        if ( user != null && trackedEntityInstance != null && trackedEntityInstance.getTrackedEntityType() != null
            && trackedEntityInstance.getTrackedEntityType().isAllowAuditLog() )
        {
            TrackedEntityInstanceAudit trackedEntityInstanceAudit = new TrackedEntityInstanceAudit(
                trackedEntityInstance.getUid(), user, auditType );
            trackedEntityInstanceAuditService.addTrackedEntityInstanceAudit( trackedEntityInstanceAudit );
        }
    }
}
