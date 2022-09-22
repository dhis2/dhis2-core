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
package org.hisp.dhis.webapi.controller.tracker.export.support;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.dxf2.events.params.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
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
public class TrackedEntitiesSupportService extends InstanceFieldsSupportService
{
    protected static final String FIELD_PROGRAM_OWNERS = "programOwners";

    protected static final String FIELD_ENROLLMENTS = "enrollments";

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

        TrackedEntityInstanceParams trackedEntityInstanceParams = getInstanceParams( fields );

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

    public TrackedEntityInstanceParams getInstanceParams( List<String> fields )
    {
        List<FieldPath> fieldPaths = getFieldPaths( fields );

        Map<String, FieldPath> roots = rootFields( fieldPaths );

        TrackedEntityInstanceParams params = initUsingAllOrNoFields( roots );

        params = withFieldRelationships( roots, params );
        params = withFieldProgramOwners( roots, params );
        params = withFieldAttributes( roots, params );

        params = initNestedEnrollmentProperties( roots, params );

        params = withFieldEnrollmentsAndEvents( fieldPaths, roots, params );
        params = withFieldEnrollmentAndAttributes( fieldPaths, roots, params );
        params = withFieldEnrollmentAndRelationships( fieldPaths, roots, params );

        return params;
    }

    private TrackedEntityInstanceParams initUsingAllOrNoFields( Map<String, FieldPath> roots )
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

    private TrackedEntityInstanceParams withFieldRelationships( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_RELATIONSHIPS )
            ? params.withIncludeRelationships( !roots.get( FIELD_RELATIONSHIPS ).isExclude() )
            : params;
    }

    private TrackedEntityInstanceParams withFieldAttributes( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_ATTRIBUTES )
            ? params.withIncludeAttributes( !roots.get( FIELD_ATTRIBUTES ).isExclude() )
            : params;
    }

    private TrackedEntityInstanceParams withFieldProgramOwners( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_PROGRAM_OWNERS )
            ? params.withIncludeProgramOwners( !roots.get( FIELD_PROGRAM_OWNERS ).isExclude() )
            : params;
    }

    private TrackedEntityInstanceParams initNestedEnrollmentProperties( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        if ( roots.containsKey( FIELD_ENROLLMENTS ) )
        {
            FieldPath p = roots.get( FIELD_ENROLLMENTS );
            params = params.withEnrollmentParams( params.getEnrollmentParams().withIncludeRoot( !p.isExclude() ) );
            params = params.withEnrollmentParams( params.getEnrollmentParams().withIncludeEvents( !p.isExclude() ) );
            params = params
                .withEnrollmentParams( params.getEnrollmentParams().withIncludeAttributes( !p.isExclude() ) );
            params = params
                .withEnrollmentParams( params.getEnrollmentParams().withIncludeRelationships( !p.isExclude() ) );
        }
        return params;
    }

    private TrackedEntityInstanceParams withFieldEnrollmentsAndEvents( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath events = getFieldPath( fieldPaths, FIELD_EVENTS );

        if ( events == null )
        {
            return params;
        }

        if ( events.isExclude() )
        {
            return params.withEnrollmentParams( params.getEnrollmentParams().withIncludeEvents( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the events field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params.withEnrollmentParams( params.getEnrollmentParams().withIncludeEvents( !events.isExclude() ) );
        }
        return params;
    }

    protected TrackedEntityInstanceParams withFieldEnrollmentAndAttributes( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath attribute = getFieldPath( fieldPaths, FIELD_ATTRIBUTES );

        if ( attribute == null )
        {
            return params;
        }

        if ( attribute.isExclude() )
        {
            return params.withEnrollmentParams( params.getEnrollmentParams().withIncludeAttributes( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the attributes field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params
                .withEnrollmentParams( params.getEnrollmentParams().withIncludeAttributes( !attribute.isExclude() ) );
        }
        return params;
    }

    protected TrackedEntityInstanceParams withFieldEnrollmentAndRelationships( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath relationship = getFieldPath( fieldPaths, FIELD_RELATIONSHIPS );

        if ( relationship == null )
        {
            return params;
        }

        if ( relationship.isExclude() )
        {
            return params.withEnrollmentParams( params.getEnrollmentParams().withIncludeRelationships( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the relationship field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params
                .withEnrollmentParams(
                    params.getEnrollmentParams().withIncludeRelationships( !relationship.isExclude() ) );
        }
        return params;
    }

    private FieldPath getFieldPath( List<FieldPath> fieldPaths, String field )
    {
        return fieldPaths.stream().filter( fp -> isEnrollmentField( fp, field ) && fp.isExclude() ).findFirst()
            .orElse( null );
    }

    private boolean isEnrollmentField( FieldPath path, String field )
    {
        return !path.isRoot() && field.equals( path.getName() )
            && path.getPath().get( 0 ).equals( FIELD_ENROLLMENTS );
    }

}
