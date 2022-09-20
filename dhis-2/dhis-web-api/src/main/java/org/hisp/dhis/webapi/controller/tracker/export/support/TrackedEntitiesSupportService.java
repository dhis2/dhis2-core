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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class TrackedEntitiesSupportService
{

    private static final String FIELD_ENROLLMENTS = "enrollments";

    private static final String FIELD_RELATIONSHIPS = "relationships";

    private static final String FIELD_PROGRAM_OWNERS = "programOwners";

    private static final String FIELD_EVENTS = "events";

    @NonNull
    private final TrackedEntityInstanceService trackedEntityInstanceService;

    @NonNull
    private final CurrentUserService currentUserService;

    @NonNull
    private final ProgramService programService;

    @NonNull
    private final TrackerAccessManager trackerAccessManager;

    @NonNull
    private final org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

    @NonNull
    private final TrackedEntityTypeService trackedEntityTypeService;

    @SneakyThrows
    public TrackedEntityInstance getTrackedEntityInstance( String id, String pr, List<String> fields )
    {
        User user = currentUserService.getCurrentUser();

        TrackedEntityInstanceParams trackedEntityInstanceParams = getTrackedEntityInstanceParams( fields );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( id,
            trackedEntityInstanceParams );

        if ( trackedEntityInstance == null )
        {
            throw new NotFoundException( "TrackedEntityInstance", id );
        }

        if ( pr != null )
        {
            Program program = programService.getProgram( pr );

            if ( program == null )
            {
                throw new NotFoundException( "Program", pr );
            }

            List<String> errors = trackerAccessManager.canRead( user,
                instanceService.getTrackedEntityInstance( trackedEntityInstance.getTrackedEntityInstance() ), program,
                false );

            if ( !errors.isEmpty() )
            {
                if ( program.getAccessLevel() == AccessLevel.CLOSED )
                {
                    throw new WebMessageException(
                        unauthorized( TrackerOwnershipManager.PROGRAM_ACCESS_CLOSED ) );
                }
                throw new WebMessageException(
                    unauthorized( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED ) );
            }

            if ( trackedEntityInstanceParams.isIncludeProgramOwners() )
            {
                List<ProgramOwner> filteredProgramOwners = trackedEntityInstance.getProgramOwners().stream()
                    .filter( tei -> tei.getProgram().equals( pr ) ).collect( Collectors.toList() );
                trackedEntityInstance.setProgramOwners( filteredProgramOwners );
            }
        }
        else
        {
            // return only tracked entity type attributes

            TrackedEntityType trackedEntityType = trackedEntityTypeService
                .getTrackedEntityType( trackedEntityInstance.getTrackedEntityType() );

            if ( trackedEntityType != null )
            {
                List<String> tetAttributes = trackedEntityType.getTrackedEntityAttributes().stream()
                    .map( TrackedEntityAttribute::getUid ).collect( Collectors.toList() );

                trackedEntityInstance.setAttributes( trackedEntityInstance.getAttributes().stream()
                    .filter( att -> tetAttributes.contains( att.getAttribute() ) ).collect( Collectors.toList() ) );
            }
        }

        return trackedEntityInstance;
    }

    /**
     * Parse the fields query parameter values to determine which resources
     * should be fetched from the DB. The
     * {@link org.hisp.dhis.fieldfiltering.FieldFilterService} is used to filter
     * the response before serializing it into JSON. Since exclusions take
     * precedences over inclusions in the FieldFilterService do so here as well.
     * For example "enrollments", "!enrollments" will lead to
     * {@link TrackedEntityInstanceParams#isIncludeEnrollments()} being false.
     *
     * @param fields fields query parameter values
     * @return tracked entity instance parameters
     */
    public static TrackedEntityInstanceParams getTrackedEntityInstanceParams( List<String> fields )
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Collections.singleton( StringUtils.join( fields, "," ) ) );
        Map<String, FieldPath> roots = rootFields( fieldPaths );

        TrackedEntityInstanceParams params = initUsingAllOrNoFields( roots );
        params = withFieldRelationships( roots, params );
        params = withFieldEnrollmentsAndEvents( roots, params );
        params = withFieldProgramOwners( roots, params );
        params = withFieldEvents( fieldPaths, roots, params );

        return params;
    }

    private static Map<String, FieldPath> rootFields( List<FieldPath> fieldPaths )
    {

        Map<String, FieldPath> roots = new HashMap<>();
        for ( FieldPath p : fieldPaths )
        {
            if ( p.isRoot() && (!roots.containsKey( p.getName() ) || p.isExclude()) )
            {
                roots.put( p.getName(), p );
            }
        }
        return roots;
    }

    private static TrackedEntityInstanceParams initUsingAllOrNoFields( Map<String, FieldPath> roots )
    {

        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        if ( roots.containsKey( FieldPreset.ALL ) )
        {
            FieldPath p = roots.get( FieldPreset.ALL );
            if ( p.isRoot() && !p.isExclude() )
            {
                params = TrackedEntityInstanceParams.TRUE;
            }
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldRelationships( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {

        if ( roots.containsKey( FIELD_RELATIONSHIPS ) )
        {
            params = params.withIncludeRelationships( !roots.get( FIELD_RELATIONSHIPS ).isExclude() );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldEnrollmentsAndEvents( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {

        if ( roots.containsKey( FIELD_ENROLLMENTS ) )
        {
            FieldPath p = roots.get( FIELD_ENROLLMENTS );
            params = params.withIncludeEnrollments( !p.isExclude() );
            // since its enrollments.events initialize it using the enrollments
            // field value
            params = params.withIncludeEvents( !p.isExclude() );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldProgramOwners( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {

        if ( roots.containsKey( FIELD_PROGRAM_OWNERS ) )
        {
            params = params.withIncludeProgramOwners( !roots.get( FIELD_PROGRAM_OWNERS ).isExclude() );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldEvents( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {

        // since its enrollments.events, we have to take the enrollments root
        // and its child events into account
        FieldPath events = null;
        for ( FieldPath p : fieldPaths )
        {
            // exclusion takes precedence over inclusion
            if ( isEnrollmentEventsField( p ) && (events == null || p.isExclude()) )
            {
                events = p;
            }
        }
        if ( events == null )
        {
            return params;
        }

        if ( events.isExclude() )
        {
            return params.withIncludeEvents( false );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the events field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params.withIncludeEvents( !events.isExclude() );
        }
        return params;
    }

    /**
     * @param path field path to check
     * @return true if field is enrollments.events
     */
    private static boolean isEnrollmentEventsField( FieldPath path )
    {
        return !path.isRoot() && FIELD_EVENTS.equals( path.getName() )
            && path.getPath().get( 0 ).equals( FIELD_ENROLLMENTS );
    }
}
