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
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.TeiFields.Static.values;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.shared.query.Field;
import org.hisp.dhis.analytics.shared.query.RenderableDimensionIdentifier;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;

public class TeiFields
{
    public enum Static
    {
        TRACKED_ENTITY_INSTANCE( "trackedentityinstanceuid", "Tracked Entity Instance", TEXT ),
        TRACKER_ENTITY_TYPE( "trackedentitytypeuid", "Tracked Entity Type", TEXT ),
        LAST_UPDATED( "lastupdated", "Last Updated", DATETIME ),
        CREATED_BY_DISPLAY_NAME( "createdbydisplayname", "Created by (display name)", TEXT ),
        LAST_UPDATED_BY_DISPLAY_NAME( "lastupdatedbydisplayname", "Last updated by (display name)", TEXT ),
        GEOMETRY( "geometry", "Geometry", TEXT ),
        LONGITUDE( "longitude", "Longitude", NUMBER ),
        LATITUDE( "latitude", "Latitude", NUMBER ),
        ORG_UNIT_NAME( "ouname", "Organisation unit name", TEXT ),
        ORG_UNIT_CODE( "oucode", "Organisation unit code", TEXT ),
        ENROLLMENTS( "enrollments", "Enrollments", TEXT );

        private final String alias;

        private final String fullName;

        private final ValueType type;

        Static( final String alias, final String fullName, final ValueType type )
        {
            this.alias = alias;
            this.fullName = fullName;
            this.type = type;
        }

        public String alias()
        {
            return alias;
        }

        public String fullName()
        {
            return fullName;
        }

        public ValueType type()
        {
            return type;
        }
    }

    public static Stream<Field> getDimensionFields( final TeiQueryParams teiQueryParams )
    {
        // TODO remove next line when attributes match those in the table.
        final List<String> notGeneratedColumns = List.of( "Jdd8hMStmvF", "EGn5VqU7pHv", "JBJ3AWsrg9P" );

        // TET and program attribute fields
        return Stream.concat(
            // Tracked entity Type attributes
            teiQueryParams.getTrackedEntityType().getTrackedEntityTypeAttributes().stream(),
            // Program attributes
            teiQueryParams.getCommonParams().getPrograms().stream()
                .map( Program::getProgramAttributes )
                .flatMap( Collection::stream )
                .map( ProgramTrackedEntityAttribute::getAttribute ) )
            .map( BaseIdentifiableObject::getUid )
            // distinct to remove overlapping attributes
            .distinct()
            // TODO remove next line when attributes match those in the table.
            .filter( uid -> !notGeneratedColumns.contains( uid ) )
            .map( attributeUid -> "\"" + attributeUid + "\"" )
            .map( a -> Field.of( TEI_ALIAS, () -> a, a ) );
    }

    public static Stream<Field> getOrderingFields( final TeiQueryParams teiQueryParams )
    {
        return teiQueryParams.getCommonParams().getOrderParams()
            .stream()
            .map( AnalyticsSortingParams::getOrderBy )
            .map( RenderableDimensionIdentifier::of )
            .map( RenderableDimensionIdentifier::render )
            .map( s -> Field.of( EMPTY, () -> "\"" + s + "\".VALUE", "VALUE" ) );
    }

    public static Stream<Field> getStaticFields()
    {
        return Stream.of( Static.values() ).map( v -> v.alias ).map( a -> Field.of( EMPTY, () -> a, a ) );
    }

    public static Set<GridHeader> getGridHeaders( final TeiQueryParams teiQueryParams )
    {
        final Set<GridHeader> headers = new LinkedHashSet<>();

        // Adding static headers.
        stream( values() )
            .forEach( f -> headers.add( new GridHeader( f.alias(), f.fullName(), f.type(), false, true ) ) );

        // TODO: Map the correct columns alias, names and types.
        getDimensionFields( teiQueryParams ).forEach(
            f -> headers.add( new GridHeader( f.getFieldAlias(), "", TEXT, false, true ) ) );

        return headers;
    }
}
