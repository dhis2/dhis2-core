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
package org.hisp.dhis.analytics.common;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.EventOutputType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.FILTERS;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.EventDataQueryRequest.ExtendedEventDataQueryRequestBuilder.DIMENSION_OR_SEPARATOR;
import static org.hisp.dhis.common.IdScheme.UID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.dimension.StringUid;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * Component responsible for mapping the input request parameters into the
 * actual queryable objects. This component requires a few different services so
 * the queryable objects can be built as needed for further usage at DB query
 * time.
 */
@Component
@RequiredArgsConstructor
public class CommonQueryRequestMapper
{

    private final I18nManager i18nManager;

    private final DataQueryService dataQueryService;

    private final EventDataQueryService eventDataQueryService;

    private final ProgramService programService;

    private final DimensionIdentifierConverter dimensionIdentifierConverter;

    public CommonParams map( CommonQueryRequest request )
    {
        List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits( null, request.getUserOrgUnit() );

        List<Program> programs = getPrograms( request );

        return CommonParams.builder()
            .programs( programs )
            .pagingAndSortingParams( AnalyticsPagingParams.builder()
                .countRequested( request.isTotalPages() )
                .requestPaged( request.isPaging() )
                .page( request.getPage() )
                .pageSize( request.getPageSize() )
                .build() )
            .orderParams( getSortingParams( request, programs, userOrgUnits ) )
            .dimensionIdentifiers( retrieveDimensionParams( request, programs, userOrgUnits ) )
            .build();
    }

    private List<AnalyticsSortingParams> getSortingParams( CommonQueryRequest request, List<Program> programs,
        List<OrganisationUnit> userOrgUnits )
    {
        return DimensionParamType.SORTING.getUidsGetter().apply( request )
            .stream()
            .map( dimId -> toSortParam( dimId, request, programs, userOrgUnits ) )
            .collect( toList() );
    }

    private AnalyticsSortingParams toSortParam( String sortParam, CommonQueryRequest request, List<Program> programs,
        List<OrganisationUnit> userOrgUnits )
    {
        String[] parts = sortParam.split( ":" );
        return AnalyticsSortingParams.builder()
            .sortDirection( SortDirection.of( parts[1] ) )
            .orderBy( toDimensionIdentifier( parts[0], DimensionParamType.SORTING, request, programs, userOrgUnits ) )
            .build();
    }

    private List<Program> getPrograms( CommonQueryRequest queryRequest )
    {
        List<Program> programs = ImmutableList.copyOf( programService.getPrograms( queryRequest.getProgram() ) );
        boolean programsCouldNotBeRetrieved = programs.size() != queryRequest.getProgram().size();

        if ( programsCouldNotBeRetrieved )
        {
            List<String> foundProgramUids = programs.stream()
                .map( Program::getUid )
                .collect( toList() );

            List<String> missingProgramUids = Optional.of( queryRequest )
                .map( CommonQueryRequest::getProgram )
                .orElse( Collections.emptySet() ).stream()
                .filter( uidFromRequest -> !foundProgramUids.contains( uidFromRequest ) )
                .collect( toList() );

            throw new IllegalArgumentException( "The following programs couldn't be found: " + missingProgramUids );
        }

        return programs;
    }

    private List<List<DimensionIdentifier<Program, ProgramStage, DimensionParam>>> retrieveDimensionParams(
        CommonQueryRequest request, List<Program> programs, List<OrganisationUnit> userOrgUnits )
    {
        List<List<DimensionIdentifier<Program, ProgramStage, DimensionParam>>> dimensionParams = new ArrayList<>();

        Stream.of( DIMENSIONS, FILTERS )
            .forEach( dimensionParamType -> {
                // A Collection of dimensions or filters coming from the request
                Collection<String> dimensionsOrFilter = dimensionParamType.getUidsGetter().apply( request );
                dimensionParams.addAll(
                    ImmutableList.copyOf( dimensionsOrFilter.stream()
                        .map( this::splitOnOrIfNecessary )
                        .map( dof -> toDimensionIdentifier( dof, dimensionParamType, request, programs, userOrgUnits ) )
                        .collect( toList() ) ) );
            } );

        return ImmutableList.copyOf( dimensionParams );
    }

    private List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> toDimensionIdentifier(
        Collection<String> dimensions, DimensionParamType dimensionParamType, CommonQueryRequest request,
        List<Program> programs, List<OrganisationUnit> userOrgUnits )
    {
        return dimensions.stream()
            .map( dimensionAsString -> toDimensionIdentifier( dimensionAsString, dimensionParamType, request, programs,
                userOrgUnits ) )
            .collect( toList() );
    }

    private List<String> splitOnOrIfNecessary( String dimensionAsString )
    {
        return Arrays.stream( dimensionAsString.split( DIMENSION_OR_SEPARATOR ) )
            .collect( toList() );
    }

    /**
     * Return a collection of DimensionParams built from request
     * dimension/filter parameter
     */
    private DimensionIdentifier<Program, ProgramStage, DimensionParam> toDimensionIdentifier( String dimensionOrFilter,
        DimensionParamType dimensionParamType, CommonQueryRequest request, List<Program> programs,
        List<OrganisationUnit> userOrgUnits )
    {
        String dimensionId = getDimensionFromParam( dimensionOrFilter );

        // We first parse the dimensionId into <Program, ProgramStage, String>
        // to be able to operate on the string version (uid) of the dimension.
        DimensionIdentifier<Program, ProgramStage, StringUid> dimensionIdentifier = dimensionIdentifierConverter
            .fromString( programs, dimensionId );
        List<String> items = getDimensionItemsFromParam( dimensionOrFilter );

        // first we check if it's a static dimension
        if ( DimensionParam.isStaticDimensionIdentifier( dimensionId ) )
        {
            return DimensionIdentifier.of(
                dimensionIdentifier.getProgram(),
                dimensionIdentifier.getProgramStage(),
                DimensionParam.ofObject( dimensionId, dimensionParamType, items ) );
        }

        // then we check if it's a DimensionalObject
        DimensionalObject dimensionalObject = dataQueryService.getDimension(
            dimensionIdentifier.getDimension().getUid(),
            items, request.getRelativePeriodDate(), userOrgUnits, i18nManager.getI18nFormat(), true, UID );

        if ( Objects.nonNull( dimensionalObject ) )
        {
            DimensionParam dimensionParam = DimensionParam.ofObject( dimensionalObject, dimensionParamType, items );
            return DimensionIdentifier.of(
                dimensionIdentifier.getProgram(),
                dimensionIdentifier.getProgramStage(),
                dimensionParam );
        }

        // then it should be a queryItem: queryItems needs to be prefixed by
        // {programUid}.{programStageUid}
        if ( dimensionIdentifier.hasProgram() && dimensionIdentifier.hasProgramStage() )
        {
            // The fully qualified dimension identification is required here
            DimensionParam dimensionParam = DimensionParam.ofObject(
                eventDataQueryService.getQueryItem( dimensionIdentifier.getDimension().getUid(),
                    dimensionIdentifier.getProgram().getElement(), TRACKED_ENTITY_INSTANCE ),
                dimensionParamType, items );

            return DimensionIdentifier.of(
                dimensionIdentifier.getProgram(),
                dimensionIdentifier.getProgramStage(),
                dimensionParam );
        }
        throw new IllegalArgumentException( dimensionId + " is not a fully qualified dimension" );
    }

}
