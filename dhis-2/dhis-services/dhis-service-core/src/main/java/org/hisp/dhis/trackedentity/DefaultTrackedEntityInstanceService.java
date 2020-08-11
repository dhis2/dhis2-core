package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.common.*;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.*;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.*;

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

    private final ProgramService programService;

    private final OrganisationUnitService organisationUnitService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final TrackerOwnershipManager trackerOwnershipAccessManager;

    private final TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

    private final TrackedEntityAttributeValueAuditService attributeValueAuditService;

    // FIXME luciano using @Lazy here because we have circular dependencies:
    // TrackedEntityInstanceService --> TrackerOwnershipManager --> TrackedEntityProgramOwnerService --> TrackedEntityInstanceService
    public DefaultTrackedEntityInstanceService( TrackedEntityInstanceStore trackedEntityInstanceStore,
        TrackedEntityAttributeValueService attributeValueService, TrackedEntityAttributeService attributeService,
        TrackedEntityTypeService trackedEntityTypeService, ProgramService programService,
        OrganisationUnitService organisationUnitService, CurrentUserService currentUserService, AclService aclService,
        @Lazy TrackerOwnershipManager trackerOwnershipAccessManager,
        @Lazy TrackedEntityInstanceAuditService trackedEntityInstanceAuditService,
        @Lazy TrackedEntityAttributeValueAuditService attributeValueAuditService )
    {
        checkNotNull( trackedEntityInstanceStore );
        checkNotNull( attributeValueService );
        checkNotNull( attributeService );
        checkNotNull( trackedEntityTypeService );
        checkNotNull( programService );
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
        this.programService = programService;
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
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation )
    {
        if ( params.isOrQuery() && !params.hasAttributes() && !params.hasProgram() )
        {
            Collection<TrackedEntityAttribute> attributes = attributeService.getTrackedEntityAttributesDisplayInListNoProgram();
            params.addAttributes( QueryItem.getQueryItems( attributes ) );
            params.addFiltersIfNotExist( QueryItem.getQueryItems( attributes ) );
        }

        decideAccess( params );
        //AccessValidation should be skipped only and only if it is internal service that runs the task (for example sync job)
        if ( !skipAccessValidation )
        {
            validate( params );
        }

        User user = currentUserService.getCurrentUser();

        params.setUser( user );

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        params.handleCurrentUserSelectionMode();

        List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceStore.getTrackedEntityInstances( params );

        trackedEntityInstances = trackedEntityInstances.stream()
            .filter( (tei) -> aclService.canDataRead( user, tei.getTrackedEntityType() ) )
            .collect( Collectors.toList() );

        //Avoiding NullPointerException
        String accessedBy = user != null ? user.getUsername() : currentUserService.getCurrentUsername();

        for ( TrackedEntityInstance tei : trackedEntityInstances )
        {
            addTrackedEntityInstanceAudit( tei, accessedBy, AuditType.SEARCH );
        }

        return trackedEntityInstances;
    }

    @Override
    @Transactional( readOnly = true )
    public int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation, boolean skipSearchScopeValidation )
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

        return trackedEntityInstanceStore.countTrackedEntityInstances( params );
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

        if ( params.isIncludeDeleted() )
        {
            grid.addHeader( new GridHeader( DELETED, "Deleted", ValueType.BOOLEAN, "boolean", false, false ) );
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
            trackedEntityTypes.put( params.getProgram().getTrackedEntityType().getUid(), params.getProgram().getTrackedEntityType() );
        }

        Set<String> tes = new HashSet<>();

        for ( Map<String, String> entity : entities )
        {
            if ( params.getUser() != null && !params.getUser().isSuper() && params.hasProgram() &&
                (params.getProgram().getAccessLevel().equals( AccessLevel.PROTECTED ) ||
                    params.getProgram().getAccessLevel().equals( AccessLevel.CLOSED )) )
            {
                TrackedEntityInstance tei = trackedEntityInstanceStore.getByUid( entity.get( TRACKED_ENTITY_INSTANCE_ID ) );

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
                TrackedEntityInstanceAudit trackedEntityInstanceAudit = new TrackedEntityInstanceAudit( entity.get( TRACKED_ENTITY_INSTANCE_ID ), accessedBy, AuditType.SEARCH );
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
     * - query: add display in list attributes
     * - attributes
     * - program: add program attributes
     * - query + attributes
     * - query + program: add program attributes
     * - attributes + program
     * - query + attributes + program
     */
    private void handleAttributes( TrackedEntityInstanceQueryParams params )
    {
        if ( params.isOrQuery() && !params.hasAttributes() && !params.hasProgram() )
        {
            Collection<TrackedEntityAttribute> attributes = attributeService.getTrackedEntityAttributesDisplayInListNoProgram();
            params.addAttributes( QueryItem.getQueryItems( attributes ) );
            params.addFiltersIfNotExist( QueryItem.getQueryItems( attributes ) );
        }
        else if ( params.hasProgram() )
        {
            params.addAttributesIfNotExist( QueryItem.getQueryItems( params.getProgram().getTrackedEntityAttributes() ) );
        }
        else if ( params.hasTrackedEntityType() )
        {
            params.addAttributesIfNotExist( QueryItem.getQueryItems( params.getTrackedEntityType().getTrackedEntityAttributes() ) );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public void decideAccess( TrackedEntityInstanceQueryParams params )
    {
        User user = params.isInternalSearch() ? null : params.getUser();

        if ( params.isOrganisationUnitMode( ALL ) &&
            !currentUserService.currentUserIsAuthorized( Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name() ) &&
            !params.isInternalSearch() )
        {
            throw new IllegalQueryException( "Current user is not authorized to query across all organisation units" );
        }

        if ( params.hasProgram() )
        {
            if ( !aclService.canDataRead( user, params.getProgram() ) )
            {
                throw new IllegalQueryException( "Current user is not authorized to read data from selected program:  " + params.getProgram().getUid() );
            }

            if ( params.getProgram().getTrackedEntityType() != null && !aclService.canDataRead( user, params.getProgram().getTrackedEntityType() ) )
            {
                throw new IllegalQueryException( "Current user is not authorized to read data from selected program's tracked entity type:  " + params.getProgram().getTrackedEntityType().getUid() );
            }

        }

        if ( params.hasTrackedEntityType() && !aclService.canDataRead( user, params.getTrackedEntityType() ) )
        {
            throw new IllegalQueryException( "Current user is not authorized to read data from selected tracked entity type:  " + params.getTrackedEntityType().getUid() );
        }
        else
        {
            params.setTrackedEntityTypes( trackedEntityTypeService.getAllTrackedEntityType().stream()
                .filter( tet -> aclService.canDataRead( user, tet ) )
                .collect( Collectors.toList() )
            );
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

        if ( !params.hasOrganisationUnits() && !(params.isOrganisationUnitMode( ALL ) || params.isOrganisationUnitMode( ACCESSIBLE ) || params.isOrganisationUnitMode( CAPTURE )) )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( params.isOrganisationUnitMode( ACCESSIBLE ) && (user == null || !user.hasDataViewOrganisationUnitWithFallback()) )
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

        if ( user.getOrganisationUnits().isEmpty() )
        {
            throw new IllegalQueryException( "User need to be associated with at least one organisation unit." );
        }

        if ( !params.hasProgram() && !params.hasTrackedEntityType() && params.hasAttributesOrFilters() && !params.hasOrganisationUnits() )
        {
            List<String> uniqeAttributeIds = attributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream().map( TrackedEntityAttribute::getUid ).collect( Collectors.toList() );

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
                    searchableAttributeIds.addAll( attributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream().map( TrackedEntityAttribute::getUid ).collect( Collectors.toList() ) );
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
                    throw new IllegalQueryException( "Non-searchable attribute(s) can not be used during global search:  " + violatingAttributes.toString() );
                }
            }

            if ( params.hasProgram() )
            {
                maxTeiLimit = params.getProgram().getMaxTeiCountToReturn();

                if ( isProgramMinAttributesViolated( params ) )
                {
                    throw new IllegalQueryException( "At least " + params.getProgram().getMinAttributesRequiredToSearch() + " attributes should be mentioned in the search criteria." );
                }
            }

            if ( params.hasTrackedEntityType() )
            {
                maxTeiLimit = params.getTrackedEntityType().getMaxTeiCountToReturn();

                if ( isTeTypeMinAttributesViolated( params ) )
                {
                    throw new IllegalQueryException( "At least " + params.getTrackedEntityType().getMinAttributesRequiredToSearch() + " attributes should be mentioned in the search criteria." );
                }
            }

            if ( maxTeiLimit > 0 &&
                ((isGridSearch && trackedEntityInstanceStore.getTrackedEntityInstanceCountForGrid( params ) > maxTeiLimit) ||
                    (!isGridSearch && trackedEntityInstanceStore.countTrackedEntityInstances( params ) > maxTeiLimit)) )
            {
                throw new IllegalQueryException( "maxteicountreached" );
            }
        }
    }

    private boolean isProgramMinAttributesViolated( TrackedEntityInstanceQueryParams params )
    {
        if ( params.hasUniqueFilters() )
        {
            return false;
        }

        return (!params.hasFilters() && !params.hasAttributes() && params.getProgram().getMinAttributesRequiredToSearch() > 0)
            || (params.hasFilters() && params.getFilters().size() < params.getProgram().getMinAttributesRequiredToSearch())
            || (params.hasAttributes() && params.getAttributes().size() < params.getProgram().getMinAttributesRequiredToSearch());
    }

    private boolean isTeTypeMinAttributesViolated( TrackedEntityInstanceQueryParams params )
    {
        if ( params.hasUniqueFilters() )
        {
            return false;
        }

        return (!params.hasFilters() && !params.hasAttributes() && params.getTrackedEntityType().getMinAttributesRequiredToSearch() > 0)
            || (params.hasFilters() && params.getFilters().size() < params.getTrackedEntityType().getMinAttributesRequiredToSearch())
            || (params.hasAttributes() && params.getAttributes().size() < params.getTrackedEntityType().getMinAttributesRequiredToSearch());
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityInstanceQueryParams getFromUrl( String query, Set<String> attribute, Set<String> filter,
        Set<String> ou, OrganisationUnitSelectionMode ouMode, String program, ProgramStatus programStatus,
        Boolean followUp, Date lastUpdatedStartDate, Date lastUpdatedEndDate, String lastUpdatedDuration,
        Date programEnrollmentStartDate, Date programEnrollmentEndDate, Date programIncidentStartDate,
        Date programIncidentEndDate, String trackedEntityType, String programStage, EventStatus eventStatus, Date eventStartDate,
        Date eventEndDate, AssignedUserSelectionMode assignedUserSelectionMode, Set<String> assignedUsers,
        boolean skipMeta, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging,
        boolean includeDeleted, boolean includeAllAttributes, List<String> orders )
    {
        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        Set<OrganisationUnit> possibleSearchOrgUnits = new HashSet<>();

        User user = currentUserService.getCurrentUser();

        if ( user != null )
        {
            possibleSearchOrgUnits = user.getTeiSearchOrganisationUnitsWithFallback();
        }

        QueryFilter queryFilter = getQueryFilter( query );

        if ( attribute != null )
        {
            for ( String attr : attribute )
            {
                QueryItem it = getQueryItem( attr );

                params.getAttributes().add( it );
            }
        }

        if ( filter != null )
        {
            for ( String filt : filter )
            {
                QueryItem it = getQueryItem( filt );

                params.getFilters().add( it );
            }
        }

        if ( ou != null )
        {
            for ( String orgUnit : ou )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnit );

                if ( organisationUnit == null )
                {
                    throw new IllegalQueryException( "Organisation unit does not exist: " + orgUnit );
                }

                if ( !organisationUnitService.isInUserHierarchy( organisationUnit.getUid(), possibleSearchOrgUnits ) )
                {
                    throw new IllegalQueryException( "Organisation unit is not part of the search scope: " + orgUnit );
                }

                params.getOrganisationUnits().add( organisationUnit );
            }
        }

        Program pr = program != null ? programService.getProgram( program ) : null;

        if ( program != null && pr == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + program );
        }
        
        ProgramStage ps = programStage != null ? getProgramStageFromProgram( pr, programStage ) : null;
       
        if ( programStage != null && ps == null )
        {
            throw new IllegalQueryException( "Program does not contain the specified programStage: " + programStage );
        }

        TrackedEntityType te = trackedEntityType != null ? trackedEntityTypeService.getTrackedEntityType( trackedEntityType ) : null;

        if ( trackedEntityType != null && te == null )
        {
            throw new IllegalQueryException( "Tracked entity type does not exist: " + trackedEntityType );
        }

        if ( ouMode == OrganisationUnitSelectionMode.CAPTURE && user != null )
        {
            params.getOrganisationUnits().addAll( user.getOrganisationUnits() );
        }

        if ( assignedUserSelectionMode != null && assignedUsers != null && !assignedUsers.isEmpty()
            && !assignedUserSelectionMode.equals( AssignedUserSelectionMode.PROVIDED ) )
        {
            throw new IllegalQueryException( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
        }

        if ( assignedUsers != null )
        {
            assignedUsers = assignedUsers.stream()
                .filter( CodeGenerator::isValidUid )
                .collect( Collectors.toSet() );
        }

        params.setQuery( queryFilter )
            .setProgram( pr )
            .setProgramStatus( programStatus )
            .setFollowUp( followUp )
            .setLastUpdatedStartDate( lastUpdatedStartDate )
            .setLastUpdatedEndDate( lastUpdatedEndDate )
            .setLastUpdatedDuration( lastUpdatedDuration )
            .setProgramEnrollmentStartDate( programEnrollmentStartDate )
            .setProgramEnrollmentEndDate( programEnrollmentEndDate )
            .setProgramIncidentStartDate( programIncidentStartDate )
            .setProgramIncidentEndDate( programIncidentEndDate )
            .setTrackedEntityType( te )
            .setOrganisationUnitMode( ouMode )
            .setProgramStage( ps )
            .setEventStatus( eventStatus )
            .setEventStartDate( eventStartDate )
            .setEventEndDate( eventEndDate )
            .setAssignedUserSelectionMode( assignedUserSelectionMode )
            .setAssignedUsers( assignedUsers )
            .setSkipMeta( skipMeta )
            .setPage( page )
            .setPageSize( pageSize )
            .setTotalPages( totalPages )
            .setSkipPaging( skipPaging )
            .setIncludeDeleted( includeDeleted )
            .setIncludeAllAttributes( includeAllAttributes )
            .setUser( user )
            .setOrders( orders );

        return params;
    }

    private ProgramStage getProgramStageFromProgram( Program pr, String programStage )
    {
        if ( pr == null )
        {
            return null;
        }
        
        return pr.getProgramStages().stream().filter( pstage-> pstage.getUid().equals( programStage ) ).findFirst().orElse( null );
    }

    /**
     * Creates a QueryItem from the given item string. Item is on format
     * {attribute-id}:{operator}:{filter-value}[:{operator}:{filter-value}].
     * Only the attribute-id is mandatory.
     */
    private QueryItem getQueryItem( String item )
    {
        String[] split = item.split( DimensionalObject.DIMENSION_NAME_SEP );

        if ( split.length % 2 != 1 )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + item );
        }

        QueryItem queryItem = getItem( split[0] );

        if ( split.length > 1 ) // Filters specified
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                queryItem.getFilters().add( new QueryFilter( operator, split[i + 1] ) );
            }
        }

        return queryItem;
    }

    private QueryItem getItem( String item )
    {
        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( item );

        if ( at == null )
        {
            throw new IllegalQueryException( "Attribute does not exist: " + item );
        }

        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(), at.isUnique() );
    }

    /**
     * Creates a QueryFilter from the given query string. Query is on format
     * {operator}:{filter-value}. Only the filter-value is mandatory. The EQ
     * QueryOperator is used as operator if not specified.
     */
    private QueryFilter getQueryFilter( String query )
    {
        if ( query == null || query.isEmpty() )
        {
            return null;
        }

        if ( !query.contains( DimensionalObject.DIMENSION_NAME_SEP ) )
        {
            return new QueryFilter( QueryOperator.EQ, query );
        }
        else
        {
            String[] split = query.split( DimensionalObject.DIMENSION_NAME_SEP );

            if ( split.length != 2 )
            {
                throw new IllegalQueryException( "Query has invalid format: " + query );
            }

            QueryOperator op = QueryOperator.fromString( split[0] );

            return new QueryFilter( op, split[1] );
        }
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
    public long createTrackedEntityInstance( TrackedEntityInstance instance, Set<TrackedEntityAttributeValue> attributeValues )
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
        return trackedEntityInstanceStore.getTrackedEntityInstancesByUid( uids, user );
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
    public void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs, Date lastSynchronized )
    {
        trackedEntityInstanceStore.updateTrackedEntityInstancesSyncTimestamp( trackedEntityInstanceUIDs, lastSynchronized );
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
            if ( !ou.isDescendant( localOrgUnits ) )
            {
                return false;
            }
        }

        return true;
    }

    private void addTrackedEntityInstanceAudit( TrackedEntityInstance trackedEntityInstance, String user, AuditType auditType )
    {
        if ( user != null && trackedEntityInstance != null && trackedEntityInstance.getTrackedEntityType() != null && trackedEntityInstance.getTrackedEntityType().isAllowAuditLog() )
        {
            TrackedEntityInstanceAudit trackedEntityInstanceAudit = new TrackedEntityInstanceAudit( trackedEntityInstance.getUid(), user, auditType );
            trackedEntityInstanceAuditService.addTrackedEntityInstanceAudit( trackedEntityInstanceAudit );
        }
    }
}
