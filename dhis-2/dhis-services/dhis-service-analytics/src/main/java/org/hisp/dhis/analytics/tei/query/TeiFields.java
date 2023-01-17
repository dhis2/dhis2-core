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
package org.hisp.dhis.analytics.tei.query;

import static java.util.Arrays.stream;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;

public class TeiFields
{
    private interface HeaderProvider
    {
        String getAlias();

        String getFullName();

        ValueType getType();
    }

    @Getter
    @RequiredArgsConstructor
    private enum Dynamic implements HeaderProvider
    {
        ENROLLMENTS( "(select JSON_AGG(JSON_BUILD_OBJECT('programUid', p_0.uid," +
            " 'programInstanceUid', pi_0.uid, 'enrollmentDate', pi_0.enrollmentdate," +
            " 'incidentDate', pi_0.incidentdate,'endDate', pi_0.enddate, 'events'," +
            " (select JSON_AGG(JSON_BUILD_OBJECT('programStageUid', ps_0.uid," +
            " 'programStageInstanceUid', psi_0.uid, 'executionDate', psi_0.executiondate," +
            " 'dueDate', psi_0.duedate, 'eventDataValues', psi_0.eventdatavalues))" +
            " from programstageinstance psi_0, programstage ps_0" +
            " where psi_0.programinstanceid = pi_0.programinstanceid" +
            " and ps_0.programstageid = psi_0.programstageid)))" +
            " from programinstance pi_0, program p_0" +
            " where pi_0.trackedentityinstanceid = " + TEI_ALIAS + ".trackedentityinstanceid" +
            " and p_0.programid = pi_0.programid)", "enrollments", "Enrollments", TEXT );

        private final String query;

        private final String alias;

        private final String fullName;

        private final ValueType type;

    }

    @Getter
    @RequiredArgsConstructor
    private enum Static implements HeaderProvider
    {
        TRACKED_ENTITY_INSTANCE( "trackedentityinstanceuid", "Tracked Entity Instance", TEXT ),
        LAST_UPDATED( "lastupdated", "Last Updated", DATETIME ),
        CREATED_BY_DISPLAY_NAME( "createdbydisplayname", "Created by (display name)", TEXT ),
        LAST_UPDATED_BY_DISPLAY_NAME( "lastupdatedbydisplayname", "Last updated by (display name)", TEXT ),
        GEOMETRY( "geometry", "Geometry", TEXT ),
        LONGITUDE( "longitude", "Longitude", NUMBER ),
        LATITUDE( "latitude", "Latitude", NUMBER ),
        ORG_UNIT_NAME( "ouname", "Organisation unit name", TEXT ),
        ORG_UNIT_CODE( "oucode", "Organisation unit code", TEXT );

        private final String alias;

        private final String fullName;

        private final ValueType type;
    }

    public static Stream<Field> getDimensionFields( final TeiQueryParams teiQueryParams )
    {
        return getAllAttributes( teiQueryParams )
            .map( attr -> Field.of( TEI_ALIAS, () -> attr, attr ) );
    }

    public static Stream<String> getAllAttributes( final TeiQueryParams teiQueryParams )
    {
        Stream<TrackedEntityAttribute> trackedEntityAttributesFromType = getTrackedEntityAttributes(
            teiQueryParams.getTrackedEntityType() );

        Stream<TrackedEntityAttribute> programAttributes = getProgramAttributes(
            teiQueryParams.getCommonParams().getPrograms() );

        // TET and program attribute fields
        return Stream.concat( trackedEntityAttributesFromType, programAttributes )
            .map( BaseIdentifiableObject::getUid )
            // distinct to remove overlapping attributes
            .distinct();
    }

    public static Stream<TrackedEntityAttribute> getProgramAttributes( Collection<Program> programs )
    {
        // Attributes from Programs
        return programs.stream()
            .map( Program::getProgramAttributes )
            .flatMap( Collection::stream )
            .map( ProgramTrackedEntityAttribute::getAttribute );
    }

    public static Stream<TrackedEntityAttribute> getTrackedEntityAttributes( TrackedEntityType trackedEntityType )
    {
        // Attributes from Tracked entity Type
        return trackedEntityType.getTrackedEntityAttributes().stream();
    }

    private static Stream<Field> getStaticFields()
    {
        return Stream.of( Static.values() ).map( v -> v.alias ).map( a -> Field.of( TEI_ALIAS, () -> a, a ) );
    }

    private static Stream<Field> getDynamicFields()
    {
        return Stream.of( Dynamic.values() )
            .map( dynamic -> Field.ofUnquotedField( EMPTY, () -> dynamic.query, dynamic.alias ) );
    }

    public static Stream<Field> getTeiFields()
    {
        return Stream.concat( getStaticFields(), getDynamicFields() );
    }

    public static Set<GridHeader> getGridHeaders( final TeiQueryParams teiQueryParams )
    {
        final Set<GridHeader> headers = new LinkedHashSet<>();

        // Adding static and dynamic headers.
        Stream.concat( stream( Static.values() ), stream( Dynamic.values() ) )
            .forEach( f -> headers.add( new GridHeader( f.getAlias(), f.getFullName(), f.getType(), false, true ) ) );

        getDimensionFields( teiQueryParams )
            .map( field -> findDimensionIdentifier( teiQueryParams, field ) )
            .map( TeiFields::getHeaderForField )
            .forEach( headers::add );

        return headers;
    }

    private static GridHeader getHeaderForField(
        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier )
    {
        String uid = dimensionIdentifier.getDimension().getUid();
        String name = dimensionIdentifier.getDimension().getName();
        ValueType valueType = dimensionIdentifier.getDimension().getValueType();
        return new GridHeader( uid, name, valueType, false, true );
    }

    private static DimensionIdentifier<Program, ProgramStage, DimensionParam> findDimensionIdentifier(
        TeiQueryParams teiQueryParams, Field field )
    {
        return teiQueryParams.getCommonParams().getDimensionIdentifiers().stream()
            .flatMap( Collection::stream )
            .filter( di -> di.getDimension().getUid().equals( field.getFieldAlias() ) )
            .findFirst()
            .orElseThrow( IllegalStateException::new );
    }
}
