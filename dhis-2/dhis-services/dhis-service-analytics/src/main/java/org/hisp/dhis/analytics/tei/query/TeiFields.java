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
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.ValueType.REFERENCE;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * This class provides methods responsible for extracting collections from
 * different objects types, like {@link TrackedEntityAttribute},
 * {@link ProgramTrackedEntityAttribute}, {@link GridHeader} and {@link Field}.
 */
public class TeiFields
{
    private interface HeaderProvider
    {
        String getAlias();

        String getFullName();

        ValueType getType();
    }

    public static boolean isStaticField( String field )
    {
        return stream( Static.values() ).anyMatch( f -> f.getAlias().equals( field ) );
    }

    @Getter
    @RequiredArgsConstructor
    private enum Dynamic implements HeaderProvider
    {
        ENROLLMENTS( "(select json_agg(json_build_object('programUid', p_0.uid," +
            " 'programInstanceUid', pi_0.uid, 'enrollmentDate', pi_0.enrollmentdate," +
            " 'incidentDate', pi_0.incidentdate,'endDate', pi_0.enddate, 'events'," +
            " (select json_agg(json_build_object('programStageUid', ps_0.uid," +
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

    /**
     * Retrieves all object attributes from the given param encapsulating them
     * into a stream of {@link Field}.
     *
     * @param teiQueryParams the {@link TeiQueryParams}.
     * @return a {@link Stream} of {@link Field}.
     */
    public static Stream<Field> getDimensionFields( @Nonnull TeiQueryParams teiQueryParams )
    {
        Set<String> programAttributesUids = teiQueryParams.getCommonParams().getPrograms().stream()
            .map( Program::getProgramAttributes )
            .flatMap( List::stream )
            .map( ProgramTrackedEntityAttribute::getUid ).collect( toSet() );

        Stream<Field> programAttributes = teiQueryParams.getCommonParams().getPrograms().stream()
            .map( Program::getProgramAttributes )
            .flatMap( List::stream )
            .map( programAttr -> Field.of( TEI_ALIAS,
                () -> programAttr.getAttribute().getUid(),
                String.join( ".", programAttr.getProgram().getUid(), programAttr.getAttribute().getUid() ) ) );

        Stream<Field> trackedEntityAttributesFromType = getTrackedEntityAttributes(
            teiQueryParams.getTrackedEntityType() )
                .filter( programTrackedEntityAttribute -> programAttributesUids
                    .contains( programTrackedEntityAttribute.getUid() ) )
                .map( BaseIdentifiableObject::getUid )
                .map( attr -> Field.of( TEI_ALIAS, () -> attr, attr ) );

        // TET and program attribute uids.
        return Stream.concat( trackedEntityAttributesFromType, programAttributes );
    }

    /**
     * Extracts a stream of {@link TrackedEntityAttribute} found in the given
     * list of {@link Program}.
     *
     * @param programs the list of {@link Program}.
     * @return a {@link Stream} of {@link TrackedEntityAttribute}.
     */
    public static Stream<TrackedEntityAttribute> getProgramAttributes( @Nonnull List<Program> programs )
    {
        // Attributes from Programs.
        return programs.stream()
            .map( Program::getProgramAttributes )
            .flatMap( List::stream )
            .map( ProgramTrackedEntityAttribute::getAttribute );
    }

    /**
     * Extracts a stream of {@link TrackedEntityAttribute} from the given
     * {@link TrackedEntityType}.
     *
     * @param trackedEntityType the {@link TrackedEntityType}.
     * @return a {@link Stream} of {@link TrackedEntityAttribute}, or empty.
     */
    public static Stream<TrackedEntityAttribute> getTrackedEntityAttributes(
        @CheckForNull TrackedEntityType trackedEntityType )
    {
        if ( trackedEntityType != null )
        {
            return trackedEntityType.getTrackedEntityAttributes().stream();
        }

        return Stream.empty();
    }

    /**
     * Retrieves only static fields.
     *
     * @return the {@link Stream} of {@link Field}.
     */
    public static Stream<Field> getStaticFields()
    {
        return Stream.of( Static.values() ).map( v -> v.alias ).map( a -> Field.of( TEI_ALIAS, () -> a, a ) );
    }

    /**
     * Retrieves only dynamic fields.
     *
     * @return the {@link Stream} of {@link Field}.
     */
    private static Stream<Field> getDynamicFields()
    {
        return Stream.of( Dynamic.values() )
            .map( dynamic -> Field.ofUnquoted( EMPTY, () -> dynamic.query, dynamic.alias ) );
    }

    /**
     * Retrieves all static plus dynamic fields.
     *
     * @return the {@link Stream} of {@link Field}.
     */
    public static Stream<Field> getStaticAndDynamicFields()
    {
        return Stream.concat( getStaticFields(), getDynamicFields() );
    }

    /**
     * Returns a collection of all possible headers for the given
     * {@link TeiQueryParams}. It includes headers extracted for static and
     * dynamic dimensions that matches the given list of {@link Field}.
     *
     * The static headers are also delivered on the top of the collection. The
     * dynamic comes after the static ones.
     *
     * @param teiQueryParams the {@link TeiQueryParams}.
     * @param fields the list of {@link Field}.
     * @return a {@link Set} of {@link GridHeader}.
     */
    public static Set<GridHeader> getGridHeaders( @Nonnull TeiQueryParams teiQueryParams, @Nonnull List<Field> fields )
    {
        Map<String, GridHeader> headersMap = new HashMap<>();

        // Adding static and dynamic headers.
        Stream.concat( stream( Static.values() ), stream( Dynamic.values() ) )
            .forEach( f -> headersMap.put( f.getAlias(),
                new GridHeader( f.getAlias(), f.getFullName(), f.getType(), false, true ) ) );

        getDimensionFields( teiQueryParams )
            .map( field -> findDimensionParamForField( field,
                teiQueryParams.getCommonParams().getDimensionIdentifiers() ) )
            .map( dimensionParam -> getHeaderForDimensionParam( dimensionParam, teiQueryParams.getCommonParams() ) )
            .forEach( g -> headersMap.put( g.getName(), g ) );

        return reorder( headersMap, fields );
    }

    /**
     * Based on the given map of {@link GridHeader}, it will return a set of
     * headers, reordering the headers respecting the given fields ordering.
     * Only elements inside the given map are returned. The rest is ignored.
     *
     * @param headersMap the map of {@link GridHeader}.
     * @param fields the list of {@link Field} to be respected.
     * @return the reordered set of {@link GridHeader}.
     */
    private static Set<GridHeader> reorder( Map<String, GridHeader> headersMap, List<Field> fields )
    {
        Set<GridHeader> headers = new LinkedHashSet<>();

        fields.forEach( field -> {
            if ( headersMap.containsKey( field.getDimensionIdentifier() ) )
            {
                headers.add( headersMap.get( field.getDimensionIdentifier() ) );
            }
        } );

        return headers;
    }

    /**
     * Based on the given item this method returns the correct UID based on
     * internal rules.
     *
     * @param item the current QueryItem.
     * @return the respective item's uid.
     */
    private static String getItemUid( @Nonnull QueryItem item )
    {
        String uid = item.getItemId();

        if ( item.hasProgramStage() )
        {
            uid = joinWith( ".", item.getProgramStage().getUid(), uid );
        }

        return uid;
    }

    /**
     * Creates a {@link GridHeader} for the given {@link DimensionParam} based
     * on the given {@link CommonParams}. The last is needed because of
     * particular cases where we need a custom version of the header.
     *
     * @param dimensionParam the {@link DimensionParam}.
     * @param commonParams the {@link CommonParams}.
     * @return the respective {@link GridHeader}.
     */
    private static GridHeader getHeaderForDimensionParam( @Nonnull DimensionParam dimensionParam,
        @Nonnull CommonParams commonParams )
    {
        QueryItem item = dimensionParam.getQueryItem();

        if ( item != null )
        {
            return getCustomGridHeaderForItem( item, commonParams );
        }
        else
        {
            String uid = dimensionParam.getUid();
            String name = dimensionParam.getName();
            ValueType valueType = dimensionParam.getValueType();

            return new GridHeader( uid, name, valueType, false, true );
        }
    }

    /**
     * Depending on the {@link QueryItem} we need to return specific headers.
     * This method will evaluate the given query item and take care of the
     * particulars cases where we need a custom {@link GridHeader} for the
     * respective {@link QueryItem}.
     *
     * @param queryItem the {@link QueryItem}.
     * @param commonParams the {@link CommonParams}.
     * @return the correct {@link GridHeader} version.
     */
    private static GridHeader getCustomGridHeaderForItem( @Nonnull QueryItem queryItem,
        @Nonnull CommonParams commonParams )
    {
        /*
         * If the request contains a query item of value type ORGANISATION_UNIT
         * and the item UID is linked to coordinates (coordinateField), then
         * create a header of ValueType COORDINATE.
         */
        if ( queryItem.getValueType() == ORGANISATION_UNIT
            && commonParams.getCoordinateFields().stream().anyMatch( f -> f.equals( queryItem.getItem().getUid() ) ) )
        {
            return new GridHeader( queryItem.getItem().getUid(),
                queryItem.getItem().getDisplayProperty( commonParams.getDisplayProperty() ), COORDINATE, false, true,
                queryItem.getOptionSet(), queryItem.getLegendSet() );
        }
        else if ( queryItem.hasNonDefaultRepeatableProgramStageOffset() )
        {
            String column = queryItem.getItem().getDisplayProperty( commonParams.getDisplayProperty() );
            RepeatableStageParams repeatableStageParams = queryItem.getRepeatableStageParams();
            String dimName = repeatableStageParams.getDimension();
            ValueType valueType = repeatableStageParams.simpleStageValueExpected()
                ? queryItem.getValueType()
                : REFERENCE;

            return new GridHeader( dimName, column, valueType, false, true, queryItem.getOptionSet(),
                queryItem.getLegendSet(), queryItem.getProgramStage().getUid(), repeatableStageParams );
        }
        else
        {
            String itemUid = getItemUid( queryItem );
            String column = queryItem.getItem().getDisplayProperty( commonParams.getDisplayProperty() );

            return new GridHeader( itemUid, column, queryItem.getValueType(), false, true, queryItem.getOptionSet(),
                queryItem.getLegendSet() );
        }
    }

    /**
     * Finds the respective {@link DimensionParam}, from the given list of
     * {@link DimensionIdentifier}, that is associated with the given
     * {@link Field}.
     *
     * @param field the {@link Field}.
     * @param dimensionIdentifiers the list of {@link DimensionIdentifier}.
     * @return the correct {@link DimensionParam}
     * @throws IllegalStateException if nothing is found.
     */
    private static DimensionParam findDimensionParamForField( @Nonnull Field field,
        @Nonnull List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers )
    {
        return dimensionIdentifiers.stream()
            .filter( di -> di.toString().equals( field.getDimensionIdentifier() ) )
            .map( DimensionIdentifier::getDimension )
            .findFirst()
            .orElseThrow( IllegalStateException::new );
    }
}
