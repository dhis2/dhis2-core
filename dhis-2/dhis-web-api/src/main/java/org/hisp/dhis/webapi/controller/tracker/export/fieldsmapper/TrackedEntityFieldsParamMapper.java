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
package org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper;

import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.FIELD_ATTRIBUTES;
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.FIELD_EVENTS;
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.FIELD_RELATIONSHIPS;
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.rootFields;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceEnrollmentParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPreset;

public class TrackedEntityFieldsParamMapper
{
    private TrackedEntityFieldsParamMapper()
    {
    }

    private static final String FIELD_PROGRAM_OWNERS = "programOwners";

    private static final String FIELD_ENROLLMENTS = "enrollments";

    public static TrackedEntityInstanceParams map( List<FieldPath> fields )
    {
        Map<String, FieldPath> roots = rootFields( fields );

        TrackedEntityInstanceParams params = initUsingAllOrNoFields( roots );

        params = withFieldRelationships( roots, params );
        params = withFieldProgramOwners( roots, params );
        params = withFieldAttributes( roots, params );

        params = initNestedEnrollmentProperties( roots, params );

        params = withFieldEnrollmentsAndEvents( fields, roots, params );
        params = withFieldEnrollmentAndAttributes( fields, roots, params );
        params = withFieldEnrollmentAndRelationships( fields, roots, params );
        params = withFieldEventsAndRelationships( fields, roots, params );

        return params;
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
        return roots.containsKey( FIELD_RELATIONSHIPS )
            ? params.withIncludeRelationships( !roots.get( FIELD_RELATIONSHIPS ).isExclude() )
            : params;
    }

    private static TrackedEntityInstanceParams withFieldAttributes( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_ATTRIBUTES )
            ? params.withIncludeAttributes( !roots.get( FIELD_ATTRIBUTES ).isExclude() )
            : params;
    }

    private static TrackedEntityInstanceParams withFieldProgramOwners( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        return roots.containsKey( FIELD_PROGRAM_OWNERS )
            ? params.withIncludeProgramOwners( !roots.get( FIELD_PROGRAM_OWNERS ).isExclude() )
            : params;
    }

    private static TrackedEntityInstanceParams initNestedEnrollmentProperties( Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        if ( roots.containsKey( FIELD_ENROLLMENTS ) )
        {
            FieldPath p = roots.get( FIELD_ENROLLMENTS );
            params = params.withTeiEnrollmentParams( !p.isExclude() ? TrackedEntityInstanceEnrollmentParams.TRUE
                : TrackedEntityInstanceEnrollmentParams.FALSE );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldEnrollmentsAndEvents( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath events = fieldPathByNestedField( fieldPaths, FIELD_EVENTS, List.of( FIELD_ENROLLMENTS ) );

        if ( events == null )
        {
            return params;
        }

        if ( events.isExclude() )
        {
            return params.withTeiEnrollmentParams(
                params.getTeiEnrollmentParams().withIncludeEvents( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the events field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params.withTeiEnrollmentParams(
                params.getTeiEnrollmentParams().withIncludeEvents( !events.isExclude() ) );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldEnrollmentAndAttributes( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath attribute = fieldPathByNestedField( fieldPaths, FIELD_ATTRIBUTES,
            List.of( FIELD_ENROLLMENTS ) );

        if ( attribute == null )
        {
            return params;
        }

        if ( attribute.isExclude() )
        {
            return params.withTeiEnrollmentParams(
                params.getTeiEnrollmentParams().withIncludeAttributes( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the attributes field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params
                .withTeiEnrollmentParams(
                    params.getTeiEnrollmentParams().withIncludeAttributes( !attribute.isExclude() ) );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldEnrollmentAndRelationships( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath relationship = fieldPathByNestedField( fieldPaths, FIELD_RELATIONSHIPS,
            List.of( FIELD_ENROLLMENTS ) );

        if ( relationship == null )
        {
            return params;
        }

        if ( relationship.isExclude() )
        {
            return params.withEnrollmentParams(
                params.getEnrollmentParams().withIncludeRelationships( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the relationship field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params.withEnrollmentParams(
                params.getEnrollmentParams().withIncludeRelationships( !relationship.isExclude() ) );
        }
        return params;
    }

    private static TrackedEntityInstanceParams withFieldEventsAndRelationships( List<FieldPath> fieldPaths,
        Map<String, FieldPath> roots,
        TrackedEntityInstanceParams params )
    {
        FieldPath relationship = fieldPathByNestedField( fieldPaths, FIELD_RELATIONSHIPS,
            List.of( FIELD_ENROLLMENTS, FIELD_EVENTS ) );

        if ( relationship == null )
        {
            return params;
        }

        if ( relationship.isExclude() )
        {
            return params.withEventParams( params.getEventParams().withIncludeRelationships( false ) );
        }
        // since exclusion takes precedence if "!enrollments" we do not need to
        // check the relationship field value
        if ( roots.containsKey( FIELD_ENROLLMENTS ) && !roots.get( FIELD_ENROLLMENTS ).isExclude() )
        {
            return params
                .withEventParams( params.getEventParams().withIncludeRelationships( !relationship.isExclude() ) );
        }
        return params;
    }

    private static FieldPath fieldPathByNestedField( List<FieldPath> fieldPaths, String nestedField,
        List<String> fieldToMatch )
    {
        return fieldPaths.stream().filter( fp -> isNestedField( fp, nestedField, fieldToMatch ) && fp.isExclude() )
            .findFirst()
            .orElse( null );
    }

    private static boolean isNestedField( FieldPath fieldPath, String field, List<String> fieldToMatch )
    {
        return !fieldPath.isRoot() && field.equals( fieldPath.getName() )
            && fieldPath.getPath().equals( fieldToMatch );
    }
}
